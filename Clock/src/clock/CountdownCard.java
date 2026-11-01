package clock;

import java.awt.*;
import javax.swing.*;

/**
 * 倒计时卡片：显示名称、剩余时间，带暂停/关闭按钮。
 * 用 Listener 和主窗口通信，自己不直接碰主窗口。
 */
public class CountdownCard extends JPanel {

    /** 卡片回调：到点、刷新列表、请求删除 */
    public interface Listener {
        void onFinished(String title);

        void refreshList();

        void onRemoveRequested(CountdownCard card);
    }

    public static String formatDuration(long millis) {
        long total = Math.max(0, millis) / 1000;
        long d = total / 86400;
        long h = (total % 86400) / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        if (d > 0) {
            return d + "天 " + String.format("%02d:%02d:%02d", h, m, s);
        }
        return String.format("%02d:%02d:%02d", h, m, s);
    }

        private static class PlayPauseButton extends JButton {

            private boolean pausedIcon;

            PlayPauseButton() {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setRolloverEnabled(true);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(34, 34));
                setToolTipText("暂停");
            }

            void setPausedIcon(boolean pausedIcon) {
                this.pausedIcon = pausedIcon;
                setToolTipText(pausedIcon ? "继续" : "暂停");
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int d = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - d) / 2;
                int y = (getHeight() - d) / 2;

                boolean hover = getModel().isRollover() || getModel().isArmed();

                if (!isEnabled()) {
                    g2.setColor(new Color(110, 110, 118));
                } else if (hover) {
                    g2.setColor(new Color(244, 76, 62));
                } else {
                    g2.setColor(new Color(226, 48, 40));   // 红色圆
                }
                g2.fillOval(x, y, d, d);

                g2.setColor(new Color(255, 255, 255, isEnabled() ? 255 : 170));
                if (pausedIcon) {
                    // 播放：三角形
                    int s = (int) (d * 0.42);
                    int cx = x + d / 2;
                    int cy = y + d / 2;
                    int[] xs = { cx - s / 3, cx - s / 3, cx + s / 2 };
                    int[] ys = { cy - s / 2, cy + s / 2, cy };
                    g2.fillPolygon(xs, ys, 3);
                } else {
                    // 暂停：两根竖条
                    int barW = Math.max(3, d / 7);
                    int barH = Math.max(8, (int) (d * 0.55));
                    int gap = Math.max(3, d / 8);
                    int totalW = barW * 2 + gap;
                    int left = x + (d - totalW) / 2;
                    int top = y + (d - barH) / 2;
                    g2.fillRoundRect(left, top, barW, barH, barW, barW);
                    g2.fillRoundRect(left + barW + gap, top, barW, barH, barW, barW);
                }

                g2.dispose();
            }
        }




        private final JLabel timeLabel;
        private final JLabel prefixLabel;
        private final JLabel nameLabel;
        private final PlayPauseButton pauseButton;
        private final String title;
        private final Listener listener;
        private long endTime;
        private long remainingMillis;
        private boolean paused;
        boolean finished;

        CountdownCard(long totalMillis, String name, Listener listener) {
            this.listener = listener;
            this.title = name == null ? "" : name;
            remainingMillis = totalMillis;
            endTime = System.currentTimeMillis() + totalMillis;

            setOpaque(false);
            setLayout(new GridBagLayout());   // 三个元素都按行垂直居中
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 12));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
            setPreferredSize(new Dimension(100, 66));

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = 0;

            timeLabel = new JLabel(formatDuration(remainingMillis));
            timeLabel.setFont(new Font("Consolas", Font.BOLD, 26));
            timeLabel.setForeground(new Color(235, 235, 240));

            // "已超时"三个字用中文字体（Consolas 没有汉字，会变方块）
            prefixLabel = new JLabel("已超时");
            prefixLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            prefixLabel.setForeground(new Color(232, 90, 90));
            prefixLabel.setVisible(false);

            JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            timeRow.setOpaque(false);
            timeRow.add(prefixLabel);
            timeRow.add(timeLabel);

            // 名称 + 时间竖着排
            nameLabel = new JLabel(name == null ? "" : name);
            nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            nameLabel.setForeground(new Color(172, 178, 190));
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);
            nameLabel.setVisible(name != null && !name.isEmpty());

            timeRow.setAlignmentX(LEFT_ALIGNMENT);

            JPanel textColumn = new JPanel();
            textColumn.setOpaque(false);
            textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
            textColumn.add(nameLabel);
            textColumn.add(timeRow);

            c.gridx = 0;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            add(textColumn, c);

            pauseButton = new PlayPauseButton();
            pauseButton.addActionListener(e -> togglePause());
            c.gridx = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, 12, 0, 10);
            add(pauseButton, c);

            JButton closeButton = UiKit.createDialogButton("关闭", new Color(200, 200, 205));
            closeButton.setPreferredSize(new Dimension(64, 28));
            closeButton.addActionListener(e -> listener.onRemoveRequested(this));
            c.gridx = 2;
            c.insets = new Insets(0, 0, 0, 0);
            add(closeButton, c);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(38, 38, 42));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(70, 70, 78));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }

        private void togglePause() {
            if (finished) {
                return;
            }
            if (!paused) {
                remainingMillis = Math.max(0, endTime - System.currentTimeMillis());
                paused = true;
            } else {
                endTime = System.currentTimeMillis() + remainingMillis;
                paused = false;
            }
            pauseButton.setPausedIcon(paused);
            updateColors();
        }

        // 正常：白色；暂停：黄色；结束/超时：红色
        private void updateColors() {
            if (finished) {
                timeLabel.setForeground(new Color(232, 90, 90));
                prefixLabel.setForeground(new Color(232, 90, 90));
            } else if (paused) {
                timeLabel.setForeground(new Color(255, 210, 74));
                prefixLabel.setForeground(new Color(255, 210, 74));
            } else {
                timeLabel.setForeground(new Color(235, 235, 240));
                prefixLabel.setForeground(new Color(232, 90, 90));
            }
        }

        void tick() {
            if (paused) {
                return;
            }
            long now = System.currentTimeMillis();

            if (!finished) {
                long left = endTime - now;
                if (left <= 0) {
                    left = 0;
                    finished = true;
                    pauseButton.setPausedIcon(false);
                    pauseButton.setEnabled(false);
                    updateColors();
                    listener.refreshList();   // 已结束的挪到最上面
                    playAlert();              // 播放选中的提示音（只响一次）
                }
                remainingMillis = left;
                timeLabel.setText(formatDuration(left));
            } else {
                // 到点之后继续往上数，显示超时了多久
                if (!prefixLabel.isVisible()) {
                    prefixLabel.setVisible(true);
                    prefixLabel.getParent().revalidate();
                }
                timeLabel.setText(formatDuration(now - endTime));
            }
        }

        // 到点播放提示音；同一张卡片只会触发一次
        private boolean alerted;

        private void playAlert() {
            if (alerted) {
                return;   // 防重复：哪怕 tick 再来也不会再响
            }
            alerted = true;
            listener.onFinished(title);
        }
    
}
