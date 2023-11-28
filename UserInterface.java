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
                    createFile();
                    break;
                case 3:
                    updateFile();
                    break;
                case 4:
                    deleteFile();
                    break;
                case 5:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
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
        // Implement logic to create a file
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
        // Implement logic to display the file table
    }
    
    private void displayBitmap() {
        byte[] bitmap = fileSystem.getBitmap();
        StringBuilder bitmapDisplay = new StringBuilder();
        for (byte b : bitmap) {
            bitmapDisplay.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        System.out.println("Bitmap: " + bitmapDisplay.toString());
    }
    

    // Additional methods to support other operations
}
