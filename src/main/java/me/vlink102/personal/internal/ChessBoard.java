package me.vlink102.personal.internal;

import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.game.pieces.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.Timer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ChessBoard extends JFrame implements MouseListener, MouseMotionListener {
    JLayeredPane layeredPane;
    JPanel chessBoard;
    JLabel chessPiece;

    Point clicked;
    Point released;

    private final int pieceSize;
    public static boolean WHITE_VIEW = true;
    public static int COMPUTER_WAIT_TIME_MS;

    public static boolean IGNORE_RULES = false;

    public JPanel getChessBoard() {
        return chessBoard;
    }

    private final GameManager manager;

    public GameManager getManager() {
        return manager;
    }

    public int getPieceSize() {
        return pieceSize;
    }

    public class EvalBoard extends JFrame {

        private final JLabel evalLabel;
        private final JLabel dtz;
        private final JLabel dtm;
        private final JPanel historyPanel;
        private final JProgressBar evalBar;
        private final JProgressBar winChanceBar;

        private final SwingWorkerRealTime swingWorkerRealTime;

        private final JScrollPane pane;

        private final HashMap<String, Double> positionalChances;

        public HashMap<String, Double> getPositionalChances() {
            return positionalChances;
        }

        public void addEvalHistory(double data) {
            swingWorkerRealTime.getMySwingWorker().addData(data);
        }

        public Double getEvaluation(String fen) {
            return positionalChances.get(fen);
        }

        public Double getLastEvaluation(String fen) {
            return positionalChances.get(fen);
        }

        public void updateEval(Double eval, String fen) {
            if (eval == null) {
                setEval(fen);
            } else if (eval != -1d) {
                setEval(eval, fen);
            }
        }

        public void updateEvalDTZDTM(Integer dtz, Integer dtm) {
            if (dtz != null) {
                setDtz(dtz);
            } else {
                setDtz();
            }
            if (dtm != null) {
                setDtm(dtm);
            } else {
                setDtm();
            }
        }

        private void setEval(String fen) {
            evalLabel.setText("Evaluation: ±0.00");
            double val = StockFish.getWinningChances(0d);
            positionalChances.put(fen, val);
            evalBar.setValue((evalBar.getMinimum() + evalBar.getMaximum()) / 2);
            winChanceBar.setValue((int) (val * 100));
            addEvalHistory(0);
        }

        private void setEval(Double eval, String fen) {
            evalLabel.setText("Evaluation: " + (eval > 0 ? "+" : (eval == 0 ? "±" : "-")) + Math.abs(eval));
            double val = StockFish.getWinningChances(eval * 100);
            positionalChances.put(fen, val);
            evalBar.setValue(((evalBar.getMinimum() + evalBar.getMaximum()) / 2) + ((int) (eval * 10)));
            winChanceBar.setValue((int) (val * 100));
            addEvalHistory(eval);
        }
        private void setDtz(int x) {
            dtz.setText("DTZ: " + x + "  ");
        }

        private void setDtz() {
            dtz.setText(null);
        }

        private void setDtm(int x) {
            dtm.setText("Mate in: " + x + "  ");
        }

        private void setDtm() {
            dtm.setText(null);
        }

        public void addHistory(String move, String FEN) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            JLabel moveName = new JLabel("XXXXXXX");
            final Dimension size = moveName.getPreferredSize();
            moveName.setMinimumSize(size);
            moveName.setPreferredSize(size);
            moveName.setText(move);
            panel.add(moveName);

            JButton fenButton = new JButton(new AbstractAction(FEN) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(FEN), null);
                }
            });
            panel.add(fenButton);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);

            historyPanel.add(panel);
            //JScrollBar vertical = pane.getVerticalScrollBar();
            //vertical.setValue(vertical.getMaximum());

            paintComponents(getGraphics());
        }

        public void clearHistory() {
            historyPanel.removeAll();
        }

        public void reset() {
            clearHistory();
            positionalChances.clear();
            this.evalLabel.setText("Resetting...");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (Main.stockFish == null) {
                        evalLabel.setText("[Evaluation disabled]");
                    } else {
                        setEval(manager.generateCurrentFEN());
                    }
                }
            }, 1000);
        }

        public EvalBoard(int size) {
            Dimension dimension = new Dimension((int) (size / 1.5), size);
            this.setPreferredSize(dimension);
            this.setTitle("Evaluation");
            try {
                this.setIconImage(Main.fileUtils.getImage("wp"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.positionalChances = new HashMap<>();

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            evalLabel = new JLabel();
            dtz = new JLabel();
            dtm = new JLabel();
            panel.add(evalLabel);
            JPanel dtzdtm = new JPanel();
            dtzdtm.setLayout(new BoxLayout(dtzdtm, BoxLayout.X_AXIS));
            dtzdtm.add(dtz);
            dtzdtm.add(dtm);
            dtzdtm.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(dtzdtm);
            Font newFont = new Font(Font.DIALOG, Font.PLAIN, 16);
            evalLabel.setText("Loading evaluation...");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (Main.stockFish == null) {
                        evalLabel.setText("[Evaluation disabled]");
                    } else {
                        setEval(manager.generateCurrentFEN());
                    }
                }
            }, 1000);

            evalLabel.setFont(newFont);

            dtz.setFont(newFont);
            dtm.setFont(newFont);
            this.historyPanel = new JPanel();
            historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
            historyPanel.setBorder(new EmptyBorder(0, 3, 3, 3));
            pane = new JScrollPane(historyPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            pane.setPreferredSize(new Dimension(size, size / 2));
            panel.add(new JSeparator());
            panel.add(pane);

            evalBar = new JProgressBar();
            evalBar.setPreferredSize(new Dimension(-1, 10));
            evalBar.setMinimum(0);
            evalBar.setMaximum(200);
            evalBar.setValue((evalBar.getMinimum() + evalBar.getMaximum()) / 2);
            evalBar.setOrientation(SwingConstants.HORIZONTAL);
            evalBar.setForeground(Color.lightGray);
            evalBar.setBackground(Color.darkGray);
            evalBar.setUI(new BasicProgressBarUI());

            winChanceBar = new JProgressBar();
            winChanceBar.setPreferredSize(new Dimension(-1, 10));
            winChanceBar.setMinimum(0);
            winChanceBar.setMaximum(100);
            winChanceBar.setValue((winChanceBar.getMinimum() + winChanceBar.getMaximum()) / 2);
            winChanceBar.setOrientation(SwingConstants.HORIZONTAL);
            winChanceBar.setForeground(Color.lightGray);
            winChanceBar.setBackground(Color.darkGray);
            winChanceBar.setUI(new BasicProgressBarUI());

            swingWorkerRealTime = new SwingWorkerRealTime();
            CompletableFuture.runAsync(swingWorkerRealTime::go);

            panel.add(new JSeparator());
            panel.add(winChanceBar);
            panel.add(new JSeparator());
            panel.add(evalBar);

            this.getContentPane().add(panel);

            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.setResizable(false);
            this.pack();
            this.setLocationRelativeTo(null);
            this.setLocation(0,0);
            this.setVisible(true);
        }
    }

    private static class SliderPopupListener extends MouseAdapter {
        private final JWindow toolTip = new JWindow();
        private final JLabel label = new JLabel("", SwingConstants.CENTER);
        private final Dimension size = new Dimension(80, 20);
        private int prevValue = -1;
        private final SliderClass type;

        enum SliderType implements SliderClass {
            TIME,
            VALUE;

            @Override
            public SliderType getType() {
                return this;
            }

            @Override
            public String getLabel() {
                return "";
            }
        }

        public record SliderTypeLabel(SliderType sliderType, String sliderLabel) implements SliderClass {
            @Override
            public SliderType getType() {
                return sliderType;
            }

            @Override
            public String getLabel() {
                return sliderLabel;
            }
        }

        interface SliderClass {
            SliderType getType();
            String getLabel();
        }

        protected SliderPopupListener(SliderClass type) {
            super();
            label.setOpaque(false);
            label.setBackground(UIManager.getColor("ToolTip.background"));
            label.setBorder(UIManager.getBorder("ToolTip.border"));
            toolTip.add(label);
            toolTip.setSize(size);
            toolTip.setAlwaysOnTop(true);
            this.type = type;
        }
        protected void updateToolTip(MouseEvent me) {
            JSlider slider = (JSlider) me.getComponent();
            int intValue = slider.getValue();
            if (prevValue != intValue) {
                switch (type.getType()) {
                    case TIME -> label.setText(slider.getValue() + " ms (" + (slider.getValue() / 1000) + "s)");
                    case VALUE -> {
                        if (!Objects.equals(type.getLabel(), "")) {
                            String newLabel = type.getLabel();
                            if (newLabel.startsWith(" ")) {
                                label.setText(slider.getValue() + type.getLabel());
                            } else {
                                label.setText(type.getLabel() + slider.getValue());
                            }
                        }
                    }
                }
                Point pt = me.getPoint();
                pt.y = -size.height;
                SwingUtilities.convertPointToScreen(pt, me.getComponent());
                pt.translate(-size.width / 2, 0);
                toolTip.setLocation(pt);
            }
            prevValue = intValue;
        }
        @Override
        public void mouseDragged(MouseEvent me) {
            updateToolTip(me);
        }
        @Override
        public void mousePressed(MouseEvent me) {
            if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag")
                    && SwingUtilities.isLeftMouseButton(me)) {
                toolTip.setVisible(true);
                updateToolTip(me);
            }
        }
        @Override
        public void mouseReleased(MouseEvent me) {
            toolTip.setVisible(false);
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            JSlider s = (JSlider) e.getComponent();
            int i = s.getValue() - e.getWheelRotation();
            BoundedRangeModel m = s.getModel();
            s.setValue(Math.min(Math.max(i, m.getMinimum()), m.getMaximum()));
        }
    }

    private final EvalBoard evalBoard;

    public EvalBoard getEvalBoard() {
        return evalBoard;
    }

    public ChessBoard(int size, String fen) {
        WHITE_VIEW = Main.WHITE_TO_MOVE;
        COMPUTER_WAIT_TIME_MS = Main.ENGINE_THINKING_TIME;
        this.pieceSize = size / 8;
        Dimension boardSize = new Dimension(size, size);

        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(boardSize);
        layeredPane.addMouseListener(this);
        layeredPane.addMouseMotionListener(this);
        getContentPane().add(layeredPane);

        chessBoard = new JPanel();
        chessBoard.setLayout(new GridLayout(8, 8));
        chessBoard.setPreferredSize(boardSize);
        chessBoard.setBounds(0, 0, boardSize.width, boardSize.height);
        layeredPane.add(chessBoard, JLayeredPane.DEFAULT_LAYER);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                JPanel square = new JPanel(new BorderLayout());
                square.setBackground((i + j) % 2 == 0 ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                chessBoard.add(square);
            }
        }

        this.setTitle("Chess");
        try {
            this.setIconImage(Main.fileUtils.getImage("bp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
        JMenuBar jMenuBar = new JMenuBar();
        JMenu menu = new JMenu("Settings");
        JMenuItem setFEN = new JMenuItem(new AbstractAction("Paste FEN") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    Clipboard clipboard = toolkit.getSystemClipboard();
                    String result = (String) clipboard.getData(DataFlavor.stringFlavor);
                    manager.reset(result);
                } catch (UnsupportedFlavorException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        JMenuItem copyFEN = new JMenuItem(new AbstractAction("Copy FEN") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(manager.generateCurrentFEN()), null);
            }
        });
        JCheckBoxMenuItem viewFromWhite = new JCheckBoxMenuItem("View from white?");
        viewFromWhite.addItemListener(e -> ChessBoard.WHITE_VIEW = viewFromWhite.isSelected());
        viewFromWhite.setSelected(ChessBoard.WHITE_VIEW);



        JMenu opponent = new JMenu("Opponent Type");
        ButtonGroup opponentGroup = new ButtonGroup();
        JRadioButtonMenuItem computerOpponent = new JRadioButtonMenuItem("Computer");
        computerOpponent.addItemListener(e -> {
            if (computerOpponent.isSelected()) {
                Main.OPPONENT = Main.MoveType.COMPUTER;
            }
        });
        opponentGroup.add(computerOpponent);
        JRadioButtonMenuItem playerOpponent = new JRadioButtonMenuItem("Player");
        playerOpponent.addItemListener(e -> {
            if (playerOpponent.isSelected()) {
                Main.OPPONENT = Main.MoveType.PLAYER;
            }
        });
        opponentGroup.add(playerOpponent);
        JRadioButtonMenuItem randomMoveOpponent = new JRadioButtonMenuItem("Random Moves");
        randomMoveOpponent.addItemListener(e -> {
            if (randomMoveOpponent.isSelected()) {
                Main.OPPONENT = Main.MoveType.RANDOM;
            }
        });

        opponentGroup.add(randomMoveOpponent);

        JMenu opponent2 = new JMenu("Self Type");
        ButtonGroup opponentGroup2 = new ButtonGroup();
        JRadioButtonMenuItem computerOpponent2 = new JRadioButtonMenuItem("Computer");
        computerOpponent2.addItemListener(e -> {
            if (computerOpponent2.isSelected()) {
                Main.SELF = Main.MoveType.COMPUTER;
            }
        });
        opponentGroup2.add(computerOpponent2);
        JRadioButtonMenuItem playerOpponent2 = new JRadioButtonMenuItem("Player");
        playerOpponent2.addItemListener(e -> {
            if (playerOpponent2.isSelected()) {
                Main.SELF = Main.MoveType.PLAYER;
            }
        });
        opponentGroup2.add(playerOpponent2);
        JRadioButtonMenuItem randomMoveOpponent2 = new JRadioButtonMenuItem("Random Moves");
        randomMoveOpponent2.addItemListener(e -> {
            if (randomMoveOpponent2.isSelected()) {
                Main.SELF = Main.MoveType.RANDOM;
            }
        });

        computerOpponent.setSelected(Main.OPPONENT == Main.MoveType.COMPUTER);
        computerOpponent2.setSelected(Main.SELF == Main.MoveType.COMPUTER);
        playerOpponent.setSelected(Main.OPPONENT == Main.MoveType.PLAYER);
        playerOpponent2.setSelected(Main.SELF == Main.MoveType.PLAYER);
        randomMoveOpponent.setSelected(Main.OPPONENT == Main.MoveType.RANDOM);
        randomMoveOpponent2.setSelected(Main.SELF == Main.MoveType.RANDOM);

        opponentGroup2.add(randomMoveOpponent2);

        opponent.add(playerOpponent);
        opponent.add(computerOpponent);
        opponent.add(randomMoveOpponent);
        opponent2.add(playerOpponent2);
        opponent2.add(computerOpponent2);
        opponent2.add(randomMoveOpponent2);

        JMenu commands = new JMenu("Power Actions");
        JMenuItem resetGame = new JMenuItem(new AbstractAction("Reset Game") {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.reset();
            }
        });
        JMenuItem quitProgram = new JMenuItem(new AbstractAction("Quit Program") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        JCheckBoxMenuItem ignoreRules = new JCheckBoxMenuItem("Illegal moves");
        ignoreRules.addItemListener(e -> IGNORE_RULES = ignoreRules.isSelected());
        ignoreRules.setSelected(IGNORE_RULES);



        JMenu computerSettings = new JMenu("Computer Settings");

        menu.add(setFEN);
        menu.add(copyFEN);
        menu.add(viewFromWhite);
        menu.add(opponent);
        menu.add(opponent2);
        commands.add(ignoreRules);
        commands.add(resetGame);
        commands.add(quitProgram);
        computerSettings.add(getSlider(SliderPopupListener.SliderType.TIME, 0, 20000, COMPUTER_WAIT_TIME_MS, 1000, 100, "Engine Time", e -> {
            JSlider slider = (JSlider) e.getSource();
            if (slider.getValueIsAdjusting()) return;
            COMPUTER_WAIT_TIME_MS = slider.getValue();
        }));
        computerSettings.add(getSlider(new SliderPopupListener.SliderTypeLabel(SliderPopupListener.SliderType.VALUE, " threads"), 1, Main.CPU_CORES, Main.CPU_CORES, 1, 1, "Engine Threads", e -> {
            JSlider slider = (JSlider) e.getSource();
            if (slider.getValueIsAdjusting()) return;
            Main.stockFish.sendCommand("setoption name Threads value " + slider.getValue());
        }));
        computerSettings.add(getSlider(new SliderPopupListener.SliderTypeLabel(SliderPopupListener.SliderType.VALUE, " variations"), 1, 100, 1, 1, 1, "Engine Principal Variation", e -> {
            JSlider slider = (JSlider) e.getSource();
            if (slider.getValueIsAdjusting()) return;
            Main.stockFish.sendCommand("setoption name MultiPV value " + slider.getValue());
        }));
        computerSettings.add(getSlider(new SliderPopupListener.SliderTypeLabel(SliderPopupListener.SliderType.VALUE, "Level "), 0, 20, 20, 1, 1, "Engine Strength", e -> {
            JSlider slider = (JSlider) e.getSource();
            if (slider.getValueIsAdjusting()) return;
            Main.stockFish.sendCommand("setoption name Skill Level value " + slider.getValue());
        }));
        jMenuBar.add(menu);
        jMenuBar.add(commands);
        jMenuBar.add(computerSettings);
        this.setJMenuBar(jMenuBar);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        this.manager = new GameManager(this, fen);
        //r3k2r/pbppqpb1/1pn3p1/7p/1N2p1n1/1PP4N/PB1P1PPP/2QRKR2 w kq - 0 1
        this.evalBoard = new EvalBoard(size);
    }

    public static JMenuItem getSlider(SliderPopupListener.SliderClass type, int min, int max, int defaultValue, int steps, int minorSteps, String label, ChangeListener listener) {
        JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, defaultValue) {
            private SliderPopupListener popupHandler;
            @Override
            public void updateUI() {
                removeMouseMotionListener(popupHandler);
                removeMouseListener(popupHandler);
                removeMouseWheelListener(popupHandler);
                super.updateUI();
                popupHandler = new SliderPopupListener(type);
                addMouseMotionListener(popupHandler);
                addMouseListener(popupHandler);
                addMouseWheelListener(popupHandler);
            }
        };
        slider.setMajorTickSpacing(steps);
        slider.setMinorTickSpacing(minorSteps);
        slider.setSnapToTicks(true);
        slider.addChangeListener(listener);

        JMenuItem menuItem = new JMenuItem();
        menuItem.setPreferredSize(new Dimension(150, 40));
        menuItem.setLayout(new BoxLayout(menuItem, BoxLayout.Y_AXIS));
        JLabel menuLabel = new JLabel(label);
        menuLabel.setHorizontalAlignment(SwingConstants.LEFT);
        menuItem.add(menuLabel);
        menuItem.add(slider);
        return menuItem;
    }

    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            chessPiece = null;
            Component c = chessBoard.findComponentAt(e.getX(), e.getY());

            if (c instanceof JPanel) return;

            chessPiece = (JLabel) c;
            chessPiece.setLocation(e.getX() - (chessPiece.getWidth() / 2), e.getY()  - (chessPiece.getWidth() / 2));

            layeredPane.add(chessPiece, JLayeredPane.DRAG_LAYER);
            layeredPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            clicked = e.getPoint();
        } else if (SwingUtilities.isRightMouseButton(e)) {
            GameManager.Tile clickedTile = GameManager.Tile.fromPoint(WHITE_VIEW, e.getPoint(), pieceSize);
            if (manager.getInternalBoard()[clickedTile.row()][clickedTile.column()] == null) return;

            StringJoiner joiner = new StringJoiner(", ");
            int i = 0;
            for (SimpleMove possibleMove : Objects.requireNonNull(manager.possibleMoves(manager.getInternalBoard(), GameManager.Tile.fromPoint(WHITE_VIEW, e.getPoint(), pieceSize)))) {
                joiner.add(possibleMove.deepToString(manager, manager.getInternalBoard()));
                i++;
            }
            System.out.println("Available moves: [" + joiner + "] (" + i + ")");
        }
    }

    public void mouseDragged(MouseEvent me) {
        if (SwingUtilities.isLeftMouseButton(me)) {
            if (chessPiece == null) return;

            int x = Math.max(Math.min(me.getX(), layeredPane.getWidth()), 0);
            int y = Math.max(Math.min(me.getY(), layeredPane.getHeight()), 0);

            chessPiece.setLocation(x - (chessPiece.getWidth() / 2), y - (chessPiece.getWidth() / 2));
        }
    }

    public synchronized void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            layeredPane.setCursor(null);

            if (chessPiece == null) return;

            chessPiece.setVisible(false);
            layeredPane.remove(chessPiece);
            chessPiece.setVisible(true);

            int x = Math.max(Math.min(e.getX(), layeredPane.getWidth() - chessPiece.getWidth()), 0);
            int y = Math.max(Math.min(e.getY(), layeredPane.getHeight() - chessPiece.getHeight()), 0);

            Component c = chessBoard.findComponentAt(x, y);
            released = new Point(x, y);

            Container parent;
            if (c instanceof JLabel) {
                parent = c.getParent();
                parent.remove(0);
            } else {
                parent = (Container) c;
            }
            parent.add(chessPiece);
            parent.validate();

            if (released == null || clicked == null) return;
            GameManager.Tile from = GameManager.Tile.fromPoint(WHITE_VIEW, clicked, pieceSize);
            GameManager.Tile to = GameManager.Tile.fromPoint(WHITE_VIEW, released, pieceSize);
            if (from.equals(to)) return;
            final PieceWrapper piece = manager.getInternalBoard()[from.row()][from.column()];
            if ((to.row() == 7 || to.row() == 0) && piece instanceof Pawn) {
                manager.playerMovePiece(from, to, manager.getInternalBoard(), getFromInteger(0 /* TODO Piece Promotion Panel */, piece.isWhite(), to));
            } else {
                manager.playerMovePiece(from, to, manager.getInternalBoard());
            }

            released = null;
            clicked = null;
        }
    }

    public static PieceWrapper getFromInteger(int piece, boolean white, GameManager.Tile startingSquare) {
        switch (piece) {
            case 0 -> {
                return new Queen(white, startingSquare);
            }
            case 1 -> {
                return new Rook(white, startingSquare);
            }
            case 2 -> {
                return new Bishop(white, startingSquare);
            }
            case 3 -> {
                return new Knight(white, startingSquare);
            }
        }
        return null;
    }

    public void mouseClicked(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}