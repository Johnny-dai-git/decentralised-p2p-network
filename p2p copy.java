/**
 *  Libaray I am using
 *
 */

import javax.lang.model.element.NestingKind;
import javax.print.DocFlavor;
import java.io.*;
import java.net.*;
import java.util.*;
import java.io.IOException;
import java.net.InetAddress;
import java.lang.String;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *  Main function
 */

public class p2p <args> {

    /**
     *  Define sharable parameters and data structure bewteen threads
     */
    static String localIP = new String();
    static int udpPort = 0;
    static InetAddress connectIP = null;
    static int connectPort = 0;
    static int rangeLowerBound = 0;
    static int rangeUpperBound = 0;
    static int welcomeSokcetPortQuery = 0;
    static int welcomSokcetPortFile = 0;
    static String configfilePath = new String();
    static String shareFilePath = new String();
    static int queryID = 0;
    static String getfile = new String();
    static String command = new String();
    static int numberOfGets = 0;
    static int numberOfconnect = 0;
    static ConcurrentLinkedQueue<String> floodingQuery = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<String> floodedQuery = new ConcurrentLinkedQueue<String>();
    static ConcurrentHashMap<Integer, Socket> traceBack = new ConcurrentHashMap<Integer, Socket>();
    static ConcurrentLinkedQueue<String> listOfFileResponse = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<String> traceBackQuery = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<String> traceBackedQuery = new ConcurrentLinkedQueue<String>();
    static ConcurrentHashMap<InetAddress, Socket> querySocketHashMmap = new ConcurrentHashMap<InetAddress, Socket>();
    static ConcurrentLinkedQueue<String> traceQuery = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<String> listOfNeiboughours = new ConcurrentLinkedQueue<String>();
    static ConcurrentLinkedQueue<Integer> traceBakcedResponsed = new ConcurrentLinkedQueue<Integer>();
    static ConcurrentLinkedQueue<Socket> threadedSocket = new ConcurrentLinkedQueue<>();
    static ConcurrentLinkedQueue<String> fileToShare = new ConcurrentLinkedQueue<String>();

