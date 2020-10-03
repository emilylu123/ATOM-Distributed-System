//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import javax.xml.transform.TransformerConfigurationException;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import static java.io.Console.*;

public class ContentServer extends XMLParser implements Runnable {
    private final static int TRYMAX = 5;  // define reconnect times if connection fails
    private static int tryCount = 0;
    protected static Socket contentSocket;
    private static ContentServer content;
    private static int logical_clock = (int) (Math.random()*100);
    private static String inputFile;
    private static String xmlName;
    private static String contentServerID;
    private static String serverName ;
    private static int portNumber;

    public ContentServer (String serverName, int portNumber, String id){
        this.serverName = serverName ;
        this.portNumber = portNumber ;
        this.contentServerID = id ;
        while(tryCount<TRYMAX){
            try {
                contentSocket = new Socket(this.serverName,this.portNumber);
                System.out.println("================================");
                System.out.println("Content Server [ID " + this.contentServerID +"] is ready! ");
                System.out.println("Address: "+ this.contentSocket.getLocalSocketAddress()+"\nLocal time: " + logical_clock);
                System.out.println("================================");
                break;
            } catch (ConnectException e) {
                try {
                    tryCount++;
                    System.out.println("=================================================");
                    System.out.println("Content Server Connection failed. Reconnect in " + tryCount + "s");
                    System.out.println("=================================================");
                    System.out.println("Content Server Reconnecting: " + tryCount + " time\n");
                    Thread.sleep(tryCount * 1000);
                    if (tryCount == TRYMAX) {
                        System.out.println("===============================================");
                        System.out.println("ATOM Server is offline. Please come back later.");
                        System.out.println("===============================================");
                    }
                    continue;
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // The server program creates the ServerSocket object in a try-with-resources statement:
        String address = args[0];
        String [] arr = address.split(":");
        String serverName = arr[0];

        if (arr.length > 1) portNumber = Integer.parseInt(arr[1]);
        else portNumber = 4567; // by default

        if (args.length > 1) inputFile = args[1];
        else inputFile = "input.txt";

        if (args.length > 2 )  xmlName = args[2];
        else xmlName = "feedXML.xml";

        if (args.length > 3) contentServerID = args[3];
        else contentServerID = "unknown";

        // create a new put thread to run this content sever
        content = new ContentServer(serverName, portNumber, contentServerID);
        Thread putThread = new Thread(content);
        putThread.start();
    }

    @Override
    public void run() {
        // read news feed from local file given from commond line and create a XML file
        try {
            //read input file and create a XML file
            createNewsFeed(inputFile, xmlName);

            // send xml to ATOM server and receiver returned ststus code
            String statusCode = "";
            statusCode = PUT(xmlName,this.contentServerID);

            // send feed XML file to ATOM server
            System.out.println("sending XML");
            sendXML(xmlName);
            System.out.println("XML file is sent");

            // receive status code and take actions accordingly
            if (statusCode.isEmpty()){
                System.out.println("Receive no status Code. Content Server will sent PUT request again.");
                PUT(xmlName,contentServerID);
            } else if (statusCode.equals("200")) {
                System.out.println("Content:: Reconnect Successes. PUT request Successes again!");
            } else if (statusCode.equals("201")) {
                System.out.println("Content:: HTTP_CREATED. PUT request Successes!");
            } else if (statusCode.equals("204")) {
                System.out.println("Content:: Error: The Feed received from ATOM was empty. Content Server will sent PUT request again.");
                PUT(xmlName,contentServerID);
            } else if (statusCode.equals("400")) {
                System.out.println("Content:: Wrong message");
            } else if (statusCode.equals("500")) {
                System.out.println("Content::Feeds do not make sense. Content Server will sent PUT request again.");
                PUT(xmlName,contentServerID);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void incrementLamport (){
        logical_clock++;
    }

    private void updateLamport (int timestamp){
        logical_clock = Math.max(logical_clock, timestamp) + 1;
    }

    //read [input.txt] file and create a XML file [feedXML.xml]
    private void createNewsFeed(String inputName, String xmlName) throws IOException, TransformerConfigurationException {
        SaxXml sax = new SaxXml();
        sax.parsingXML(inputName, xmlName);
    }

    private String PUT (String xmlName, String id) throws IOException {
        DataInputStream inContent = new DataInputStream(contentSocket.getInputStream());
        DataOutputStream outContent = new DataOutputStream(contentSocket.getOutputStream());

        // read feed XML file and format it as a String
        String newsFeed = readXML(xmlName);
        String type = "XML";
        int length = newsFeed.length();

        String header = "PUT /atom.xml HTTP/1.1\n" + "User-Agent: ATOMClient/1/0\n";
        header += "Content-Type: " + type + "\n" + "Content-Length: " + length + "\n";

        String putMSG = header + newsFeed + "@" + logical_clock + "@" +id;

        // send to ATOM server + timestamp+1
        outContent.writeUTF(putMSG);

        System.out.println("Content:: Send a new feed to ATOM Server");

        incrementLamport();

        // get status code from ATOM server
        String inMSG = inContent.readUTF();
        String[] array = inMSG.split("@");
        String statusCode = array[0];
        int timestamp = Integer.parseInt(array[1]);

        //update Lamport by max + 1
        updateLamport(timestamp);
        System.out.println("Content:: Receive Status Code ["+ statusCode + "] @ time " + logical_clock);
        return statusCode;
    }

    // read XML file and return a string in the correct format
    private String readXML (String xmlName) {
        String readXML = "";
        BufferedReader reader;
        try{
            reader = new BufferedReader(new FileReader(xmlName));
            String line = reader.readLine();;
            do{
                readXML += line + "\n";
                line = reader.readLine();
            }  while (line != null);
            reader.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return readXML;
    }

    private void sendXML(String xmlName) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            // send file
            File myFile = new File (xmlName);
            byte [] mybytearray  = new byte [(int)myFile.length()];
            fis = new FileInputStream(myFile);
            bis = new BufferedInputStream(fis);
            bis.read(mybytearray,0,mybytearray.length);
            os = contentSocket.getOutputStream();
            System.out.println("Content:: Sending " + xmlName + "(" + mybytearray.length + " bytes)");
            os.write(mybytearray,0,mybytearray.length);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bis != null) bis.close();
            if (os != null) os.close();
            System.out.println("Content:: XML file is sent.");
        }
    }
}

