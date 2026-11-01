package clock;

import java.awt.Toolkit;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * 提示音播放：负责加载 wav、播放/停止、以及记住用户选的是哪一个。
 */
public final class SoundPlayer {

    private static int choice = loadChoice();
    private static Clip clip;
    private static int loadedIndex = -1;
    private static boolean preview;

    private SoundPlayer() {
    }

    private static int loadChoice() {
        int c = Settings.getInt("soundChoice", 0);
        return (c < 0 || c >= AppConfig.SOUND_NAMES.length) ? 0 : c;
    }

    public static boolean isPreview() {
        return preview;
    }

    public static int getChoice() {
        return choice;
    }

    public static void setChoice(int index) {
        choice = index;
        saveChoice();
    }

    public static String name() {
        return AppConfig.SOUND_NAMES[choice];
    }

    public static String nameAt(int index) {
        return AppConfig.SOUND_NAMES[index];
    }

    public static int count() {
        return AppConfig.SOUND_NAMES.length;
    }

    public static void play() {
        play(false);
    }

    public static void play(boolean isPreview) {
        preview = isPreview;
        try {
            if (clip == null || loadedIndex != choice) {
                if (clip != null) {
                    clip.close();
                    clip = null;
                }
                File file = Assets.findSoundFile(AppConfig.SOUND_FILES[choice]);
                if (file == null) {
                    // 选中的那个找不到，就试另一个
                    int other = (choice + 1) % AppConfig.SOUND_FILES.length;
                    file = Assets.findSoundFile(AppConfig.SOUND_FILES[other]);
                    if (file != null) {
                        loadedIndex = other;
                    }
                }
                if (file == null) {
                    Toolkit.getDefaultToolkit().beep();   // 两个都没有，退回系统提示音
                    return;
                }
                AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                Clip newClip = AudioSystem.getClip();
                newClip.open(stream);
                clip = newClip;
                loadedIndex = choice;
            }
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void stop() {
        try {
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
        } catch (Exception ignored) {
            // 忽略
        }
        preview = false;
    }

    public static void saveChoice() {
        Settings.setInt("soundChoice", choice);
    }
}
