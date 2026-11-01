package clock;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import static clock.AppConfig.APP_PORT;
import static clock.AppInfo.VERSION;
import static clock.AppConfig.IMAGE_DIR;
import static clock.Assets.classDir;
import static clock.Assets.findFileInRoots;
import static clock.Assets.findIconFile;
import static clock.Assets.findImageFile;
import static clock.UiKit.applyDialogShape;
import static clock.UiKit.applyPopupShape;
import static clock.UiKit.blankIcon;
import static clock.UiKit.checkIcon;
import static clock.UiKit.createDarkButton;
import static clock.UiKit.createDialogButton;
import static clock.UiKit.createMutedButton;
import static clock.UiKit.createSoundItem;
import static clock.UiKit.lighten;
import static clock.UiKit.supportsTranslucency;

public class MainWindow extends JFrame {

    private Point dragOffset;
    private TrayIcon trayIcon;
    private JDialog trayMenu;
    private boolean closing;

    // 卡片回调用：到点响铃+通知、刷新列表、删除卡片
    private final CountdownCard.Listener cardListener = new CountdownCard.Listener() {
        @Override
        public void onFinished(String title) {
            SoundPlayer.play();
            showFinishedNotification(title);
        }

        @Override
        public void refreshList() {
            refreshCountdownList();
        }

        @Override
        public void onRemoveRequested(CountdownCard card) {
            removeCountdown(card);
        }
    };
    JButton addClockButton;
    JButton settingsButton;
    JButton announceButton;
    private JPanel glassOverlay;
    private int overlayCount;   // 有几个弹窗在用蒙版
    private JPanel titleBar;

    private ServerSocket instanceServer;

    private JPanel countdownList;
    private JScrollBar activeScrollBar;
    private int scrollTarget;
    private Timer scrollAnimation;
    private final List<CountdownCard> countdowns = new ArrayList<CountdownCard>();
    private final Timer tickTimer = new Timer(200, e -> {
        for (CountdownCard card : countdowns) {
            card.tick();
        }
    });

