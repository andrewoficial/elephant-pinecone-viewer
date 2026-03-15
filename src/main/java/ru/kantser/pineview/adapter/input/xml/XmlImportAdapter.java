package ru.kantser.pineview.adapter.input.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.kantser.pineview.domain.error.ImportException;
import ru.kantser.pineview.domain.model.ImportItem;
import ru.kantser.pineview.domain.model.ImportSource;
import ru.kantser.pineview.domain.port.ImportPort;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


public class XmlImportAdapter implements ImportPort {
    
    private static final Logger log = LoggerFactory.getLogger(XmlImportAdapter.class);
    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    
    public XmlImportAdapter() {
        // Защита от XXE-атак
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            log.warn("Could not disable DTD declarations", e);
        }
    }
    
    @Override
    public List<ImportItem> parse(ImportSource source) throws ImportException {
        try {
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(
                new InputStreamReader(source.getStream(), source.getEncoding())
            ));
            
            NodeList records = doc.getElementsByTagName("record");
            List<ImportItem> items = new ArrayList<>(records.getLength());
            
            for (int i = 0; i < records.getLength(); i++) {
                Element record = (Element) records.item(i);
                ImportItem item = parseRecord(record);
                
                if (item.isValid()) {
                    items.add(item);
                } else {
                    log.warn("Skipped invalid XML record at index {}: {}", i, item);
                }
            }
            
            log.info("Parsed {} items from XML: {}", items.size(), source.getFileName());
            return items;
            
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new ImportException("Failed to parse XML: " + source.getFileName(), e);
        }
    }
    
    @Override
    public Set<String> supportedExtensions() {
        return Set.of("xml");
    }
    
    private ImportItem parseRecord(Element record) {
        String id = getElementText(record, "id");
        String text = getElementText(record, "text");
        
        Map<String, Object> metadata = new HashMap<>();
        NodeList children = record.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                String tag = child.getTagName();
                if (!tag.equals("id") && !tag.equals("text")) {
                    metadata.put(tag, getElementText(record, tag));
                }
            }
        }
        
        return new ImportItem(id, text, metadata);
    }
    
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return (nodes.getLength() > 0) ? nodes.item(0).getTextContent() : null;
    }
}