    public static void main (String[] args) throws IOException, InterruptedException {

        System.out.println("Peer start \n");
        System.out.println("\n");

        configfilePath = "/home/yxd429/p2p/config_peer.txt";
        shareFilePath = "/home/yxd429/p2p/config_sharing.txt";
        BufferedReader readline = new BufferedReader(new FileReader(configfilePath));
        String line = new String();
        String hostName = " ";
        //Get and assigned all the required port numbers//

        System.out.println("Get the required parameters and setup global variables\n");
        System.out.println("\n");

        /**
         *  Get all datas from config_peer.txt files
         */
        while (null != (line = readline.readLine())) {
            if (line.contains("UDP")) {
                String[] list = line.split(" ", 2);
                udpPort = Integer.parseInt(list[1]);
            } else if (line.contains("QUERY")) {
                String[] list = line.split(" ", 2);
                welcomeSokcetPortQuery = Integer.parseInt(list[1]);
            } else if (line.contains("FILE")) {
                String[] list = line.split(" ", 2);
                welcomSokcetPortFile = Integer.parseInt(list[1]);
            } else if (line.contains("HOST")) {
                String[] list = line.split(" ", 2);
                hostName = list[1];
                String[] list1 = hostName.split("-", 2);
                String[] list2 = list1[1].split(".case.", 2);
                rangeLowerBound = Integer.parseInt(list2[0]) % 10 * 1000;
                rangeUpperBound = ((Integer.parseInt(list2[0]) + 1) % 10) * 1000;
            }
        }


        queryID = rangeLowerBound;
        File sharingFile = new File(shareFilePath);
        Scanner sharingReader = new Scanner(sharingFile);
        String temp_line = sharingReader.next();

        /**
         * Get all file name can be transfered from config_sharing file
         */

        while (temp_line.length() > 0 && sharingReader.hasNext()) {
            p2p.fileToShare.add(temp_line);
            temp_line = sharingReader.next();
        }

        System.out.println("\n");

        System.out.println("All files can be shared \n");

        System.out.println("\n");
        for (String s : p2p.fileToShare) {
            System.out.println(s);
        }
        String[] ip;
        ip = InetAddress.getByName(hostName).toString().split("/", 2);
        localIP = ip[1];
        System.out.println("localIP address " + localIP);
        DatagramSocket serverSocket = new DatagramSocket(udpPort);


        /**
         *  Start all th threads
         *
         */

        UDPThread upd = new UDPThread(serverSocket);  // UDP server  //
        upd.start();                                  // UDP client //
        UDPsend udPsend = new UDPsend(serverSocket);
        udPsend.start();
        queryServerSocket queryServerSocket = new queryServerSocket();  // query server
        queryServerSocket.start();
        queryClientSocket queryClientSocket = new queryClientSocket();  // query client
        queryClientSocket.start();
        fileServerSocket fileServerSocket = new fileServerSocket();     // file server
        fileServerSocket.start();
        fileReceiverConnection fileReceiverConnection = new fileReceiverConnection();   //file client
        fileReceiverConnection.start();
        centralManagementControl centralManagementControl;     // responsible for query flooding
        centralManagementControl = new centralManagementControl();
        centralManagementControl.start();

        Scanner userInput = new Scanner(System.in);
        String input;


        /**
         *  Monitor user input
         */


        while (!command.equals("Exit")) {

            if (userInput.hasNext()) {
                input = userInput.nextLine();
                if (input.contains("Get")) {
                    String[] list = input.split(" ", 2);
                    command = list[0];
                    getfile = list[1];
                    numberOfGets++;
                } else if (input.contains("Leave")) {
                    System.out.println(("Leave Command and all connection closed!\n"));
                    command = "Leave";
                } else if (input.contains("Exit")) {
                    System.out.println("The peer closes all connections and terminates");
                    command = "Exit";
                    System.exit(0);
                } else if (input.contains("Connect")) {
                    String[] list = input.split(" ", 3);
                    command = list[0];
                    String[] ipStr = list[1].split("\\.");
                    byte[] ipBuf = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        ipBuf[i] = (byte) (Integer.parseInt(ipStr[i]) & 0xff);
                    }
                    connectIP = InetAddress.getByAddress(ipBuf);
                    connectPort = udpPort;
                    ipBuf = new byte[4];
                    numberOfconnect++;
                }
            }
        }
    }

    /**
     * FileTCP connection serverSocket
     * Accept file connection and transfer file
     */
    public static class fileServerSocket extends Thread {

        private ServerSocket welcomeSocketFile;
        private Socket connectionSocket;

        public fileServerSocket ( ) {
            this.welcomeSocketFile = null;
            this.connectionSocket = null;
        }

        public void run ( ) {
            try {

                welcomeSocketFile = new ServerSocket(welcomSokcetPortFile);
                while (!p2p.command.contains("Exit") && !p2p.command.contains("Leave")) {
                    Socket connectionSocket = welcomeSocketFile.accept();
                    if (connectionSocket.isConnected()) {
                        System.out.println("Socket is connected\n");
                        if (p2p.command.contains("Leave")) {
                            connectionSocket.close();
                            System.out.println(" file sockt have been closed \n");
                        }
                        InputStream is = connectionSocket.getInputStream();
                        InputStreamReader isr = new InputStreamReader(is);
                        BufferedReader br = new BufferedReader(isr);
                        String line1 = br.readLine();
                        System.out.println("File received message \n");
                        if (line1.contains("T:")) {
                            System.out.println("Received file request message " + line1);
                            System.out.println("Start sending files \n");
                            OutputStream outputStream = connectionSocket.getOutputStream();
                            PrintWriter printWriter = new PrintWriter(outputStream);
                            String[] list = line1.split(":", 2);
                            String filename = list[1];
                            BufferedReader readline = new BufferedReader(new FileReader("/home/yxd429/p2p/shared/" + filename));
                            System.out.println("Reading file from local share directory\n");
                            String line;
                            while (null != (line = readline.readLine())) {
                                printWriter.write(line);
                            }
                            System.out.println("Finished file transfer to " + connectionSocket.getInetAddress().toString());
                            br.close();
                            printWriter.close();
                            connectionSocket.close();
                        }
                    }
                }
                welcomeSocketFile.close();
                System.out.println("File serverSocket is closed and thread terminated\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Build up file transmission socket and received file
     */

    public static class fileReceiverConnection extends Thread {
        private Socket connectionSokcet;

        public fileReceiverConnection ( ) {
        }

        public void run ( ) {
            try {
                while (!p2p.command.equals("Exit")) {
                    if (p2p.listOfFileResponse.size() > 0) {
                        String message = p2p.listOfFileResponse.poll();
                        String[] temp = message.split(";", 3);
                        String[] IPandPort = temp[1].split(":", 2);
                        String IP = IPandPort[0];
                        String Port = IPandPort[1];
                        String fileName = temp[2];
                        String[] queryIdtemp = temp[0].split(":", 2);
                        Integer queryID = Integer.parseInt(queryIdtemp[1]);


                        InetSocketAddress address = new InetSocketAddress(IP, Integer.parseInt(Port));
                        connectionSokcet = new Socket();
                        connectionSokcet.setSoTimeout(100000000);
                        connectionSokcet.connect(address, 2000);
                        System.out.println("\n");
                        System.out.println("Set heartbeat for file transfer socket(Using set so alive function )\n");
                        connectionSokcet.setKeepAlive(true);
                        FileWriter fw = new FileWriter("/home/yxd429/p2p/obtained/" + fileName, true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        System.out.println("\n");
                        System.out.println("Create file writer and write file in /home/yxd429/p2p/obtained" + fileName);


                        if (p2p.command == "Leave") {
                            fw.close();
                            bw.close();
                            connectionSokcet.close();
                        }

                        /** If the TCP connection is complete then we need to send request and receieved file */
                        if (connectionSokcet.isConnected()) {
                            //Send request//
                            Thread.sleep(100);
                            System.out.println("\n");
                            System.out.println("File transaction TCP have been established to" + IP);
                            OutputStream outputStream = connectionSokcet.getOutputStream();
                            PrintWriter printWriter = new PrintWriter(outputStream);
                            String fileRequestMessage = "T:" + fileName + '\n';
                            printWriter.write(fileRequestMessage);
                            printWriter.flush();
                            System.out.println("\n");
                            System.out.println("Send out file requested message : " + fileRequestMessage);
                            Thread.sleep(20);
                            //Received requested file //
                            InputStream is = connectionSokcet.getInputStream();
                            InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader br = new BufferedReader(isr);
                            // Get the file descriptor//
                            String line1 = new String();
                            while (connectionSokcet.isConnected()) {
                                line1 = br.readLine();
                                if (line1 == null) {
                                    break;
                                }
                                bw.write(line1);
                            }
                            System.out.println("\n");
                            System.out.println("FIle transfer finished \n");
                        }
                        bw.close();
                        fw.close();
                        connectionSokcet.close();
                        System.out.println("\n");
                        System.out.println("File transfer socket closed \n");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** queryTCP connection **/
    /**
     * Central management Of Socket Thread //
     * Central control of all the Query Socket including query flooding, prevent duplicates reponse, traceback
     */

    public static class centralManagementControl extends Thread {

        private int Gets;

        public centralManagementControl ( ) {
            this.Gets = 0;
        }

        public void run ( ) {
            try {

                byte[] sendData = new byte[65535];
                while (!p2p.command.equals("Exit")) {
                    if (p2p.threadedSocket.size() != p2p.querySocketHashMmap.size()) {
                        String[] ip;
                        for (InetAddress key : p2p.querySocketHashMmap.keySet()) {
                            if (!p2p.threadedSocket.contains(p2p.querySocketHashMmap.get(key))) {
                                listenThread listenThread = new listenThread(p2p.querySocketHashMmap.get(key));
                                listenThread.start();
                                p2p.threadedSocket.add(p2p.querySocketHashMmap.get(key));
                                //System.out.println("Size of writer " + hashOfPrintWrite.size());
                                //System.out.println("Size of reader " + hashOfBufferedReader.size());
                            }
                        }
                    }

                    /**
                     *  Query flooding
                     */
                    if (p2p.command.equals("Get") && (p2p.numberOfGets > Gets)) {
                        BufferedReader readline1 = new BufferedReader(new FileReader(p2p.shareFilePath));
                        boolean found = false;
                        for (String s : fileToShare) {
                            if (s.equals(getfile)) {
                                found = true;
                                System.out.println("\n");
                                System.out.println("Found the file within local dir\n");
                                break;
                            }
                        }
                        if (!found) {
                            if (p2p.queryID < p2p.rangeUpperBound) {
                                p2p.queryID++;
                            }
                            String query = "Q:" + Integer.toString(p2p.queryID) + ";" + p2p.getfile + '\n';
                            if (!compare(p2p.floodedQuery, query) && !compare(p2p.floodingQuery, query)) {
                                p2p.floodingQuery.add(query);
                                System.out.println("\n");
                                System.out.println("Create Query message  " + query);
                            }
                        }
                        Gets = p2p.numberOfGets;
                    }
                    /**
                     * Query flooding
                     */
                    if (!p2p.floodingQuery.isEmpty()) {
                        String floodingQueryMessage = p2p.floodingQuery.poll();
                        //Compare queryID//
                        if (!compare(p2p.floodedQuery, floodingQueryMessage)) {
                            for (InetAddress key : p2p.querySocketHashMmap.keySet()) {
                                OutputStream out = p2p.querySocketHashMmap.get(key).getOutputStream();
                                DataOutputStream output = new DataOutputStream(out);
                                floodingQueryMessage = floodingQueryMessage + '\n';
                                output.writeBytes(floodingQueryMessage);
                                System.out.println("\n");
                                System.out.println("Query flooding  Query flooding  Query flooding  Query flooding  Query flooding  Query flooding  Query flooding  Query flooding\n ");
                                System.out.println("Query flooding to " + floodingQueryMessage);
                            }
                            p2p.floodingQuery.remove(floodingQueryMessage);
                            p2p.floodedQuery.add(floodingQueryMessage);
                        }
                    }
                    /**
                     *  traceback
                     */

                    if (p2p.traceBackQuery.size() > 0) {
                        String responseMessage = p2p.traceBackQuery.poll();
                        System.out.println("\n");
                        System.out.println("traceBack step 0 get message :"  + responseMessage);
                        String[] temp = responseMessage.split(";", 3);
                        String[] queryID = temp[0].split(":",2);
                        String ID = queryID[1];
                        Integer id = Integer.parseInt(ID);
                        System.out.println("\n");
                        System.out.println("traceBack Query ID : " + id);
                        if (p2p.traceBack.get(id) != null) {
                            System.out.println("\n");
                            System.out.println("tracing back step one ");
                            OutputStream out = p2p.traceBack.get(id).getOutputStream();
                            DataOutputStream output = new DataOutputStream(out);
                            responseMessage = responseMessage + '\n';
                            System.out.println("\n");
                            System.out.println("traceBack message " + responseMessage);
                            System.out.println("Tracing back step two to IP " + p2p.traceBack.get(id).getInetAddress().toString());
                            output.writeBytes(responseMessage);
                            p2p.traceBakcedResponsed.add(id);
                        }
                    }
                }
            } catch (IOException e) {
                    e.printStackTrace();
            }
        }
    }

    /**
     * Helper function
     */
    public static boolean compare (ConcurrentLinkedQueue<String> Query, String floodingQuery) {
        for (String s : Query) {
            if (s.contains(floodingQuery)) {
                return true;
            }
        }
        return false;
    }


    /**
     * ListenThread received Q and R from other peers
     **/

    public static class listenThread extends Thread {

        private Socket connectedSocket;


        public listenThread (Socket connectedSocket) {
            this.connectedSocket = connectedSocket;
        }

        public void run ( ) {
            try {
                System.out.println("\n");
                System.out.println("New thread for Query message listening from IP  " + connectedSocket.getInetAddress().toString());
                File shareFile = new File(shareFilePath);
                byte[] read = new byte[300];
                int bytesRead = 0;
                byte[] send = new byte[300];
                String[] fileName;
                String filename;
                String queryID;
                String[] temp;
                while (!p2p.command.equals("Exit")) {
                    InputStream in = connectedSocket.getInputStream();
                    BufferedReader input = new BufferedReader(new InputStreamReader(in));
                    String message = input.readLine();
                    read = new byte[300];
                    if (message == null) {
                        continue;
                    }
                    System.out.println("\n");
                    System.out.println("Received message from listern thread " + message);
                    if (message.contains("Q:")) {
                        temp = message.split(":", 2);
                        fileName = temp[1].split(";", 2);
                        filename = fileName[1];
                        queryID = fileName[0];
                        Integer id = Integer.parseInt(queryID);
                        System.out.println("\n");
                        System.out.println("Received Query message " + message);
                        if (!compare(floodedQuery, message) && !compare(floodedQuery, message)) {
                            // Open the share.txt files to check the information is here ot not//
                            BufferedReader readline = new BufferedReader(new FileReader(shareFile));
                            boolean found = false;
                            if (p2p.fileToShare.contains(filename)) {
                                //Ifound the message locally then send back a R Message//
                                //          System.out.println(" Found the requested file " + filename );
                                String responseMessage = "R:" + queryID + ";" + localIP.toString() + ":" + welcomSokcetPortFile + ";" + filename + '\n';
                                //       System.out.println("Reply query response message " + responseMessage + " \n ");
                                OutputStream out = connectedSocket.getOutputStream();
                                DataOutputStream output = new DataOutputStream(connectedSocket.getOutputStream());
                                responseMessage = responseMessage;
                                System.out.println("\n");
                                System.out.println("Response message " + responseMessage);
                                output.writeBytes(responseMessage);
                                floodedQuery.add(message);
                                found = true;
                            }
                            if (!found) {
                                floodingQuery.add(message);
                                traceBack.put(id, connectedSocket);
                                System.out.println("Can not found the request files will do query flooding in the following actions\n");
                                System.out.println("\n");
                                System.out.println("Adding flooding query " + message);
                                System.out.println("Mapping queryID  " + id + "to socket :" + connectedSocket.getInetAddress().toString());
                            }
                        }
                    } else if (message.contains("R:")) {
                        System.out.println("\n");
                        System.out.println("Received R type message ");

                        fileName = message.split(";", 3);
                        filename = fileName[2];
                        temp = fileName[0].split(":",2);
                        queryID = temp[1];
                        System.out.println("\n");
                        System.out.println("Receive response message " + message);
                        System.out.println("\n");
                        System.out.println("file name " + filename + " queryID " + queryID);
                        if (p2p.rangeLowerBound <= Integer.parseInt(queryID) && Integer.parseInt(queryID) <= p2p.rangeUpperBound) {
                            boolean found = false;
                            for (String s : p2p.listOfFileResponse){
                                if(s.contains(message)){
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) {
                                p2p.listOfFileResponse.add(message);
                                System.out.println("\n");
                                System.out.println("Keep track of found files " + message);
                            }
                        }else{
                            if(!compare(p2p.traceBackQuery,message) && !compare(p2p.traceBackedQuery,message)) {
                                p2p.traceBackQuery.add(message);
                                System.out.println("\n");
                                System.out.println("Keep track of list " + message);
                                System.out.println("Size of traceBackQuery" + traceBackQuery.size());
                            }
                        }
                    }
                    message = new String();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Query Client Socket : build up Query TCP connection to other peers
     */

    public static class queryClientSocket extends Thread {

        private Socket NeiboughoursSocket;
        private long endtime;

        public queryClientSocket () {
            this.NeiboughoursSocket = null;
            this.endtime=0;

        }

        public void run ( ) {
            try {
                while (!p2p.command.equals("Exit")) {
                    if (p2p.command.equals("Leave")) {
                        //Close all the TCP connection//
                        for (InetAddress key : p2p.querySocketHashMmap.keySet()) {
                            synchronized (p2p.querySocketHashMmap) {
                                Socket Socket = p2p.querySocketHashMmap.get(key);
                                Socket.close();
                                System.out.println("\n");
                                System.out.println("Socket closed to :" + p2p.querySocketHashMmap.get(key)) ;
                            }
                        }
                    }
                    if (p2p.listOfNeiboughours.size() > 0) {
                        for (String s : p2p.listOfNeiboughours) {
                            String[] inforOfNeiboughours = s.toString().split(":", 3);
                            String Ip = inforOfNeiboughours[1];
                            if (!p2p.querySocketHashMmap.containsKey(InetAddress.getByName(Ip))) {
                                // Create a new Socket contains the IP and port number//
                                NeiboughoursSocket = new Socket();
                                System.out.println("\n");
                                System.out.println("Send out new Socket connection requested \n ");
                                NeiboughoursSocket.connect(new InetSocketAddress(InetAddress.getByName(Ip), p2p.welcomeSokcetPortQuery), 100);
                                /**
                                 *  HeartBeat implementation using setkeepalive functions
                                 */
                                while(System.currentTimeMillis() > endtime){
                                    System.out.println("Connect is alive with IP " + NeiboughoursSocket.getInetAddress().toString());
                                    endtime = System.currentTimeMillis() +100000;
                                }

                                    if(NeiboughoursSocket.isConnected()){System.out.println("\n");
                                    System.out.println(" HeartBeat for Socket" + NeiboughoursSocket.getInetAddress().toString() +" is on ");
                                }
                                System.out.println("\n");
                                System.out.println("Successfulley buildup new connection to " + NeiboughoursSocket.getInetAddress().toString());
                                p2p.querySocketHashMmap.put(NeiboughoursSocket.getInetAddress(), NeiboughoursSocket);
                            }
                        }
                    }
                }
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    /**
     * ServerSocket of Query TCP connection: accepting TCP connection request from other peers
     */

    public static class queryServerSocket extends Thread {
        private ServerSocket welcomSocket;

        public queryServerSocket (){
            this.welcomSocket = null;
        }

        public void run ( ) {
            try {
                welcomSocket = new ServerSocket(welcomeSokcetPortQuery);
                while (!p2p.command.equals("Exit")) {
                    if (p2p.command.equals("Leave")) {
                        //Close all the TCP connection//
                        for (InetAddress key : querySocketHashMmap.keySet()) {
                            Socket Socket = querySocketHashMmap.get(key);
                            Socket.close();
                        }
                        System.out.println("\n");
                        System.out.println("All TCP socket have been closed \n");
                        //Close the welcomeSocket Connection//
                        welcomSocket.close();
                        System.out.println("\n");
                        System.out.println("TCP welcomeSocket haven been closed ");
                    }
                    boolean keepAlive = true;
                    Socket connectionSocket = welcomSocket.accept();
                    connectionSocket.setKeepAlive(keepAlive);

                    /**
                     *  HeartBeat implementation using setkeepalive functions
                     */
                    System.out.println("\n");
                    System.out.println(" HeartBeat for Socket" + connectionSocket.getInetAddress().toString() +" is on ");

                    if (!querySocketHashMmap.containsKey(connectionSocket.getInetAddress())) {
                        synchronized (querySocketHashMmap) {
                            System.out.println("Successfulley accept new connection to " + connectionSocket.getInetAddress().toString());
                            querySocketHashMmap.put(connectionSocket.getInetAddress(), connectionSocket);
                        }
                    }
                }
                welcomSocket.close();
                System.exit(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Send out UPD ping message  to other peers
     */

    public static class UDPsend extends Thread {
        private DatagramSocket serverSocket;
        private int connect;

        public UDPsend (DatagramSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.connect= 0;
        }

        public void run ( ) {
            try {
                byte[] sendData = new byte[65535];
                while (!p2p.command.equals("Exit")) {
                    Thread.sleep(10);
                    if (p2p.command.toString().equals("Connect") && (numberOfconnect > connect)) {
                        if (!connectIP.equals(localIP)) {
                            //Send out ping message to the destIP and destPort//System.out.println(" I am here!\n");
                            String ip = localIP.toString();
                            String ping = "PI:" + ip + ":" + Integer.toString(udpPort);
                            System.out.println("\n");
                            System.out.println("Sendout Ping message " + ping);
                            sendData = ping.getBytes();
                            DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, connectIP, connectPort);
                            serverSocket.send(sendPack);
                            connect++;
                        }
                        sendData = new byte[65535];
                    }

                }
                serverSocket.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Received UPD ping message and pong back
     */

    public static class UDPThread extends Thread {

        private DatagramSocket serverSocket;
        private LinkedList<String> listOfPing;

        public UDPThread (DatagramSocket serverSocket) {
            this.serverSocket = serverSocket;
            this.listOfPing = null;
        }

        public void run ( ) {
            try {
                //Create UDP serverSocket//
                //Create UDP data Received Message//
                LinkedList<String> listOfPing = new LinkedList<String>();
                //Infinite loop to monitor the input data//

                while (!p2p.command.contains( "Exit")) {
                    if(p2p.command.contains("Leave")){
                        serverSocket.close();
                        System.out.println("\n");
                        System.out.println("welcomeSocket for UPD has been closed \n ");
                    }
                    byte[] sendData = new byte[200];
                    byte[] receiveData = new byte[200];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    String message = new String(receivePacket.getData());
                    System.out.println(message);
                    String[] list = new String[3];
                    list = message.split(":", 3);


                    //Analyze Ping or Pong message//
                    if (list[0].toString().equals("PI")) {
                        if (!listOfPing.contains(message)) {
                            String pongMessage = "PO:" + localIP + ":" + welcomeSokcetPortQuery;
                            InetAddress senderIP = InetAddress.getByName(list[1]);
                            int senderPort = receivePacket.getPort();
                            sendData = pongMessage.getBytes();
                            DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, senderIP, senderPort);
                            serverSocket.send(sendPack);
                            System.out.println("\n");
                            System.out.println("Response Pong");
                            synchronized (listOfNeiboughours) {
                                System.out.println("\n");
                                System.out.println("Peers broadcasting Peers broadcasting Peers broadcasting Peers broadcasting Peers broadcasting Peers broadcasting Peers broadcasting Peers broadcasting \n");
                                for (String s : listOfNeiboughours) {
                                    System.out.println("\n");
                                    System.out.println("Forwarding message to " + s.toString());
                                    String[] listTemp = new String[3];
                                    listTemp = s.split(":", 3);
                                    InetAddress peerIP = InetAddress.getByName(listTemp[1]);
                                    int peerPort = udpPort;
                                    sendData = new byte[200];
                                    sendData = message.getBytes();
                                    DatagramPacket broadCastToPeers = new DatagramPacket(sendData, sendData.length, peerIP, peerPort);
                                    serverSocket.send(broadCastToPeers);
                                }
                            }
                        }
                        listOfPing.add(message);
                        System.out.println("\n");
                        System.out.println("Keep track of Ping message :" + message);
                    } else if (list[0].toString().equals("PO")) {
                        // Check the peers is int the listOfNeiboughoursd or not//
                        synchronized (listOfNeiboughours) {
                            if (!listOfNeiboughours.contains(message)) {
                                System.out.println("\n");
                                System.out.println("Keep track of neiboughours" + message);
                                listOfNeiboughours.add(message);
                            }
                        }
                    }
                    receiveData = new byte[200];
                }
                serverSocket.close();
                System.out.println("\n");
                System.out.println("Leave UDP Thread \n");
                System.exit(0);
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}







