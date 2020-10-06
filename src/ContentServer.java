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

public class ContentServer implements Runnable {
    private final static int TRYMAX = 5;  // define reconnect times if connection fails
    private static int tryCount = 0;
    protected static Socket contentSocket;
    private static int logical_clock = (int) (Math.random()*100);
    private static String inputFile;
    private static String xmlName;
    private static String contentServerID;
    private static String serverName ;
    private static int portNumber;

    private boolean Connect() {
        while(tryCount<TRYMAX){
            try {
                contentSocket = new Socket(serverName,portNumber);
                System.out.println("================================");
                System.out.println("Content Server [ID " + contentServerID +"] is ready! ");
                System.out.println("Address: "+ contentSocket.getLocalSocketAddress()+"\nLocal time: " + logical_clock);
                System.out.println("================================");
                return true;
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
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
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
        else xmlName = "feedXML_content.xml";

        if (args.length > 3) contentServerID = args[3];
        else contentServerID = "unknown";

        // create a new put thread to run this content sever
        ContentServer content = new ContentServer();
        boolean success = content.Connect();
        if (success){
            Thread putThread = new Thread(content);
            putThread.start();
        }
    }

    @Override
    public void run() {
        // read news feed from local file given from command line and create a XML file
        try {
            //read input file and create a XML file
            createNewsFeed(inputFile);

            // send xml to ATOM server and receiver returned ststus code
            String statusCode;
            statusCode = PUT(contentServerID);

//            // send feed XML file to ATOM server
//            System.out.println("sending XML");
//            XMLParser parser = new XMLParser();
//            parser.sendXML(xmlName,contentSocket);
//            System.out.println("XML file is sent");

            // receive status code and take actions accordingly
            if (statusCode.isEmpty()){
                System.out.println("Receive no status Code. Content Server will sent PUT request again.");
                PUT(contentServerID);
            } else if (statusCode.equals("200")) {
                System.out.println("Content:: Reconnect Successes. PUT request Successes again!");
            } else if (statusCode.equals("201")) {
                System.out.println("Content:: HTTP_CREATED. PUT request Successes!");
            } else if (statusCode.equals("204")) {
                System.out.println("Content:: Error: The Feed received from ATOM was empty. Content Server will sent PUT request again.");
                PUT(contentServerID);
            } else if (statusCode.equals("400")) {
                System.out.println("Content:: Wrong message");
            } else if (statusCode.equals("500")) {
                System.out.println("Content::Feeds do not make sense. Content Server will sent PUT request again.");
                PUT(contentServerID);
            }
        } catch (IOException | TransformerConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void incrementLamport (){
        logical_clock++;
    }

    private void updateLamport (int timestamp){
        logical_clock = Math.max(logical_clock, timestamp) + 1;
    }

    //read [input.txt] file and create a XML file [feedXML_content.xml]
    private void createNewsFeed(String inputName) throws IOException, TransformerConfigurationException {
        XMLParser parser = new XMLParser();
        parser.parsingXML(inputName, xmlName);
    }

    private String PUT(String id) throws IOException {
        DataInputStream inContent = new DataInputStream (contentSocket.getInputStream ( ));
        DataOutputStream outContent = new DataOutputStream (contentSocket.getOutputStream ( ));

        // read feed XML file and format it as a String
        XMLParser parser = new XMLParser ( );
//        String newsFeed = parser.readXML(xmlName);
        String newsFeed = readXML ( );

        String type = "XML";
        int length = newsFeed.length ( );

        String header = "PUT /atom.xml HTTP/1.1\n" + "User-Agent: ATOMClient/1/0\n";
        header += "Content-Type: " + type + "\n" + "Content-Length: " + length + "\n";

        String putMSG = header + newsFeed + "@" + logical_clock + "@" + id;


        // send to ATOM server + timestamp+1
        outContent.writeUTF (putMSG);

        // send feed XML file to ATOM server
//        parser.sendXML(xmlName,contentSocket);
        sendXML (xmlName);
        System.out.println ("Content:: A new feed has been sent to ATOM Server");

        incrementLamport ( );

        // get status code from ATOM server
        String inMSG = inContent.readUTF ( ); //todo
        String[] array = inMSG.split ("@");
        String statusCode = array[0];
        int timestamp = Integer.parseInt (array[1]);

        //update Lamport by max + 1
        updateLamport (timestamp);
        System.out.println ("Content:: Receive Status Code [" + statusCode + "] @ time " + logical_clock);
        return statusCode;
    }


    // read XML file and return a string in the correct format
    private String readXML() {
        String readXML = "";
        BufferedReader br;
        try {
            br = new BufferedReader (new FileReader (xmlName));
            String line = br.readLine ( );
            do {
                readXML += line + "\n";
                line = br.readLine ( );
            } while (line != null);
            br.close ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        }
        return readXML;
    }

    // send xml file as a feed to ATOM server
    private void sendXML(String xml) throws IOException {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            // send file
            File myFile = new File (xml);
            byte[] mybytearray = new byte[(int) myFile.length ( )];
            fis = new FileInputStream (myFile);
            bis = new BufferedInputStream (fis);
            bis.read (mybytearray, 0, mybytearray.length);
            os = contentSocket.getOutputStream ( );
            os.write (mybytearray, 0, mybytearray.length);
            System.out.println ("Content:: is " + isConnected ( ));
            System.out.println ("Content:: Sending " + xml + "(" + mybytearray.length + " bytes)");
            os.flush ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        } finally {
//            bis.close ( );
            System.out.println ("Content:: is " + isConnected ( ));
//            fis = null;
//            os = null;
        }
    }

    public boolean isConnected() {
        return contentSocket != null && contentSocket.isConnected ( );
    }
}