public class DiskAllocationSimulation {
    public static void main(String[] args) {
        String allocationMethod = args.length > 0 ? args[0] : "contiguous";
        DiskDrive diskDrive = new DiskDrive();
        
        byte[] testBlock = diskDrive.readBlock(0); // Test read
        FileSystem fileSystem = new FileSystem(diskDrive, allocationMethod);
        UserInterface ui = new UserInterface(fileSystem);
        ui.start();
    }
}
