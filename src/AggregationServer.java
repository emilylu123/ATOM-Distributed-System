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
import java.util.*;

/*todo check replicated feeds (by feed id)
* todo error codes - empty xml malformed xml
* */
public class AggregationServer extends Thread{
    private volatile static int logical_clock = 0;
    protected static ServerSocket serverSocket;
    private static Socket atomSocket;
    private static int portNumber;
    private static LinkedList<Feed> feedList = new LinkedList<Feed>();
    private static LinkedList<String> activeContentServers = new LinkedList<String>();

    public AggregationServer (int port){
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("==============================");
            System.out.println("ATOM Server is ready! ");
            System.out.println("Address: "+ serverSocket.getLocalSocketAddress());
            System.out.println("==============================");

            // always check local backupFile (backup.txt) to recover feeds when start ATOM server
            recoverFeeds();
            // start heart beat timer for disconnecting expired feeds (12s)
            timer();

//            atomSocket = serverSocket.accept();
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
                new Thread(atom).start();
            }
        }catch (IOException e) {
                e.printStackTrace();
        }
    }

    @Override
    public synchronized void run (){
        try {
            DataInputStream inATOM = new DataInputStream(atomSocket.getInputStream());
            DataOutputStream outATOM = new DataOutputStream(atomSocket.getOutputStream());

            String receiveMSG = inATOM.readUTF();
            String[] arr = receiveMSG.split("@");
            String inMSG = arr[0];
            int timestamp = Integer.parseInt(arr[1]);

            // update Lamport timestamp by max + 1
            updateLamport(timestamp);

            // read keyword to process PUT and GET requests.
            String keyword = inMSG.substring(0,3);
            // PUT request from Content server, update heartbeat test and process news feeds, return status code
            if (keyword.equals("PUT")){
                String contentServerID = "";
                if (arr.length>2) contentServerID = arr[2];

                String outMSG = processPUT(inMSG,contentServerID);
                outATOM.writeUTF(outMSG);
                System.out.println("ATOM:: Send Status Code to Content Server: " + outMSG);
            }

            // GET request from Client, read backup File and send out active aggregation feeds message
            else if (keyword.equals("GET")){
                String outMSG = processGET();
                outATOM.writeUTF(outMSG);
            }
            // Process illegal request by sending error status code 400
            else {
                System.out.println("ATOM:: [400] Request Error. Please try again.");
                outATOM.writeUTF("400");
            }

            // update local time by +1
            incrementLamport();

            System.out.println("\nATOM:: Finish all processes. Wait for next request.");
            System.out.println("= = = = = = = = = = = = = = = = = = = = = = = =\n");
        } catch(SocketTimeoutException s) {
            System.out.println("ATOM:: Socket timed out!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String processPUT(String inMSG, String contentServerID){
        System.out.println("\n*********************************************");
        System.out.println("ATOM:: Receive [PUT] from ContentServer: [" + contentServerID + "]");
        System.out.println("ATOM:: Update Lamport TimeStamp: " + logical_clock  );
        System.out.println("*********************************************\n");

        // update this content server's feed timer to zero
        resetFeedTimer(contentServerID);

        // aggregate new feed with id
        aggregationFeeds(inMSG, contentServerID);

        // save news feeds to local file
        backupFeeds();

        // return correct status code & timestamp to content server
        int statusCode = returnStatusCode(contentServerID);

        String outMSG = statusCode + "@" + logical_clock;

        return outMSG;
    }

    private void resetFeedTimer(String contentServerID){
        System.out.println("ATOM:: Reset Feeds timer to zero for " + contentServerID);
        for (int i = 0; i < feedList.size(); i++) {
            if (feedList.get(i).getSource().equals(contentServerID))  {
                feedList.get(i).setTimer(0);
            }
        }
    }

    // get feeds from backup file
    protected String processGET(){
        System.out.println("\n****************************************");
        System.out.println("ATOM:: Receive [GET] from Client Application");
        System.out.println("****************************************\n");

        // read feeds from local backup.txt file
        String readBackup = "";
        BufferedReader reader;
        try{
            File backup = new File ("backup.txt");
            if (backup.exists()){
                reader = new BufferedReader(new FileReader(backup));
                String line = reader.readLine();
                do{
                    readBackup += line + "\n";
                    line = reader.readLine();
                }  while (line != null);
                reader.close();
                readBackup += "@" + logical_clock;
                System.out.println("ATOM:: Send News Feeds to Client.\nATOM:: Update Lamport TimeStamp: " + logical_clock);
            } else {
                System.out.println("ATOM:: Error! Backup file is not available");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return readBackup;
    }

    protected void timer() {
        System.out.println("ATOM:: Start timer...");
        new Thread(){
            @Override
            public void run() {
                try {
                    while (true){
                        Thread.sleep(1000);

                        if(!feedList.isEmpty()){
                            for (Feed f: feedList) {
                                f.setTimer(f.getTimer()+1);
                            }
                            System.out.print("ATOM:: Timer Test: "+ feedList.getFirst().getTimer()  + "  ATOM:: Total feeds number: " + feedList.size());
                            String active = "";
                            for (int i = 0; i < activeContentServers.size(); i++) {
                                active += activeContentServers.get(i) + " ";
                            }
                            System.out.println(" ATOM:: ContentServer ID: " + active);
                            updateFeeds();
                        }

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void incrementLamport (){
        logical_clock++;
        System.out.println("ATOM:: Increment timestamp: " + logical_clock + "\n");
    }

    private void updateLamport (int timestamp){
        logical_clock = Math.max(logical_clock,timestamp) + 1;
        System.out.println("ATOM:: Update timestamp: " + logical_clock);
    }

    // recover from backup file and create Feed object  w/ content ID
    protected void recoverFeeds() {
        String pathname = "backup.txt";
        File backupFile = new File(pathname);
        feedList = new LinkedList<Feed>();

        if (!backupFile.exists()){
            System.out.println("ATOM:: First time start ATOM SERVER. Create empty backup.txt file...");
            try {
                backupFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (backupFile.exists() && backupFile.length()==0) {
            System.out.println("ATOM:: Recovering... Backup File is EMPTY.");
        } else if(backupFile.exists() && backupFile.length()!=0){
            System.out.println("ATOM:: Recovering feeds from backup file...");
            String recovery = "";
            BufferedReader reader;
            try{
                reader = new BufferedReader(new FileReader(backupFile));
                String line = reader.readLine();
                do{
                    recovery += line + "\n";
                    line = reader.readLine();
                }  while (line != null);
                reader.close();
            } catch (IOException e){
                e.printStackTrace();
            }

            String [] arr = recovery.split("^^^^^");

            for (int i = 0; i < arr.length; i++) {
                String [] tmp = arr[i].split("~~");
                String content = tmp[0];
                String id = tmp[1];
                Feed aFeed = new Feed(content,id);
//                System.out.println("Content: \n" +content);
//                System.out.println("Source ContentServer ID: " + id);

                feedList.add(aFeed);
            }
            System.out.println("ATOM:: Recovery feeds number: " + feedList.size());
        }
    }

    // aggregate new feed to finalFeeds, and update to latest 20 and within 12s
    private void aggregationFeeds(String inMSG, String id){
        System.out.println("ATOM:: Aggregate new feed ");
        String content = "<" + inMSG.split("<",2)[1];
        Feed aFeed = new Feed(content,id);
        feedList.add(aFeed);

        // update to latest 20
        if(feedList.size() > 20) feedList.removeFirst();
    }

    // update finalFeeds to local file
    private void updateFeeds (){
        boolean isUpdated = false;
        for (int i = 0; i < feedList.size(); i++) {
            if (feedList.get(i).getTimer() >= 12) { // ?? >=
                String removeID = feedList.get(i).getSource();
                feedList.remove(i);
                activeContentServers.remove(removeID);
                System.out.println("\nATOM:: Update & Remove all Content from Server ID [" + removeID + "]");
                isUpdated = true;
            }
        }
        // if remove feeds, backup latest feeds to local file
        if(isUpdated)  backupFeeds();
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
        System.out.println("ATOM:: Backup feeds to local file backup.txt\n");
        String latestFeeds = "";
        for (int i = 0; i < feedList.size(); i++) {
            latestFeeds = latestFeeds + feedList.get(i).getContent() + "~~"+ feedList.get(i).getSource() +"^^^^^\n";
        }

        String fileName = "backup.txt";
        FileWriter writer;
        try {
            writer = new FileWriter(fileName);
            writer.write(latestFeeds);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}