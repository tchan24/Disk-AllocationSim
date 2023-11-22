import java.util.Scanner;

public class UserInterface {
    private FileSystem fileSystem;

    public UserInterface(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        while (!exit) {
            // Display menu and handle user input
            System.out.println("1) Display a file");
            // ... other menu items
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            // Handle choice
        }
    }

    // Additional methods to handle each menu option
}
