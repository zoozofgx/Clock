package clock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 关于作者：显示 GitHub 和哔哩哔哩两个账号，点一下用浏览器打开。
 */
public final class AboutDialog {

    private AboutDialog() {
    }

    public static void show(MainWindow owner) {
        // 模态：打开期间其它界面都点不了，也抢不到前面
        final JDialog dialog = new JDialog(owner, true);
        dialog.setUndecorated(true);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        try {
            dialog.setType(Window.Type.UTILITY);
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }

        final int w = 580;   // 和设置面板一样大
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

        JLabel title = new JLabel("作者：" + AppInfo.AUTHOR_NAME);
        title.setForeground(new Color(235, 235, 240));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        title.setBounds(20, 12, 200, 24);
        panel.add(title);

        // GitHub
        JLabel ghTag = new JLabel("GitHub");
        ghTag.setForeground(new Color(235, 235, 240));
        ghTag.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        ghTag.setBounds(22, 70, 160, 20);
        panel.add(ghTag);

        JLabel ghLink = linkLabel(AppInfo.GITHUB_URL);
        ghLink.setBounds(22, 93, w - 44, 20);
        panel.add(ghLink);

        // 哔哩哔哩
        JLabel blTag = new JLabel("哔哩哔哩");
        blTag.setForeground(new Color(235, 235, 240));
        blTag.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        blTag.setBounds(22, 133, 160, 20);
        panel.add(blTag);

        JLabel blLink = linkLabel(AppInfo.BILIBILI_URL);
        blLink.setBounds(22, 156, w - 44, 20);
        panel.add(blLink);

        // QQ 群：点击复制群号
        JLabel qqTag = new JLabel("QQ 群");
        qqTag.setForeground(new Color(235, 235, 240));
        qqTag.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        qqTag.setBounds(22, 196, 160, 20);
        panel.add(qqTag);

        final JLabel qqValue = new JLabel(AppInfo.QQ_GROUP);
        qqValue.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        qqValue.setForeground(new Color(173, 206, 238));
        qqValue.setBounds(22, 219, w - 44, 20);
        qqValue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        qqValue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(AppInfo.QQ_GROUP), null);
                qqValue.setText(AppInfo.QQ_GROUP + "  （已复制，去 QQ 粘贴搜索）");
                Timer timer = new Timer(1500, ev -> qqValue.setText(AppInfo.QQ_GROUP));
                timer.setRepeats(false);
                timer.start();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                qqValue.setForeground(new Color(215, 233, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                qqValue.setForeground(new Color(173, 206, 238));
            }
        });
        panel.add(qqValue);

        JButton closeBtn = UiKit.createDialogButton("关闭", new Color(200, 200, 205));
        closeBtn.setBounds(w - 18 - 72, h - 16 - 28, 72, 28);
        closeBtn.addActionListener(e -> dialog.dispose());
        panel.add(closeBtn);

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(owner);

        dialog.setSize(w, h);
        UiKit.prepareUndecorated(dialog);
        UiKit.disableShape(dialog);

        // 不做出现动画，直接按最终大小显示
        UiKit.applyDialogShape(dialog, dialog.getBounds());

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (owner != null) {
                    owner.hideGlassOverlay();
                }
            }
        });

        if (owner != null) {
            owner.showGlassOverlay();
        }
        dialog.setVisible(true);
    }

    // 一行可点击的网址
    private static JLabel linkLabel(String url) {
        JLabel label = new JLabel(url);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        label.setForeground(new Color(173, 206, 238));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UiKit.openUrl(url);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(new Color(215, 233, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(new Color(173, 206, 238));
            }
        });
        return label;
    }
}
