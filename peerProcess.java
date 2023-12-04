import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.File;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;




public class peerProcess {

    private boolean allDone = false;
    public int index;
    private PeerInfo peerInfo;
    private int peerId;
    private boolean hasFile;
    private static BitSet bitfield;
    private List<PeerInfo> allPeers = new ArrayList<>();
    private List<Peer> connectedPeers = new ArrayList<>();
    private HashMap<Integer, PeerInfo> map = new HashMap<>();
    private CommonConfig config;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private int numChoked = 0;
    private  byte[] fileArr;

    private DataInputStream dataIn;
    private DataOutputStream dataOut;

    public peerProcess(int peerId) {
        this.peerId = peerId;
    }

    public static BitSet getBitfield() {
        return bitfield;
    }


    public static void choke(Peer peer) {
        // record as being choked by other user 
    }

    public static void unchoke(Peer peer) {

        // record being unchoked by other user

    }

    public void interested(Peer peer) throws IOException {
        // Check if you are interested in the peer
        if (amInterestedIn(peer)) {
            MessageUtil.sendMessage(peer.getDataOut(), MessageUtil.INTERESTED, null);
            peer.setInterestIn(true);
        }
    }
    
    public void notInterested(Peer peer) throws IOException {
        MessageUtil.sendMessage(peer.getDataOut(), MessageUtil.NOT_INTERESTED, null);
        peer.setInterestIn(false);
    }
    
    private boolean amInterestedIn(Peer peer) {
        BitSet theirBitfield = BitSet.valueOf(peer.getInfo().getBitfield()); 
        BitSet myBitfield = peerProcess.getBitfield(); 
        theirBitfield.andNot(myBitfield);
        return !theirBitfield.isEmpty();
    }

    public void have(MessageUtil.Message message, Peer peer) {
        // record what user ID has in there bitfield and change accordingly

        ByteBuffer buf = ByteBuffer.wrap(message.getPayload());
        int index = buf.getInt();

        //TODO: add bitfield change

    }

     // record bitfield of user ID
    public void bitfield(MessageUtil.Message message, Peer peer) {
        PeerInfo peerInfo = peer.getInfo();

        //adds bitfield to the peers info
        peerInfo.addBitfield(message.getPayload());

        System.out.println("bitfield recieved " + message.getPayload().length);

    }

    public void request(MessageUtil.Message message, Peer peer) {
        // record request of which piece(s) a user ID wants
    }

    public void piece(MessageUtil.Message message, Peer peer) {
        // download piece received from a user
        byte[] payload = message.getPayload();
        
        byte[] index = Arrays.copyOfRange(payload, 0, 4);
        byte[] piece = Arrays.copyOfRange(payload, 4, payload.length);

        ByteBuffer buf = ByteBuffer.wrap(index);
        int pIndex = buf.getInt();

        System.arraycopy(piece, 0, fileArr, pIndex * (int) config.getPieceSize(), piece.length);


        // TODO: change bitfield
    }

    //parses what type of message it is and makes a decision based on that
    public void parseMessage(MessageUtil.Message message, Peer peer) throws IOException {
        byte type = message.getType();

        // switch statement to parse type
        switch(type) {
            case MessageUtil.CHOKE -> choke(peer);
            case MessageUtil.UNCHOKE -> unchoke(peer);
            case MessageUtil.INTERESTED -> interested(peer);
            case MessageUtil.NOT_INTERESTED -> notInterested(peer);
            case MessageUtil.HAVE -> have(message, peer);
            case MessageUtil.BITFIELD -> bitfield(message, peer);
            case MessageUtil.REQUEST -> request(message, peer);
            case MessageUtil.PIECE -> piece(message, peer);
            default -> System.out.println("invalid type");
        }
    }

    //sends a message for messages that require no payload
    public void makeGenMessage(byte type, Peer peer) throws IOException {
        MessageUtil.sendMessage(peer.getDataOut(), type, null);
    }

    // makes a have message
    public  void makeHave(Peer peer, int index) throws IOException{
        byte type = 4;

        // index of 4 bytes for piece had
        byte[] payload = ByteBuffer.allocate(4).putInt(index).array();

        MessageUtil.sendMessage(peer.getDataOut(), type, payload); 
    }

    public void makeBitfieldMsg(Peer peer) throws IOException {
        byte type = 5;

        // converts bitfield to byte array
        byte[] payload = this.bitfield.toByteArray();

        //calls message function with payload
        MessageUtil.sendMessage(peer.getDataOut(), type, payload);

        System.out.println("Sent Bitfield " + payload.length);
    }
    
