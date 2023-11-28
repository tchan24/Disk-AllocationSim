public class DiskDrive {
    public final static int BLOCK_SIZE = 512;
    private final static int NUM_BLOCKS = 256;
    private byte[][] blocks;

    public static int getBlockSize() {
        return BLOCK_SIZE;
    }

    public DiskDrive() {
        blocks = new byte[NUM_BLOCKS][BLOCK_SIZE];
        // Initialize the FAT and bitmap.
        initializeFAT();
        initializeBitmap();
    }

    private void initializeFAT() {
        // Initialize the first block with FAT data
        // For simplicity, can initially set it all to zero.
        for(int i = 0; i < BLOCK_SIZE; i++) {
            blocks[0][i] = 0;
        }
    }

    private void initializeBitmap() {
        // Initialize the second block with bitmap data.
        // Initially, all blocks are free, except for the first two.
        blocks[1][0] = (byte) 0b11000000; // First two blocks are used.
        for(int i = 1; i < BLOCK_SIZE; i++) {
            blocks[1][i] = 0;
        }
    }

    public byte[] readBlock(int blockNumber) {
        if(blockNumber < 0 || blockNumber >= NUM_BLOCKS) {
            throw new IllegalArgumentException("Invalid block number");
        }
        return blocks[blockNumber];
    }

    public void writeBlock(int blockNumber, byte[] data) {
        if(blockNumber < 0 || blockNumber >= NUM_BLOCKS) {
            throw new IllegalArgumentException("Invalid block number");
        }
        if(data.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("Data size does not match block size");
        }
        blocks[blockNumber] = data;
    }
}