    public MainWindow() {
        super("时钟");

        setUndecorated(true);
        // 工具窗口类型：不会出现在任务栏，只留在托盘里
        try {
            setType(Window.Type.UTILITY);
        } catch (Exception e) {
            System.out.println("⚠ 当前平台不支持工具窗口类型，任务栏可能仍会出现");
        }
        // 只有系统/显卡支持"逐像素半透明"时才设置全透明背景，
        // 否则在远程桌面、虚拟机、老显卡等环境下会抛 UnsupportedOperationException
        if (supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            setBackground(new Color(0, 0, 0, 0));
        }
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);   // 退出只能走托盘右键菜单
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (trayIcon != null) {
                    hideToTray();
                } else {
                    requestClose();
                }
            }
        });
        setSize(820, 520);
        setLocationRelativeTo(null);

        // ==============================
        // 加载图标
        // ==============================
        try {
            Image iconImage = loadIcon();
            if (iconImage != null) {
                this.setIconImage(iconImage);
            } else {
                System.out.println("❌ 找不到图片，请检查图片名字是否真的是 10001.gif");
            }
        } catch (Exception e) {
            System.out.println("❌ 图标加载出错: " + e.getMessage());
        }

        // 2. 主容器（自绘圆角背景）
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int radius = 16;
                g2d.setColor(new Color(45, 45, 48));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                
                g2d.setColor(new Color(70, 70, 74));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
                
                g2d.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // 3. 自定义标题栏（整条淡一点点，用来区分"能拖动"和"不能拖动"的区域）
        titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 10));   // 很淡的提亮
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(0, 32));
        titleBar.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));

        JLabel title = new JLabel("时钟");
        title.setForeground(new Color(230, 230, 230));
        title.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        // 动图放在"时钟"两个字前面（标题栏左侧）
        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        titleLeft.setOpaque(false);
        File gifFile = findIconFile();
        if (gifFile != null) {
            try {
                titleLeft.add(new GifPanel(gifFile, 24, 24));
            } catch (Exception e) {
                System.out.println("❌ 动图加载失败: " + e.getMessage());
            }
        }
        titleLeft.add(title);

        // 版本号：小字 + 半透明（内容来自 VERSION）
        JLabel version = new JLabel(VERSION);
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setForeground(new Color(205, 210, 220, 115));
        titleLeft.add(version);

        titleBar.add(titleLeft, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));   // 只留右边距，两个按钮靠在一起

        // 只剩一个按钮：样式不变（还是 X），但作用变成"收进托盘"
        JButton closeBtn = createTitleButton("X");
        closeBtn.setToolTipText("最小化到托盘");
        closeBtn.addActionListener(e -> {
            if (trayIcon != null) {
                hideToTray();               // 淡出后收进托盘
            } else {
                setState(JFrame.ICONIFIED);
            }
        });

        announceButton = createAnnounceButton();
        buttonPanel.add(announceButton);
        buttonPanel.add(closeBtn);
        titleBar.add(buttonPanel, BorderLayout.EAST);

        // 4. 拖动逻辑
        MouseAdapter dragger = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset == null) {
                    return;
                }
                Point current = e.getLocationOnScreen();
                setLocation(current.x - dragOffset.x, current.y - dragOffset.y);
            }
        };
        titleBar.addMouseListener(dragger);
        titleBar.addMouseMotionListener(dragger);
        titleLeft.addMouseListener(dragger);
        titleLeft.addMouseMotionListener(dragger);
        title.addMouseListener(dragger);
        title.addMouseMotionListener(dragger);

        // 5. 内容区（顶部一排功能按钮，第一个是"时钟"）
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        toolbar.setOpaque(false);

        addClockButton = createAddButton();
        addClockButton.addActionListener(e -> AddDialog.show(this));
        toolbar.add(addClockButton);

        settingsButton = createSettingsButton();
        toolbar.add(settingsButton);

        // 按钮下面就是倒计时列表
        countdownList = new JPanel();
        countdownList.setOpaque(false);
        countdownList.setLayout(new BoxLayout(countdownList, BoxLayout.Y_AXIS));
        countdownList.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        // 卡片放不下时出现垂直滚动条，不压缩已有卡片
        JScrollPane listScroll = new JScrollPane(countdownList);
        listScroll.setOpaque(false);
        listScroll.getViewport().setOpaque(false);
        listScroll.setBorder(null);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        listScroll.setWheelScrollingEnabled(false);   // 关掉默认的一格一跳，换成平滑滚动
        listScroll.addMouseWheelListener(e -> smoothScroll(
                listScroll.getVerticalScrollBar(), e.getWheelRotation() * 48));

        JScrollBar bar = listScroll.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setBackground(new Color(45, 45, 48));
        bar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(95, 95, 104);
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

        content.add(toolbar, BorderLayout.NORTH);
        content.add(listScroll, BorderLayout.CENTER);

        root.add(titleBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // 托盘图标：双击显示窗口，右键菜单可以显示/退出
        setupTray(gifFile);

        // 单实例：自己占一个本机端口，后来的程序能通过它叫醒我
        startInstanceServer();

        // 6. 圆角裁剪（同样需要系统支持"逐像素透明"，否则跳过，窗口保持直角）
        if (supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                }
            });
        }
    }

    // ==============================
    // 提示音
    // ==============================

    // 停掉正在播放的提示音

    // 托盘点"关闭"时的确认窗口

    // 到点提示：用托盘的系统通知（Windows 右下角那种，和 QQ 一样）
    private void showFinishedNotification(String name) {
        String text = (name == null || name.isEmpty())
                ? "有一个倒计时已经到时间了"
                : "「" + name + "」已经到时间了";

        try {
            if (trayIcon != null) {
                trayIcon.displayMessage("倒计时到点", text, TrayIcon.MessageType.INFO);
                return;
            }
        } catch (Exception ignored) {
            // 托盘不支持弹通知时忽略
        }
        System.out.println("⏰ " + text);   // 没有托盘时退化成控制台提示
    }

    // 滚轮平滑滚动：把滚动条的数值一格一格地"追"过去
    private void smoothScroll(JScrollBar bar, int delta) {
        activeScrollBar = bar;

        int max = Math.max(0, bar.getMaximum() - bar.getVisibleAmount());
        int base = (scrollAnimation != null && scrollAnimation.isRunning()) ? scrollTarget : bar.getValue();
        scrollTarget = Math.max(0, Math.min(base + delta, max));

        if (scrollAnimation == null) {
            scrollAnimation = new Timer(15, e -> {
                if (activeScrollBar == null) {
                    scrollAnimation.stop();
                    return;
                }
                int current = activeScrollBar.getValue();
                int diff = scrollTarget - current;

                if (Math.abs(diff) <= 1) {
                    activeScrollBar.setValue(scrollTarget);
                    scrollAnimation.stop();
                    return;
                }
                activeScrollBar.setValue(current + (int) Math.round(diff * 0.35));
            });
        }
        if (!scrollAnimation.isRunning()) {
            scrollAnimation.start();
        }
    }

    // ==============================
    // 倒计时
    // ==============================

    void addCountdown(int days, int hours, int minutes, int seconds) {
        addCountdown(days, hours, minutes, seconds, "");
    }

    void addCountdown(int days, int hours, int minutes, int seconds, String name) {
        long totalMillis = (((long) days * 24 + hours) * 3600L + minutes * 60L + seconds) * 1000L;
        if (totalMillis <= 0) {
            return;   // 没填时间就不加
        }
        CountdownCard card = new CountdownCard(totalMillis, name, cardListener);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.setPreferredSize(new Dimension(100, 1));

        countdowns.add(card);
        refreshCountdownList();
        animateCardIn(card);

        if (!tickTimer.isRunning()) {
            tickTimer.start();
        }
    }

    // 新卡片从 1px 平滑展开到 66px，看起来像"滑进列表"
    private void animateCardIn(final CountdownCard card) {
        final int targetHeight = 66;
        final int frames = 12;
        final Timer[] anim = new Timer[1];
        final int[] frame = { 0 };

        anim[0] = new Timer(16, e -> {
            frame[0]++;
            float p = Math.min(1f, frame[0] / (float) frames);
            float eased = 1 - (1 - p) * (1 - p);          // 先快后慢
            int h = Math.max(1, Math.round(targetHeight * eased));

            card.setPreferredSize(new Dimension(100, h));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
            countdownList.revalidate();
            countdownList.repaint();

            if (frame[0] >= frames) {
                anim[0].stop();
                card.setPreferredSize(new Dimension(100, targetHeight));
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, targetHeight));
                countdownList.revalidate();
                countdownList.repaint();
            }
        });
        anim[0].setInitialDelay(0);
        anim[0].start();
    }

    private void removeCountdown(CountdownCard card) {
        countdowns.remove(card);
        refreshCountdownList();
        SoundPlayer.stop();   // 关掉这张卡时，把正在响的提示音也停掉

        if (countdowns.isEmpty()) {
            tickTimer.stop();
        }
    }

    private void refreshCountdownList() {
        // 已结束 / 超时的排最上面，其余保持添加顺序（新的在下面）
        List<CountdownCard> ordered = new ArrayList<CountdownCard>(countdowns);
        ordered.sort((a, b) -> {
            if (a.finished == b.finished) {
                return 0;          // 稳定排序：同类保持原来的先后
            }
            return a.finished ? -1 : 1;
        });

        countdownList.removeAll();
        for (CountdownCard card : ordered) {
            countdownList.add(card);
            countdownList.add(Box.createVerticalStrut(8));
        }
        countdownList.revalidate();
        countdownList.repaint();
    }


    // ==============================
    // 红色圆形"播放/暂停"按钮（视频播放器那种）
    // ==============================


    // ==============================
    // 单实例：已经开着一个的话，把它叫出来，本次直接退出
    // ==============================
    // 有已启动的实例就发 SHOW 并返回 true
    public static boolean notifyRunningInstance() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), APP_PORT), 400);
            socket.setSoTimeout(800);

            socket.getOutputStream().write("SHOW\n".getBytes("UTF-8"));
            socket.getOutputStream().flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
            return "CLOCK".equals(reader.readLine());
        } catch (Exception e) {
            return false;   // 连不上 = 没人在跑
        }
    }

    // 本进程作为"第一个实例"来监听
    private void startInstanceServer() {
        try {
            instanceServer = new ServerSocket(APP_PORT, 1, InetAddress.getLoopbackAddress());
        } catch (Exception e) {
            // 端口被占用：如果对方是我们自己（比如同时双击了两次），
            // 就叫醒它然后退出，避免开出第二个窗口
            if (notifyRunningInstance()) {
                System.out.println("↪ 已经有一个在运行了，已把它的窗口叫到前面。");
                System.exit(0);
            }
            return;   // 被别的软件占用：放弃单实例检测，不影响正常使用
        }

        Thread thread = new Thread(() -> {
            while (instanceServer != null && !instanceServer.isClosed()) {
                try (Socket socket = instanceServer.accept()) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    String line = reader.readLine();

                    socket.getOutputStream().write("CLOCK\n".getBytes("UTF-8"));
                    socket.getOutputStream().flush();

                    if (line != null && line.startsWith("SHOW")) {
                        SwingUtilities.invokeLater(this::showWindow);
                    }
                } catch (Exception ignored) {
                    // 忽略，继续等下一个
                }
            }
        }, "single-instance");
        thread.setDaemon(true);
        thread.start();
    }

    // ==============================
    // 系统托盘：启动后只占托盘，不占任务栏
    // ==============================
    private void setupTray(File gifFile) {
        if (!SystemTray.isSupported()) {
            System.out.println("⚠ 当前系统不支持托盘，窗口仍会显示");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Dimension size = tray.getTrayIconSize();

            Image icon = null;
            if (gifFile != null) {
                Image first = ImageIO.read(gifFile);   // 拿 GIF 第一帧当托盘图标
                if (first != null) {
                    icon = first.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
                }
            }
            if (icon == null) {
                icon = getIconImage();
            }
            if (icon == null) {
                System.out.println("⚠ 没有可用的托盘图标，跳过托盘");
                return;
            }

            trayIcon = new TrayIcon(icon, "时钟");
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> showWindow());   // 双击托盘图标：显示窗口
            trayIcon.addMouseListener(new MouseAdapter() {   // 右键：只弹一个"关闭"
                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowPopup(e);
                }
            });
            tray.add(trayIcon);
        } catch (Exception e) {
            System.out.println("❌ 托盘图标创建失败: " + e.getMessage());
        }
    }

    private void showWindow() {
        setOpacitySafe(0f);
        setVisible(true);
        setState(JFrame.NORMAL);
        toFront();
        fade(0f, 1f, 220, null);
    }

    // ==============================
    // 淡入 / 淡出
    // ==============================
    private boolean canFade() {
        return supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
    }

    private void setOpacitySafe(float opacity) {
        if (!canFade()) {
            return;
        }
        try {
            setOpacity(Math.max(0f, Math.min(1f, opacity)));
        } catch (Exception ignored) {
            // 个别环境不允许改透明度，忽略即可
        }
    }

    private void fade(final float from, final float to, int durationMs, final Runnable done) {
        if (!canFade()) {
            if (done != null) {
                done.run();
            }
            return;
        }
        final int frames = Math.max(6, durationMs / 16);
        final int interval = Math.max(10, durationMs / frames);
        final Timer[] timer = new Timer[1];
        final int[] frame = { 0 };

        setOpacitySafe(from);
        timer[0] = new Timer(interval, e -> {
            frame[0]++;
            float p = Math.min(1f, frame[0] / (float) frames);
            float eased = 1 - (1 - p) * (1 - p);
            setOpacitySafe(from + (to - from) * eased);
            if (frame[0] >= frames) {
                timer[0].stop();
                setOpacitySafe(to);
                if (done != null) {
                    done.run();
                }
            }
        });
        timer[0].setInitialDelay(0);
        timer[0].start();
    }

    // 启动时淡入
    public void showWithFadeIn() {
        // 先把原生窗口建出来，再设成全透明，最后显示并淡入，
        // 否则第一帧会以不透明状态闪一下
        if (canFade()) {
            addNotify();
            setOpacity(0f);
        }
        setVisible(true);
        fade(0f, 1f, 220, null);
    }

    // 淡出后再退出
    void requestClose() {
        if (closing) {
            return;
        }
        closing = true;
        if (!isShowing()) {   // 窗口已经收进托盘了，不用再淡出，直接退
            quit();
            return;
        }
        fade(1f, 0f, 160, this::quit);
    }

    // 淡出后收进托盘
    private void hideToTray() {
        fade(1f, 0f, 140, () -> setVisible(false));
    }

    // ==============================
    // 托盘右键菜单：只有一项"关闭"
    // 用小窗口来做，失去焦点就自动消失（不会赖在屏幕上）
    // ==============================
    private void maybeShowPopup(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        showTrayMenu(MouseInfo.getPointerInfo().getLocation());
    }

    private void showTrayMenu(Point at) {
        if (trayMenu != null && trayMenu.isShowing()) {
            trayMenu.dispose();
        }

        // 菜单项：悬停底色也是圆角
        JButton closeItem = new JButton("关闭") {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover() || getModel().isArmed()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(232, 17, 35));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        closeItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        closeItem.setForeground(new Color(235, 235, 240));
        closeItem.setFocusPainted(false);
        closeItem.setBorderPainted(false);
        closeItem.setContentAreaFilled(false);
        closeItem.setOpaque(false);
        closeItem.setMargin(new Insets(5, 18, 5, 18));
        closeItem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeItem.addActionListener(e -> {
            trayMenu.dispose();
            ConfirmDialog.show(this);
        });

        // 菜单底板：自己画圆角背景 + 圆角描边
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 45, 48));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(80, 80, 86));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        panel.add(closeItem, BorderLayout.CENTER);

        trayMenu = new JDialog();
        trayMenu.setUndecorated(true);
        trayMenu.setAlwaysOnTop(true);   // 托盘在最右下角，不置顶会被任务栏盖住
        try {
            trayMenu.setType(Window.Type.UTILITY);   // 不出现在任务栏
        } catch (Exception ignored) {
            // 平台不支持就保持普通类型
        }
        trayMenu.setContentPane(panel);
        trayMenu.pack();

        // 菜单外面也切圆角，四角透明
        if (supportsTranslucency(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            trayMenu.setShape(new RoundRectangle2D.Double(
                    0, 0, trayMenu.getWidth(), trayMenu.getHeight(), 12, 12));
        }

        // 弹在光标"上方"，别钻到任务栏下面；同时别跑出屏幕
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = Math.max(4, Math.min(at.x, screen.width - trayMenu.getWidth() - 4));
        int y = at.y - trayMenu.getHeight() - 2;
        if (y < 4) {
            y = at.y + 2;   // 上面放不下就放下面
        }
        y = Math.max(0, Math.min(y, screen.height - trayMenu.getHeight() - 4));
        trayMenu.setLocation(x, y);

        // 点到别处（包括别的程序、桌面）就自动关掉；
        // 但没真正拿到过焦点之前不乱关，否则会一弹出就没
        final boolean[] hadFocus = { false };
        trayMenu.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent ev) {
                hadFocus[0] = true;
            }

            @Override
            public void windowLostFocus(WindowEvent ev) {
                if (hadFocus[0]) {
                    trayMenu.dispose();
                }
            }
        });

        // 鼠标离开菜单范围（Windows 11 任务栏自动收起、点到别处都会导致鼠标移开）
        // 就自动关掉，避免它"赖在屏幕上不走"
        final Timer[] watcher = new Timer[1];
        final int[] awayTicks = { 0 };
        watcher[0] = new Timer(200, ev -> {
            if (trayMenu == null || !trayMenu.isShowing()) {
                watcher[0].stop();
                return;
            }
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            Rectangle area = trayMenu.getBounds();
            area.grow(30, 30);
            if (area.contains(mouse)) {
                awayTicks[0] = 0;
            } else if (++awayTicks[0] >= 2) {   // 连续两次（约 0.4 秒）都不在菜单上才关，避免手抖误关
                trayMenu.dispose();
                watcher[0].stop();
            }
        });

        trayMenu.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent ev) {
                watcher[0].stop();
            }
        });

        trayMenu.setVisible(true);
        trayMenu.toFront();
        trayMenu.requestFocus();
        watcher[0].start();
    }

    // 退出：先把托盘图标摘掉再退出，避免托盘里留下"幽灵"图标
    private void quit() {
        try {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
        } catch (Exception ignored) {
            // 系统已经把图标移除时忽略
        }
        System.exit(0);
    }

    // ==============================
    // 图标查找：不依赖"从哪个目录启动"
    // ==============================

    // 按相对路径查找文件：当前目录 / class 目录 / 上级目录 / Clock 兜底

    // 图片：优先 assets/images/，旧位置（根目录）也兼容

    private Image loadIcon() throws Exception {
        File file = findIconFile();
        if (file != null) {
            Image image = ImageIO.read(file);
            if (image != null) {
                System.out.println("✅ 图标加载成功！路径: " + file.getAbsolutePath());
                return image;
            }
        }

        // classpath 里的资源（以后打成 jar 也能用）
        URL resource = getClass().getResource("/" + IMAGE_DIR + "/10001.gif");
        if (resource == null) {
            resource = getClass().getResource("/10001.gif");   // 兼容旧位置
        }
        if (resource != null) {
            Image image = ImageIO.read(resource);
            if (image != null) {
                System.out.println("✅ 图标加载成功！资源: " + resource);
                return image;
            }
        }
        return null;
    }

    // ==============================
    // 右上角的动图：交给 AWT 的 GIF 解码器逐帧播放
    // ==============================
    private static class GifPanel extends JComponent {

        private final Image image;

        GifPanel(File file, int width, int height) throws Exception {
            setOpaque(false);
            image = Toolkit.getDefaultToolkit().createImage(file.toURI().toURL());

            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            setPreferredSize(new Dimension(width, height));
        }

        // GIF 换帧时 AWT 会回调这里，重画一次
        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
            if ((infoflags & FRAMEBITS) != 0) {
                repaint();
                return true;
            }
            return super.imageUpdate(img, infoflags, x, y, w, h);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            // 像素画用最近邻缩放，保持清晰
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, 0, 0, getWidth(), getHeight(), this);
            g2.dispose();
        }
    }

    // 当前 .class 文件所在目录（找不到时返回 null，不影响程序运行）

    // 判断当前系统是否支持某种窗口透明效果

    // ==============================
    // 标题栏右侧的公告图标（点开看公告）
    // ==============================
    private JButton createAnnounceButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover() || getModel().isArmed();
                if (hover) {
                    g2.setColor(new Color(70, 70, 74));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                // 一个对话气泡
                int bw = 14;
                int bh = 10;
                int bx = (getWidth() - bw) / 2;
                int by = (getHeight() - bh) / 2 - 1;

                g2.setColor(hover ? Color.WHITE : new Color(220, 222, 228));
                g2.fillRoundRect(bx, by, bw, bh, 4, 4);
                g2.fillPolygon(new int[] { bx + 3, bx + 3, bx + 8 },
                        new int[] { by + bh - 1, by + bh + 3, by + bh - 1 }, 3);

                // 气泡里的三个点
                g2.setColor(new Color(60, 60, 66));
                for (int i = 0; i < 3; i++) {
                    g2.fillOval(bx + 2 + i * 4, by + 4, 2, 2);
                }
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(24, 22));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("公告");
        btn.addActionListener(e -> AnnounceDialog.show(this));
        return btn;
    }

    // 点公告图标弹出公告窗口（和设置面板一样大）

    // ==============================
    // 设置按钮：方形、圆角、淡灰底 + 齿轮
    // ==============================
    private JButton createSettingsButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover() || getModel().isArmed();
                Color bg = hover ? new Color(216, 216, 222) : new Color(198, 198, 205);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int r = Math.min(getWidth(), getHeight()) / 2 - 9;

                // 齿轮的 8 个齿
                g2.setColor(new Color(52, 52, 58));
                int teeth = 8;
                int toothW = Math.max(3, r / 2);
                int toothH = Math.max(4, r / 2);
                for (int i = 0; i < teeth; i++) {
                    Graphics2D t = (Graphics2D) g2.create();
                    t.rotate(Math.toRadians(i * 360.0 / teeth), cx, cy);
                    t.fillRoundRect(cx - toothW / 2, cy - r - toothH / 3, toothW, toothH, 2, 2);
                    t.dispose();
                }

                // 齿轮本体
                int body = Math.max(6, (int) (r * 1.5));
                g2.fillOval(cx - body / 2, cy - body / 2, body, body);

                // 中心孔
                int hole = Math.max(3, (int) (r * 0.65));
                g2.setColor(bg);
                g2.fillOval(cx - hole / 2, cy - hole / 2, hole, hole);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(34, 34));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("设置");
        btn.addActionListener(e -> SettingsDialog.show(this));
        return btn;
    }

    // 画一个对勾图标（14x14）

    // 同样的空白图标，用来让"没选中"的项也能对齐

    // 深色圆角按钮（用于提示音下拉的触发按钮）

    // 灰掉的按钮：点了没反应，鼠标悬停显示"功能开发中"

    // 下拉里的每一项

    // 下拉弹窗的圆角

    // 深色圆角下拉列表：展开动画 + 已选项打勾

    // 点设置：弹一个同样风格的小面板（内容以后再加）

    // ==============================
    // 内容区功能按钮："添加时钟"
    // ==============================
    private JButton createAddButton() {
        JButton btn = new JButton("添加时钟") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean hover = getModel().isRollover() || getModel().isArmed();
                int arc = 10;

                // 淡蓝底
                g2.setColor(hover ? new Color(198, 222, 248) : new Color(173, 206, 238));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                // 略深一点的淡蓝描边（"框"）
                g2.setColor(new Color(128, 170, 214));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        btn.setForeground(new Color(30, 46, 66));   // 配淡蓝底用深色字
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setIconTextGap(6);
        btn.setMargin(new Insets(5, 16, 5, 16));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("添加时钟");
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(new Color(15, 30, 50));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(30, 46, 66));
            }
        });
        return btn;
    }

    // ==============================
    // 点"添加时钟"弹出的小面板（和程序风格一致：深色圆角、无边框）
    // ==============================

    // ==============================
    // 弹窗背后的蒙版 + 磨砂玻璃
    // ==============================
    void showGlassOverlay() {
        overlayCount++;
        if (glassOverlay != null) {
            return;   // 已经有蒙版了，不用重画
        }
        try {
            // 先把当前窗口画成一张图
            BufferedImage shot = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = shot.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, shot.getWidth(), shot.getHeight());
            getRootPane().printAll(g);
            g.dispose();

            // 缩小 -> 模糊 -> 放大，就是磨砂玻璃
            final BufferedImage frosted = makeFrosted(shot, 4, 2);
            final int shotW = shot.getWidth();
            final int shotH = shot.getHeight();

            glassOverlay = new JPanel() {
                @Override
                protected void paintComponent(Graphics gr) {
                    Graphics2D g2 = (Graphics2D) gr.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.drawImage(frosted, 0, 0, getWidth(), getHeight(), null);

                    // 再压暗一层，弹窗才显得突出
                    g2.setColor(new Color(12, 14, 18, 110));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            glassOverlay.setOpaque(false);
            // 蒙版盖满整个窗口（含标题栏）：既挡住点击拖动，又因为是普通 Swing 组件不会响"叮"
            glassOverlay.addMouseListener(new MouseAdapter() {
            });
            glassOverlay.addMouseMotionListener(new MouseAdapter() {
            });

            getRootPane().setGlassPane(glassOverlay);
            glassOverlay.setVisible(true);
        } catch (Exception ignored) {
            // 出问题就退化成没有蒙版，不影响弹窗本身
        }
    }

    void hideGlassOverlay() {
        overlayCount = Math.max(0, overlayCount - 1);
        if (overlayCount > 0) {
            return;   // 还有别的弹窗在用，先留着
        }
        if (glassOverlay != null) {
            glassOverlay.setVisible(false);
            getRootPane().setGlassPane(new JPanel());
            glassOverlay = null;
        }
    }

    // 缩小 + 两遍方框模糊 + 放大
    private BufferedImage makeFrosted(BufferedImage src, int shrink, int radius) {
        int sw = Math.max(1, src.getWidth() / shrink);
        int sh = Math.max(1, src.getHeight() / shrink);

        BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, sw, sh, null);
        g.dispose();

        int[] pixels = small.getRGB(0, 0, sw, sh, null, 0, sw);
        int[] temp = new int[pixels.length];

        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    int a = 0, r = 0, gg = 0, b = 0, n = 0;
                    for (int dy = -radius; dy <= radius; dy++) {
                        int yy = Math.max(0, Math.min(sh - 1, y + dy));
                        for (int dx = -radius; dx <= radius; dx++) {
                            int xx = Math.max(0, Math.min(sw - 1, x + dx));
                            int c = pixels[yy * sw + xx];
                            a += (c >>> 24) & 0xFF;
                            r += (c >> 16) & 0xFF;
                            gg += (c >> 8) & 0xFF;
                            b += c & 0xFF;
                            n++;
                        }
                    }
                    temp[y * sw + x] = ((a / n) << 24) | ((r / n) << 16) | ((gg / n) << 8) | (b / n);
                }
            }
            int[] swap = pixels;
            pixels = temp;
            temp = swap;
        }

        BufferedImage out = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, sw, sh, pixels, 0, sw);
        return out;
    }

    // 弹窗的圆角（尺寸变化时也要跟着重设，否则圆角会错位）

    // 关闭时把展开动画倒着放一遍：缩回"添加时钟"按钮那里
    void animateDialogClose(final JDialog dialog, JButton anchor) {
        Rectangle target = null;
        if (anchor != null && anchor.isShowing()) {
            try {
                Point p = anchor.getLocationOnScreen();
                target = new Rectangle(p.x, p.y, anchor.getWidth(), anchor.getHeight());
            } catch (Exception ignored) {
                // 取不到就关闭即
            }
        }
        if (target == null) {
            dialog.dispose();
            return;
        }

        final Rectangle from = dialog.getBounds();
        final Rectangle to = target;
        final int frames = 5;
        final Timer[] anim = new Timer[1];
        final int[] frame = { 0 };

        anim[0] = new Timer(10, e -> {
            frame[0]++;
            float p = Math.min(1f, frame[0] / (float) frames);
            float eased = p * p;      // 倒放：慢起快收

            int x = Math.round(from.x + (to.x - from.x) * eased);
            int y = Math.round(from.y + (to.y - from.y) * eased);
            int w = Math.round(from.width + (to.width - from.width) * eased);
            int h = Math.round(from.height + (to.height - from.height) * eased);

            dialog.setBounds(x, y, Math.max(w, 1), Math.max(h, 1));
            applyDialogShape(dialog, new Rectangle(0, 0, Math.max(w, 1), Math.max(h, 1)));

            if (frame[0] >= frames) {
                anim[0].stop();
                dialog.dispose();
            }
        });
        anim[0].setInitialDelay(0);
        anim[0].start();
    }

    // 从按钮位置"拉出来"的展开动画
    void startOpenAnimation(final JDialog dialog, final Rectangle from, final Rectangle to, int durationMs) {
        final int frames = 4;
        final int interval = Math.max(8, durationMs / frames);
        final Timer[] anim = new Timer[1];
        final int[] frame = { 0 };

        anim[0] = new Timer(interval, e -> {
            frame[0]++;
            float p = Math.min(1f, frame[0] / (float) frames);
            float eased = 1 - (1 - p) * (1 - p);      // 先快后慢

            int x = Math.round(from.x + (to.x - from.x) * eased);
            int y = Math.round(from.y + (to.y - from.y) * eased);
            int w = Math.round(from.width + (to.width - from.width) * eased);
            int h = Math.round(from.height + (to.height - from.height) * eased);

            dialog.setBounds(x, y, w, h);
            applyDialogShape(dialog, new Rectangle(0, 0, w, h));

            if (frame[0] >= frames) {
                anim[0].stop();
                dialog.setBounds(to);
                applyDialogShape(dialog, to);
            }
        });
        anim[0].setInitialDelay(0);
        anim[0].start();
    }

    // 按比例朝白色靠近一点（Color.brighter() 会把浅色直接拉成纯白）

    // ==============================
    // 名称输入框：圆角深色底，空的时候显示提示文字
    // ==============================

    // ==============================
    // 数字输入框：只收数字，超过上限自动变成上限
    // ==============================

    // 只保留数字；超过上限直接写成上限（小时 23、分/秒 59）

    // 弹窗上的小按钮：给定底色，圆角，悬停变亮

    private JButton createTitleButton(String text) {
        final boolean isClose = "X".equals(text);

        // 悬停底色自己画成圆角（默认是直角方块）
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isRollover() || getModel().isArmed()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isClose ? new Color(232, 17, 35) : new Color(70, 70, 74));
                    int arc = Math.min(getHeight(), getWidth());   // 圆角拉到最大 = 胶囊形
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(30, 22));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(true);
        btn.setForeground(new Color(220, 220, 220));
        btn.setFont(new Font("Segoe UI", Font.BOLD, isClose ? 12 : 11));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // "—" 在字体里比 "X" 低 1px，用底部内边距把它顶回中线
        btn.setMargin(isClose ? new Insets(0, 0, 0, 0) : new Insets(0, 0, 2, 0));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(220, 220, 220));
            }
        });
        return btn;
    }

}