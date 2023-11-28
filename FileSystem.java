import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FileSystem {
    private DiskDrive diskDrive;
    private Map<String, FileMetadata> fileTable;
    private final static int FAT_BLOCK_NUM = 0;
    private final static int BITMAP_BLOCK_NUM = 1;
    private final static int MAX_FILE_SIZE = 10; // Maximum blocks per file

    private String allocationMethod;

    public FileSystem(DiskDrive diskDrive, String allocationMethod) {
        this.diskDrive = diskDrive;
        this.allocationMethod = allocationMethod;
        // Initialization based on allocation method
    }

    public void createFile(String fileName, byte[] data) {
        if (fileName.length() > 8 || !fileName.matches("[a-z]+")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (data.length > MAX_FILE_SIZE * DiskDrive.getBlockSize()) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }
        
        int[] freeBlocks = findFreeBlocks(data.length);
        if (freeBlocks.length == 0) {
            throw new IllegalStateException("No free blocks available");
        }
        // Write data to the found blocks
        for (int i = 0; i < freeBlocks.length; i++) {
            diskDrive.writeBlock(freeBlocks[i], Arrays.copyOfRange(data, i * DiskDrive.BLOCK_SIZE, (i + 1) * DiskDrive.BLOCK_SIZE));
        }
        updateFAT(fileName, freeBlocks[0], freeBlocks.length);
        updateBitmap(freeBlocks, true);
    }
    

    private int[] findFreeBlocks(int dataSize) {
        byte[] bitmap = diskDrive.readBlock(BITMAP_BLOCK_NUM);
        int requiredBlocks = (int) Math.ceil((double) dataSize / DiskDrive.getBlockSize());

        for (int i = 0; i < bitmap.length * 8; i++) {
            if (isBlockFree(bitmap, i)) {
               int freeCount = 1;
                while (freeCount < requiredBlocks && isBlockFree(bitmap, i + freeCount)) {
                    freeCount++;
                }
                if (freeCount == requiredBlocks) {
                    return IntStream.range(i, i + freeCount).toArray();
                }
                i += freeCount;
            }
        }
        return new int[0]; // No sufficient contiguous free space found
    }

    private boolean isBlockFree(byte[] bitmap, int blockIndex) {
        int byteIndex = blockIndex / 8;
        int bitIndex = blockIndex % 8;
        return (bitmap[byteIndex] & (1 << bitIndex)) == 0;
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

    private void updateFAT(String fileName, int startBlock, int length) {
        // This is a simplified version. You need to define how you store this information.
        fileTable.put(fileName, new FileMetadata(startBlock, length));
    }
    
    private void updateBitmap(int[] blocks, boolean used) {
        byte[] bitmap = diskDrive.readBlock(BITMAP_BLOCK_NUM);
        for (int block : blocks) {
            int byteIndex = block / 8;
            int bitIndex = block % 8;
            if (used) {
                bitmap[byteIndex] |= (1 << bitIndex);
            } else {
                bitmap[byteIndex] &= ~(1 << bitIndex);
            }
        }
        diskDrive.writeBlock(BITMAP_BLOCK_NUM, bitmap);
    }    
    
    
    public byte[] readFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        int fileLength = metadata.getLength();
        int startBlock = metadata.getStartBlock();

        byte[] data = new byte[fileLength * DiskDrive.getBlockSize()];
        for (int i = 0; i < fileLength; i++) {
            System.arraycopy(diskDrive.readBlock(startBlock + i), 0, data, i * DiskDrive.getBlockSize(), DiskDrive.getBlockSize());
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
        updateFAT(fileName, metadata.getStartBlock(), 0); // Setting length to 0 to indicate deletion
        updateBitmap(new int[]{metadata.getStartBlock()}, false);
        fileTable.remove(fileName);
    }
    
    public byte[] getBitmap() {
        return diskDrive.readBlock(BITMAP_BLOCK_NUM);
    }
    
    
}
