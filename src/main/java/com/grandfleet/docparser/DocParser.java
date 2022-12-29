package com.grandfleet.docparser;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io;
import java.io.File;
import java.lang.Object;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class DocParser {
    private String filePath;
    private String type;
    private Map<String, String> parsedData;
    private ParsingMode mode;
    private String currHeader;
    private String xmlTagFoundLine;
    private Map<String, String> currDoc;

    public enum ParsingMode {
        HEADER,
        FILER,
        DOCUMENT
    }

    public DocParser(String filePath, String fileType) {
        this.filePath = filePath;
        this.type = "13F";
        this.parsedData = new HashMap<>();
        this.mode = ParsingMode.HEADER;
        this.currHeader = "";
        this.xmlTagFoundLine = null;
        this.currDoc = new HashMap<>();
    }

    private static String[] splitBySemicolon(String line) {
        if (!line.contains(":")) {
            return null;
        }
        String[] parts = line.split(":");
        return new String[] { parts[0], parts[1] };
    }

    private static String stripTag(String line) {
        if (!line.startsWith("<")) {
            return null;
        }
        try {
            return line.split(">")[1];
        } catch (IndexOutOfBoundsException e) {
            return line;
        }
    }

    private static String stripXmlNs(String line) {
        if (!line.contains("}")) {
            return null;
        }
        return line.split("}")[1];
    }
  private static void iterDocs(Document docTable) {
    NodeList infoTables = docTable.getElementsByTagName("infoTable");
    for (int i = 0; i < infoTables.getLength(); i++) {
        Element infoTable = (Element) infoTables.item(i);
        Map<String, String> docDict = new HashMap<>();
        NodeList children = infoTable.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Element child = (Element) children.item(j);
            String tag = stripXmlNs(child.getTagName());
            if (child.getChildNodes().getLength() == 0 && child.getTextContent() != null) {
                docDict.put(tag, child.getTextContent());
            }
            if (tag.equals("shrsOrPrnAmt")) {
                NodeList elems = child.getChildNodes();
                for (int k = 0; k < elems.getLength(); k++) {
                    Element elem = (Element) elems.item(k);
                    String newTag = stripXmlNs(elem.getTagName());
                    docDict.put(newTag, elem.getTextContent());
                }
            }
        }
        // do something with docDict
    }
  }
  
  public Map<String, String> parse() {
    try (Scanner scanner = new Scanner(new File(filePath))) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.startsWith("<")) {
                String cleanLine = stripTag(line);
                if (line.startsWith("<SEC-DOCUMENT>")) {
                    String[] parts = splitBySemicolon(cleanLine);
                    parsedData.put("document_name", parts[0]);
                    parsedData.put("date", parts[1]);
                } else if (line.startsWith("<XML>")) {
                    mode = ParsingMode.FILER;
                    xmlTagFoundLine = line;
                } else if (line.startsWith("<")) {
                    currHeader = cleanLine;
                    mode = ParsingMode.DOCUMENT;
                }
            } else if (line.startsWith("</")) {
                if (mode == ParsingMode.DOCUMENT) {
                    String xml = currDoc.get(xmlTagFoundLine);
                    Document docTable = createDocTable(xml);
                    iterDocs(docTable);
                    currDoc.clear();
                }
                mode = ParsingMode.HEADER;
            } else {
                if (mode == ParsingMode.FILER) {
                    parsedData.put(currHeader, line);
                } else if (mode == ParsingMode.DOCUMENT) {
                    currDoc.put(xmlTagFoundLine, line);
                }
            }
        }
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    }
    return parsedData;
  }

  private Document createDocTable(String xml) {
      Document docTable = null;
      try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          DocumentBuilder builder = factory.newDocumentBuilder();
          InputSource is = new InputSource(new StringReader(xml));
          docTable = builder.parse(is);
          docTable.getDocumentElement().normalize();
      } catch (Exception e) {
          e.printStackTrace();
      }
      return docTable;
  }
}
