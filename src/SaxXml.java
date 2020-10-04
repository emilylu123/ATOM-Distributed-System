////==============================================================
//// Distributed System
//// Name: Yingyao Lu
//// ID: a1784870
//// Semester: S2
//// Year: 2020
//// Assignment2: ATOM feeds aggregation and distribution system
////==============================================================
//import java.io.*;
//import java.net.Socket;
//import java.util.*;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.parsers.SAXParser;
//import javax.xml.parsers.SAXParserFactory;
//import javax.xml.transform.*;
//import javax.xml.transform.sax.*;
//import javax.xml.transform.stream.*;
//import org.xml.sax.*;
//import org.xml.sax.helpers.AttributesImpl;
//
//// create XML entry as a object
//public class SaxXml {
//    private static XMLinfo xml = new XMLinfo();
//
//    public void parsingXML(String inputName, String xmlName) throws IOException, TransformerConfigurationException {
//        readFile(inputName);
//        createXml(xmlName);
//    }
//
//    public void readXML(String xmlName){
//        String xml = "";
//        BufferedReader reader;
//        try{
//            reader = new BufferedReader(new FileReader(xmlName));
//            String line = reader.readLine();;
//            do{
//                xml += line + "\n";
//                line = reader.readLine();
//            }  while (line != null);
//            reader.close();
//        } catch (IOException e){
//            e.printStackTrace();
//        }
//
//        SAXParserFactory spf = SAXParserFactory.newInstance();
//
//        try{
//            SAXParser sax = spf.newSAXParser() ;
//            InputStream is = getStringStream( xml );
//            if(is == null)
//                System.out.println("The XML file is empty.");
//            else {
//                System.out.println("Parsing in Sax XML Parser");
//                sax.parse(is, new XMLParser());
//            }
//        }catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        } catch (SAXException e) {
//            e.printStackTrace();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private InputStream getStringStream(String xml) {
//        if (xml != null && !xml.trim().equals("")) {
//            try {
//                ByteArrayInputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
//                return xmlStream;
//            }catch (Exception ex){
//                System.out.println("something wrong happened.");;
//            }
//        }
//        return null;
//    }
//
//    private void readFile(String inputName) throws TransformerConfigurationException, IOException {
//        System.out.println("Sax XML :: Reading Input File...");
//        BufferedReader reader;
//        try{
//            reader = new BufferedReader(new FileReader(inputName));
//            String line = reader.readLine();
//            List <String> list = new LinkedList<String>();
//            list.add(line);
//            while (line != null) {
//                line = reader.readLine();
//                list.add(line);
//            }
//            reader.close();
//            for (int i = 0; i < list.size()-1; i++) {
//                String tmp = list.get(i);
//                String [] arr = tmp.split(":",2);
//                switch (arr[0]){
//                    case "title":       xml.setTitle(arr[1]);    break;
//                    case "subtitle":    xml.setSubtitle(arr[1]); break;
//                    case "link":        xml.setLink(arr[1]);     break;
//                    case "id":          xml.setId(arr[1]);       break;
//                    case "updated":     xml.setUpdated(arr[1]);  break;
//                    case "summary":     xml.setSummary(arr[1]);  break;
//                    case "entry":       break;
//                    default:            break;
//                }
//            }
//
//        } catch (IOException e){
//            e.printStackTrace();
//        }
//    }
//
//    // generate xml file
//    private static void createXml(String xmlName) throws TransformerConfigurationException, IOException {
//        System.out.println("Sax XML :: Creating Feed XML file...");
//        // create a SAXTransformerFactory object
//        SAXTransformerFactory tff = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
//
//        try {
//            TransformerHandler handler = tff.newTransformerHandler();
//            Transformer tr = handler.getTransformer();
//            // write xml content
//            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            tr.setOutputProperty(OutputKeys.INDENT, "yes");
//
//            File f = new File(xmlName);
//            if(!f.exists()){
//                f.createNewFile();
//            }
//
//            Result result = new StreamResult(new FileOutputStream(f));
//            // ser result to handler
//            handler.setResult(result);
//
//            // open document
//            handler.startDocument();
//            AttributesImpl attr = new AttributesImpl();
//
//            handler.startElement("", "", "feed xml:lang=\"en-US\" xmlns=\"http://www.w3.org/2005/Atom\"", attr);
//
//            // create title
//            if(xml.getTitle() != null && !"".equals(xml.getTitle().trim())){
//                attr.clear();
//                handler.startElement("", "", "title", attr);
//                handler.characters(xml.getTitle().toCharArray(), 0, xml.getTitle().length());
//                handler.endElement("", "", "title");
//            }
//
//            // create subtitle
//            if(xml.getSubtitle() != null && !"".equals(xml.getSubtitle().trim())){
//                attr.clear();
//                handler.startElement("", "", "subtitle", attr);
//                handler.characters(xml.getSubtitle().toCharArray(), 0, xml.getSubtitle().length());
//                handler.endElement("", "", "subtitle");
//            }
//
//            // create link
//            if(xml.getLink() != null && !"".equals(xml.getLink().trim())){
//                attr.clear();
//                handler.startElement("", "", "link", attr);
//                handler.characters(xml.getLink().toCharArray(), 0, xml.getLink().length());
//                handler.endElement("", "", "link");
//            }
//
//            // create updated
//            if(xml.getUpdated() != null && !"".equals(xml.getUpdated().trim())){
//                attr.clear();
//                handler.startElement("", "", "updated", attr);
//                handler.characters(xml.getUpdated().toCharArray(), 0, xml.getUpdated().length());
//                handler.endElement("", "", "updated");
//            }
//
//            // create author
//            if(xml.getAuthor() != null && !"".equals(xml.getAuthor().trim())){
//                attr.clear();
//                handler.startElement("", "", "author", attr);
//                handler.characters(xml.getAuthor().toCharArray(), 0, xml.getAuthor().length());
//                handler.endElement("", "", "author");
//            }
//            // create Id
//            if(xml.getId() != null && !"".equals(xml.getId().trim())){
//                attr.clear();
//                handler.startElement("", "", "id", attr);
//                handler.characters(xml.getId().toCharArray(), 0, xml.getId().length());
//                handler.endElement("", "", "id");
//            }
//            // create summary
//            if(xml.getSummary() != null && !"".equals(xml.getSummary().trim())){
//                attr.clear();
//                handler.startElement("", "", "summary", attr);
//                handler.characters(xml.getSummary().toCharArray(), 0, xml.getSummary().length());
//                handler.endElement("", "", "summary");
//            }
//            handler.endElement("", "", "feed");
//            // close document
//            handler.endDocument();
//            System.out.println("XML :: XML File is successfully created");
//        } catch (SAXException saxException) {
//            saxException.printStackTrace();
//            System.out.println("XML :: Error: XML File creating is failed");
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("XML :: Error: XML File creating is failed");
//        }
//    }
//
//    private void receiveXML(String FILE_TO_RECEIVED, Socket a_socket) throws IOException {
//        int bytesRead;
//        int current = 0;
//        FileOutputStream fos = null;
//        BufferedOutputStream bos = null;
//
//        int FILE_SIZE = 102400;
//        byte[] mybytearray = new byte[FILE_SIZE];
//
//        InputStream is = a_socket.getInputStream();
//
//        fos = new FileOutputStream(FILE_TO_RECEIVED);
//        bos = new BufferedOutputStream(fos);
//        bytesRead = is.read(mybytearray, 0, mybytearray.length);
//        current = bytesRead;
//
//        do {
//            bytesRead =
//                    is.read(mybytearray, current, (mybytearray.length - current));
//            if (bytesRead >= 0) current += bytesRead;
//        } while (bytesRead > -1);
//
//        bos.write(mybytearray, 0, current);
//        bos.flush();
//        System.out.println("XML:: File " + FILE_TO_RECEIVED
//                + " downloaded (" + current + " bytes read)");
//
//        if (fos != null) fos.close();
//        if (bos != null) bos.close();
//    }
//
//    private void sendXML(String FILE_TO_SEND, Socket a_socket) throws IOException {
//        FileInputStream fis = null;
//        BufferedInputStream bis = null;
//        OutputStream os = null;
//        try {
//            // send file
//            File myFile = new File (FILE_TO_SEND);
//            byte [] mybytearray  = new byte [(int)myFile.length()];
//            fis = new FileInputStream(myFile);
//            bis = new BufferedInputStream(fis);
//            bis.read(mybytearray,0,mybytearray.length);
//            os = a_socket.getOutputStream();
//            System.out.println("ATOM:: Sending " + FILE_TO_SEND + "(" + mybytearray.length + " bytes)");
//            os.write(mybytearray,0,mybytearray.length);
//            os.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (bis != null) bis.close();
//            if (os != null) os.close();
//            System.out.println("ATOM:: XML file hss sent to GETClient.");
//        }
//    }
//
//    public static void main(String[] args) throws IOException, TransformerConfigurationException {
//        SaxXml sax = new SaxXml();
//        sax.parsingXML("input.txt","testXML.xml");
//    }
//}