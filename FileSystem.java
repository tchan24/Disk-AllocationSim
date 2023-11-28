import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FileSystem {
    private DiskDrive diskDrive;
    //private String allocationMethod;
    private Map<String, FileMetadata> fileTable;

    public FileSystem(DiskDrive diskDrive, String allocationMethod) {
        this.diskDrive = diskDrive;
        this.fileTable = new HashMap<>();
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
}


    public void createFile(String fileName, byte[] data) {
        if (fileName.length() > 8 || !fileName.matches("[a-z]+")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (data.length > 10 * DiskDrive.getBlockSize()) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }

        int[] freeBlocks = findFreeBlocksContiguous(data.length);
        if (freeBlocks.length == 0) {
            throw new IllegalStateException("Not enough contiguous space");
        }

        writeDataToBlocks(freeBlocks, data);
        updateFAT(fileName, freeBlocks[0], freeBlocks.length);
        updateBitmap(freeBlocks, true);
    }

    public byte[] readFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        byte[] data = new byte[metadata.getLength() * DiskDrive.getBlockSize()];
        for (int i = 0; i < metadata.getLength(); i++) {
            byte[] blockData = diskDrive.readBlock(metadata.getStartBlock() + i);
            System.arraycopy(blockData, 0, data, i * DiskDrive.getBlockSize(), blockData.length);
        }
        return data;
    }

    public void updateFile(String fileName, byte[] newData) {
        // Check if the file exists
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        // Delete the old file
        deleteFile(fileName);

        // Create a new file with the new data
        createFile(fileName, newData);
    }

    public void deleteFile(String fileName) {
        // Check if the file exists
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        // Mark the blocks as free in the bitmap
        int[] blocks = new int[metadata.getLength()];
        for (int i = 0; i < metadata.getLength(); i++) {
            blocks[i] = metadata.getStartBlock() + i;
        }
        updateBitmap(blocks, false);

        // Remove the file entry from the FAT
        fileTable.remove(fileName);
    }

    private int[] findFreeBlocksContiguous(int dataSize) {
        byte[] bitmap = diskDrive.readBlock(1);
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
        return new int[0];
    }

    private boolean isBlockFree(byte[] bitmap, int blockIndex) {
        int byteIndex = blockIndex / 8;
        int bitIndex = blockIndex % 8;
        return (bitmap[byteIndex] & (1 << bitIndex)) == 0;
    }

    private void writeDataToBlocks(int[] blocks, byte[] data) {
        for (int i = 0; i < blocks.length; i++) {
            int start = i * DiskDrive.getBlockSize();
            int end = Math.min(start + DiskDrive.getBlockSize(), data.length);
            byte[] blockData = new byte[DiskDrive.getBlockSize()]; // Create a block-sized array
    
            // Copy the relevant portion of data into blockData
            System.arraycopy(data, start, blockData, 0, end - start);
            
            diskDrive.writeBlock(blocks[i], blockData);
        }
    }
    

    private void updateFAT(String fileName, int startBlock, int length) {
        fileTable.put(fileName, new FileMetadata(startBlock, length));
    }

    public byte[] getBitmap() {
        return diskDrive.readBlock(1); // Assuming block 1 is used for the bitmap
    }

    private void updateBitmap(int[] blocks, boolean used) {
        byte[] bitmap = diskDrive.readBlock(1);
        for (int block : blocks) {
            int byteIndex = block / 8;
            int bitIndex = block % 8;
            if (used) {
                bitmap[byteIndex] |= (1 << bitIndex);
            } else {
                bitmap[byteIndex] &= ~(1 << bitIndex);
            }
        }
        diskDrive.writeBlock(1, bitmap);
    }

    // Additional methods like updateFile, deleteFile, etc. will be added in subsequent parts
}
