
import java.io.*;
import java.nio.file.*;
import java.util.*;

import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.net.Socket;
import java.nio.ByteBuffer;




public class peerProcess {

    private static int peerId;
    private boolean hasFile;
    private BitSet bitfield;
    private List<PeerInfo> allPeers = new ArrayList<>();
    private List<Peer> connectedPeers = new ArrayList<>();
    private CommonConfig config;

    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public peerProcess(int peerId) {
        this.peerId = peerId;
    }


    public static void choke() {

    }

    public static void unchoke() {

    }

    public static void interested() {

    }

    public static void notInterested() {

    }

    public static void have(MessageUtil.Message message) {

    }

    public static void bitfield(MessageUtil.Message message) {

    }

    public static void request(MessageUtil.Message message) {

    }

    public static void piece(MessageUtil.Message message) {
        
    }

    //parses what type of message it is and makes a decision based on that
    public static void parseMessage(MessageUtil.Message message) {
        byte type = message.getType();

        // switch statement to parse type
        switch(type) {
            case MessageUtil.CHOKE -> choke();
            case MessageUtil.UNCHOKE -> unchoke();
            case MessageUtil.INTERESTED -> interested();
            case MessageUtil.NOT_INTERESTED -> notInterested();
            case MessageUtil.HAVE -> have(message);
            case MessageUtil.BITFIELD -> bitfield(message);
            case MessageUtil.REQUEST -> request(message);
            case MessageUtil.PIECE -> piece(message);
            default -> System.out.println("invalid type");
        }
    }

    public static void makeHave() {

    }

    public static void makeBitfield() {
        
    }

    public static void makeRequest() {
        
    }

    public static void makePiece() {
        
    }

