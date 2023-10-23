import java.net.*;
import java.io.*;

public class Peer {
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private PeerInfo info;

    public Peer(PeerInfo info) {
        this.info = info;
    }

    public void connectTo(PeerInfo peerInfo) throws IOException {
        socket = new Socket(peerInfo.hostName, peerInfo.port);
        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
    }

    public DataInputStream getDataIn() {
        return dataIn;
    }

    public DataOutputStream getDataOut() {
        return dataOut;
    }

    public PeerInfo getInfo() {
        return info;
    }

    public void close() throws IOException {
        socket.close();
    }
}
