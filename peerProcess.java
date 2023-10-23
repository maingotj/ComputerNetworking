import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.net.Socket;

public class peerProcess {

    private int peerId;
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

    /*  Log Method for TCP Connection
        Whenever a peer makes a TCP connection to other peer, it generates the following log:
        [Time]: Peer [peer_ID 1] makes a connection to Peer [peer_ID 2].

        [peer_ID 1] is the ID of peer who generates the log, [peer_ID 2] is the peer connected
        from [peer_ID 1]. The [Time] field represents the current time, which contains the date,
        hour, minute, and second. The format of [Time] is up to you.

        Whenever a peer is connected from another peer, it generates the following log:
        [Time]: Peer [peer_ID 1] is connected from Peer [peer_ID 2].

        [peer_ID 1] is the ID of peer who generates the log, [peer_ID 2] is the peer who has
        made TCP connection to [peer_ID 1].
    */
    public static void logTCPConnection(int peerID1, int peerID2) {
        // Get the names of the log files to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
        String logFileName2 = "log_peer_" + peerID2 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Create log messages
        String logMessage1 = currentTime + ": Peer " + peerID1 + " makes a connection to Peer " + peerID2 + ".";
        String logMessage2 = currentTime + ": Peer " + peerID2 + " is connected from Peer " + peerID1 + ".";
    
        // Log the messages to the corresponding log files
        logToLogFile(logFileName1, logMessage1);
        logToLogFile(logFileName2, logMessage2);
    }
    
    /*  Log Method for Change of Preferred Neighbors
        Whenever a peer changes its preferred neighbors, it generates the following log:
        [Time]: Peer [peer_ID] has the preferred neighbors [preferred neighbor ID list].

        [preferred neighbor list] is the list of peer IDs separated by comma ‘,’.

        Example Call:
        int peerID = 1001;
        List<Integer> preferredNeighbors = List.of(1002, 1003, 1004);
        logPreferredNeighborsChange(peerID, preferredNeighbors);
    */
    public static void logPreferredNeighborsChange(int peerID, List<Integer> preferredNeighbors) {
        // Get name of Log File to write to
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

    /*  Log Method for Change of Optimistically unchoked neighbor
        Whenever a peer changes its optimistically unchoked neighbor, it generates the following log:
        [Time]: Peer [peer_ID] has the optimistically unchoked neighbor [optimistically unchoked neighbor ID].

        [optimistically unchoked neighbor ID] is the peer ID of the optimistically unchoked
        neighbor.

        Example Call:
        int peerID = 1001;
        int optimisticallyUnchokedNeighbor = 1005;
        logOptimisticallyUnchokedNeighborChange(peerID, optimisticallyUnchokedNeighbor);
    */
    public static void logOptimisticallyUnchokedNeighborChange(int peerID, int optimisticallyUnchokedNeighbor) {
        // Get name of Log File to write to
        String logFileName = "log_peer_" + peerID + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID + " has the optimistically unchoked neighbor " + optimisticallyUnchokedNeighbor + ".";

        // Log the message to the log file
        logToLogFile(logFileName, logMessage);
    }

    /*  Log Method for Unchoking event
        Whenever a peer is unchoked by a neighbor (which means when a peer receives an
        unchoking message from a neighbor), it generates the following log:
        [Time]: Peer [peer_ID 1] is unchoked by [peer_ID 2].

        [peer_ID 1] represents the peer who is unchoked and [peer_ID 2] represents the peer
        who unchokes [peer_ID 1]
    */
    public static void logUnchokingEvent(int peerID1, int peerID2) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";

        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " is unchoked by " + peerID2 + ".";

        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for Choking event
        Whenever a peer is choked by a neighbor (which means when a peer receives a choking
        message from a neighbor), it generates the following log:
        [Time]: Peer [peer_ID 1] is choked by [peer_ID 2].

        [peer_ID 1] represents the peer who is choked and [peer_ID 2] represents the peer who
        chokes [peer_ID 1].
     */
    public static void logChokingEvent(int peerID1, int peerID2) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " is choked by " + peerID2 + ".";
    
        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for receiving 'have' Message
        Whenever a peer receives a ‘have’ message, it generates the following log:
        [Time]: Peer [peer_ID 1] received the ‘have’ message from [peer_ID 2] for the piece
        [piece index].

        [peer_ID 1] represents the peer who received the ‘have’ message and [peer_ID 2]
        represents the peer who sent the message. [piece index] is the piece index contained in
        the message.
     */
    public static void logHaveMessage(int peerID1, int peerID2, int pieceIndex) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " received the 'have' message from " + peerID2 + " for the piece " + pieceIndex + ".";
    
        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for receiving 'interested' message
        Whenever a peer receives an ‘interested’ message, it generates the following log:
        [Time]: Peer [peer_ID 1] received the ‘interested’ message from [peer_ID 2].

        [peer_ID 1] represents the peer who received the ‘interested’ message and [peer_ID 2]
        represents the peer who sent the message
     */
    public static void logInterestedMessage(int peerID1, int peerID2) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " received the 'interested' message from " + peerID2 + ".";
    
        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for receiving 'not interested' message
        Whenever a peer receives a ‘not interested’ message, it generates the following log:
        [Time]: Peer [peer_ID 1] received the ‘not interested’ message from [peer_ID 2].

        [peer_ID 1] represents the peer who received the ‘not interested’ message and [peer_ID
        2] represents the peer who sent the message.
     */
    public static void logNotInterestedMessage(int peerID1, int peerID2) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " received the 'not interested' message from " + peerID2 + ".";
    
        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for Downloading a Piece
        Whenever a peer finishes downloading a piece, it generates the following log:
        [Time]: Peer [peer_ID 1] has downloaded the piece [piece index] from [peer_ID 2]. Now
        the number of pieces it has is [number of pieces].

        [peer_ID 1] represents the peer who downloaded the piece and [peer_ID 2] represents
        the peer who sent the piece. [piece index] is the piece index the peer has downloaded.
        [number of pieces] represents the number of pieces the peer currently has.
     */
    public static void logPieceDownload(int peerID1, int peerID2, int pieceIndex, int numberOfPieces) {
        // Get name of Log File to write to
        String logFileName1 = "log_peer_" + peerID1 + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID1 + " has downloaded the piece " + pieceIndex + " from " + peerID2 + ". Now the number of pieces it has is " + numberOfPieces + ".";
    
        // Log the message to the log file
        logToLogFile(logFileName1, logMessage);
    }

    /*  Log Method for Completion of Download
        Whenever a peer downloads the complete file, it generates the following log:
        [Time]: Peer [peer_ID] has downloaded the complete file.
     */
    public static void logDownloadCompletion(int peerID) {
        // Get name of Log File to write to
        String logFileName = "log_peer_" + peerID + ".log";
    
        // Get the current time in the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());
    
        // Build the log message
        String logMessage = currentTime + ": Peer " + peerID + " has downloaded the complete file.";
    
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
        // You can implement this based on how you handle the exchange of pieces and keeping track 
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
    }
}

