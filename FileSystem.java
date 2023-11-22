import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FileSystem {
    private DiskDrive diskDrive;
    private Map<String, FileMetadata> fileTable;
    private final static int FAT_BLOCK_NUM = 0;
    private final static int BITMAP_BLOCK_NUM = 1;
    private final static int MAX_FILE_SIZE = 10; // Maximum blocks per file

    public FileSystem(DiskDrive diskDrive) {
        this.diskDrive = diskDrive;
        this.fileTable = new HashMap<>();
    }

    public void createFile(String fileName, byte[] data) {
        int[] freeBlocks = findFreeBlocks(data.length);
        if (freeBlocks.length == 0) {
            throw new IllegalStateException("No free blocks available");
        }
        // Write data to the found blocks
        for (int i = 0; i < freeBlocks.length; i++) {
            diskDrive.writeBlock(freeBlocks[i], Arrays.copyOfRange(data, i * DiskDrive.BLOCK_SIZE, (i + 1) * DiskDrive.BLOCK_SIZE));
        }
        updateFAT(freeBlocks[0], freeBlocks);
        updateBitmap(freeBlocks, true);
    }
    

    private int[] findFreeBlocks(int dataSize) {
        // Example implementation for contiguous allocation
        int requiredBlocks = (int) Math.ceil((double) dataSize / DiskDrive.BLOCK_SIZE);
        byte[] bitmap = diskDrive.readBlock(BITMAP_BLOCK_NUM);
    
        for (int i = 0; i < bitmap.length; i++) {
            // Find a contiguous sequence of free blocks
            // Implement logic based on the allocation strategy
        }
    
        return new int[0]; // Placeholder for found block numbers
    }
    
    public class FileMetadata {
        private int startBlock;
        private int length;
    
        public FileMetadata(int startBlock, int length) {
            this.startBlock = startBlock;
            this.length = length;
        }
    
        public int getStartBlock() {
            return startBlock;
        }
    
        public int getLength() {
            return length;
        }
    
        // Optionally, can add setters if needed
    }
        

    // Methods to read, update, and delete files

    private void updateFAT(int fileStartBlock, int[] fileBlocks) {
        // Update FAT with file block information
        byte[] fatBlock = diskDrive.readBlock(FAT_BLOCK_NUM);
        // Logic to update FAT entries
        diskDrive.writeBlock(FAT_BLOCK_NUM, fatBlock);
    }
    
    private void updateBitmap(int[] usedBlocks, boolean used) {
        // Update bitmap to reflect block usage
        byte[] bitmapBlock = diskDrive.readBlock(BITMAP_BLOCK_NUM);
        // Logic to update bitmap entries
        diskDrive.writeBlock(BITMAP_BLOCK_NUM, bitmapBlock);
    }
    
    public byte[] readFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        byte[] data = new byte[metadata.getLength() * DiskDrive.getBlockSize()];
        for (int i = 0; i < metadata.getLength(); i++) {
            System.arraycopy(diskDrive.readBlock(metadata.getStartBlock() + i), 0, data, i * DiskDrive.getBlockSize(), DiskDrive.getBlockSize());
        }
        return data;
    }
    
    
    public void updateFile(String fileName, byte[] newData) {
        deleteFile(fileName);
        createFile(fileName, newData);
    }
    
    
    public void deleteFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        // Clear the FAT entries and update the bitmap
        updateFAT(0, new int[]{metadata.getStartBlock()}); // Simplified for illustration
        updateBitmap(new int[]{metadata.getStartBlock()}, false);
        fileTable.remove(fileName);
    }
    
    
}
