package filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RemovableDeviceManager {
    private static final String MEDIA_PATH = "/media"; // Путь к монтированным устройствам
    private static final String MNT_PATH = "/mnt";

    public List<File> getMountedDevices() {
        List<File> devices = new ArrayList<>();
        File mediaDir = new File(MEDIA_PATH);
        File mntDir = new File(MNT_PATH);

        if (mediaDir.exists() && mediaDir.isDirectory()) {
            addMountedDevices(devices, mediaDir);
        }
        if (mntDir.exists() && mntDir.isDirectory()) {
            addMountedDevices(devices, mntDir);
        }

        return devices;
    }

    private void addMountedDevices(List<File> devices, File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    devices.add(file);
                }
            }
        }
    }
}