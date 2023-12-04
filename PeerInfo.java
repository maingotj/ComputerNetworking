import java.util.BitSet;
import java.util.HashSet;

public class PeerInfo {
    public int peerId;
    public String hostName;
    public int port;
    public boolean hasFile;
    public BitSet bitfield;
    public HashSet<Integer> neededPieces;

    // peer info read from the peerinfo.cfg
    public PeerInfo(int peerId, String hostName, int port, boolean hasFile) {
        this.peerId = peerId;
        this.hostName = hostName;
        this.port = port;
        this.hasFile = hasFile;
    }

    public void addBitfield(BitSet bitfield1) {
        bitfield = bitfield1;

        if (this.neededPieces == null) {
            this.neededPieces = new HashSet<>();
        }
        
        for (int i = bitfield.nextSetBit(0); i != -1; i = bitfield.nextSetBit(i + 1)) {
            neededPieces.add(i);
        }
    }

    public BitSet getBitfield() {
        return this.bitfield;
    }

}
