//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;

public class XMLParser {
    public static void main(String[] args) throws Exception {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(new File("feedXML.xml"), new MyDefaultHandler());
    }
}

class MyDefaultHandler extends DefaultHandler {
    @Override
    public void startDocument() throws SAXException {
        System.out.println("----XML Document Start-------");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.print("<" + qName);

        for(int i = 0; i < attributes.getLength(); i++) {
            System.out.print(" " + attributes.getQName(i) + "=" + attributes.getValue(i));
        }
        System.out.print(">");
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        System.out.print(new String(ch, start, length));
    }
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        System.out.print("</" + qName + ">");
    }

    @Override
    public void endDocument() throws SAXException {
        System.out.println("\n----XML Document parsing finished-----");
    }
}
