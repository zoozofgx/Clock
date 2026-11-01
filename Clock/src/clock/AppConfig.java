package clock;

/**
 * 程序的固定配置：资源目录、提示音、单实例端口。
 * （版本号和公告内容在 AppInfo.java 里）
 */
public final class AppConfig {

    private AppConfig() {
    }

    // ===== 资源目录 =====
    public static final String IMAGE_DIR = "assets/images";
    public static final String SOUND_DIR = "assets/sounds";

    // ===== 提示音 =====
    public static final String[] SOUND_NAMES = { "新世界", "universfield" };
    public static final String[] SOUND_FILES = { "ringtone_1.wav", "ringtone_2.wav" };

    // ===== 单实例端口 =====
    public static final int APP_PORT = 47531;
}
