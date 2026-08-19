package com.xcess.ocs.dto.xml;

import com.xcess.ocs.dto.InvoiceDTO;
import com.xcess.ocs.exception.XmlConversionException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

public class XmlConverter {

    public static String convertToXml(InvoiceXmlDTO invoice) {
        try{
        JAXBContext context = JAXBContext.newInstance(InvoiceXmlDTO.class);
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter writer = new StringWriter();
        marshaller.marshal(invoice, writer);

        return writer.toString();
    } catch (Exception e) {
            throw new XmlConversionException("Failed to convert invoice to XML: " + e.getMessage(), e);
        }
    }
}
