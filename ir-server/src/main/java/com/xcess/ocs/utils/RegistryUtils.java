package com.xcess.ocs.utils;

import com.xcess.ocs.dto.RequestParameters;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RegistryUtils {

    public static List<RequestParameters> convertParameterStringToJson(String input) {
        List<RequestParameters> result = new ArrayList<>();

        if (input == null || input.isEmpty()) {
            return result;
        }

        if (input.endsWith(",")) {
            input = input.substring(0, input.length() - 1);
        }

        String[] pairs = input.split(",");

        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                String rawField = parts[0];
                String value = parts[1];
                String serviceType = null;
                String field = rawField;
                
                if (rawField.contains(":")) {
                    String[] typeAndField = rawField.split(":", 2);
                    serviceType = typeAndField[0];
                    field = typeAndField[1];
                }
                
                RequestParameters param = new RequestParameters(serviceType, field, value);
                result.add(param);
            }
        }

        return result;
    }
}