    // Log Method for TCP Connection
    public static void logTCPConnection(int ID1, int ID2) {

        // Get the names of the log files
        String logFileName1 = "log_peer_" + ID1 + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage1 = currentTime + ": Peer " + ID1 + " makes a connection to Peer " + ID2 + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName1, logMessage1);
    }
    
    // Log Method for Change of Preferred Neighbors
    public static void logPreferredNeighborsChange(int peerID, List<Integer> preferredNeighbors) {
        // Get name of Log File
        String logFileName = "log_peer_" + peerID + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Build the log message with the list of preferred neighbors
        StringBuilder preferredNeighborList = new StringBuilder();
        for (int neighborID : preferredNeighbors) {
            preferredNeighborList.append(neighborID).append(",");
        }
        // Remove the trailing comma
        if (preferredNeighbors.size() > 0) {
            preferredNeighborList.setLength(preferredNeighborList.length() - 1);
        }

        String logMessage = currentTime + ": Peer " + peerID + " has the preferred neighbors " + preferredNeighborList + ".";

        // Log the message to the log file
        logToLogFile(logFileName, logMessage);
    }

    // Log Method for Change of optimistically preffered Neighbor
    public void logOptNeighborChange(int neighborID) {
        // Get name of Log File
        String logFileName = "log_peer_" + this.peerId + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage = currentTime + ": Peer " + this.peerId + " has the optimistically unchoked neighbor " + neighborID + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName, logMessage);
    }

    // Log Method for being unchoked
    public void logUnchoking(int neighborID) {
        // Get name of Log File
        String logFileName = "log_peer_" + this.peerId + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage = currentTime + ": Peer " + this.peerId + " is unchoked by " + neighborID + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName, logMessage);
    }


    // Log Method for being choked
    public void logChoking(int neighborID) {
        // Get name of Log File
        String logFileName = "log_peer_" + this.peerId + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage = currentTime + ": Peer " + this.peerId + " is choked by " + neighborID + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName, logMessage);
    }

    // Log Method for receiving interested message
    public void logInterested(int neighborID) {
        // Get name of Log File
        String logFileName = "log_peer_" + this.peerId + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage = currentTime + ": Peer " + this.peerId + " received the ‘interested’ message from " + neighborID + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName, logMessage);
    }




    // Method to write to the log files
    public static void logToLogFile(String fileName, String message) {
        // Specify the relative path for the log directory as an empty string since 
        // log files are in the same directory
        String logDirectory = "";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logDirectory + fileName, true))) {
            // Logs the message and a newline after it
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init() throws IOException {
        readConfigurations();
        initBitfield();

        if (peerId != allPeers.get(0).peerId) {
            connectToPreviousPeers();
        }

        listenForConnections();

        // Begin exchanging pieces here...

        // Wait for all peers to download the complete file, then terminate.
        waitForCompletion();
    }

    private void readConfigurations() throws IOException {
        config = new CommonConfig();
        allPeers = readPeerInfo("PeerInfo.cfg");
        for (PeerInfo peer : allPeers) {
            if (peer.peerId == this.peerId) {
                this.hasFile = peer.hasFile;
                break;
            }
        }
    }


        //reads peerinformation from PeerInfo.cfg
        private List<PeerInfo> readPeerInfo(String filename) throws IOException {
            List<PeerInfo> peers = new ArrayList<>();
            List<String> lines = Files.readAllLines(Paths.get(filename));
    
            for (String line : lines) {
                String[] parts = line.split(" ");
                int id = Integer.parseInt(parts[0].trim());
                String hostName = parts[1].trim();
                int port = Integer.parseInt(parts[2].trim());
                boolean hasFile = parts[3].trim().equals("1");
    
                peers.add(new PeerInfo(id, hostName, port, hasFile));
            }
    
            return peers;
        }
        

    // makes bitfield for the file
    private void initBitfield() {
        int numOfPieces = (int) Math.ceil((double) config.getFileSize() / config.getPieceSize());
        this.bitfield = new BitSet(numOfPieces);
        if (hasFile) {
            bitfield.set(0, numOfPieces);
        }
    }

    // connects to any already made peers
    private void connectToPreviousPeers() {
        for (PeerInfo info : allPeers) {
            if (info.peerId >= this.peerId) {
                break;
            }
            try {
                Peer peer = new Peer(info);
                peer.connectTo(info);
                performHandshake(peer);
                connectedPeers.add(peer);
            } catch (IOException e) {
                System.out.println("Error connecting to peer " + info.peerId + ": " + e.getMessage());
            }
        }
    }

    // performs the handshake to start TCP connection
    private void performHandshake(Peer peer) throws IOException {
        MessageUtil.sendHandshake(dataOut, this.peerId);

        int receivedPeerId = MessageUtil.receiveHandshake(dataIn);
        if (receivedPeerId != peer.getInfo().peerId) {
            throw new IOException("Mismatched Peer ID in Handshake");
        }

        logTCPConnection(this.peerId, receivedPeerId);
        // logs that the connection occured

        System.out.println("Handshake successful with Peer " + receivedPeerId);
    }

    // listens for any new connections 
    private void listenForConnections() {
        Thread listenerThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(allPeers.get(0).port)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    this.dataIn = new DataInputStream(socket.getInputStream());
                    this.dataOut = new DataOutputStream(socket.getOutputStream());

                    int receivedPeerId = MessageUtil.receiveHandshake(dataIn);
                    
                    // You might want to check if this peer ID is in your expected list of peers.
                    
                    MessageUtil.sendHandshake(dataOut, this.peerId);
                    System.out.println("Handshake successful with Peer " + receivedPeerId);
                    // Handle the new connection (like adding to connectedPeers, etc.)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenerThread.start();
    }

    private void waitForCompletion() {
        //implement this based on how we handle the exchange of pieces and keeping track 
        // of which peers have the complete file.
    }

    public static void main(String[] args) {
         
        if (args.length != 1) {
            System.out.println("Usage: java peerProcess <peerID>");
            return;
        }

        System.out.println("Made it to main");

        int peerId = Integer.parseInt(args[0]);
        peerProcess process = new peerProcess(peerId);
        CommonConfig config = new CommonConfig();
        try {
            process.init();
            
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* HOW TO CALL logPreferredNeighborsChange EXAMPLE
        int peerID = 1001;
        List<Integer> preferredNeighbors = List.of(1002, 1003, 1004);
        logPreferredNeighborsChange(peerID, preferredNeighbors);
        */
    }
}

