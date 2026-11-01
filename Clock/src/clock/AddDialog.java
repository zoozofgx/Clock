package clock;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * “添加时钟”弹窗：填名称和时间，点“添加”生成倒计时。
 */
public final class AddDialog {

    private AddDialog() {
    }

    public static void show(MainWindow owner) {
        final JDialog dialog = new JDialog(owner, false);   // 非模态：点后面的窗口不会再"叮"
        dialog.setUndecorated(true);
        try {
            dialog.setType(Window.Type.UTILITY);
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }

        final int w = 520;
        final int h = 320;

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

        // ---- 名称输入 ----
        final int nameX = 41;
        final int nameW = 438;

        JLabel nameLabel = new JLabel("名称");
        nameLabel.setForeground(new Color(150, 155, 165));
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        nameLabel.setBounds(nameX, 38, 80, 18);
        panel.add(nameLabel);

        JTextField nameField = createNameField();
        nameField.setBounds(nameX, 58, nameW, 34);
        panel.add(nameField);

        // ---- 天 / 时 / 分 / 秒 四个数字输入框（单位写在数字后面）----
        final int fieldW = 72;
        final int fieldH = 52;
        final int unitW = 20;
        final int cellGap = 18;
        final int cellW = fieldW + 4 + unitW;
        final int groupW = cellW * 4 + cellGap * 3;
        final int startX = (w - groupW) / 2;
        final int fieldY = 118;

        String[] units = { "天", "时", "分", "秒" };
        JTextField[] fields = {
                createNumberField(1),    // 天：上限 1
                createNumberField(23),
                createNumberField(59),
                createNumberField(59)
        };

        for (int i = 0; i < fields.length; i++) {
            int x = startX + i * (cellW + cellGap);

            fields[i].setBounds(x, fieldY, fieldW, fieldH);
            panel.add(fields[i]);

            JLabel unit = new JLabel(units[i]);
            unit.setForeground(new Color(170, 175, 185));
            unit.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            unit.setBounds(x + fieldW + 4, fieldY + (fieldH - 20) / 2, unitW, 20);
            panel.add(unit);
        }

        // 左：取消（淡灰）
        JButton cancelBtn = UiKit.createDialogButton("取消", new Color(200, 200, 205));
        cancelBtn.setBounds(w - 18 - 72 - 10 - 72, h - 16 - 28, 72, 28);
        cancelBtn.addActionListener(e -> owner.animateDialogClose(dialog, owner.addClockButton));
        panel.add(cancelBtn);

        // 右：添加（淡蓝）
        JButton addBtn = UiKit.createDialogButton("添加", new Color(173, 206, 238));
        addBtn.setBounds(w - 18 - 72, h - 16 - 28, 72, 28);
        addBtn.addActionListener(e -> {
            owner.addCountdown(
                    readNumber(fields[0]),
                    readNumber(fields[1]),
                    readNumber(fields[2]),
                    readNumber(fields[3]),
                    nameField.getText().trim());
            owner.animateDialogClose(dialog, owner.addClockButton);
        });
        panel.add(addBtn);

        // 这个弹窗固定居中，不提供拖动

        dialog.setContentPane(panel);
        dialog.setSize(w, h);
        dialog.setLocationRelativeTo(owner);

        dialog.setSize(w, h);
        // 这个弹窗里有文本输入框：不能用透明窗口，否则一开输入法就会白屏花屏
        dialog.getRootPane().putClientProperty("noFade", Boolean.TRUE);
        dialog.setBackground(new Color(45, 45, 48));
        dialog.validate();

        // 结束位置：居中在窗口上
        final Rectangle to = dialog.getBounds();

        // 起始位置：内容区"添加时钟"按钮那里，看起来像从按钮里拉出来
        Rectangle from = to;
        if (owner.addClockButton != null && owner.addClockButton.isShowing()) {
            try {
                Point p = owner.addClockButton.getLocationOnScreen();
                from = new Rectangle(p.x, p.y, owner.addClockButton.getWidth(), owner.addClockButton.getHeight());
            } catch (Exception ignored) {
                // 取不到坐标就退化成直接出现在最终位置
            }
        }

        dialog.validate();

        dialog.setBounds(from);
        UiKit.applyDialogShape(dialog, from);

        // 弹窗关掉后把蒙版撤掉
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                owner.hideGlassOverlay();
            }
        });

        owner.showGlassOverlay();

        owner.startOpenAnimation(dialog, from, to, 200);

        dialog.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            dialog.toFront();
            nameField.requestFocusInWindow();
        });
    }

    private static JTextField createNameField() {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 33, 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(85, 85, 92));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();

                super.paintComponent(g);

                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g3 = (Graphics2D) g.create();
                    g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g3.setColor(new Color(125, 130, 142));
                    g3.setFont(getFont());
                    FontMetrics fm = g3.getFontMetrics();
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g3.drawString("给这个倒计时起个名字（可不填）", getInsets().left, y);
                    g3.dispose();
                }
            }
        };
        field.setOpaque(false);
        field.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        field.setForeground(new Color(235, 235, 240));
        field.setCaretColor(new Color(235, 235, 240));
        field.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        return field;
    }

    private static JTextField createNumberField(int max) {
        JTextField field = new JTextField("0") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(33, 33, 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(85, 85, 92));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.enableInputMethods(false);          // 数字框不允许输入法（拼音）
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("Consolas", Font.PLAIN, 24));
        field.setForeground(new Color(235, 235, 240));
        field.setCaretColor(new Color(235, 235, 240));
        field.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        ((AbstractDocument) field.getDocument()).setDocumentFilter(new NumberFilter(max));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // 延迟一点再全选，避免和鼠标点击抢光标位置
                javax.swing.SwingUtilities.invokeLater(field::selectAll);
            }
        });

        field.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                // 当前是 0 的时候，点哪儿都算"全选"，这样输入 1 就是 1，而不是变成 10
                field.requestFocusInWindow();
                if ("0".equals(field.getText())) {
                    javax.swing.SwingUtilities.invokeLater(field::selectAll);
                }
            }
        });
        return field;
    }

    private static int readNumber(JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static class NumberFilter extends DocumentFilter {
        private final int max;

        NumberFilter(int max) {
            this.max = max;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            replace(fb, offset, length, "", null);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String old = fb.getDocument().getText(0, fb.getDocument().getLength());
            StringBuilder sb = new StringBuilder(old);
            String insert = text == null ? "" : text.replaceAll("[^0-9]", "");

            offset = Math.max(0, Math.min(offset, sb.length()));
            int end = Math.min(sb.length(), offset + length);
            sb.replace(offset, end, insert);

            String digits = sb.toString().replaceAll("[^0-9]", "");
            int value;
            if (digits.isEmpty()) {
                value = 0;
            } else if (digits.length() > 3) {
                value = max;                       // 位数太多，肯定超上限
            } else {
                value = Integer.parseInt(digits);
                if (value > max) {
                    value = max;                   // 小时超 23 变 23，分/秒超 59 变 59
                }
            }

            fb.replace(0, fb.getDocument().getLength(), String.valueOf(value), attrs);
        }
    }
}
