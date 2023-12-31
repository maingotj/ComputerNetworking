import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class peerProcess {

    private boolean allDone = false;
    private PeerInfo peerInfo;
    private int peerId;
    private boolean hasFile;
    private static BitSet bitfield;
    private List<PeerInfo> allPeers = new ArrayList<>();
    private List<Peer> connectedPeers = new ArrayList<>();
    private HashMap<Integer, PeerInfo> map = new HashMap<>();
    Set<Peer> preferredNeighbors;
    Peer optimisticallyUnchokedNeighbor;
    private CommonConfig config;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private int numChoked = 0;
    private  byte[] fileArr;
    private int numBits = 0;
    private HashSet<Integer> dontHave = new HashSet<>();
    private ServerSocket serverSocket;

    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public peerProcess(int peerId) {
        this.peerId = peerId;
    }

    public static BitSet getBitfield() {
        return bitfield;
    }


    public void choke(Peer peer) {
        peer.setChokedBy(true);
        logChokingEvent(this.peerId, peer.getInfo().peerId);
    }

    public void unchoke(Peer peer) {
        // record being unchoked by other user

        peer.setChokedBy(false);
        logUnchokingEvent(this.peerId, peer.getInfo().peerId);

    }

    // Handles the case when the remote peer expresses interest in the current peer.
    public void interested(Peer peer) throws IOException {
        // Set the interest flag for the remote peer to true
        peer.setInterestIn(true);
    
        // Log an "interested" message for tracking
        logInterestedMessage(this.peerId, peer.getInfo().peerId);
    }
    
    // Handles the case when the remote peer expresses no interest in the current peer.
    public void notInterested(Peer peer) throws IOException {
        // Set the interest flag for the remote peer to false
        peer.setInterestIn(false);
    
        // Log a "not interested" message for tracking
        logNotInterestedMessage(this.peerId, peer.getInfo().peerId);
    }

    // Handles the case when a remote peer informs the current peer about having a specific piece.
    public void have(MessageUtil.Message message, Peer peer) {
        // Extract the index of the piece from the message payload
        ByteBuffer buf = ByteBuffer.wrap(message.getPayload());
        int index = buf.getInt();
    
        // Update the bitfield of the remote peer to reflect the received piece
        peer.getInfo().getBitfield().set(index);
    
        // Log a "have" message for tracking
        logHaveMessage(this.peerId, peer.getInfo().peerId, index);
    
        // TODO: Add logic to handle changes in the bitfield of the current peer
    }

    // Handles the case when a remote peer sends a request for a specific piece.
    public void request(MessageUtil.Message message, Peer peer) {
        // Extract the index of the requested piece from the message payload
        ByteBuffer buf = ByteBuffer.wrap(message.getPayload());
        int index = buf.getInt();
    
        // Attempt to fulfill the requested piece and send it to the remote peer
        try {
            makePiece(peer, index);
        } catch (IOException e) {
            // Handle potential I/O errors when fulfilling the request
            e.printStackTrace(); // Print the stack trace for debugging purposes
            // TODO: Add appropriate error handling or logging as needed
        }
    }

    // Handles the reception of a piece from a remote peer.
    public void piece(MessageUtil.Message message, Peer peer) {
        // Extract the payload from the received message
        byte[] payload = message.getPayload();
    
        // Extract the index and piece data from the payload
        byte[] index = Arrays.copyOfRange(payload, 0, 4);
        byte[] piece = Arrays.copyOfRange(payload, 4, payload.length);
    
        // Convert the index bytes to an integer
        ByteBuffer buf = ByteBuffer.wrap(index);
        int pIndex = buf.getInt();

        if(dontHave.contains(pIndex)) {
            System.err.println("piece");
            System.arraycopy(piece, 0, fileArr, pIndex * (int) config.getPieceSize(), piece.length);
    
            // Update the bitfield to mark the received piece as available
            bitfield.set(pIndex);

    
            // Log the successful piece download for tracking
            logPieceDownload(this.peerId, peer.getInfo().peerId, pIndex, ++numBits);
    
            // Remove the piece index from the set of missing pieces
            dontHave.remove(pIndex);
    
            // Update the hash information for connected peers
            for (Peer connectedPeer : connectedPeers) {
                connectedPeer.addToHash(pIndex);
            }
    
            // Update the bytesDownloaded for the peer
            peer.setBytesDownloaded(peer.getBytesDownloaded() + piece.length);
        }
    
        // TODO: Add logic to handle changes in the local bitfield if needed
    }

    // Parses the type of message received and takes appropriate actions based on the message type.
    public void parseMessage(MessageUtil.Message message, Peer peer) throws IOException {
        // Extract the type of the received message
        byte type = message.getType();
    
        // Use a switch statement to handle different message types
        switch (type) {
            case MessageUtil.CHOKE -> choke(peer);
            case MessageUtil.UNCHOKE -> unchoke(peer);
            case MessageUtil.INTERESTED -> interested(peer);
            case MessageUtil.NOT_INTERESTED -> notInterested(peer);
            case MessageUtil.HAVE -> have(message, peer);
            case MessageUtil.BITFIELD -> bitfield(message, peer);
            case MessageUtil.REQUEST -> request(message, peer);
            case MessageUtil.PIECE -> piece(message, peer);
            default -> System.out.println("Invalid message type");
        }
    }

    // Sends a message for messages that require no payload
    public void makeGenMessage(byte type, Peer peer) throws IOException {
        if(type == MessageUtil.INTERESTED) {
            peer.setInterestedBy(true);
        }
        else if(type == MessageUtil.NOT_INTERESTED) {
            peer.setInterestedBy(false);
        }

        // Use MessageUtil to send a message with the specified type and no payload to the specified peer
        MessageUtil.sendMessage(peer.getDataOut(), type, null);
    }

    // Makes a have message
    public void makeHave(Peer peer, int index) throws IOException {
        // Set the message type for "have" to 4
        byte type = 4;
    
        // Convert the piece index to a byte array of 4 bytes
        byte[] payload = ByteBuffer.allocate(4).putInt(index).array();
    
        // Use MessageUtil to send a "have" message with the specified payload to the specified peer
        MessageUtil.sendMessage(peer.getDataOut(), type, payload);
    }
    
    // makes a request message
    public void makeRequest(Peer peer) throws IOException {
        // Set the message type for "request" to 6
        byte type = 6;
    
        // TODO: Add logic to determine a random index value for the requested piece
        int size = 0;
        int index = 0;
    
        // Get the bitfield of the current peer and the remote peer
        BitSet myBitfield = this.bitfield;
        BitSet interestingPieces = (BitSet) peer.getInfo().getBitfield().clone();
        interestingPieces.andNot(myBitfield);
    
        // Count the number of interesting pieces
        for (int i = interestingPieces.nextSetBit(0); i != -1; i = interestingPieces.nextSetBit(i + 1)) {
            size++;
        }

        //System.out.println(size);

        int random = 1;
        Random rand = new Random();
        // System.out.println(size);
        if(size == 1) {
            random = 1;
        }
        else {
            random = rand.nextInt(size - 1);
        }
        
        //System.out.println(random);



        int j = 0;
        // Find the piece index corresponding to the randomly chosen position
        for (int i = interestingPieces.nextSetBit(0); i != -1; i = interestingPieces.nextSetBit(i + 1)) {
            if (j == random) {
                index = i;
            }
            j++;
        }
    
        // Convert the piece index to a byte array of 4 bytes
        byte[] payload = ByteBuffer.allocate(4).putInt(index).array();

        //System.out.println("sent piece to " + index);

        //calls message function with payload
        MessageUtil.sendMessage(peer.getDataOut(), type, payload); 

    }

    // Makes a piece message
    public void makePiece(Peer peer, int pieceIndex) throws IOException {
        // Set the message type for "piece" to 7
        byte type = 7;
    
        // Calculate the starting index in the file array for the specified piece
        int arrIndex = pieceIndex * (int) config.getPieceSize();
    
        // Convert the piece index to a byte array of 4 bytes
        byte[] pIndex = ByteBuffer.allocate(4).putInt(pieceIndex).array();
    
        // Extract the piece data from the file array for the specified piece
        byte[] piece = Arrays.copyOfRange(fileArr, arrIndex, arrIndex + (int) config.getPieceSize());
    
        // Combine the piece index and piece data into a single byte array
        byte[] message = new byte[pIndex.length + piece.length];
        System.arraycopy(pIndex, 0, message, 0, pIndex.length);
        System.arraycopy(piece, 0, message, pIndex.length, piece.length);
    
        // Use MessageUtil to send a "piece" message with the specified payload to the specified peer
        MessageUtil.sendMessage(peer.getDataOut(), type, message);
    }

    //#region
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
    //#endregion

    public void init() throws IOException {
        // Read configurations from the configuration file
        readConfigurations();
    
        // Initialize the bitfield based on file information
        initBitfield();
    
        // Set up the file stream and allocate space for the file array
        initFileStream();
    
        // If the peer has the complete file, copy it
        if (hasFile) {
            copyFile();
        }
    
        // If the current peer is not the first peer in the list, connect to previous peers
        if (peerId != allPeers.get(0).peerId) {
            connectToPreviousPeers();
        }
    
        // Listen for incoming connections from other peers
        listenForConnections();
    
        // Start the choking and unchoking process in a new thread
        new Thread(() -> {
            try {
                startChokingUnchoking();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    
        // Start the optimistic unchoking process in another new thread
        new Thread(() -> {
            try {
                startOptimisticUnchoking();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    
        // Begin exchanging pieces here...
    
        // Wait for all peers to download the complete file, then terminate.
        waitForCompletion();
    }
    // Start the choking and unchoking process
    public void startChokingUnchoking() throws IOException {
        while (!allDone) {
            try {
                // Calculate download rates and select preferred neighbors
                List<Peer> preferredNeighbors = calculatePreferredNeighbors();

                // Send unchoke messages to preferred neighbors
                for (Peer preferredNeighbor : preferredNeighbors) {
                    // Set choked to false
                    preferredNeighbor.setChoking(false);
                    // Send unchoke message to the peer
                    makeGenMessage(MessageUtil.UNCHOKE, preferredNeighbor);
                }

                // Choke all other neighbors
                chokeNonPreferredNeighbors(preferredNeighbors);

                // Sleep for the specified interval
                Thread.sleep(config.getUnchokingInterval() * 1000); // Convert seconds to milliseconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Start the optimistic unchoking process
    public void startOptimisticUnchoking() throws IOException {
        while (!allDone) {
            try {
                // Select an optimistically unchoked neighbor
                Peer optimisticNeighbor = selectOptimisticNeighbor();
                if(optimisticNeighbor != null){
                    // Set Choking to false
                    optimisticNeighbor.setChoking(false);
                    // Send unchoke message to the optimistic neighbor
                    makeGenMessage(MessageUtil.UNCHOKE, optimisticNeighbor);
                }

                // Sleep for the specified interval
                Thread.sleep(config.getOptimisticUnchokingInterval() * 1000); // Convert seconds to milliseconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public List<Peer> getInterestedPeersAmongConnected() {
        List<Peer> interestedPeers = new ArrayList<>();
    
        for (Peer connectedPeer : connectedPeers) {
            if (connectedPeer.isInterestedIn()) {
                interestedPeers.add(connectedPeer);
            }
        }
    
        return interestedPeers;
    }

    public List<Peer> calculatePreferredNeighbors() {
        List<Peer> interestedPeers = getInterestedPeersAmongConnected();

        if (hasFile) {
            // If peer has a complete file, shuffle the interested peers randomly
            Collections.shuffle(interestedPeers);
        } else {
            // Calculate download rates for each peer
            Map<Peer, Double> downloadRates = calculateDownloadRates(interestedPeers);

            // Sort peers by download rate in descending order
            interestedPeers = interestedPeers.stream()
            .sorted(Comparator.comparingDouble(downloadRates::get)
                    .thenComparing(peer -> Math.random())) // Secondary RANDOM sort for ties
            .collect(Collectors.toList());
        }

        // Select the top k peers as preferred neighbors
        int k = Math.min(config.getNumberOfPreferredNeighbors(), interestedPeers.size());
        return interestedPeers.subList(0, k);
    }
    public Map<Peer, Double> calculateDownloadRates(List<Peer> peers) {
        Map<Peer, Double> downloadRates = new HashMap<>();
    
        for (Peer peer : peers) {
            double downloadRate = calculateDownloadRate(peer);
            downloadRates.put(peer, downloadRate);
        }
    
        return downloadRates;
    }
    
    public double calculateDownloadRate(Peer peer) {
        long currentTime = System.currentTimeMillis();
        long lastDownloadTime = peer.getLastDownloadTime(); 
        int bytesDownloaded = peer.getBytesDownloaded(); 

        // Calculate time taken to download the last pieces
        double timeTakenInMilliseconds = (currentTime - lastDownloadTime);

        // Calculate download rate in bytes per millisecond
        if (timeTakenInMilliseconds > 0) {
            return bytesDownloaded / timeTakenInMilliseconds;
        } else {
            return 0.0; // Avoid division by zero
        }
    }

    public void chokeNonPreferredNeighbors(List<Peer> preferredNeighbors) {
        for (Peer connectedPeer : connectedPeers) {
            connectedPeer.setLastDownloadTime(System.currentTimeMillis());
            connectedPeer.setBytesDownloaded(0);
            if (!preferredNeighbors.contains(connectedPeer) && connectedPeer != optimisticallyUnchokedNeighbor) {
                // Choke non-preferred neighbors
                try {
                    // Set choked to true
                    connectedPeer.setChoking(true);
                    // Send message to Choke
                    makeGenMessage(MessageUtil.CHOKE, connectedPeer);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Peer selectOptimisticNeighbor() {
        List<Peer> chokedInterestedPeers = getChokedInterestedPeers();

        if (!chokedInterestedPeers.isEmpty()) {
            // Randomly select an optimistically unchoked neighbor
            Random random = new Random();
            int randomIndex = random.nextInt(chokedInterestedPeers.size());
            Peer optimisticNeighbor = chokedInterestedPeers.get(randomIndex);

            return optimisticNeighbor;
        }

        return null;
    }

    private List<Peer> getChokedInterestedPeers() {
        // Create a list to store peers that are interested and not choked
        List<Peer> chokedInterestedPeers = new ArrayList<>();

        // Iterate through connected peers to filter interested and not choked peers
        for (Peer connectedPeer : connectedPeers) {
            // Check if the connected peer is not being choked and is interested in the current peer
            if (!connectedPeer.isChoking() && connectedPeer.isInterestedIn()) {
                // Add the connected peer to the list of choked and interested peers
                chokedInterestedPeers.add(connectedPeer);
            }
        }

        // Return the list of choked and interested peers
        return chokedInterestedPeers;
    }

    private void readConfigurations() throws IOException {
        // Create a new CommonConfig instance to store common configuration settings
        config = new CommonConfig();
    
        // Read peer information from the "PeerInfo.cfg" file and store it in the allPeers list
        allPeers = readPeerInfo("PeerInfo.cfg");
    
        // Iterate through the list of peers to find information specific to the current peer
        for (PeerInfo peer : allPeers) {
            // Check if the peer ID matches the current peer's ID
            if (peer.peerId == this.peerId) {
                // Set the hasFile attribute based on whether the current peer has the complete file
                this.hasFile = peer.hasFile;
                // Exit the loop since the relevant information has been found
                break;
            }
        }
    }

    private void copyFile() throws IOException {
        inputStream.read(fileArr);
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

                if(this.peerId == id) {
                    this.peerInfo = new PeerInfo(id, hostName, port, hasFile);
                }

                map.put(id, new PeerInfo(id, hostName, port, hasFile));
                peers.add(new PeerInfo(id, hostName, port, hasFile));
            }
    
            return peers;
        }
        

    // makes bitfield for the file
    private void initBitfield() {
        int numOfPieces = (int) Math.ceil((double) config.getFileSize() / config.getPieceSize());
        this.bitfield = new BitSet(numOfPieces);

        // //System.out.println(bitfield.size());

        if (hasFile) {
            bitfield.set(0, numOfPieces);
        }
        else {
            for(int i = 0; i < numOfPieces; i++) {
                dontHave.add(i);
            }
        }

        System.out.println("dont have size " + dontHave.size());



        // StringBuilder s = new StringBuilder();
        //     for( int i = 0; i < bitfield.length();  i++ )
        //     {
        //         s.append( bitfield.get( i ) == true ? 1: 0 );
        //     }

            // //System.out.println( s );
            // //System.out.println(bitfield.size());
    }

    private void initFileStream() throws FileNotFoundException {


        String filePath = "peer_" + this.peerId + "" + File.separator + config.getFileName();

        if(!hasFile) {
            outputStream = new FileOutputStream(filePath);
        }
        else {
            outputStream = null;
        }
        inputStream = new FileInputStream(filePath);
    }

    // connects to any already made peers
    private void connectToPreviousPeers() {
        //System.out.println(this.bitfield.size());
        //System.out.println("connect to prev");
        for (PeerInfo info : allPeers) {
            if (info.peerId >= this.peerId) {
                break;
            }
            try {
                Peer peer = new Peer(info);
                peer.connectTo(info);
                performHandshake(peer);
                connectedPeers.add(peer);
                Thread listenerThread = new Thread(() -> {
  
                    try {
                        DataInputStream in = peer.getDataIn();
                        DataOutputStream out = peer.getDataOut();


                        // String message = "heloooo";
                        // out.write(message.getBytes());

                        performBitfieldExchange(peer);

                        while(!allDone) {//waits till all are checked to halt

                            MessageUtil.Message message = MessageUtil.receiveMessage(in); //receive message
                            parseMessage(message, peer);

                            if(!peer.isChoking() && peer.isInterestedBy()) {
                                makeRequest(peer);
                            }

                            if(peer.getHash().size() != 0) {
                                System.out.println("sending have");
                                for(int index: peer.getHash()) {
                                    makeHave(peer, index);
                                    peer.removeFromHash(index);
                                }
                            }

                        }



                        // byte[] buffer = new byte[1024];
                        // int bytesRead = in.read(buffer);
                        // String receivedMessage = new String(buffer, 0, bytesRead);
                        // //System.out.println("Received message from client: " + receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    

                });
                listenerThread.start();
            } catch (IOException e) {
                //System.out.println("Error connecting to peer " + info.peerId + ": " + e.getMessage());
            }
        }
    }

    // performs the handshake to start TCP connection
    private void performHandshake(Peer peer) throws IOException {
        MessageUtil.sendHandshake(peer.getDataOut(), this.peerId);

        int receivedPeerId = MessageUtil.receiveHandshake(peer.getDataIn());
        if (receivedPeerId != peer.getInfo().peerId) {
            throw new IOException("Mismatched Peer ID in Handshake");
        }

        logTCPConnection(this.peerId, receivedPeerId);
        // logs that the connection occured

        //System.out.println("Handshake successful with Peer " + receivedPeerId);

        
    }
    public boolean hasInterestingPieces(BitSet receivedBitfield) {
        // Check if the received bitfield has any pieces that the other peer has and you don't
        BitSet myBitfield = this.bitfield;
        BitSet interestingPieces = (BitSet) receivedBitfield.clone();
        interestingPieces.andNot(myBitfield);
        // If there are interesting pieces, return true
        if (!interestingPieces.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }
    public void bitfield(MessageUtil.Message message, Peer peer) {
        PeerInfo peerInfo = peer.getInfo();
        // Get the received bitfield
        BitSet receivedBitfield = BitSet.valueOf(message.getPayload());
        // Add the bitfield to the peer's info
        peerInfo.addBitfield(receivedBitfield);
        // Check if the received bitfield has any pieces that the current peer doesn't have
        if (hasInterestingPieces(receivedBitfield)) {
            try {
                // If interested, send an "interested" message
                makeGenMessage(MessageUtil.INTERESTED, peer);
                //System.out.println("Sent Interested Message");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // If not interested, send a "not interested" message
                makeGenMessage(MessageUtil.NOT_INTERESTED, peer);
                //System.out.println("Sent NOT Interested Message");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Method to send the initial bitfield message when a connection is established
    private void sendInitialBitfield(Peer peer) {
        try {
            // Convert the local bitfield to a byte array
            byte[] payload = this.bitfield.toByteArray();
            // Send the "bitfield" message using MessageUtil
            MessageUtil.sendMessage(peer.getDataOut(), MessageUtil.BITFIELD, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Method to perform the initial exchange of bitfields after the handshake
    private void performBitfieldExchange(Peer peer) throws IOException {
        // Send the local bitfield to the connected peer
        sendInitialBitfield(peer);
        //System.out.println("Sent Bitfield");
        // Receive and process the bitfield from the connected peer
        MessageUtil.Message bitfieldMessage = MessageUtil.receiveMessage(peer.getDataIn());
        //System.out.println("Recieved Bitfield");
        parseMessage(bitfieldMessage, peer);
    }

    private void handleMessages(Socket socket) {

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            int receivedPeerId = MessageUtil.receiveHandshake(in);
                        
                        
                        MessageUtil.sendHandshake(out, this.peerId);
                        //System.out.println("Handshake successful with Peer " + receivedPeerId);

                        // byte[] buffer = new byte[1024];
                        // int bytesRead = in.read(buffer);
                        // String receivedMessage = new String(buffer, 0, bytesRead);
                        // //System.out.println("Received message from client: " + receivedMessage);

                        // String responseMessage = "Hello, client! Your message was received.";
                        // out.write(responseMessage.getBytes());

                        Peer peer = new Peer(map.get(receivedPeerId));
                        peer.connectedTo(in, out, socket);

                        connectedPeers.add(peer);

                        performBitfieldExchange(peer);

                        while(!allDone) {//waits till all are checked to halt

                            MessageUtil.Message message = MessageUtil.receiveMessage(in); //receive message
                            parseMessage(message, peer);

                            if(!peer.isChoking() && peer.isInterestedBy()) {
                                makeRequest(peer);
                            }

                            if(peer.getHash().size() != 0) {
                                for(int index: peer.getHash()) {
                                    makeHave(peer, index);
                                    peer.removeFromHash(index);
                                }
                            }

                        }

        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    // listens for any new connections 
    private void listenForConnections() throws IOException {

        System.out.println("listening on port" + this.peerInfo.port);
        try {
            serverSocket = new ServerSocket(this.peerInfo.port);


            while (true) {
                Socket clientSocket = serverSocket.accept();
                //System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                new Thread(() -> handleMessages(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForCompletion() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
        // Schedule a task to check the download completion status periodically
        scheduler.scheduleAtFixedRate(() -> {
            boolean allCompleted = true;
    
            // Check the bitfield of each peer to determine if they have downloaded the complete file
            for (PeerInfo peerInfo : allPeers) {
                if (!isDownloadComplete(peerInfo)) {
                    allCompleted = false;
                    break;
                }
            }
    
            // If all peers have completed their downloads, perform shutdown operations
            if (allCompleted) {
                //System.out.println("All peers have completed downloading. Shutting down...");
                shutdown();
                scheduler.shutdownNow(); // Stop the scheduled task
            }
    
        }, 0, 5, TimeUnit.SECONDS); // Adjust the period as needed
    }
    
    
    // Method to determine if a peer has completed the download
    private boolean isDownloadComplete(PeerInfo peerInfo) {
        BitSet peerBitfield = peerInfo.getBitfield();
        return peerBitfield.cardinality() == bitfield.size();
    }

    // Method to perform shutdown operations

    private void shutdown() {
        System.out.println("Shutting down the peer process...");

        // Close all peer connections
        for (Peer peer : connectedPeers) {
            try {
                peer.close();
                System.out.println("Closed connection with Peer " + peer.getInfo().peerId);
            } catch (IOException e) {
                System.err.println("Error closing connection with Peer " + peer.getInfo().peerId);
                e.printStackTrace();
            }
        }


        // Close server socket if open
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing server socket.");
                e.printStackTrace();
            }
        }


        // Close file streams if they are open
        if (inputStream != null) {
            try {
                inputStream.close();
                System.out.println("Input stream closed.");
            } catch (IOException e) {
                System.err.println("Error closing input stream.");
                e.printStackTrace();
            }
        }


        if (outputStream != null) {
            try {
                outputStream.close();
                System.out.println("Output stream closed.");
            } catch (IOException e) {
                System.err.println("Error closing output stream.");
                e.printStackTrace();
            }
        }


        System.out.println("Peer process shutdown complete.");
    }


    public static void main(String[] args) {
        if (args.length != 1) {
           //System.out.println("Usage: java peerProcess <peerID>");
           return;
        }



        //System.out.println("Made it to main");

        // int peerId = Integer.parseInt("1001");
        int peerId = Integer.parseInt(args[0]);
        peerProcess process = new peerProcess(peerId);

        try {
            process.init();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("Program finished");


    }
}

