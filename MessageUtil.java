import java.io.*;
import java.nio.ByteBuffer;

public class MessageUtil {

    // Handshake constants
    private static final String handHead = "P2PFILESHARINGPROJ";
    private static final byte[] ZERO_BITS = new byte[10];

    // Message types
    public static final byte CHOKE = 0;
    public static final byte UNCHOKE = 1;
    public static final byte INTERESTED = 2;
    public static final byte NOT_INTERESTED = 3;
    public static final byte HAVE = 4;
    public static final byte BITFIELD = 5;
    public static final byte REQUEST = 6;
    public static final byte PIECE = 7;

    // Send Handshake message
    public static void sendHandshake(DataOutputStream out, int peerID) throws IOException {
        out.writeBytes(handHead);
        out.write(ZERO_BITS);
        out.writeInt(peerID);
    }

    // Receive Handshake message and return the Peer ID
    public static int receiveHandshake(DataInputStream in) throws IOException {
        byte[] headerBytes = new byte[18];
        in.readFully(headerBytes);
        String header = new String(headerBytes);
        
        if (!handHead.equals(header)) {
            throw new IOException("Invalid Handshake Header");
        }
        
        in.skipBytes(10);  // Skip zero bits
        return in.readInt();
    }

    // Send actual message
    public static void sendMessage(DataOutputStream out, byte messageType, byte[] payload) throws IOException {
        int length = payload != null ? payload.length + 1 : 1;
        out.writeInt(length);
        out.writeByte(messageType);
        if (payload != null) {
            out.write(payload);
        }
    }

    // Receive actual message
    public static Message receiveMessage(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte messageType = in.readByte();
        byte[] payload = new byte[length - 1];
        if (length > 1) {
            in.readFully(payload);
        }
        return new Message(messageType, payload);
    }

    // Message class
    public static class Message {
        private byte type;
        private byte[] payload;

        public Message(byte type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }

        public byte getType() {
            return type;
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
