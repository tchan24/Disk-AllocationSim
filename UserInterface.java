import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class UserInterface {
    private FileSystem fileSystem;
    private Scanner scanner;

    public UserInterface(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean exit = false;
        while (!exit) {
            try {
                // Display menu
                System.out.println("\n1) Display a file");
                System.out.println("2) Display the file table");
                System.out.println("3) Display the free space bitmap");
                System.out.println("4) Display a disk block");
                System.out.println("5) Copy a file from the simulation to a file on the real system");
                System.out.println("6) Copy a file from the real system to a file in the simulation");
                System.out.println("7) Delete a file");
                System.out.println("8) Exit");
                System.out.print("Choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline

                switch (choice) {
                    case 1:
                        displayFile();
                        break;
                    case 2:
                        displayFAT();
                        break;
                    case 3:
                        displayBitmap();
                        break;
                    case 4:
                        displayDiskBlock();
                        break;
                    case 5:
                        copyFileToRealSystem();
                        break;
                    case 6:
                        createFile(); // Option to copy file from real system to simulation
                        break;
                    case 7:
                        deleteFile();
                        break;
                    case 8:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine(); // consume the incorrect input
            }
        }
    }

    private void displayFile() {
        System.out.print("Enter file name to display: ");
        String fileName = scanner.nextLine();
        try {
            byte[] data = fileSystem.readFile(fileName);
            System.out.println("File Content: " + new String(data));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void createFile() {
        System.out.print("Enter file name to create: ");
        String fileName = scanner.nextLine();
        System.out.print("Enter file content: ");
        String content = scanner.nextLine();
        try {
            fileSystem.createFile(fileName, content.getBytes());
            System.out.println("File created successfully.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateFile() {
        // Implement logic to update a file
    }

    private void deleteFile() {
        System.out.print("Enter file name to delete: ");
        String fileName = scanner.nextLine();
        try {
            fileSystem.deleteFile(fileName);
            System.out.println("File deleted successfully.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void displayFAT() {
        // Logic to fetch and display the file allocation table
        // This will depend on how you have structured your FAT
    }
    
    private void displayBitmap() {
        byte[] bitmap = fileSystem.getBitmap();
        for (int i = 0; i < bitmap.length; i++) {
            System.out.println(String.format("Block %d: %s", i, bitmap[i] == 0 ? "Free" : "Occupied"));
        }
    }

    private void displayDiskBlock() {
        System.out.print("Enter disk block number to display: ");
        int blockNumber = scanner.nextInt();
        scanner.nextLine(); // Consume the newline

        try {
            byte[] blockData = fileSystem.readBlock(blockNumber);
            System.out.println("Block " + blockNumber + " Content: " + Arrays.toString(blockData));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void copyFileToRealSystem() {
        System.out.print("Enter the name of the file in the simulation to copy: ");
        String simFileName = scanner.nextLine();
        System.out.print("Enter the path to save the file on the real system: ");
        String realFilePath = scanner.nextLine();

        try {
            byte[] fileData = fileSystem.readFile(simFileName);
            Files.write(Paths.get(realFilePath), fileData);
            System.out.println("File copied successfully to " + realFilePath);
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public byte[] readBlock(int blockNumber) {
        // Validate block number
        if (blockNumber < 0 || blockNumber >= DiskDrive.NUM_BLOCKS) {
            throw new IllegalArgumentException("Invalid block number");
        }

        // Return the block data
        return DiskDrive.readBlock(blockNumber);
    }
    
    // Additional methods to support other operations
}
