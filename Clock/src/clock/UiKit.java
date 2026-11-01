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

    public static void applyDialogShape(Window window, Rectangle r) {
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

    public static JButton createDialogButton(String text, final Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover() || getModel().isArmed();
                g2.setColor(hover ? lighten(bg, 0.22f) : bg);
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
        return btn;
    }

    public static JButton createDarkButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover() || getModel().isArmed();
                g2.setColor(hover ? new Color(58, 58, 64) : new Color(42, 42, 46));
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
