package com.xcess.ocs.roaming.service;

import com.xcess.ocs.exception.XmlConversionException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import java.io.StringWriter;

public class RoamingTapOutXmlConverter {

    public static String convertToXml(RoamingTapOutInvoiceXmlDTO dto) {
        try {
            JAXBContext context = JAXBContext.newInstance(RoamingTapOutInvoiceXmlDTO.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(dto, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new XmlConversionException("Failed to convert TAP OUT invoice to XML: " + e.getMessage(), e);
        }
    }
}
