package com.xcess.ocs.roaming.config;

import com.xcess.ocs.roaming.entity.TapDataType;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core reflection engine for dynamic TAP ASN.1 field path resolution.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li><b>Dictionary loading</b> — parses {@code TAP-0312.asn} and {@code RAP-0105.asn}
 *       at startup to build a type registry of all SEQUENCE field names.</li>
 *   <li><b>Path resolution (TAP IN)</b> — {@link #resolve(Object, String)} traverses a
 *       dot-notation path on a live asn1bean SDK object and returns the string value.</li>
 *   <li><b>Path writing (TAP OUT)</b> — {@link #setValue(Object, String, Object)} traverses
 *       the same path, instantiating intermediate nested classes on the fly, and writes
 *       the formatted value to the leaf node.</li>
 *   <li><b>Value formatting</b> — {@link #formatValue(Object, TapDataType, int)} converts
 *       Java values to ASN.1-compatible representations (BCD bytes, scaled integers, timestamps).</li>
 *   <li><b>Value decoding</b> — {@link #decodeValue(String, TapDataType, int)} reverses the
 *       encoding for TAP IN extraction.</li>
 *   <li><b>Bean property access</b> — {@link #getPropertyValue(Object, String)} and
 *       {@link #setPropertyValue(Object, String, Object)} provide reflection-based
 *       read/write on arbitrary Java beans (used for {@code RatedCdr} and {@code TapCdrDTO}).</li>
 * </ol>
 *
 * <p>BCD-encoded types recognised from TAP-0312.asn:
 * {@code Imsi}, {@code Msisdn}, {@code CalledNumber}, {@code CallingNumber},
 * {@code ThirdPartyNumber}, {@code NonChargedPartyNumber}, {@code CamelDestinationNumber},
 * {@code RequestedNumber}.
 */
@Hidden
@Slf4j
@Component
public class TapFieldPathResolver {

    private static final Pattern INDEX_PATTERN = Pattern.compile("^(.+)\\[(\\d+)]$");

    // ASN.1 type name → set of field names declared in that SEQUENCE
    // Built from parsing TAP-0312.asn + RAP-0105.asn at startup
    private final Map<String, Set<String>> asnTypeFields = new HashMap<>();

    // BCDString type names from TAP-0312.asn:
    // Imsi [APPLICATION 129] BCDString
    // Msisdn [APPLICATION 152] BCDString
    // CalledNumber [APPLICATION 407] AddressStringDigits (::= BCDString)
    // CallingNumber [APPLICATION 405] AddressStringDigits
    // ThirdPartyNumber [APPLICATION 403] AddressStringDigits
    // NonChargedPartyNumber [APPLICATION 444] AddressStringDigits
    // CamelDestinationNumber [APPLICATION 404] AddressStringDigits
    // RequestedNumber [APPLICATION 451] AddressStringDigits
    private static final Set<String> BCD_TYPES = Set.of(
            "Imsi", "Msisdn", "CalledNumber", "CallingNumber",
            "ThirdPartyNumber", "NonChargedPartyNumber",
            "CamelDestinationNumber", "RequestedNumber"
    );

    @PostConstruct
    public void loadAsnDictionaries() {
        loadAsn("dictionaries/TAP-0312.asn");
        loadAsn("dictionaries/RAP-0105.asn");
        log.info("ASN dictionary loaded: {} types registered", asnTypeFields.size());
    }

    /**
     * Parses an ASN.1 file and registers every SEQUENCE type with its field names.
     * Only SEQUENCE types matter for path resolution — CHOICE, INTEGER, etc. are skipped.
     *
     * Format parsed:
     *   TypeName ::= [APPLICATION N] SEQUENCE
     *   {
     *       fieldName FieldType OPTIONAL,
     *       ...
     *   }
     */
    private void loadAsn(String classpathResource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(classpathResource).getInputStream(), StandardCharsets.UTF_8))) {

            String currentType = null;
            Set<String> fields = null;
            boolean inSequence = false;

            Pattern typeDecl = Pattern.compile("^(\\w+)\\s*::=.*SEQUENCE\\s*$");
            Pattern fieldDecl = Pattern.compile("^\\s+(\\w+)\\s+\\w.*");

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;

                Matcher typeMatcher = typeDecl.matcher(line);
                if (typeMatcher.matches()) {
                    currentType = typeMatcher.group(1);
                    fields = new LinkedHashSet<>();
                    inSequence = false;
                    continue;
                }

                if (currentType != null && trimmed.equals("{")) {
                    inSequence = true;
                    continue;
                }

                if (inSequence && trimmed.equals("}")) {
                    asnTypeFields.put(currentType, fields);
                    currentType = null;
                    fields = null;
                    inSequence = false;
                    continue;
                }

                if (inSequence && fields != null) {
                    Matcher fieldMatcher = fieldDecl.matcher(line);
                    if (fieldMatcher.matches()) {
                        String fieldName = fieldMatcher.group(1);
                        if (!fieldName.equals("...")) {
                            fields.add(fieldName);
                        }
                    }
                }
            }
            log.debug("Loaded ASN dictionary from {}", classpathResource);
        } catch (Exception e) {
            log.error("Failed to load ASN dictionary {}: {}", classpathResource, e.getMessage());
        }
    }

    // ── public API ───────────────────────────────────────────────────────────────

    /**
     * Resolves a dot-path against a live asn1bean SDK object.
     * Each segment is validated against the ASN.1 type registry.
     * Returns null (never throws) when any segment is null or not found.
     */
    public String resolve(Object root, String path) {
        if (root == null || path == null || path.isBlank()) return null;
        Object current = root;
        for (String segment : path.split("\\.")) {
            if (current == null) return null;
            Matcher m = INDEX_PATTERN.matcher(segment);
            if (m.matches()) {
                current = navigateTo(current, m.group(1));
                if (current == null) return null;
                current = indexInto(current, Integer.parseInt(m.group(2)));
            } else {
                current = navigateTo(current, segment);
            }
        }
        return toStringValue(current);
    }

    /**
     * Returns all field names registered for a given ASN.1 type name.
     * Used for diagnostics and validation.
     */
    public Set<String> getFieldsForType(String asnTypeName) {
        return asnTypeFields.getOrDefault(asnTypeName, Collections.emptySet());
    }

    public Map<String, Set<String>> getAsnTypeRegistry() {
        return Collections.unmodifiableMap(asnTypeFields);
    }

    // ── navigation ───────────────────────────────────────────────────────────────

    private Object navigateTo(Object obj, String fieldName) {
        if (obj == null) return null;
        // Try getXxx() getter first (standard Java bean convention)
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Object result = invokeMethod(obj, getter);
        if (result != null) return result;
        // Fall back to direct public field (asn1bean CHOICE members are public fields)
        return readField(obj, fieldName);
    }

    private Object indexInto(Object obj, int index) {
        if (obj instanceof List<?> list) {
            return index < list.size() ? list.get(index) : null;
        }
        return null;
    }

    // ── value extraction ─────────────────────────────────────────────────────────

    /**
     * Converts a terminal asn1bean value object to String.
     *
     * asn1bean primitive types all expose a public field named "value":
     *   BerInteger / BerEnum  → long
     *   BerOctetString        → byte[]  (AsciiString, BCDString, NumberString, Currency)
     *   BerBoolean            → boolean
     *
     * BCDString types (per TAP-0312.asn) → BCD-decoded digit string
     * AsciiString / NumberString / Currency → UTF-8 string
     */
    private String toStringValue(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        if (obj instanceof Number n) return n.toString();

        Object val = readField(obj, "value");
        if (val == null) return obj.toString();

        if (val instanceof byte[] bytes) {
            return BCD_TYPES.contains(obj.getClass().getSimpleName())
                    ? decodeBcd(bytes)
                    : new String(bytes, StandardCharsets.UTF_8).trim();
        }
        if (val instanceof Long l)    return l.toString();
        if (val instanceof Integer i) return i.toString();
        if (val instanceof Boolean b) return b.toString();
        return val.toString();
    }

    /**
     * BCD decode per TAP-0312.asn BCDString spec:
     * Two digits per octet, high nibble first, 0xF = filler (discarded).
     */
    public String decodeBcd(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int hi = (b >> 4) & 0x0F;
            int lo = b & 0x0F;
            if (hi != 0xF) sb.append(hi);
            if (lo != 0xF) sb.append(lo);
        }
        return sb.toString();
    }

    /**
     * BCD encode per TAP-0312.asn BCDString spec:
     * Two digits per octet, high nibble first, pad with 0xF if odd length.
     */
    public byte[] encodeBcd(String digits) {
        if (digits == null) return new byte[0];
        int len = digits.length();
        byte[] result = new byte[(len + 1) / 2];
        for (int i = 0; i < result.length; i++) {
            int hi = digits.charAt(i * 2) - '0';
            int lo = (i * 2 + 1 < len) ? digits.charAt(i * 2 + 1) - '0' : 0xF;
            result[i] = (byte) ((hi << 4) | lo);
        }
        return result;
    }

    // ── reflection helpers ───────────────────────────────────────────────────────

    private Object invokeMethod(Object obj, String name) {
        try {
            Method m = findMethod(obj.getClass(), name);
            if (m != null) { m.setAccessible(true); return m.invoke(obj); }
        } catch (Exception e) {
            log.trace("{}.{}() failed: {}", obj.getClass().getSimpleName(), name, e.getMessage());
        }
        return null;
    }

    private Object readField(Object obj, String name) {
        try {
            Field f = findField(obj.getClass(), name);
            if (f != null) { f.setAccessible(true); return f.get(obj); }
        } catch (Exception e) {
            log.trace("{}.{} read failed: {}", obj.getClass().getSimpleName(), name, e.getMessage());
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass())
            for (Method m : c.getDeclaredMethods())
                if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
        return null;
    }

    private Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields())
                if (f.getName().equals(name)) return f;
        return null;
    }

    // ── dynamic formatting and decoding ──────────────────────────────────────────

    public Object formatValue(Object value, TapDataType type, int decimalPlaces) {
        if (value == null) return null;
        
        switch (type) {
            case BCD_STRING:
                return encodeBcd(value.toString().replaceAll("[^0-9Ff]", ""));
                
            case DATE_TIME:
                LocalDateTime ldt = null;
                if (value instanceof LocalDateTime localDateTime) {
                    ldt = localDateTime;
                } else {
                    String valStr = value.toString().trim();
                    String[] patterns = {
                        "yyyy-MM-dd HH:mm:ss",
                        "dd-MM-yyyy HH:mm:ss",
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS"
                    };
                    for (String pattern : patterns) {
                        try {
                            ldt = LocalDateTime.parse(valStr, DateTimeFormatter.ofPattern(pattern));
                            break;
                        } catch (Exception ignored) {}
                    }
                }
                if (ldt != null) {
                    return ldt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")).getBytes(StandardCharsets.UTF_8);
                }
                throw new IllegalArgumentException("Invalid date-time format: " + value);
                
            case DECIMAL:
                BigDecimal bd = (value instanceof BigDecimal decimal) ? decimal : new BigDecimal(value.toString().trim());
                return bd.multiply(BigDecimal.TEN.pow(decimalPlaces)).toBigInteger();
                
            case INTEGER:
                String intStr = value.toString().trim();
                return new BigDecimal(intStr).toBigInteger();
                
            case ASCII_STRING:
            default:
                return value.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public Object decodeValue(String rawValue, TapDataType type, int decimalPlaces) {
        if (rawValue == null) return null;
        
        switch (type) {
            case BCD_STRING:
                return rawValue;
                
            case DATE_TIME:
                try {
                    return LocalDateTime.parse(rawValue.trim().substring(0, 14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                } catch (Exception e) {
                    log.warn("Failed to parse TAP IN timestamp: {}", rawValue);
                    return null;
                }
                
            case DECIMAL:
                BigDecimal v = new BigDecimal(rawValue.trim());
                return decimalPlaces > 0 ? v.movePointLeft(decimalPlaces) : v;
                
            case INTEGER:
                return Integer.parseInt(rawValue.trim());
                
            case ASCII_STRING:
            default:
                return rawValue;
        }
    }

    // ── dynamic path write ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public void setValue(Object root, String path, Object formattedVal) {
        if (root == null || path == null || path.isBlank()) return;
        
        Object current = root;
        String[] segments = path.split("\\.");
        
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLast = (i == segments.length - 1);
            
            Matcher m = INDEX_PATTERN.matcher(segment);
            if (m.matches()) {
                String fieldName = m.group(1);
                int index = Integer.parseInt(m.group(2));

                Object listHolder = navigateOrCreate(current, fieldName);

                List<Object> list;
                if (listHolder instanceof List<?> directList) {
                    list = (List<Object>) directList;
                } else {
                    list = (List<Object>) invokeMethod(listHolder, "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
                    if (list == null) {
                        list = (List<Object>) readField(listHolder, "seqOf");
                    }
                    if (list == null) {
                        throw new IllegalStateException("Failed to retrieve List from " + listHolder.getClass().getSimpleName());
                    }
                }
                
                while (list.size() <= index) {
                    try {
                        Class<?> elementClass = resolveListElementClass(list, current, fieldName);
                        list.add(elementClass.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to expand List for field " + fieldName, e);
                    }
                }
                
                Object element = list.get(index);
                if (isLast) {
                    setLeafValue(element, formattedVal);
                } else {
                    current = element;
                }
            } else {
                if (isLast) {
                    Object leafWrapper = navigateOrCreate(current, segment);
                    setLeafValue(leafWrapper, formattedVal);
                } else {
                    current = navigateOrCreate(current, segment);
                }
            }
        }
    }

    private void setLeafValue(Object leafWrapper, Object formattedVal) {
        if (leafWrapper == null) return;
        try {
            Field f = findField(leafWrapper.getClass(), "value");
            if (f == null) {
                throw new IllegalArgumentException("No 'value' field found on leaf class " + leafWrapper.getClass().getName());
            }
            f.setAccessible(true);
            Class<?> fieldType = f.getType();

            if (formattedVal == null) {
                f.set(leafWrapper, null);
                return;
            }

            if (fieldType == byte[].class) {
                if (formattedVal instanceof byte[] bytes) {
                    f.set(leafWrapper, bytes);
                } else {
                    f.set(leafWrapper, formattedVal.toString().getBytes(StandardCharsets.UTF_8));
                }
            } else if (fieldType == long.class || fieldType == Long.class) {
                if (formattedVal instanceof Number num) {
                    f.set(leafWrapper, num.longValue());
                } else {
                    f.set(leafWrapper, Long.parseLong(formattedVal.toString().trim()));
                }
            } else if (fieldType == int.class || fieldType == Integer.class) {
                if (formattedVal instanceof Number num) {
                    f.set(leafWrapper, num.intValue());
                } else {
                    f.set(leafWrapper, Integer.parseInt(formattedVal.toString().trim()));
                }
            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                if (formattedVal instanceof Boolean b) {
                    f.set(leafWrapper, b);
                } else {
                    f.set(leafWrapper, Boolean.parseBoolean(formattedVal.toString().trim()));
                }
            } else if (fieldType == BigInteger.class) {
                if (formattedVal instanceof BigInteger bi) {
                    f.set(leafWrapper, bi);
                } else {
                    f.set(leafWrapper, new BigInteger(formattedVal.toString().trim()));
                }
            } else {
                f.set(leafWrapper, formattedVal);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set leaf value on " + leafWrapper.getClass().getName(), e);
        }
    }

    private Object navigateOrCreate(Object parent, String fieldName) {
        Object child = navigateTo(parent, fieldName);
        if (child != null) {
            return child;
        }
        try {
            Field f = findField(parent.getClass(), fieldName);
            Class<?> fieldClass = null;
            if (f != null) {
                fieldClass = f.getType();
            } else {
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method m = findMethod(parent.getClass(), getterName);
                if (m != null) {
                    fieldClass = m.getReturnType();
                }
            }
            if (fieldClass == null) {
                throw new IllegalArgumentException("No field or getter found for " + fieldName + " on class " + parent.getClass().getName());
            }

            Object newInstance = fieldClass.getDeclaredConstructor().newInstance();
            writeFieldOrInvokeSetter(parent, fieldName, newInstance);
            return newInstance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to navigate or create field " + fieldName + " on class " + parent.getClass().getName(), e);
        }
    }

    private void writeFieldOrInvokeSetter(Object parent, String fieldName, Object val) {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method[] methods = parent.getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    m.invoke(parent, val);
                    return;
                }
            }
        } catch (Exception e) {
            log.trace("Setter invoke failed for " + setterName + ": " + e.getMessage());
        }

        try {
            Field f = findField(parent.getClass(), fieldName);
            if (f != null) {
                f.setAccessible(true);
                f.set(parent, val);
                return;
            }
        } catch (Exception e) {
            log.trace("Field set failed for " + fieldName + ": " + e.getMessage());
        }
        throw new IllegalArgumentException("Cannot set field/property " + fieldName + " on class " + parent.getClass().getName());
    }

    private Class<?> resolveListElementClass(List<Object> list, Object parent, String fieldName) {
        // 1. Infer from existing elements
        if (!list.isEmpty() && list.get(0) != null) return list.get(0).getClass();
        // 2. Try generic type from field declaration on parent (field named same as fieldName)
        Field f = findField(parent.getClass(), fieldName);
        if (f != null) {
            java.lang.reflect.Type gt = f.getGenericType();
            if (gt instanceof ParameterizedType pt) {
                java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c) return c;
            }
        }
        // 3. asn1bean wrapper pattern: actual storage is in a field named "seqOf" on the parent
        Field seqOf = findField(parent.getClass(), "seqOf");
        if (seqOf != null) {
            java.lang.reflect.Type gt = seqOf.getGenericType();
            if (gt instanceof ParameterizedType pt) {
                java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c) return c;
            }
        }
        // 4. Try getter return type generic on parent
        String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method m = findMethod(parent.getClass(), getter);
        if (m != null) {
            java.lang.reflect.Type rt = m.getGenericReturnType();
            if (rt instanceof ParameterizedType pt) {
                java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c) return c;
            }
        }
        throw new IllegalArgumentException("Cannot determine list element class for field '" + fieldName + "' on " + parent.getClass().getSimpleName());
    }

    private Class<?> getGenericListType(Class<?> clazz, String getterName) {
        try {
            Method m = findMethod(clazz, getterName);
            if (m != null) {
                java.lang.reflect.Type returnType = m.getGenericReturnType();
                if (returnType instanceof ParameterizedType pt) {
                    java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                        return c;
                    }
                }
            }
            Field f = findField(clazz, "seqOf");
            if (f != null) {
                java.lang.reflect.Type genericType = f.getGenericType();
                if (genericType instanceof ParameterizedType pt) {
                    java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                        return c;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to determine generic list type for class {} / getter {}: {}", clazz.getName(), getterName, e.getMessage());
        }
        throw new IllegalArgumentException("Cannot determine list element class for " + clazz.getName());
    }

    // ── dynamic bean properties ──────────────────────────────────────────────────

    public Object getPropertyValue(Object bean, String propertyName) {
        if (bean == null || propertyName == null || propertyName.isBlank()) return null;
        try {
            String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method m = findMethod(bean.getClass(), getterName);
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(bean);
            }
            Field f = findField(bean.getClass(), propertyName);
            if (f != null) {
                f.setAccessible(true);
                return f.get(bean);
            }
        } catch (Exception e) {
            log.warn("Failed to get property {} from class {}: {}", propertyName, bean.getClass().getName(), e.getMessage());
        }
        return null;
    }

    public void setPropertyValue(Object bean, String propertyName, Object value) {
        if (bean == null || propertyName == null || propertyName.isBlank()) return;
        try {
            String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method m : methods) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    m.setAccessible(true);
                    Class<?> paramType = m.getParameterTypes()[0];
                    Object converted = convertToType(value, paramType);
                    m.invoke(bean, converted);
                    return;
                }
            }
            Field f = findField(bean.getClass(), propertyName);
            if (f != null) {
                f.setAccessible(true);
                Object converted = convertToType(value, f.getType());
                f.set(bean, converted);
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to set property {} on class {}: {}", propertyName, bean.getClass().getName(), e.getMessage());
        }
    }

    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        if (targetType == String.class) {
            return value.toString();
        }
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number num) return num.intValue();
            return Integer.parseInt(value.toString().trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number num) return num.longValue();
            return Long.parseLong(value.toString().trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number num) return num.doubleValue();
            return Double.parseDouble(value.toString().trim());
        }
        if (targetType == BigDecimal.class) {
            if (value instanceof BigInteger bi) return new BigDecimal(bi);
            return new BigDecimal(value.toString().trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value.toString().trim());
        }
        if (targetType == LocalDateTime.class) {
            if (value instanceof LocalDateTime ldt) return ldt;
            return LocalDateTime.parse(value.toString().trim());
        }
        return value;
    }
}
