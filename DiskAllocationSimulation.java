public class DiskAllocationSimulation {
    public static void main(String[] args) {
        String allocationMethod = "contiguous"; // Default allocation method

        if (args.length > 0) {
            if (args[0].equals("contiguous") || args[0].equals("chained") || args[0].equals("indexed")) {
                allocationMethod = args[0];
            } else {
                System.out.println("Invalid allocation method. Defaulting to contiguous.");
            }
        }

        DiskDrive diskDrive = new DiskDrive();
        FileSystem fileSystem = new FileSystem(diskDrive, allocationMethod);
        UserInterface ui = new UserInterface(fileSystem);
        ui.start();
    }
}
