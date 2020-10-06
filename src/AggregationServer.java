//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import java.io.*;
import java.net.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AggregationServer extends Thread {
    private DataInputStream dis; //输入流
    private DataOutputStream dos; //输出流
    private volatile static int logical_clock = 0;
    protected static ServerSocket serverSocket;
    private static Socket atomSocket;
    private static int portNumber;
    private static LinkedList<Feed> feedList = new LinkedList<Feed> ( );
    private static final LinkedList<String> activeContentServers = new LinkedList<String> ( );

    public AggregationServer(int port) {

        try {
            serverSocket = new ServerSocket (port);
            System.out.println (" >>" + isConnected ( ));
            System.out.println ("==============================");
            System.out.println("ATOM Server is ready! ");
            System.out.println("Address: "+ serverSocket.getLocalSocketAddress());
            System.out.println("==============================");

            // always check local backupFile to recover feeds when start ATOM server
            recoverFeeds();
            // start heart beat timer for disconnecting expired feeds (12s)
            timer();
        } catch (Exception e){
            System.out.println("ATOM::Error in initialize ATOM Server");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverName;
        if (args.length > 0) serverName = args[0];
        else serverName = "ATOMServer";
        if (args.length > 1) portNumber = Integer.parseInt(args[1]);
        else portNumber = 4567; // by default

        AggregationServer atom = new AggregationServer(portNumber);

        // accept multiple connection on the socket
        try {
            while(true){
                atomSocket = serverSocket.accept();
                new Thread (atom).start ( );
                System.out.println (">>>" + atom.isConnected ( ));
            }
        } catch (IOException e) {
            System.out.println ("ATOM:: Error in ATOM Socket");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void run (){
        System.out.println (" >>" + isConnected ( ));
        try {
            dis = new DataInputStream (atomSocket.getInputStream ( ));
            dos = new DataOutputStream (atomSocket.getOutputStream ( ));

            String receiveMSG = dis.readUTF ( );
            String[] arr = receiveMSG.split ("@");
            String inMSG = arr[0];
            int timestamp = Integer.parseInt (arr[1]);

            // update Lamport timestamp by max + 1
            updateLamport (timestamp);

            // read keyword to process PUT and GET requests.
            String keyword = inMSG.substring(0,3);

            XMLParser parser = new XMLParser();

            // PUT request from Content server, update heartbeat test and process news feeds, return status code
            if (keyword.equals("PUT")){
                String contentServerID = "";
                if (arr.length > 2) contentServerID = arr[2];

                String FILE_TO_RECEIVE = "feedXML_atom_received.xml";
                File file = new File (FILE_TO_RECEIVE);

                if (!file.exists ( )) {
                    file.createNewFile ( );
                }

                System.out.println (" if connected >>" + isConnected ( ));

                String outMSG = processPUT (FILE_TO_RECEIVE, contentServerID);
                dos.writeUTF (outMSG);
                System.out.println ("ATOM:: Send Status Code to Content Server: " + outMSG);

                parser.receiveXML (FILE_TO_RECEIVE, atomSocket);
                System.out.println ("ATOM:: Receive a feed from content server: " + contentServerID);

                // aggregate new feed with id
                aggregationFeeds (contentServerID);

                // save news feeds to local file
                backupFeeds ( );
            }

            // GET request from Client, read backup File and send out active aggregation feeds message
            else if (keyword.equals("GET")){
                String outMSG = "";
                File file = new File ("feeds_atom_to_client.xml");

                if(!file.exists()){
                    file.createNewFile();
                }

                if (file.length ()!=0){
                    outMSG = String.valueOf (file.length ()) ;
                } else {
                    outMSG = "EMPTY";
                }
                outMSG += "@" + logical_clock;
                dos.writeUTF (outMSG);
                processGET ( );
            }
            // Process illegal request by sending error status code 400
            else {
                System.out.println ("ATOM:: [400] Request Error. Please try again.");
                dos.writeUTF ("400");
            }

            // update local time by +1
            incrementLamport ( );

            System.out.println ("\nATOM:: Finish all processes. Wait for next request.\n= = = = = = = = = = = = = = = = = = = = = = = = =\n");
        } catch (SocketTimeoutException s) {
            System.out.println ("ATOM:: Socket timed out!");
            s.printStackTrace ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        }
    }

    public boolean isConnected() {
        return atomSocket != null && atomSocket.isConnected ( );
    }

    public void disConnect() {
        if (atomSocket != null) {
            try {
                dis.close ( );
                dos.close ( );
                atomSocket.close ( );
                atomSocket = null;
            } catch (IOException e) {
                e.printStackTrace ( );
            }
        }
    }


    protected String processPUT(String FILE_TO_RECEIVE, String contentServerID) throws IOException {
        System.out.println ("\n*********************************************");
        System.out.println ("ATOM:: Receive [PUT] from Content Server [" + contentServerID + "]");
        System.out.println ("ATOM:: Update Lamport TimeStamp: " + logical_clock);
        System.out.println ("*********************************************\n");

//        XMLParser parser = new XMLParser ();
//        parser.receiveXML(FILE_TO_RECEIVE, atomSocket);
//        System.out.println("ATOM:: Receive a feed from content server: " + contentServerID);

        // update this content server's feed timer to zero
        if (!feedList.isEmpty ( )) {
            resetFeedTimer (contentServerID);
        }

//        // aggregate new feed with id
//        aggregationFeeds(contentServerID);
//
//        // save news feeds to local file
//        backupFeeds();

        // return correct status code & timestamp to content server
        int statusCode = returnStatusCode (contentServerID);

        return statusCode + "@" + logical_clock;
    }

    private void resetFeedTimer(String contentServerID){
        if (activeContentServers.contains(contentServerID)){
            System.out.println("ATOM:: Reset Feeds timer to zero for " + contentServerID);
            for (int i = 0; i < feedList.size(); i++) {
                if (feedList.get(i).getSource().equals(contentServerID))  {
                    feedList.get(i).setTimer(0);
                }
            }
        }
    }

    // get feeds from backup file
    protected void processGET() throws IOException {
        System.out.println("\n********************************************");
        System.out.println("ATOM:: Receive [GET] from Client Application");
        System.out.println("********************************************\n");

        // read feeds from local backup file
        String FILE_TO_SEND = "feeds_atom_to_client.xml";
        File sendFile = new File (FILE_TO_SEND);
        if (!sendFile.exists ( )) sendFile.createNewFile ( );

        // create latest feeds xml file to send to client
        String feeds = atomFeeds(FILE_TO_SEND);

        XMLParser parser = new XMLParser ( );

        if (sendFile.length ( ) != 0) {
            parser.sendXML (FILE_TO_SEND, atomSocket);
            System.out.println ("ATOM:: News Feeds have sent to Client: (" + sendFile.length () +" bytes)");
        } else{
            // parser.sendXML (FILE_TO_SEND, atomSocket);
            System.out.println("ATOM:: News Feed is Empty. Please come back later.");
        }

    }

    protected void timer() {
        System.out.println("ATOM:: Start timer...");
        new Thread(() -> {
            try {
                while (true){
                    Thread.sleep(1000);

                    if(!feedList.isEmpty()){
                        for (Feed f: feedList) {
                            f.setTimer(f.getTimer()+1);
                        }
                        System.out.print("ATOM:: Heart Beat Test: "+ feedList.getFirst().getTimer()  + "\tTotal feeds number: [" + feedList.size()+"]\t");
                        String active = "";
                        for (int i = 0; i < activeContentServers.size(); i++) {
                            active += activeContentServers.get(i) + " ";
                        }
                        System.out.println("Active Content Server(ID): " + active);
                        updateFeeds();
                        if (feedList.isEmpty()){
                            System.out.println("\nATOM:: Finish all processes. Wait for next request.\n= = = = = = = = = = = = = = = = = = = = = = = = =\n");
                        }
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void incrementLamport (){
        logical_clock++;
        System.out.println("ATOM:: Increment timestamp: " + logical_clock + "\n");
    }

    private void updateLamport (int timestamp){
        logical_clock = Math.max(logical_clock,timestamp) + 1;
        System.out.println("ATOM:: Update timestamp: " + logical_clock);
    }
    protected String atomFeeds(String fileName){
        String latestFeeds = "";
        PrintWriter writer = null;
        try {
            writer = new PrintWriter (fileName, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            e.printStackTrace ( );
        } catch (IOException e) {
            e.printStackTrace ( );
        }

        for (int i = 0; i < feedList.size(); i++) {
            latestFeeds = latestFeeds + feedList.get(i).getContent() + "\n";

            writer.println(feedList.get(i).getContent());
        }
        writer.close();
        return latestFeeds;
    }
    // recover from backup file and create Feed object  w/ content ID
    protected void recoverFeeds() {
        String FILE_TO_RECOVER = "backup.xml";
        File backupFile = new File(FILE_TO_RECOVER);
        feedList = new LinkedList<Feed>();

        if (!backupFile.exists()){
            System.out.println("ATOM:: First time start ATOM SERVER. Create empty backup file...");
            try {
                backupFile.createNewFile();
            } catch (IOException e) {
                System.out.println("ATOM:: Error in creating backup.xml file");
                e.printStackTrace();
            }
        } else if (backupFile.exists() && backupFile.length()==0) {
            System.out.println("ATOM:: Recovering... Backup File is EMPTY.");
        } else if(backupFile.exists() && backupFile.length()!=0){
            System.out.println("ATOM:: Recovering feeds from backup file...");
            String recovery = "";
            BufferedReader br;
            try {
                br = new BufferedReader (new FileReader (backupFile));
                String line = br.readLine ( );
                do {
                    recovery += line + "\n";
                    line = br.readLine ( );
                } while (line != null);
                br.close ( );
            } catch (IOException e){
                e.printStackTrace();
            }

            String [] arr = recovery.split("^^^^^");

            for (int i = 0; i < arr.length; i++) {
                String [] tmp = arr[i].split("~~");
                String content = tmp[0];
                String id = tmp[1];
                Feed aFeed = new Feed(content,id);
                feedList.add(aFeed);
            }
            System.out.println("ATOM:: Recovery feeds number: " + feedList.size());
        }
    }

    // aggregate new feed to finalFeeds, and update to latest 20 and within 12s
    private void aggregationFeeds(String id){
        System.out.println("ATOM:: Aggregate new feed to latest feeds list");

        XMLParser parser = new XMLParser ();
        File file = new File ("feedXML_atom_received.xml");
//        if (file.length ()!=0){
            String content = parser.readFile ("feedXML_atom_received.xml");
            System.out.println (">>>>>>>>>>" + content );
//        }
//        String content = "<" + inMSG.split("<",2)[1];
        Feed aFeed = new Feed(content,id);
        feedList.add(aFeed);

        // update to latest 20
        if(feedList.size() > 20) feedList.removeFirst();
    }

    // update finalFeeds to local file
    private void updateFeeds (){
        boolean isUpdated = false;
        for (int i = 0; i < feedList.size(); i++) {
            if (feedList.get (i).getTimer ( ) >= 12) {
                String removeID = feedList.get (i).getSource ( );
                feedList.remove (i);
                activeContentServers.remove (removeID);
                System.out.println ("\nATOM:: Content Server [" + removeID + "] has Expired. Remove all feeds from this server.");

                if (!activeContentServers.isEmpty ( )) {
                    String active = "ATOM:: Active Content Server: ";
                    for (String str : activeContentServers) {
                        active += str + " ";
                    }
                    System.out.println (active);
                } else {
                    System.out.println ("ATOM:: No active Content Server in the system.");
                }
                isUpdated = true;
            }
        }
        // if remove feeds, backup latest feeds to local file
        if (isUpdated) {
            backupFeeds ( );
        }
    }

    /*The first time your ATOM feed is created, you should return status 201 - HTTP_CREATED.
     If later uploads are ok, you should return status 200.
     (This means, if a Content Server first connects to the Aggregation Server,
     then return 201 as succeed code, then before the content server lost connection,
     all other succeed response should use 200).
     Any request other than GET or PUT should return status 400
     (note: this is not standard but to simplify your task).
     Sending no content to the server should cause a 204 status code to be returned.
     Finally, if the ATOM XML does not make sense you may return status code 500 - Internal server error.
     */
    private int returnStatusCode (String id){
        if (!activeContentServers.isEmpty()) {
            for (int i = 0; i < activeContentServers.size(); i++) {
                if (activeContentServers.get(i).equals(id)) {
                    System.out.println("ATOM:: Content Server [" + id + "] connects again.");
                    return 200;
                }
            }
        }
        System.out.println("ATOM:: A new Content Server [" + id + "] HTTP_CREATED.");
        activeContentServers.addLast(id);
        return 201;
    }

    // write finalFeeds to local file
    private void backupFeeds (){
        String fileName = "backup.xml";
        System.out.println("ATOM:: Backup feeds to local file " + fileName);
        String latestFeeds = "";
        for (int i = 0; i < feedList.size(); i++) {
            latestFeeds = latestFeeds + feedList.get (i).getContent ( ) + "~~" + feedList.get (i).getSource ( ) + "^^^^^\n";
//            latestFeeds = latestFeeds + feedList.get(i).getContent() + "\n";
        }

        FileWriter writer;
        try {
            writer = new FileWriter(fileName);
            writer.write(latestFeeds);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("ATOM:: Backup is finished: " + fileName);
    }
}