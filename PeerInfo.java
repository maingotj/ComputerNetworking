public class PeerInfo {
    public int peerId;
    public String hostName;
    public int port;
    public boolean hasFile;
    public byte[] bitfield;

    //peer info read from the peerinfo.cfg
    public PeerInfo(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

    public void addBitfield(byte[] bitfield1) {
        bitfield = bitfield1;
    }

    public byte[] getBitfield() {
        return this.bitfield;
    }

}
