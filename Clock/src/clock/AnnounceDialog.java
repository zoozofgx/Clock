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

        // 公告用 HTML 渲染，这样文字可以带颜色：
        //   <color=#FFD24A>要变色的文字</color>
        JEditorPane body = new JEditorPane();
        body.setContentType("text/html");
        body.setEditable(false);
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        body.setText(toHtml(ANNOUNCEMENT));
        // 公告可能很长：放进滚动面板，可以上下滑
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBounds(22, 62, w - 44, h - 62 - 64);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JScrollBar bar = scroll.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setBackground(new Color(45, 45, 48));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(115, 115, 126);
                trackColor = new Color(45, 45, 48);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return zeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return zeroButton();
            }

            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
        });
        bar.setUnitIncrement(18);

        // 滚轮平滑滚动：像倒计时列表那样"追"过去，而不是一格一跳
        scroll.setWheelScrollingEnabled(false);
        scroll.addMouseWheelListener(e -> smoothScroll(bar, e.getWheelRotation() * 64));

        panel.add(scroll);

        JButton closeBtn = UiKit.createDialogButton("关闭", new Color(200, 200, 205));
        closeBtn.setBounds(w - 18 - 72, h - 16 - 28, 72, 28);
        closeBtn.addActionListener(e -> owner.animateDialogClose(dialog, owner.announceButton));
        panel.add(closeBtn);

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(owner);

        dialog.setSize(w, h);
        UiKit.prepareUndecorated(dialog);
        UiKit.disableShape(dialog);

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

        dialog.validate();

        dialog.setBounds(from);
        UiKit.applyDialogShape(dialog, from);

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                owner.hideGlassOverlay();
            }
        });

        owner.showGlassOverlay();
        owner.startOpenAnimation(dialog, from, to, 200);
        dialog.setVisible(true);
    }

    /**
     * 把公告文本转成 HTML：
     * - 换行 -> <br>
     * - <color=#RRGGBB>文字</color> -> <font color="#RRGGBB">文字</font>
     */
    private static String toHtml(String text) {
        String escaped = text.replace("\n", "<br>")
                .replace("<color=", "<font color=")
                .replace("</color>", "</font>");
        return "<html><body style=\"margin:0; padding:0; color:#D7DCE6; "
                + "font-family:'微软雅黑'; font-size:12px; line-height:1.6;\">"
                + escaped + "</body></html>";
    }

    // ===== 平滑滚动：滚轮每次推进一点，靠定时器逐帧追过去 =====
    private static javax.swing.Timer scrollAnimation;
    private static JScrollBar scrollBarRef;
    private static int scrollTarget;

    private static void smoothScroll(JScrollBar bar, int delta) {
        scrollBarRef = bar;

        int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
        int base = (scrollAnimation != null && scrollAnimation.isRunning()) ? scrollTarget : bar.getValue();
        scrollTarget = Math.max(0, Math.min(base + delta, max));

        if (scrollAnimation == null) {
            scrollAnimation = new javax.swing.Timer(15, e -> {
                if (scrollBarRef == null) {
                    scrollAnimation.stop();
                    return;
                }
                int current = scrollBarRef.getValue();
                int diff = scrollTarget - current;
                if (Math.abs(diff) <= 1) {
                    scrollBarRef.setValue(scrollTarget);
                    scrollAnimation.stop();
                    return;
                }
                scrollBarRef.setValue(current + (int) Math.round(diff * 0.28));
            });
        }
        if (!scrollAnimation.isRunning()) {
            scrollAnimation.start();
        }
    }
}
