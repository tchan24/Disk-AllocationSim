import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

public class FileSystem {
    private DiskDrive diskDrive;
    private String allocationMethod;
    public Map<String, FileMetadata> fileTable;

    public FileSystem(DiskDrive diskDrive, String allocationMethod) {
        this.diskDrive = diskDrive;
        this.allocationMethod = allocationMethod;
        this.fileTable = new HashMap<>();
    }

    public class FileMetadata {
        private int startBlock;
        private int length;
        private int indexBlock;

        public FileMetadata(int startBlock, int length) {
            this.startBlock = startBlock;
            this.length = length;
        }
        
        public FileMetadata(int indexBlock) {
            this.indexBlock = indexBlock;
        }

        public int getIndexBlock() {
            return indexBlock;
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
        } else if (allocationMethod.equals("indexed")) {
            createFileIndexed(fileName, data);
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

    private void createFileIndexed(String fileName, byte[] data) {
        // Find a free block for the index
        int indexBlock = findFreeIndexBlock();
        if (indexBlock == -1) {
            throw new IllegalStateException("No free block for index");
        }
    
        // Find free blocks for the file data
        List<Integer> dataBlocks = findFreeDataBlocks(data.length);
        if (dataBlocks.size() * DiskDrive.getBlockSize() < data.length) {
            throw new IllegalStateException("Not enough space for file data");
        }
    
        // Write index block with the data blocks information
        writeIndexBlock(indexBlock, dataBlocks);
    
        // Write file data to the data blocks
        writeDataToBlocks(dataBlocks, data);
    
        // Update file table
        fileTable.put(fileName, new FileMetadata(indexBlock));
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
        } else if (allocationMethod.equals("indexed")) {
            return readFileIndexed(metadata);
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

    private byte[] readFileIndexed(FileMetadata metadata) {
        int indexBlock = metadata.getIndexBlock();
        byte[] indexData = diskDrive.readBlock(indexBlock);
    
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        for (int i = 0; i < indexData.length; i++) {
            if (indexData[i] == -1) break; // Assuming -1 indicates no more data blocks
    
            byte[] dataBlock = diskDrive.readBlock(indexData[i]);
            dataStream.write(dataBlock, 0, DiskDrive.getBlockSize());
        }
    
        return dataStream.toByteArray();
    }
    

    public void updateFile(String fileName, byte[] newData) {
        if (allocationMethod.equals("contiguous")) {
            updateFileContiguous(fileName, newData);
        } else if (allocationMethod.equals("chained")) {
            updateFileChained(fileName, newData);
        } else if (allocationMethod.equals("indexed")) {
            updateFileIndexed(fileName, newData);
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

    private void updateFileIndexed(String fileName, byte[] newData) {
        // Delete the existing file
        deleteFileIndexed(fileTable.get(fileName));
    
        // Create a new file with the updated data
        createFileIndexed(fileName, newData);
    }
    

    public void deleteFile(String fileName) {
        FileMetadata metadata = fileTable.get(fileName);
        if (metadata == null) {
            throw new IllegalArgumentException("File not found");
        }
    
        if (allocationMethod.equals("contiguous")) {
            deleteFileContiguous(fileName);
        } else if (allocationMethod.equals("chained")) {
            deleteFileChained(metadata);
        } else if (allocationMethod.equals("indexed")) {
            deleteFileIndexed(metadata);
        } else {
            throw new IllegalStateException("Unknown allocation method: " + allocationMethod);
        }
    }
    

    private void deleteFileContiguous(String fileName) {
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

    private void deleteFileIndexed(FileMetadata metadata) {
        int indexBlock = metadata.getIndexBlock();
        byte[] indexData = diskDrive.readBlock(indexBlock);
    
        // Free data blocks
        for (int i = 0; i < indexData.length; i++) {
            if (indexData[i] == -1) break; // Assuming -1 indicates no more data blocks
            markBlockAsFree(indexData[i]);
        }
    
        // Free index block
        markBlockAsFree(indexBlock);
    
        // Update file table
        fileTable.remove(metadata);
    }
    
    private void markBlockAsFree(int blockNumber) {
        // Read the bitmap block
        byte[] bitmap = diskDrive.readBlock(1); // Assuming bitmap is stored in block 1
    
        // Calculate the byte and bit position for the block number in the bitmap
        int byteIndex = blockNumber / 8;
        int bitIndex = blockNumber % 8;
    
        // Clear the bit corresponding to the block number
        bitmap[byteIndex] &= ~(1 << bitIndex);
    
        // Write the updated bitmap back to the disk
        diskDrive.writeBlock(1, bitmap);
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

    private void writeDataToBlocks(List<Integer> blocks, byte[] data) {
        int bytesPerBlock = DiskDrive.getBlockSize();
        int dataIndex = 0;
    
        for (int blockNumber : blocks) {
            byte[] blockData = new byte[bytesPerBlock];
            int length = Math.min(dataIndex + bytesPerBlock, data.length) - dataIndex;
            System.arraycopy(data, dataIndex, blockData, 0, length);
    
            diskDrive.writeBlock(blockNumber, blockData);
            dataIndex += length;
            if (dataIndex >= data.length) {
                break;
            }
        }
    }

    private void writeIndexBlock(int indexBlock, List<Integer> dataBlocks) {
        byte[] indexData = new byte[DiskDrive.getBlockSize()];
        int i = 0;
        for (int block : dataBlocks) {
            indexData[i++] = (byte) block;
            if (i >= DiskDrive.getBlockSize()) {
                break; // Prevent exceeding the block size
            }
        }
        while (i < DiskDrive.getBlockSize()) {
            indexData[i++] = (byte) -1; // Fill the rest with -1 to indicate no more data blocks
        }
        diskDrive.writeBlock(indexBlock, indexData);
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
        List<Integer> potentialBlocks = new ArrayList<>();
    
        // Populate the list with indices of all free blocks
        byte[] bitmap = diskDrive.readBlock(1); // Assuming bitmap is stored in block 1
        for (int i = 0; i < DiskDrive.NUM_BLOCKS; i++) {
            if (isBlockFree(bitmap, i)) {
                potentialBlocks.add(i);
            }
        }
    
        // Randomly pick blocks from the list of potential blocks
        Random random = new Random();
        while (!potentialBlocks.isEmpty() && freeBlocks.size() < requiredBlocks) {
            int randomIndex = random.nextInt(potentialBlocks.size());
            freeBlocks.add(potentialBlocks.remove(randomIndex));
        }
    
        if (freeBlocks.size() < requiredBlocks) {
            return Collections.emptyList(); // Not enough space
        }
        return freeBlocks;
    }
    
    
    private void chainAndWriteBlocks(List<Integer> blocks, byte[] data) {
        int bytesPerBlock = DiskDrive.getBlockSize() - 1; // Last byte for next block reference
        int dataIndex = 0;
    
        for (int i = 0; i < blocks.size(); i++) {
            byte[] blockData = new byte[DiskDrive.getBlockSize()];
            int nextBlock = (i == blocks.size() - 1) ? -1 : blocks.get(i + 1); // -1 indicates the end
    
            int length = Math.min(dataIndex + bytesPerBlock, data.length) - dataIndex;
            System.arraycopy(data, dataIndex, blockData, 0, length);
            blockData[bytesPerBlock] = (byte) nextBlock; // Set next block reference
    
            diskDrive.writeBlock(blocks.get(i), blockData);
            dataIndex += length;
        }
    }
    
    
    private void updateFATForChained(String fileName, int startBlock) {
        fileTable.put(fileName, new FileMetadata(startBlock, -1)); // Length might not be needed for chained
    }

    private int findFreeIndexBlock() {
        byte[] bitmap = diskDrive.readBlock(1); // Assuming bitmap is stored in block 1
        for (int i = 0; i < DiskDrive.NUM_BLOCKS; i++) {
            if (isBlockFree(bitmap, i)) {
                return i;
            }
        }
        return -1; // Indicate that no free block is available
    }
    
    
    private List<Integer> findFreeDataBlocks(int dataSize) {
        int requiredBlocks = (int) Math.ceil((double) dataSize / DiskDrive.getBlockSize());
        List<Integer> freeBlocks = new ArrayList<>();
        byte[] bitmap = diskDrive.readBlock(1); // Assuming bitmap is stored in block 1
    
        for (int i = 0; i < DiskDrive.NUM_BLOCKS && freeBlocks.size() < requiredBlocks; i++) {
            if (isBlockFree(bitmap, i)) {
                freeBlocks.add(i);
            }
        }
    
        if (freeBlocks.size() < requiredBlocks) {
            return Collections.emptyList(); // Not enough space
        }
        return freeBlocks;
    }
    

}
