package clock;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * 界面小工具：圆角按钮、对勾图标、窗口圆角、颜色微调。
 */
public final class UiKit {

    private UiKit() {
    }

    // ===== 动画工具：按时间插值（比按帧数插值平滑得多）=====

    /**
     * 无边框弹窗的准备工作：
     * 1. 背景设成全透明（否则第一帧会露出系统默认的灰底 —— 就是那一下"空白框"）
     * 2. 按最终尺寸把内容先布局好（否则缩小显示时内容是空的，看起来也是白框）
     */
    public static void prepareUndecorated(JDialog dialog) {
        try {
            if (supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
                dialog.setBackground(new Color(0, 0, 0, 0));
            } else {
                dialog.setBackground(new Color(45, 45, 48));
            }
        } catch (Exception ignored) {
            // 忽略
        }
        dialog.validate();
    }

    /** 缓动曲线 */
    public enum Ease {
        LINEAR, OUT_CUBIC, IN_CUBIC, OUT_QUINT, IN_OUT_CUBIC
    }

    /**
     * 跑一段动画：durationMs 毫秒内，每 16ms 回调一次 progress(0~1 已缓动)。
     * 返回 Timer，调用方可随时 stop。
     */
    public static javax.swing.Timer animate(int durationMs, Ease ease,
            java.util.function.DoubleConsumer onFrame, Runnable onDone) {
        final long start = System.currentTimeMillis();
        final int duration = Math.max(1, durationMs);
        final javax.swing.Timer[] holder = new javax.swing.Timer[1];
        holder[0] = new javax.swing.Timer(16, e -> {
            long elapsed = System.currentTimeMillis() - start;
            double p = Math.min(1.0, elapsed / (double) duration);
            double eased = applyEase(ease, p);
            onFrame.accept(eased);
            if (p >= 1.0) {
                holder[0].stop();
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
        holder[0].setCoalesce(true);
        holder[0].setInitialDelay(0);
        holder[0].start();
        return holder[0];
    }

    public static double applyEase(Ease ease, double p) {
        switch (ease) {
            case OUT_CUBIC:
                return 1 - Math.pow(1 - p, 3);
            case IN_CUBIC:
                return p * p * p;
            case OUT_QUINT:
                return 1 - Math.pow(1 - p, 5);
            case IN_OUT_CUBIC:
                return p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2;
            default:
                return p;
        }
    }

    // 用系统默认浏览器打开网址
    public static void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new java.net.URI(url));
                return;
            }
        } catch (Exception ignored) {
            // 落到后面的兜底方式
        }
        try {
            Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", url });
        } catch (Exception ignored) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static boolean supportsTranslucency(GraphicsDevice.WindowTranslucency kind) {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .isWindowTranslucencySupported(kind);
        } catch (Exception e) {
            return false;
        }
    }

    /** 两个颜色按 t(0~1) 混合 */
    public static Color blend(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    public static Color lighten(Color c, float amount) {
        int r = Math.round(c.getRed() + (255 - c.getRed()) * amount);
        int g = Math.round(c.getGreen() + (255 - c.getGreen()) * amount);
        int b = Math.round(c.getBlue() + (255 - c.getBlue()) * amount);
        return new Color(r, g, b);
    }

    public static ImageIcon checkIcon(Color color) {
        BufferedImage img = new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawPolyline(new int[] { 2, 6, 12 }, new int[] { 7, 11, 3 }, 3);
        g.dispose();
        return new ImageIcon(img);
    }

    public static ImageIcon blankIcon() {
        return new ImageIcon(new BufferedImage(14, 14, BufferedImage.TYPE_INT_ARGB));
    }

    /** 让某个窗口不再做圆角裁剪（输入法在带 Shape 的窗口里会出问题） */
    public static void disableShape(JDialog dialog) {
        if (dialog != null && dialog.getRootPane() != null) {
            dialog.getRootPane().putClientProperty("noShape", Boolean.TRUE);
        }
    }

    public static void applyDialogShape(Window window, Rectangle r) {
        if (window instanceof JDialog) {
            Object noShape = ((JDialog) window).getRootPane().getClientProperty("noShape");
            if (Boolean.TRUE.equals(noShape)) {
                return;   // 这个窗口不裁剪
            }
        }
        if (!supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            return;
        }
        try {
            window.setShape(new RoundRectangle2D.Double(0, 0, r.width, r.height, 14, 14));
        } catch (Exception ignored) {
            // 忽略
        }
    }

    public static void applyPopupShape(JDialog popup, int w, int h) {
        if (!supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            return;
        }
        try {
            popup.setShape(new RoundRectangle2D.Double(0, 0, w, h, 10, 10));
        } catch (Exception ignored) {
            // 忽略
        }
    }

    /** 让按钮的悬停状态平滑过渡（0~1），返回按钮自己 */
    public static void smoothHover(JButton btn) {
        final float[] glow = { 0f };
        final float[] target = { 0f };
        btn.getModel().addChangeListener(e -> {
            boolean on = btn.getModel().isRollover() || btn.getModel().isArmed();
            float want = on ? 1f : 0f;
            if (want == target[0]) {
                return;
            }
            target[0] = want;
            final float start = glow[0];
            animate(150, Ease.OUT_CUBIC, p -> {
                glow[0] = (float) (start + (want - start) * p);
                btn.repaint();
            }, null);
        });
        btn.putClientProperty("hoverGlow", glow);
    }

    /** 取按钮当前的悬停强度（0~1） */
    public static float hoverGlow(JButton btn) {
        Object o = btn.getClientProperty("hoverGlow");
        return o instanceof float[] ? ((float[]) o)[0] : 0f;
    }

    public static JButton createDialogButton(String text, final Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float glow = hoverGlow(this);
                g2.setColor(blend(bg, lighten(bg, 0.22f), glow));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        btn.setForeground(new Color(30, 40, 55));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        smoothHover(btn);
        return btn;
    }

    public static JButton createDarkButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float glow = hoverGlow(this);
                g2.setColor(blend(new Color(42, 42, 46), new Color(58, 58, 64), glow));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(85, 85, 92));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // 右侧的小三角（代替 ▾ 字符，避免字体缺字变方块）
                int tx = getWidth() - 20;
                int ty = getHeight() / 2 - 2;
                g2.setColor(new Color(200, 205, 215));
                g2.fillPolygon(new int[] { tx, tx + 8, tx + 4 },
                        new int[] { ty, ty, ty + 5 }, 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        btn.setForeground(new Color(225, 228, 233));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 12, 0, 28));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        UiKit.smoothHover(btn);
        return btn;
    }

    public static JButton createSoundItem(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover() || getModel().isArmed()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(62, 62, 68));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        btn.setForeground(new Color(225, 228, 233));
        btn.setHorizontalAlignment(JButton.LEFT);
        btn.setIconTextGap(8);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 10, 0, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        UiKit.smoothHover(btn);
        return btn;
    }

    public static JButton createMutedButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(38, 38, 42));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(62, 62, 68));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                int tx = getWidth() - 20;
                int ty = getHeight() / 2 - 2;
                g2.setColor(new Color(110, 114, 122));
                g2.fillPolygon(new int[] { tx, tx + 8, tx + 4 },
                        new int[] { ty, ty, ty + 5 }, 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        btn.setForeground(new Color(140, 145, 155));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setMargin(new Insets(0, 12, 0, 28));
        btn.setCursor(Cursor.getDefaultCursor());
        btn.addActionListener(e -> {
            // 功能开发中，点了不做任何事
        });
        return btn;
    }
}
