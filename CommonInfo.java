public class CommonInfo {
    public int numPrefNeighbors;
    // number of preffered neighbors
    public int unchokingInterval;
    // unchocking interval in seconds
    public int optimisticUnchokingInterval;
    // optimistic unchocking interval in seconds 
    public String fileName;
    public int fileSize;
    public int pieceSize;
    // piece size in bytes

    public CommonInfo(int numNeigh, int ui, int oui, String fn, int fs, int ps) {
        // stores information about 
        numPrefNeighbors = numNeigh;
        unchokingInterval = ui;
        optimisticUnchokingInterval = oui;
        fileName = fn;
        fileSize = fs;
        pieceSize = ps;
    }
}
