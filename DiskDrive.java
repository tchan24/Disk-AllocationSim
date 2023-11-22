public class DiskDrive {
    private final int BLOCK_SIZE = 512;
    private final int NUM_BLOCKS = 256;
    private byte[][] blocks;

    public DiskDrive() {
        blocks = new byte[NUM_BLOCKS][BLOCK_SIZE];
        // Initialize blocks here (e.g., setting up the FAT and bitmap)
    }

    public byte[] readBlock(int blockNumber) {
        // Implement reading a block
        return blocks[blockNumber];
    }

    public void writeBlock(int blockNumber, byte[] data) {
        // Implement writing to a block
        blocks[blockNumber] = data;
    }

    // Additional methods for handling bitmap and FAT
}
