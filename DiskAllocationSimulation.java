public class DiskAllocationSimulation {
    public static void main(String[] args) {
        // Check command line arguments for allocation method
        String allocationMethod = args.length > 0 ? args[0] : "contiguous";

        DiskDrive diskDrive = new DiskDrive();
        FileSystem fileSystem = new FileSystem(diskDrive);
        UserInterface ui = new UserInterface(fileSystem);

        ui.start();
    }
}
