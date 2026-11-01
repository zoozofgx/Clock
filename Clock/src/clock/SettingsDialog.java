package clock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * 设置面板：选提示音（主题暂未开放）。
 */
public final class SettingsDialog {

    private static JDialog soundPopup;   // 下拉弹窗（点第二次按钮可以收起）


    public static void show(MainWindow owner) {
        final JDialog dialog = new JDialog(owner, false);   // 非模态：点后面的窗口不会再"叮"
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

                // 标题下面一条淡淡的分隔线，给设置项留出区域
                g2.setColor(new Color(62, 62, 68));
                g2.drawLine(18, 44, getWidth() - 19, 44);
                g2.dispose();
            }
        };
        panel.setOpaque(false);

        JLabel title = new JLabel("设置");
        title.setForeground(new Color(235, 235, 240));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        title.setBounds(20, 12, 200, 24);
        panel.add(title);

        // ---- 提示音 ----
        JLabel soundLabel = new JLabel("提示音");
        soundLabel.setForeground(new Color(205, 210, 220));
        soundLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        soundLabel.setBounds(22, 66, 140, 28);
        panel.add(soundLabel);

        final JButton soundBtn = UiKit.createDarkButton(SoundPlayer.name());
        soundBtn.setBounds(w - 18 - 220, 64, 220, 32);
        soundBtn.addActionListener(e -> showSoundMenu(panel, soundBtn));
        panel.add(soundBtn);

        // ---- 主题（占位，功能开发中）----
        JLabel themeLabel = new JLabel("主题");
        themeLabel.setForeground(new Color(205, 210, 220));
        themeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        themeLabel.setBounds(22, 114, 140, 28);
        panel.add(themeLabel);

        JButton themeBtn = UiKit.createMutedButton("深色");
        themeBtn.setBounds(w - 18 - 220, 112, 220, 32);
        panel.add(themeBtn);

        // 悬停提示画在面板里（系统 tooltip 会被置顶的设置面板挡住）
        final JLabel themeHint = new JLabel("功能开发中", JLabel.RIGHT);
        themeHint.setForeground(new Color(232, 170, 90));
        themeHint.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        themeHint.setBounds(w - 18 - 220, 148, 220, 20);
        themeHint.setVisible(false);
        panel.add(themeHint);

        themeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                themeHint.setVisible(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                themeHint.setVisible(false);
            }
        });

        // 左下角：作者主页链接，点了用浏览器打开
        JLabel authorLink = new JLabel("作者：" + AppInfo.AUTHOR_NAME + "（GitHub · 哔哩哔哩）");
        authorLink.setForeground(new Color(150, 155, 165));
        authorLink.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        authorLink.setBounds(22, h - 16 - 16, w - 18 - 72 - 40, 18);
        authorLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        authorLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AboutDialog.show(owner);    // 打开关于窗口，设置面板留在下面
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                authorLink.setForeground(new Color(173, 206, 238));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                authorLink.setForeground(new Color(150, 155, 165));
            }
        });
        panel.add(authorLink);

        JButton closeBtn = UiKit.createDialogButton("关闭", new Color(200, 200, 205));
        closeBtn.setBounds(w - 18 - 72, h - 16 - 28, 72, 28);
        closeBtn.addActionListener(e -> owner.animateDialogClose(dialog, owner.settingsButton));
        panel.add(closeBtn);

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(owner);

        dialog.setSize(w, h);
        UiKit.prepareUndecorated(dialog);
        UiKit.disableShape(dialog);

        final Rectangle to = dialog.getBounds();
        Rectangle from = to;
        if (owner.settingsButton != null && owner.settingsButton.isShowing()) {
            try {
                Point p = owner.settingsButton.getLocationOnScreen();
                from = new Rectangle(p.x, p.y, owner.settingsButton.getWidth(), owner.settingsButton.getHeight());
            } catch (Exception ignored) {
                // 取不到坐标就直接出现在最终位置
            }
        }

        dialog.validate();

        dialog.setBounds(from);
        UiKit.applyDialogShape(dialog, from);

        // 设置关掉时：撤蒙版 + 停掉试听的音效
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                owner.hideGlassOverlay();
                if (SoundPlayer.isPreview()) {
                    SoundPlayer.stop();
                }
            }
        });

        owner.showGlassOverlay();
        owner.startOpenAnimation(dialog, from, to, 200);
        dialog.setVisible(true);
    }

    private static void showSoundMenu(JPanel panel, JButton soundBtn) {
        if (soundPopup != null && soundPopup.isShowing()) {
            soundPopup.dispose();
            soundPopup = null;
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(panel);
        final JDialog popup;
        if (owner instanceof Dialog) {
            popup = new JDialog((Dialog) owner, false);
        } else if (owner instanceof Frame) {
            popup = new JDialog((Frame) owner, false);
        } else {
            popup = new JDialog();
        }
        popup.setUndecorated(true);
        try {
            popup.setType(Window.Type.UTILITY);
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }

        final int width = Math.max(180, soundBtn.getWidth());
        final int rowH = 34;
        final int height = 8 + SoundPlayer.count() * rowH + 4;

        JPanel body = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 42, 46));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(85, 85, 92));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        body.setOpaque(false);

        for (int i = 0; i < SoundPlayer.count(); i++) {
            final int index = i;
            boolean selected = (index == SoundPlayer.getChoice());

            JButton item = UiKit.createSoundItem(SoundPlayer.nameAt(index));
            item.setBounds(5, 4 + i * rowH, width - 10, rowH - 2);
            if (selected) {
                item.setIcon(UiKit.checkIcon(new Color(173, 206, 238)));
                item.setForeground(new Color(173, 206, 238));
            }
            item.addActionListener(e -> {
                soundBtn.setText(SoundPlayer.nameAt(index));
                SoundPlayer.setChoice(index);
                SoundPlayer.play(true);   // 标记为试听，关设置时会被停掉
                popup.dispose();
                if (soundPopup == popup) {
                    soundPopup = null;
                }
            });
            body.add(item);
        }

        popup.setContentPane(body);

        Point at = soundBtn.getLocationOnScreen();
        final int sx = at.x;
        final int sy = at.y + soundBtn.getHeight() + 4;

        popup.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                popup.dispose();
                if (soundPopup == popup) {
                    soundPopup = null;
                }
            }
        });

        soundPopup = popup;

        // 展开动画：从 8px 平滑拉到完整高度（60fps）
        popup.setBounds(sx, sy, width, 8);
        UiKit.applyPopupShape(popup, width, 8);
        popup.setVisible(true);
        popup.requestFocus();

        final int startH = 8;
        final int endH = height;
        UiKit.animate(180, UiKit.Ease.OUT_QUINT, p -> {
            int h = Math.max(8, (int) Math.round(startH + (endH - startH) * p));
            popup.setBounds(sx, sy, width, h);
            UiKit.applyPopupShape(popup, width, h);
        }, () -> {
            popup.setBounds(sx, sy, width, endH);
            UiKit.applyPopupShape(popup, width, endH);
        });
    }
}
