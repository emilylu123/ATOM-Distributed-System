//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

/* GET client will start up, read the command line
    to find the server name and port number (in URL format)
    and will send a GET request for the ATOM feed.
    This feed will then be stripped of XML and displayed,
    one line at a time, with the attribute and its value.
    Possible formats for the server name and port number include
    "http://servername.domain.domain:portnumber",
    "http://servername:portnumber" (with implicit domain information)
    and "servername:portnumber" (with implicit domain and protocol information).
*/
public class GETClient implements Runnable {
    private static final int TRYMAX = 5;
    private static int tryCount = 0;
    private static Socket clientSocket;
    private static int logical_clock = (int)(Math.random()*100+22);
    private static String serverName;
    private static int portNumber;

    private boolean Connect() {
        while(tryCount<TRYMAX){
            try {
                clientSocket = new Socket(serverName,portNumber);
                System.out.println("================================");
                System.out.println("GetClient Application is ready! ");
                System.out.println("================================");
                return true;
            } catch (ConnectException e) {
                try {
                    tryCount++;
                    System.out.println("============================================");
                    System.out.println("GETClient Connection failed. Reconnect in " + tryCount + "s");
                    System.out.println("============================================");
                    System.out.println("GETClient Reconnecting: " + tryCount + " time\n");
                    Thread.sleep(tryCount * 1000); // retry in adaptive time n seconds
                    if (tryCount == TRYMAX){
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

    // "servername:portnumber"
    public static void main(String[] args) {
        // The server program creates the ServerSocket object in a try-with-resources statement:
        if (args.length==0){
            serverName = "0.0.0.0";
            portNumber = 4567;
        } else {
            String address = args[0];
            String [] arr = address.split(":");
            if (arr.length>=2){
                serverName = arr[0];
                portNumber = Integer.parseInt(arr[1]);
            } else {
                serverName = "0.0.0.0";
                portNumber = 4567;
            }
        }

        // create a new get thread to run this get client application
        GETClient client = new GETClient();
        boolean success = client.Connect();
        if (success){
            Thread getThread = new Thread(client);
            getThread.start();
        }
    }

    private void incrementLamport (){
        logical_clock++;
    }

    private void updateLamport (int timestamp){
        logical_clock = Math.max(logical_clock,timestamp) + 1;
    }

    // send GET request with timestamp
    private void GET () {
        DataInputStream inGET;
        DataOutputStream outGET;
        try {
            inGET = new DataInputStream(clientSocket.getInputStream());
            outGET = new DataOutputStream(clientSocket.getOutputStream());

            // send GET request and increment Lamport timestamp
            String getMSG = "GET@" + logical_clock;
            outGET.writeUTF(getMSG);
            incrementLamport();

            // receive news from ATOM server and update timestamp
            String inMSG = inGET.readUTF();
            XMLParser parser = new XMLParser();
            String FILE_TO_RECEIVED = "feedXML_client.xml";
            parser.receiveXML(FILE_TO_RECEIVED, clientSocket);

            BufferedReader br = new BufferedReader(new FileReader(FILE_TO_RECEIVED));
            if (br.readLine() == null) {
                System.out.println("GETClient:: News feed is EMPTY. Please try later. \n");
            } else {
                int timestamp = Integer.parseInt(inMSG);
                updateLamport(timestamp);
                // display news if it is not empty and then increment Lamport timestamp
                displayXML();
                incrementLamport();
            }
        } catch (IOException e) {
            System.out.println("ATOM Server is not available.");
        }
    }

    // display XML in a pretty format
    private void displayXML () {
        System.out.println("============ GET Client:: Display News ==========");
        String xmlName = "feedXML_client.xml";
        XMLParser parser = new XMLParser();
        parser.readXML(xmlName);
        System.out.println("=================================================\n");
    }

    @Override
    public void run() {
        GET();
    }
}

