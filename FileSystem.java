import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class FileSystem {
    private DiskDrive diskDrive;
    private String allocationMethod;
    private Map<String, FileMetadata> fileTable;

    public FileSystem(DiskDrive diskDrive, String allocationMethod) {
        this.diskDrive = diskDrive;
        this.allocationMethod = allocationMethod;
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
        if (allocationMethod.equals("contiguous")) {
            createFileContiguous(fileName, data);
        } else if (allocationMethod.equals("chained")) {
            createFileChained(fileName, data);
        } else {
            throw new IllegalStateException("Unknown allocation method: " + allocationMethod);
        }
    }

    private void createFileContiguous(String fileName, byte[] data) {
        if (fileName.length() > 8 || !fileName.matches("[a-z]+")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        if (data.length > 10 * DiskDrive.getBlockSize()) {
            throw new IllegalArgumentException("File size exceeds maximum limit");
        }

        List<Integer> freeBlocks = findFreeBlocksChained(data.length);
        if (freeBlocks.isEmpty()) {
            throw new IllegalStateException("Not enough space");
        }

        chainAndWriteBlocks(freeBlocks, data);
        updateFATForChained(fileName, freeBlocks.get(0));
        updateBitmap(convertListToArray(freeBlocks), true); // Convert to array before updating bitmap
    }

    private void createFileChained(String fileName, byte[] data) {
        List<Integer> freeBlocks = findFreeBlocksChained(data.length);
        if (freeBlocks.isEmpty()) {
            throw new IllegalStateException("Not enough space");
        }

        chainAndWriteBlocks(freeBlocks, data);
        updateFATForChained(fileName, freeBlocks.get(0));
        updateBitmap(convertListToArray(freeBlocks), true);
    }
    

    private int[] convertListToArray(List<Integer> list) {
        return list.stream().mapToInt(i -> i).toArray();
    }

    public byte[] readFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
        throw new IllegalArgumentException("File not found");
        }

        if (allocationMethod.equals("contiguous")) {
            return readFileContiguous(metadata);
        } else if (allocationMethod.equals("chained")) {
            return readFileChained(metadata);
        } else {
            throw new IllegalStateException("Unknown allocation method: " + allocationMethod);
        }
    }

    private byte[] readFileContiguous(FileMetadata metadata) {
        
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

    private byte[] readFileChained(FileMetadata metadata) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int currentBlock = metadata.getStartBlock();

        while (currentBlock != -1) { // Assuming -1 indicates the end of the chain
            byte[] blockData = diskDrive.readBlock(currentBlock);
            // Assuming the last byte of each block stores the next block's index
            int nextBlock = blockData[DiskDrive.getBlockSize() - 1];
            outputStream.write(blockData, 0, DiskDrive.getBlockSize() - 1); // Exclude the last byte
            currentBlock = nextBlock;
        }

        return outputStream.toByteArray();
    }

    public void updateFile(String fileName, byte[] newData) {
        if (allocationMethod.equals("contiguous")) {
            updateFileContiguous(fileName, newData);
        } else if (allocationMethod.equals("chained")) {
            updateFileChained(fileName, newData);
        } else {
            throw new IllegalStateException("Unknown allocation method: " + allocationMethod);
        }
    }

    private void updateFileContiguous(String fileName, byte[] newData) {
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
    
    private void updateFileChained(String fileName, byte[] newData) {
        deleteFileChained(fileTable.get(fileName));
        createFileChained(fileName, newData);
    }

    public void deleteFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }

        if (allocationMethod.equals("contiguous")) {
            deleteFileContiguous(metadata);
        } else if (allocationMethod.equals("chained")) {
            deleteFileChained(metadata);
        } else {
            throw new IllegalStateException("Unknown allocation method: " + allocationMethod);
        }
    }

    private void deleteFileContiguous(FileMetadata metadata) {
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
    
    private void deleteFileChained(FileMetadata metadata) {
        int currentBlock = metadata.getStartBlock();
        List<Integer> blocksToFree = new ArrayList<>();
    
        while (currentBlock != -1) { // Assuming -1 indicates the end of the chain
            byte[] blockData = diskDrive.readBlock(currentBlock);
            blocksToFree.add(currentBlock);
            currentBlock = blockData[DiskDrive.getBlockSize() - 1]; // Get the next block index
        }
    
        updateBitmap(convertListToArray(blocksToFree), false);
        fileTable.remove(metadata); // Remove the file entry from the FAT
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

    public byte[] readDiskBlock(int blockNumber) {
        // Validate block number
        if (blockNumber < 0 || blockNumber >= DiskDrive.NUM_BLOCKS) {
            throw new IllegalArgumentException("Invalid block number");
        }
    
        // Return the block data
        return diskDrive.readBlock(blockNumber);
    }
    
    private List<Integer> findFreeBlocksChained(int dataSize) {
        int requiredBlocks = (int) Math.ceil((double) dataSize / DiskDrive.getBlockSize());
        List<Integer> freeBlocks = new ArrayList<>();

        byte[] bitmap = diskDrive.readBlock(1); // Assuming bitmap is stored in block 1
        for (int i = 0; i < bitmap.length * 8; i++) {
            if (isBlockFree(bitmap, i)) {
                freeBlocks.add(i);
                if (freeBlocks.size() == requiredBlocks) {
                break;
                }
            }
        }

        if (freeBlocks.size() < requiredBlocks) {
            return Collections.emptyList(); // Not enough space
        }
        return freeBlocks;
    }
    
    private void chainAndWriteBlocks(List<Integer> blocks, byte[] data) {
        // Logic to write and chain blocks
        // Convert List<Integer> to int[] if needed, or adjust logic to work with List<Integer>
        // ...
    }
    
    private void updateFATForChained(String fileName, int startBlock) {
        // Update FAT with the start block for the file
        // ...
    }

    // Additional methods like updateFile, deleteFile, etc. will be added in subsequent parts
}
