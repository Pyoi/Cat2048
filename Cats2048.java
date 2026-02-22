import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;

public class Cats2048 extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Cats2048().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "起動に失敗しました:\n" + e.getMessage());
                }
            }
        });
    }

    private final CardLayout card = new CardLayout();
    private final JPanel root = new JPanel(card);

    private final MenuPanel menuPanel = new MenuPanel();
    private GamePanel gamePanel;

    public Cats2048() {
        super("ガルムとハヤテの2048ゲーム（5×5）");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 1100);
        setLocationRelativeTo(null);

        root.add(menuPanel, "menu");
        add(root);

        card.show(root, "menu");
    }

    private void startGame(Mode mode) {
        if (gamePanel != null) {
            root.remove(gamePanel);
        }
        gamePanel = new GamePanel(mode, new Runnable() {
            public void run() {
                card.show(root, "menu");
                menuPanel.requestFocusInWindow();
            }
        });

        root.add(gamePanel, "game");
        card.show(root, "game");
        gamePanel.requestFocusInWindow();
    }

    enum Mode {
        GARM("garm.PNG"),
        HAYATE("hayate.PNG");

        final String bgFile;
        Mode(String bgFile) { this.bgFile = bgFile; }
    }

    // メニュー画面はここで
    class MenuPanel extends JPanel {
        public MenuPanel() {
            setLayout(new GridBagLayout());
            setBackground(new Color(243, 232, 196));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(18, 28, 18, 28);

            JLabel title = new JLabel("ガルムとハヤテの 2048ゲーム", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 34));
            title.setForeground(new Color(160, 20, 60));

            JButton garmBtn = makeBigButton("ガルムでスタート");
            JButton hayateBtn = makeBigButton("ハヤテでスタート");
            JButton exitBtn = makeBigButton("ゲーム終了");

            garmBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startGame(Mode.GARM);
                }
            });
            hayateBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startGame(Mode.HAYATE);
                }
            });
            exitBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });

            gbc.gridy = 0;
            add(title, gbc);
            gbc.gridy = 1;
            add(garmBtn, gbc);
            gbc.gridy = 2;
            add(hayateBtn, gbc);
            gbc.gridy = 3;
            add(exitBtn, gbc);
        }

        private JButton makeBigButton(String text) {
            JButton b = new JButton(text);
            b.setFont(new Font("SansSerif", Font.BOLD, 28));
            b.setFocusPainted(false);
            b.setBackground(Color.WHITE);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 110, 180), 4),
                    BorderFactory.createEmptyBorder(30, 40, 30, 40)
            ));
            b.setPreferredSize(new Dimension(650, 130));
            return b;
        }
    }

    // ゲーム本編。
    static class GamePanel extends JPanel {
        // 画像内の正方形エリアを指定（0.0〜1.0）
        // ※ズレる場合はここ調整でたぶん大丈夫かな？
        private static final double GRID_X = 0.05;
        private static final double GRID_Y = 0.22;
        private static final double GRID_W = 0.90;
        private static final double GRID_H = 0.73;

        private static final int N = 5;

        private final Cats2048.Mode mode;
        private BufferedImage bg;

        private final int[][] board = new int[N][N];
        private int score = 0;
        private boolean gameOver = false;

        private final Random rnd = new Random();
        private final Runnable onBackToMenu;

        public GamePanel(Cats2048.Mode mode, Runnable onBackToMenu) {
            this.mode = mode;
            this.onBackToMenu = onBackToMenu;

            setFocusable(true);
            setBackground(Color.BLACK);

            loadBackground();
            resetGame();
            setupKeyBindings();
        }

        private void loadBackground() {
            try {
                bg = ImageIO.read(new File(mode.bgFile));
            } catch (Exception e) {
                bg = null;
                JOptionPane.showMessageDialog(this,
                        "背景画像を読み込めませんでした: " + mode.bgFile +
                                "\n同じフォルダに " + mode.bgFile + " を置いてください。\n\n" +
                                "詳細: " + e.getMessage());
            }
        }

        private void setupKeyBindings() {
            int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
            InputMap im = getInputMap(condition);
            ActionMap am = getActionMap();

            im.put(KeyStroke.getKeyStroke("LEFT"), "left");
            im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
            im.put(KeyStroke.getKeyStroke("UP"), "up");
            im.put(KeyStroke.getKeyStroke("DOWN"), "down");

            // そのたキーの機能、Rでリスタート、ESCでメニュー
            im.put(KeyStroke.getKeyStroke("R"), "restart");
            im.put(KeyStroke.getKeyStroke("ESCAPE"), "menu");

            am.put("left", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { step(Direction.LEFT); }
            });
            am.put("right", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { step(Direction.RIGHT); }
            });
            am.put("up", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { step(Direction.UP); }
            });
            am.put("down", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { step(Direction.DOWN); }
            });

            am.put("restart", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { resetGame(); repaint(); }
            });
            am.put("menu", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { onBackToMenu.run(); }
            });
        }

        private void resetGame() {
            for (int r = 0; r < N; r++) Arrays.fill(board[r], 0);
            score = 0;
            gameOver = false;

            spawnTile();
            spawnTile();
        }

        enum Direction { LEFT, RIGHT, UP, DOWN }

        private void step(Direction dir) {
            if (gameOver) return;

            boolean moved = move(dir);
            if (moved) {
                spawnTile();
                if (!canMove()) {
                    gameOver = true;
                    repaint();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog(GamePanel.this,
                                    "ゲームオーバー！\nスコア: " + score +
                                            "\n\nRでリスタート / ESCでメニュー");
                        }
                    });
                }
            }
            repaint();
        }

        private boolean move(Direction dir) {
            boolean moved = false;

            for (int i = 0; i < N; i++) {
                int[] line = new int[N];
                for (int j = 0; j < N; j++) {
                    switch (dir) {
                        case LEFT:
                            line[j] = board[i][j];
                            break;
                        case RIGHT:
                            line[j] = board[i][N - 1 - j];
                            break;
                        case UP:
                            line[j] = board[j][i];
                            break;
                        case DOWN:
                            line[j] = board[N - 1 - j][i];
                            break;
                    }
                }

                int[] merged = compressAndMerge(line);
                if (!Arrays.equals(line, merged)) moved = true;

                for (int j = 0; j < N; j++) {
                    switch (dir) {
                        case LEFT:
                            board[i][j] = merged[j];
                            break;
                        case RIGHT:
                            board[i][N - 1 - j] = merged[j];
                            break;
                        case UP:
                            board[j][i] = merged[j];
                            break;
                        case DOWN:
                            board[N - 1 - j][i] = merged[j];
                            break;
                    }
                }
            }
            return moved;
        }

        private int[] compressAndMerge(int[] line) {
            int[] tmp = new int[N];
            int t = 0;
            for (int k = 0; k < N; k++) {
                int v = line[k];
                if (v != 0) tmp[t++] = v;
            }

            for (int i = 0; i < N - 1; i++) {
                if (tmp[i] != 0 && tmp[i] == tmp[i + 1]) {
                    tmp[i] *= 2;
                    score += tmp[i];
                    tmp[i + 1] = 0;
                    i++;
                }
            }

            int[] out = new int[N];
            t = 0;
            for (int k = 0; k < N; k++) {
                int v = tmp[k];
                if (v != 0) out[t++] = v;
            }
            return out;
        }

        private void spawnTile() {
            java.util.List<Point> empty = new ArrayList<Point>();
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (board[r][c] == 0) empty.add(new Point(r, c));
                }
            }
            if (empty.isEmpty()) return;

            Point p = empty.get(rnd.nextInt(empty.size()));
            board[p.x][p.y] = nextSpawnValue();
        }

        private int nextSpawnValue() {
            double x = rnd.nextDouble();

            if (mode == Cats2048.Mode.GARM) {
                // ガルム：小さい数字が出やすい、ここで調整可能。
                // 2:80% 4:18% 8:2%
                if (x < 0.80) return 2;
                if (x < 0.98) return 4;
                return 8;
            } else {
                // ハヤテ：ばらつき少なめバランス型
                return (x < 0.55) ? 2 : 4;
            }
        }

        private boolean canMove() {
            for (int r = 0; r < N; r++)
                for (int c = 0; c < N; c++)
                    if (board[r][c] == 0) return true;

            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    int v = board[r][c];
                    if (r + 1 < N && board[r + 1][c] == v) return true;
                    if (c + 1 < N && board[r][c + 1] == v) return true;
                }
            }
            return false;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (bg != null) {
                g2.drawImage(bg, 0, 0, w, h, null);
            } else {
                g2.setColor(new Color(30, 30, 30));
                g2.fillRect(0, 0, w, h);
            }

            drawHud(g2, w);

            int gx = (int) (w * GRID_X);
            int gy = (int) (h * GRID_Y);
            int gw = (int) (w * GRID_W);
            int gh = (int) (h * GRID_H);

            int side = Math.min(gw, gh);
            gx = gx + (gw - side) / 2;
            gy = gy + (gh - side) / 2;
            gw = side;
            gh = side;

            int pad = Math.max(10, side / 70);
            int cell = (side - pad * (N + 1)) / N;
            int boardSize = pad * (N + 1) + cell * N;

            int bx = gx + (side - boardSize) / 2;
            int by = gy + (side - boardSize) / 2;

            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillRoundRect(bx - pad, by - pad, boardSize + pad * 2, boardSize + pad * 2, 26, 26);

            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    int x = bx + pad + c * (cell + pad);
                    int y = by + pad + r * (cell + pad);
                    drawCell(g2, x, y, cell, board[r][c]);
                }
            }

            if (gameOver) {
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 50));
                String msg = "GAME OVER";
                int tw = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (w - tw) / 2, h / 2 - 20);

                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                String sub = "R: リスタート / ESC: メニュー";
                int sw = g2.getFontMetrics().stringWidth(sub);
                g2.drawString(sub, (w - sw) / 2, h / 2 + 30);
            }

            g2.dispose();
        }

        private void drawHud(Graphics2D g2, int w) {
            String left = "モード: " + (mode == Cats2048.Mode.GARM ? "ガルム" : "ハヤテ");
            String right = "スコア: " + score + "   (R:リスタート / ESC:メニュー)";

            g2.setFont(new Font("SansSerif", Font.BOLD, 20));
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRoundRect(20, 18, w - 40, 46, 18, 18);

            g2.setColor(Color.WHITE);
            g2.drawString(left, 34, 48);

            int rw = g2.getFontMetrics().stringWidth(right);
            g2.drawString(right, w - 34 - rw, 48);
        }

        private void drawCell(Graphics2D g2, int x, int y, int size, int value) {
            if (value == 0) {
                g2.setColor(new Color(255, 255, 255, 90));
                g2.fillRoundRect(x, y, size, size, 20, 20);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawRoundRect(x, y, size, size, 20, 20);
                return;
            }

            Color fill = tileColor(value);
            g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 210));
            g2.fillRoundRect(x, y, size, size, 20, 20);

            g2.setColor(new Color(0, 0, 0, 90));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, size, size, 20, 20);

            String s = String.valueOf(value);
            int fontSize = calcFontSize(size, s.length());
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();

            int tx = x + (size - fm.stringWidth(s)) / 2;
            int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();

            g2.setColor(Color.WHITE);
            g2.drawString(s, tx - 1, ty);
            g2.drawString(s, tx + 1, ty);
            g2.drawString(s, tx, ty - 1);
            g2.drawString(s, tx, ty + 1);

            g2.setColor(new Color(20, 20, 20));
            g2.drawString(s, tx, ty);
        }

        private int calcFontSize(int cell, int digits) {
            if (digits <= 1) return (int) (cell * 0.55);
            if (digits == 2) return (int) (cell * 0.50);
            if (digits == 3) return (int) (cell * 0.42);
            return (int) (cell * 0.36);
        }

        private Color tileColor(int v) {
            // Java8対応、なんかおかしいと思った。
            switch (v) {
                case 2: return new Color(240, 240, 240);
                case 4: return new Color(230, 230, 200);
                case 8: return new Color(240, 210, 140);
                case 16: return new Color(245, 180, 120);
                case 32: return new Color(245, 150, 110);
                case 64: return new Color(245, 120, 100);
                case 128: return new Color(240, 220, 90);
                case 256: return new Color(240, 210, 60);
                case 512: return new Color(240, 190, 40);
                case 1024: return new Color(240, 170, 30);
                default: return new Color(240, 150, 20);
            }
        }
    }
}