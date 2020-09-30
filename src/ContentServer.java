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

public class ContentServer extends XMLParser implements Runnable {
    private static boolean debug = true;
    private static final int TRYMAX = 5;
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
                System.out.println("Content Server ID [" + this.contentServerID +"] is ready! ");
                System.out.println("Local time: " + logical_clock);
                System.out.println("Address: "+ this.contentSocket.getLocalSocketAddress());
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

        content = new ContentServer(serverName,portNumber,contentServerID);
        Thread putXml = new Thread(content);
        putXml.start();

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

        String type = "XML"; // todo ??
        int length = newsFeed.length();

        String header = "PUT /atom.xml HTTP/1.1\n" + "User-Agent: ATOMClient/1/0\n";
        header += "Content-Type: " + type + "\n" + "Content-Length: " + length + "\n";

        String putMSG = header + newsFeed + "@" + logical_clock + "@" +id;

        // send to ATOM server + timestamp+1
        outContent.writeUTF(putMSG);
        if (debug) System.out.println("Content:: Send new feeds to ATOM Server");

        incrementLamport();

        // get status code from ATOM server
        String inMSG = inContent.readUTF();
        String[] array = inMSG.split("@");
        String statusCode = array[0];
        int timestamp = Integer.parseInt(array[1]);

        //update Lamport by max + 1
        updateLamport(timestamp);
        System.out.println("Content::Receive Status Code ["+ statusCode + "] @local time " + logical_clock);
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

    @Override
    public void run() {
        // read news feed from local file given from commond line and create a XML file
        try {
            createNewsFeed(inputFile, xmlName);
            String statusCode = "";
            statusCode = PUT(xmlName,this.contentServerID);

            // todo status code
            if (statusCode.isEmpty()){
                System.out.println("EMPTY. Try again.");
                //todo try again
            } else if (statusCode.equals("200")) {
                System.out.println("Content:: PUT request Successes again!");
            } else if (statusCode.equals("201")) {
                System.out.println("Content:: HTTP_CREATED. PUT request Successes!");
            } else if (statusCode.equals("204")) {
                System.out.println("Content:: Feed has NO content");
                PUT(xmlName,contentServerID);
            } else if (statusCode.equals("400")) {
                System.out.println("Content:: Wrong message");
            } else if (statusCode.equals("500")) {
                System.out.println("Content::Feeds do not make sense");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }
}

