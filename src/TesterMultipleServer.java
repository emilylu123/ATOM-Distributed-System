//==============================================================
// Distributed System
// Name: Yingyao Lu
// ID: a1784870
// Semester: S2
// Year: 2020
// Assignment2: ATOM feeds aggregation and distribution system
//==============================================================
import java.io.*;

// Auto testing single client application
public class TesterMultipleServer implements Runnable {

    private static final int portNumber = 4567; // by default
    private static final String serverName = "0.0.0.0";
    static Thread content1, content2, client1, client2, client3;

    public static void main(String[] args) throws IOException, InterruptedException {
        // create new TesterSingleServer object
        TesterMultipleServer tms = new TesterMultipleServer();

        // First time run test will delete local backup files
        deleteFile();
        // create multiple threads for running content server and GETclient applications
        createThreads();

        Thread test = new Thread(tms);

        // test begins
        try {
            // 1. start content server 1 when ATOM server is offline, it will reconnect 5 times
            System.out.println("\n>> TEST 1 :: Start Content Server (ID:12) & send PUT request\n>> Expect:: Connection Failure occurred and Reconnect\n");
            content1.start();
            Thread.sleep(2750);

            // 2.Get client 1 before start ATOM server, reconnect 5 times
            System.out.println("\n>> TEST 2 :: Start GETclient 1 & send GET request\n>> Expect:: Connection Failure Occurred and Reconnect\n");
            client1.start();
            Thread.sleep(1500);

            // Start ATOM Server,
            System.out.println("\n>> TEST 3 :: Start ATOM Server\n>> Expect:: Connection Successful and process requests\n>> Expect:: GETclient 1 receives empty feed. GETclient 2 receives 1 feed from Content Server (ID:12).\n");
            test.start();
            Thread.sleep(5000);

            // PUT from content server 1 again
            System.out.println("\n>> TEST 4 ::Start Content Server (ID:12) & send PUT request again\n>> Expect:: PUT successful and keep its feeds alive\n");
            content2.start();

            // GET from client 2
            System.out.println("\n>> TEST 5 :: Start GETclient 2 & send GET request\n>> Expect:: Receive feeds successful\n");
            Thread.sleep(5000);
            client2.start();

            // 12s later will delete content feeds
            System.out.println("\n>> TEST 6 :: HeartBeat test \n>> Expect:: Atom server expired feeds works (12s)\n");
            Thread.sleep(12000);

            // Get from client 3, xml should be empty
            System.out.println("\n>> TEST 7 :: GET from client 3\n>> Expect:: Receive 0 feeds from ATOM");
            client3.start();
            Thread.sleep(2000);

            System.out.println("\n>> All TESTS are finished \n>> Expect:: Waiting for more requests");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        runCommand("java AggregationServer");
    }


    public static void createThreads(){
        // create a thread running as content server (id 12)
        content1 = new Thread(){
            @Override
            public void run(){
                runCommand ("java ContentServer 0.0.0.0:4567 input.txt feedXML.xml 12");
            }
        };

        // create a thread running as same content server (id 12)
        content2 = new Thread(){
            @Override
            public void run(){
                runCommand ("java ContentServer 0.0.0.0:4567 input.txt feedXML.xml 12");
            }
        };


        // create three thread running as client1,  client2 and client3
        client1 = new Thread(){
            @Override
            public void run(){
                runCommand ("java GETClient 0.0.0.0:4567");
            }
        };

        client2 = new Thread(){
            @Override
            public void run(){
                runCommand ("java GETClient 0.0.0.0:4567");
            }
        };

        client3 = new Thread(){
            @Override
            public void run(){
                runCommand ("java GETClient 0.0.0.0:4567");
            }
        };
    }

    // this function will run string commands like processing command line arguments in the terminal
    public static void runCommand(String command){
        Process aProcess;
        try {
            aProcess = Runtime.getRuntime().exec(command);
            InputStream in = aProcess.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String read;
            while((read = br.readLine())!= null){
                System.out.println(read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // delete local backup file named "backup.txt"
    public static void deleteFile(){
        try{
            System.out.println(">> Test Preparation: Delete local backup and XML feed files...\n");
            File b = new File("backup.txt");
            File f = new File("feedXml.xml");
            if(b.exists()) b.delete();
            if(f.exists())  f.delete();
        } catch (Exception e){
            System.out.println("Error in delete files");
            e.printStackTrace();
        }
    }
}
