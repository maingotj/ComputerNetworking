import java.io.*;
import java.util.Properties;

public class CommonConfig {

    //config parameters
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private long fileSize;
    private long pieceSize;

    // reads config file called common.cfg
    public CommonConfig() {
        try (FileInputStream fis = new FileInputStream("Common.cfg")) {
            Properties prop = new Properties();
            prop.load(fis);

            //reads and records information from the file
            numberOfPreferredNeighbors = Integer.parseInt(prop.getProperty("NumberOfPreferredNeighbors"));
            unchokingInterval = Integer.parseInt(prop.getProperty("UnchokingInterval"));
            optimisticUnchokingInterval = Integer.parseInt(prop.getProperty("OptimisticUnchokingInterval"));
            fileName = prop.getProperty("FileName");
            fileSize = Long.parseLong(prop.getProperty("FileSize"));
            pieceSize = Long.parseLong(prop.getProperty("PieceSize"));

            //System.out.println(numberOfPreferredNeighbors + " " + unchokingInterval + " " + optimisticUnchokingInterval+ " " + fileName + " " + fileSize + " " + pieceSize);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // get functions for config information
    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getPieceSize() {
        return pieceSize;
    }

    // calculates and returns number of pieces
    public int getNumOfPieces() {
        int numPieces;

        numPieces = (int) (fileSize/pieceSize);
        if ((fileSize % pieceSize) != 0) {
            numPieces++;
        }

        return numPieces;

    }
    
}
