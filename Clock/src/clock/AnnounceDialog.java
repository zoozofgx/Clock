package clock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 公告窗口：显示 AppInfo.ANNOUNCEMENT 的内容。
 */
public final class AnnounceDialog {

    private static final String ANNOUNCEMENT = AppInfo.ANNOUNCEMENT;

    private AnnounceDialog() {
    }

    public static void show(MainWindow owner) {
        final JDialog dialog = new JDialog(owner, false);
        dialog.setUndecorated(true);
        try {
            dialog.setType(Window.Type.UTILITY);
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }

        final int w = 580;
        final int h = 440;

        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 45, 48));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(new Color(80, 80, 86));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.setColor(new Color(62, 62, 68));
                g2.drawLine(18, 44, getWidth() - 19, 44);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("公告");
        title.setForeground(new Color(235, 235, 240));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        title.setBounds(20, 12, 200, 24);
        panel.add(title);

        // 第一行就是公告常量，后面要加内容直接改 ANNOUNCEMENT
        JTextArea body = new JTextArea(ANNOUNCEMENT);
        body.setEditable(false);
        body.setOpaque(false);
        body.setForeground(new Color(215, 220, 230));
        body.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBorder(null);
        body.setBounds(22, 62, w - 44, h - 62 - 64);
        panel.add(body);

        JButton closeBtn = UiKit.createDialogButton("关闭", new Color(200, 200, 205));
        closeBtn.setBounds(w - 18 - 72, h - 16 - 28, 72, 28);
        closeBtn.addActionListener(e -> owner.animateDialogClose(dialog, owner.announceButton));
        panel.add(closeBtn);

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(owner);

        final Rectangle to = dialog.getBounds();
        Rectangle from = to;
        if (owner.announceButton != null && owner.announceButton.isShowing()) {
            try {
                Point p = owner.announceButton.getLocationOnScreen();
                from = new Rectangle(p.x, p.y, owner.announceButton.getWidth(), owner.announceButton.getHeight());
            } catch (Exception ignored) {
                // 取不到坐标就直接出现在最终位置
            }
        }

        dialog.setBounds(from);
        UiKit.applyDialogShape(dialog, from);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                owner.hideGlassOverlay();
            }
        });

        owner.showGlassOverlay();
        owner.startOpenAnimation(dialog, from, to, 100);
        dialog.setVisible(true);
    }
}
