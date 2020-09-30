//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import java.io.*;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;

// todo XML entry
public class SaxXml {
    private static boolean debug = true;
//    private static boolean debug = false;
    private static XMLinfo xml = new XMLinfo();

    public void parsingXML(String inputName, String xmlName) throws IOException, TransformerConfigurationException {
        readFile(inputName);
        createXml(xmlName);
    }

    public void readFile(String inputName) throws TransformerConfigurationException, IOException {
        if (debug) System.out.println("Sax XML :: Reading Input File...");
        BufferedReader reader;
        try{
            reader = new BufferedReader(new FileReader(inputName));
            String line = reader.readLine();
            List <String> list = new LinkedList<String>();
            list.add(line);
            while (line != null) {
                line = reader.readLine();
                list.add(line);
            }
            reader.close();
//            System.out.println(list.size());
            // todo modify entry attributes
            for (int i = 0; i < list.size()-1; i++) {
                String tmp = list.get(i);
                String [] arr = tmp.split(":",2);
                switch (arr[0]){
                    case "title":       xml.setTitle(arr[1]);    break;
                    case "subtitle":    xml.setSubtitle(arr[1]); break;
                    case "link":        xml.setLink(arr[1]);     break;
                    case "id":          xml.setId(arr[1]);       break;
                    case "updated":     xml.setUpdated(arr[1]);  break;
                    case "summary":     xml.setSummary(arr[1]);  break;
                    case "entry":       break; // todo
                    default:            break;
                }
            }
            /* sample test
                xml.setTitle("My example feed");
                xml.setSubtitle("www.cs.adelaide.edu.au");
                xml.setLink("ww.cs.adelaide.edu.au");
                xml.setId("uuid:60a76c80-d399-11d9-b93C-0003939e0af6");
                xml.setUpdated("2015-08-07T18:30:02Z");
                xml.setAuthor("Santa Claus");
                xml.setSummary("here is some plain text. Because I'm not completely evil, you can assume that this will always be less than 1000 characters. And, as I've said before, it will always be plain text.");
            */
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // generate xml file
    public static void createXml(String xmlName) throws TransformerConfigurationException, IOException {
        if (debug) System.out.println("Sax XML :: Creating Feed XML file...");
        // create a SAXTransformerFactory object
        SAXTransformerFactory tff = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        try {
            TransformerHandler handler = tff.newTransformerHandler();
            Transformer tr = handler.getTransformer();
            // write xml content
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty(OutputKeys.INDENT, "yes");

            File f = new File(xmlName);
            if(!f.exists()){
                f.createNewFile();
            }

            Result result = new StreamResult(new FileOutputStream(f));
            // ser result to handler
            handler.setResult(result);

            // open document
            handler.startDocument();
            AttributesImpl attr = new AttributesImpl();

            handler.startElement("", "", "feed xml:lang=\"en-US\" xmlns=\"http://www.w3.org/2005/Atom\"", attr);

            // create title
            if(xml.getTitle() != null && !"".equals(xml.getTitle().trim())){
                attr.clear();
                handler.startElement("", "", "title", attr);
                handler.characters(xml.getTitle().toCharArray(), 0, xml.getTitle().length());
                handler.endElement("", "", "title");
            }

            // create subtitle
            if(xml.getSubtitle() != null && !"".equals(xml.getSubtitle().trim())){
                attr.clear();
                handler.startElement("", "", "subtitle", attr);
                handler.characters(xml.getSubtitle().toCharArray(), 0, xml.getSubtitle().length());
                handler.endElement("", "", "subtitle");
            }

            // create link
            if(xml.getLink() != null && !"".equals(xml.getLink().trim())){
                attr.clear();
                handler.startElement("", "", "link", attr);
                handler.characters(xml.getLink().toCharArray(), 0, xml.getLink().length());
                handler.endElement("", "", "link");
            }

            // create updated
            if(xml.getUpdated() != null && !"".equals(xml.getUpdated().trim())){
                attr.clear();
                handler.startElement("", "", "updated", attr);
                handler.characters(xml.getUpdated().toCharArray(), 0, xml.getUpdated().length());
                handler.endElement("", "", "updated");
            }

            // create author
            if(xml.getAuthor() != null && !"".equals(xml.getAuthor().trim())){
                attr.clear();
                handler.startElement("", "", "author", attr);
                handler.characters(xml.getAuthor().toCharArray(), 0, xml.getAuthor().length());
                handler.endElement("", "", "author");
            }
            // create Id
            if(xml.getId() != null && !"".equals(xml.getId().trim())){
                attr.clear();
                handler.startElement("", "", "id", attr);
                handler.characters(xml.getId().toCharArray(), 0, xml.getId().length());
                handler.endElement("", "", "id");
            }
            // create summary
            if(xml.getSummary() != null && !"".equals(xml.getSummary().trim())){
                attr.clear();
                handler.startElement("", "", "summary", attr);
                handler.characters(xml.getSummary().toCharArray(), 0, xml.getSummary().length());
                handler.endElement("", "", "summary");
            }
            handler.endElement("", "", "feed");
            // close document
            handler.endDocument();
            System.out.println("XML :: XML File is successfully created");
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            System.out.println("XML :: Error: XML File creating is failed");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("XML :: Error: XML File creating is failed");
        }
    }

    public static void main(String[] args) throws IOException, TransformerConfigurationException {
        SaxXml sax = new SaxXml();
        sax.parsingXML("input.txt","testXML.xml");
    }
}