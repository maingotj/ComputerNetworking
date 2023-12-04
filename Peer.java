import java.net.*;
import java.io.*;

public class Peer {
    private Socket socket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private PeerInfo info;
    private boolean chokedBy;
    private boolean interestFrom;
    private boolean choking;
    private boolean interestedIn;
    private long lastDownloadTime;
    private int bytesDownloaded;

    public Peer(PeerInfo info) {
        this.info = info;
    }

    // keeps record of what socket each peer has and the data streams
    public void connectTo(PeerInfo peerInfo) throws IOException {
        socket = new Socket(peerInfo.hostName, peerInfo.port);
        dataIn = new DataInputStream(socket.getInputStream());
        dataOut = new DataOutputStream(socket.getOutputStream());
    }

    public void connectedTo(DataInputStream dataInputStream, DataOutputStream dataOutputStream, Socket socketIn) {
        socket = socketIn;
        dataIn = dataInputStream;
        dataOut = dataOutputStream;
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

    public boolean isChoked() {
        return chokedBy;
    }

    public boolean isInterestedBy() {
        return interestFrom;
    }

    public boolean isChoking() {
        return choking;
    }

    public boolean isInterestedIn() {
        return interestedIn;
    }

    public void close() throws IOException {
        socket.close();
    }

    // set whether current peer is being choked by this peer
    public void setChokedBy(boolean isChoked) {
        chokedBy = isChoked;
    }

    // set whether current peer is receiving interest from this peer
    public void setInterestedBy(boolean receiveInterest) {
        interestFrom = receiveInterest;
    }

    // set whether current peer is choking this peer
    public void setChoking(boolean isChoked) {
        choking = isChoked;
    }

    // set whether current peer is interested in this peer
    public void setInterestIn(boolean receiveInterest) {
        interestedIn = receiveInterest;
    }

    public long getLastDownloadTime(){
        return lastDownloadTime;
    }
    public void setLastDownloadTime(long lastTime){
        lastDownloadTime = lastTime;
    }

    public int getBytesDownloaded(){
        return bytesDownloaded;
    }
    public void setBytesDownloaded(int bytesDown){
        bytesDownloaded = bytesDown;
    }
}