    // makes a request message
    public  void makeRequest(Peer peer, int index) throws IOException {
        byte type = 6;

        // index of requested piece
        byte[] payload = ByteBuffer.allocate(4).putInt(index).array();

        //calls message function with payload
        MessageUtil.sendMessage(peer.getDataOut(), type, payload); 
    }

    // makes a piece message
    public void makePiece(Peer peer, int pieceIndex) throws IOException {
        byte type = 7;

        // create byte array for the piece
        int arrIndex = pieceIndex * (int)config.getPieceSize();

        byte[] pIndex = ByteBuffer.allocate(4).putInt(pieceIndex).array();

        byte[] piece = Arrays.copyOfRange(fileArr, arrIndex, arrIndex + (int)config.getPieceSize());

        byte[] message = new byte[pIndex.length + piece.length];

        System.arraycopy(pIndex, 0, message, 0, pIndex.length);
        System.arraycopy(piece, 0, message, pIndex.length, piece.length);



        //calls message function with payload
        MessageUtil.sendMessage(peer.getDataOut(), type, message);
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
        initFileStream();

        fileArr = new byte[(int) config.getFileSize()];

        if (hasFile) {
            copyFile();
        }
        
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

        System.out.println(bitfield.size());

        if (hasFile) {
            bitfield.set(0, numOfPieces);
        }

        System.out.println(numOfPieces);
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
        System.out.println("connect to prev");
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

                        makeBitfieldMsg(peer);

                        while(!allDone) {//waits till all are checked to halt

                            MessageUtil.Message message = MessageUtil.receiveMessage(in); //receive message
                            parseMessage(message, peer);

                        }



                        // byte[] buffer = new byte[1024];
                        // int bytesRead = in.read(buffer);
                        // String receivedMessage = new String(buffer, 0, bytesRead);
                        // System.out.println("Received message from client: " + receivedMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    

                });
                listenerThread.start();
            } catch (IOException e) {
                System.out.println("Error connecting to peer " + info.peerId + ": " + e.getMessage());
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

        System.out.println("Handshake successful with Peer " + receivedPeerId);

        
    }

    private void handleMessages(Socket socket) {

        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            int receivedPeerId = MessageUtil.receiveHandshake(in);
                        
                        
                        MessageUtil.sendHandshake(out, this.peerId);
                        System.out.println("Handshake successful with Peer " + receivedPeerId);

                        // byte[] buffer = new byte[1024];
                        // int bytesRead = in.read(buffer);
                        // String receivedMessage = new String(buffer, 0, bytesRead);
                        // System.out.println("Received message from client: " + receivedMessage);

                        // String responseMessage = "Hello, client! Your message was received.";
                        // out.write(responseMessage.getBytes());

                        Peer peer = new Peer(map.get(receivedPeerId));
                        peer.connectedTo(in, out, socket);

                        connectedPeers.add(peer);

                        makeBitfieldMsg(peer);

                        while(!allDone) {//waits till all are checked to halt

                            MessageUtil.Message message = MessageUtil.receiveMessage(in); //receive message
                            parseMessage(message, peer);

                        }

        } catch(IOException e) {
            e.printStackTrace();
        }

    }


    // listens for any new connections 
    private void listenForConnections() throws IOException {
        System.out.println("listening on port" + this.peerInfo.port);

        try {
            ServerSocket serverSocket = new ServerSocket(this.peerInfo.port);


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle each client in a separate thread
                new Thread(() -> handleMessages(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void waitForCompletion() {
        
        /* ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the first task to run every 5 seconds
        scheduler.scheduleAtFixedRate(() -> {
            // Your first function goes here
            System.out.println("Task 1 executed at: " + System.currentTimeMillis());
        }, 0, 5, TimeUnit.SECONDS);

        // Schedule the second task to run every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            // Your second function goes here
            System.out.println("Task 2 executed at: " + System.currentTimeMillis());
        }, 0, 10, TimeUnit.SECONDS); */


    }

    public static void main(String[] args) {
        if (args.length != 1) {
           System.out.println("Usage: java peerProcess <peerID>");
           return;
        }



        System.out.println("Made it to main");

        // int peerId = Integer.parseInt("1001");
        int peerId = Integer.parseInt(args[0]);
        peerProcess process = new peerProcess(peerId);

        try {
            process.init();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Program finished");


    }
}

