package com.xcess.ocs.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.fop.apps.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;

@Slf4j
@Service
public class PdfGenerationService {

    @Value("${invoice.storage.base-path}")
    private String basePath;

    private final FopFactory fopFactory;
    private final TransformerFactory transformerFactory;

    public PdfGenerationService() throws Exception {
        this.fopFactory = FopFactory.newInstance(new File(".").toURI());
        this.transformerFactory = TransformerFactory.newInstance();
    }

    public PdfGenerationResult generatePdf(String xmlContent, Long invoiceId, LocalDate billingStart, String templatePath) {
        try {
            String relativePath = buildRelativePath(billingStart, invoiceId);
            String fullPath = Paths.get(basePath, relativePath).toString();
            
            Files.createDirectories(Paths.get(fullPath).getParent());
            
            try (OutputStream out = new FileOutputStream(fullPath);
                 ByteArrayOutputStream foOut = new ByteArrayOutputStream()) {
                
                FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
                Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

                Source xslt = resolveTemplateSource(templatePath);
                Transformer transformer = transformerFactory.newTransformer(xslt);
                
                Source src = new StreamSource(new StringReader(xmlContent));
                Result res = new SAXResult(fop.getDefaultHandler());
                
                transformer.transform(src, res);
                
                String checksum = calculateChecksum(fullPath);
                
                return PdfGenerationResult.success(relativePath, checksum);
            }
        } catch (Exception e) {
            log.error("PDF generation failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            return PdfGenerationResult.failure(e.getMessage());
        }
    }

    private String buildRelativePath(LocalDate billingStart, Long invoiceId) {
        return String.format("%d/%02d/INV-%d.pdf", 
            billingStart.getYear(), 
            billingStart.getMonthValue(), 
            invoiceId);
    }

    private String calculateChecksum(String filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static class PdfGenerationResult {
        public final boolean success;
        public final String filePath;
        public final String checksum;
        public final String errorReason;

        private PdfGenerationResult(boolean success, String filePath, String checksum, String errorReason) {
            this.success = success;
            this.filePath = filePath;
            this.checksum = checksum;
            this.errorReason = errorReason;
        }

        public static PdfGenerationResult success(String filePath, String checksum) {
            return new PdfGenerationResult(true, filePath, checksum, null);
        }

        public static PdfGenerationResult failure(String errorReason) {
            return new PdfGenerationResult(false, null, null, errorReason);
        }
    }

    /**
     * Generates PDF and returns it as byte array without saving to filesystem.
     * Useful for preview purposes where PDF should not be persisted.
     *
     * @param xmlContent    The XML content to transform
     * @param templatePath  Path to the XSLT template file
     * @return              byte array containing PDF content, or null if generation fails
     */
    public byte[] generatePdfAsBytes(String xmlContent, String templatePath) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            Source xslt = resolveTemplateSource(templatePath);
            Transformer transformer = transformerFactory.newTransformer(xslt);

            Source src = new StreamSource(new StringReader(xmlContent));
            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed: {}", e.getMessage(), e);
            return null;
        }
    }
    private Source resolveTemplateSource(String templatePath) throws IOException {
        Path path = Paths.get(templatePath);
        if (Files.exists(path)) {
            return new StreamSource(new FileInputStream(path.toFile()));
        }
        InputStream is = getClass().getResourceAsStream(templatePath);
        if (is == null) {
            throw new IOException("Template not found: " + templatePath);
        }
        return new StreamSource(is);
    }
}
