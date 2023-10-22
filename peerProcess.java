import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.net.ServerSocket;
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

