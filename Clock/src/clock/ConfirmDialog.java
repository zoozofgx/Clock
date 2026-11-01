package clock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 退出确认窗口：托盘点“关闭”时弹出。
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
    }

    public static void show(MainWindow owner) {
        final JDialog dialog = new JDialog(owner, false);
        dialog.setUndecorated(true);
        try {
            dialog.setType(Window.Type.UTILITY);
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }

        final int w = 460;
        final int h = 200;

        JPanel panel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 45, 48));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(new Color(80, 80, 86));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("确认关闭");
        title.setForeground(new Color(235, 235, 240));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        title.setBounds(22, 16, 240, 24);
        panel.add(title);

        JLabel message = new JLabel("关闭时钟后，所有倒计时都会自动关闭。");
        message.setForeground(new Color(205, 210, 220));
        message.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        message.setBounds(22, 58, w - 44, 24);
        panel.add(message);

        JButton cancelBtn = UiKit.createDialogButton("取消", new Color(200, 200, 205));
        cancelBtn.setBounds(w - 18 - 80 - 12 - 80, h - 16 - 30, 80, 30);
        cancelBtn.addActionListener(e -> dialog.dispose());
        panel.add(cancelBtn);

        JButton okBtn = UiKit.createDialogButton("关闭", new Color(226, 48, 64));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBounds(w - 18 - 80, h - 16 - 30, 80, 30);
        okBtn.addActionListener(e -> {
            dialog.dispose();
            owner.requestClose();
        });
        panel.add(okBtn);

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        if (owner.isShowing()) {
            dialog.setLocationRelativeTo(owner);
        } else {
            dialog.setLocationRelativeTo(null);   // 窗口收在托盘里就居中到屏幕
        }

        final Rectangle to = dialog.getBounds();
        final Rectangle from = new Rectangle(
                to.x + to.width / 2 - 30, to.y + to.height / 2 - 15, 60, 30);

        dialog.setBounds(from);
        UiKit.applyDialogShape(dialog, from);
        dialog.setVisible(true);
        owner.startOpenAnimation(dialog, from, to, 100);
    }
}
