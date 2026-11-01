package clock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 配置文件：程序和 exe 同级的 config\settings.ini。
 * 换新版本时只替换 exe，这个文件会保留下来。
 */
public final class Settings {

    private static final Properties PROPS = new Properties();
    private static File configFile;

    private Settings() {
    }

    // 程序所在目录（exe 旁边；开发时是项目目录）
    public static File homeDir() {
        String home = System.getProperty("clock.home");
        if (home != null && !home.isEmpty()) {
            return new File(home);
        }
        File dir = Assets.classDir();
        if (dir != null) {
            return dir.getParentFile() != null ? dir.getParentFile() : dir;
        }
        return new File(System.getProperty("user.dir"));
    }

    public static File configDir() {
        return new File(homeDir(), "config");
    }

    public static File configFile() {
        if (configFile == null) {
            configFile = new File(configDir(), "settings.ini");
        }
        return configFile;
    }

    private static void load() {
        File f = configFile();
        if (f.isFile()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8)) {
                PROPS.load(reader);
            } catch (Exception ignored) {
                // 读不了就用默认值
            }
        }
    }

    public static int getInt(String key, int def) {
        load();
        try {
            return Integer.parseInt(PROPS.getProperty(key, String.valueOf(def)).trim());
        } catch (Exception e) {
            return def;
        }
    }

    public static void setInt(String key, int value) {
        load();
        PROPS.setProperty(key, String.valueOf(value));
        save();
    }

    public static String get(String key, String def) {
        load();
        return PROPS.getProperty(key, def);
    }

    public static void set(String key, String value) {
        load();
        PROPS.setProperty(key, value);
        save();
    }

    private static void save() {
        try {
            File dir = configDir();
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(configFile()), StandardCharsets.UTF_8)) {
                PROPS.store(writer, "Clock 设置文件");
            }
        } catch (Exception ignored) {
            // 写不了就只在本次运行生效
        }
    }
}
