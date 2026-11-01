package clock;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 程序入口：设置外观、判断是否已有实例、打开主窗口。
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 下拉列表用深色配色
        UIManager.put("PopupMenu.background", new Color(40, 40, 44));
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(new Color(80, 80, 86)));
        UIManager.put("MenuItem.background", new Color(40, 40, 44));
        UIManager.put("MenuItem.foreground", new Color(225, 228, 233));
        UIManager.put("MenuItem.selectionBackground", new Color(70, 70, 78));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);

        // 先看看是不是已经有一个在跑
        if (MainWindow.notifyRunningInstance()) {
            System.out.println("↪ 已经有一个在运行了，已把它的窗口叫到前面。");
            System.exit(0);
        }

        SwingUtilities.invokeLater(() -> {
                    MainWindow w = new MainWindow();
            w.showWithFadeIn();
        });
    }
}
