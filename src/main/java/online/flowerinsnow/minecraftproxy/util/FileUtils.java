package online.flowerinsnow.minecraftproxy.util;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
    private FileUtils() {
    }

    public static Path getCodeSourcePath() {
        URL url = FileUtils.class.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(url.getPath().substring(1));
    }
}
