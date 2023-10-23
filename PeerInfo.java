public class PeerInfo {
    public int peerId;
    public String hostName;
    public int port;
    public boolean hasFile;

    public PeerInfo(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }
}
