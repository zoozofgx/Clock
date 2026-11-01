package clock;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static clock.AppConfig.IMAGE_DIR;
import static clock.AppConfig.SOUND_DIR;

/**
 * 资源查找：图片在 assets/images，音效在 assets/sounds。
 * 会自动在“当前目录 / class 目录 / 上级目录 / E:/JAVA/Clock”这些位置找，
 * 找不到时再退回旧位置（根目录、sounds/）。
 */
public final class Assets {

    private Assets() {
    }

    public static File classDir() {
        try {
            File location = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return location.isFile() ? location.getParentFile() : location;
        } catch (Exception e) {
            return null;
        }
    }

    public static File findFileInRoots(String relative) {
        List<File> candidates = new ArrayList<File>();

        candidates.add(new File(System.getProperty("user.dir"), relative));
        candidates.add(new File(System.getProperty("user.dir"), "Clock/" + relative));

        File dir = classDir();
        if (dir != null) {
            candidates.add(new File(dir, relative));
            candidates.add(new File(dir, "Clock/" + relative));
            if (dir.getParentFile() != null) {
                candidates.add(new File(dir.getParentFile(), relative));
                candidates.add(new File(dir.getParentFile(), "Clock/" + relative));
            }
        }

        candidates.add(new File("E:/JAVA/Clock/" + relative));

        for (File file : candidates) {
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    public static File findImageFile(String name) {
        File file = findFileInRoots(IMAGE_DIR + "/" + name);
        if (file != null) {
            return file;
        }
        return findFileInRoots(name);
    }

    public static File findSoundFile(String name) {
        File file = findFileInRoots(SOUND_DIR + "/" + name);
        if (file != null) {
            return file;
        }
        file = findFileInRoots("sounds/" + name);   // 兼容旧位置
        if (file != null) {
            return file;
        }
        return findFileInRoots(name);
    }

    public static File findIconFile() {
        return findImageFile("10001.gif");
    }
}
