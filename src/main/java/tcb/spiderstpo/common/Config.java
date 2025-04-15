package tcb.spiderstpo.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    public static final Config DEFAULT = new Config(true, false);
    private static Config INSTANCE = null;
    private final boolean preventClimbingInRain;
    private final boolean pathFinderDebugPreview;

    private Config(boolean preventClimbingInRain, boolean pathFinderDebugPreview) {
        this.preventClimbingInRain = preventClimbingInRain;
        this.pathFinderDebugPreview = pathFinderDebugPreview;
    }

    public static Config getConfig() {
        return getConfig(false);
    }

    public static Config getConfig(boolean serialize) {
        if (INSTANCE == null || serialize) {
            INSTANCE = readConfig();
        }

        return INSTANCE;
    }

    private static Config readConfig() {
        final Path path = FabricLoader.getInstance().getConfigDir().resolve("spiders-2.0.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path.getParent());
                BufferedWriter writer = Files.newBufferedWriter(path);
                final String s = gson.toJson(DEFAULT);
                Files.write(path, s.getBytes());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            return gson.fromJson(new FileReader(path.toFile()), Config.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return DEFAULT;
    }

    public boolean isPathFinderDebugPreview() {
        return pathFinderDebugPreview;
    }

    public boolean isPreventClimbingInRain() {
        return preventClimbingInRain;
    }
}
