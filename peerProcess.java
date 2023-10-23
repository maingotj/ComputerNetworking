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

    // Create handshake message
    public static byte[] makeHandshake() {
        byte[] header = "P2PFILESHARINGPROJ".getBytes();
        // required header of handshake
        byte[] bytes = "0000000000".getBytes();
        // 10 0 bytes

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(peerId);
        byte[] iDinfo = buffer.array();
        // makes peerID a byte value

        byte[] message = new byte[32];
        // byte array for actual message

        System.arraycopy(header, 0, message, 0, header.length);
        // add header to the message

        System.arraycopy(bytes, 0, message, 18, bytes.length);
        // add 0 bytes to message

        System.arraycopy(iDinfo, 0, message, 28, iDinfo.length);
        // add peer ID to finish header

        return message;
    }

    //reads handshake message
    public static int readHandshake(byte[] handshake) {
        byte[] header = Arrays.copyOfRange(handshake, 0, 18);
        //read header

        byte[] headerTest = "P2PFILESHARINGPROJ".getBytes();
        if(!Arrays.equals(header, headerTest)) {
            System.out.println("header not correct");
        }
        else {

        }
        // check that header is correct


        byte[] zerobytes = Arrays.copyOfRange(handshake, 18, 28);
        // read zero bytes

        byte[] bytes = "0000000000".getBytes();

        if(!Arrays.equals(zerobytes, bytes)) {
            System.out.println("header not correct");
        }
        else {
            
        }

        byte[] peerSender = Arrays.copyOfRange(handshake, 28, handshake.length);
        ByteBuffer peerBuffer = ByteBuffer.wrap(peerSender);
        int peerSenderID = peerBuffer.getInt();
        // read the Peer ID

        return peerSenderID;
        // return the peer senders ID as a parameter
    }



    // Log Method for TCP Connection
    public static void logTCPConnection(int ID1, int ID2) {

        // Get the names of the log files
        String logFileName1 = "log_peer_" + ID1 + ".log";
        String logFileName2 = "log_peer_" + ID2 + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Create log messages
        String logMessage1 = currentTime + ": Peer " + ID1 + " makes a connection to Peer " + ID2 + ".";
        String logMessage2 = currentTime + ": Peer " + ID2 + " is connected from Peer " + ID1 + ".";

        // Log the messages to the corresponding log files
        logToLogFile(logFileName1, logMessage1);
        logToLogFile(logFileName2, logMessage2);
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

        // ... [other methods]

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
    
        // ... [rest of the class]
    

    private void initBitfield() {
        int numOfPieces = (int) Math.ceil((double) config.getFileSize() / config.getPieceSize());
        this.bitfield = new BitSet(numOfPieces);
        if (hasFile) {
            bitfield.set(0, numOfPieces);
        }
    }

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

    private void performHandshake(Peer peer) throws IOException {
        MessageUtil.sendHandshake(dataOut, this.peerId);

        int receivedPeerId = MessageUtil.receiveHandshake(dataIn);
        if (receivedPeerId != peer.getInfo().peerId) {
            throw new IOException("Mismatched Peer ID in Handshake");
        }

        System.out.println("Handshake successful with Peer " + receivedPeerId);
    }

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

