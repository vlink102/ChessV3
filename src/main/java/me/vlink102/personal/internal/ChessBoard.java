package me.vlink102.personal.internal;

import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.game.pieces.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class ChessBoard extends JFrame implements MouseListener, MouseMotionListener {
    JLayeredPane layeredPane;
    JPanel chessBoard;
    JLabel chessPiece;

    Point clicked;
    Point released;

    private final int pieceSize;
    public static final boolean WHITE = true;
    public static final float COMPUTER_WAIT_TIME = 3; // seconds

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

    public static class EvalBoard extends JFrame {

        private final JLabel label;

        public void setEval(Number eval) {
            if (eval instanceof Integer) {
                label.setText("Evaluation: Mate in " + eval);
            } else if (eval instanceof Float f) {
                label.setText("Evaluation: " + (f > 0.0f ? "+" : "") + eval);
            }
        }


        public EvalBoard(int size) {
            Dimension dimension = new Dimension(size / 2, size);
            this.setPreferredSize(dimension);
            this.setTitle("Evaluation");
            try {
                this.setIconImage(Main.fileUtils.getImage("wp"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            label = new JLabel();
            panel.add(label);
            this.getContentPane().add(panel);

            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.setResizable(false);
            this.pack();
            this.setLocationRelativeTo(null);
            this.setVisible(true);
        }
    }

    private final EvalBoard evalBoard;

    public EvalBoard getEvalBoard() {
        return evalBoard;
    }

    public ChessBoard(int size) {
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
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        this.manager = new GameManager(this, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        this.evalBoard = new EvalBoard(size);

    }

    public void mousePressed(MouseEvent e) {
        chessPiece = null;
        Component c = chessBoard.findComponentAt(e.getX(), e.getY());

        if (c instanceof JPanel) return;

        chessPiece = (JLabel) c;
        chessPiece.setLocation(e.getX() - (chessPiece.getWidth() / 2), e.getY()  - (chessPiece.getWidth() / 2));

        layeredPane.add(chessPiece, JLayeredPane.DRAG_LAYER);
        layeredPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        clicked = e.getPoint();

        GameManager.Tile clickedTile = GameManager.Tile.fromPoint(WHITE, clicked, pieceSize);
        if (manager.getInternalBoard()[clickedTile.row()][clickedTile.column()] == null) return;

        StringJoiner joiner = new StringJoiner(", ");
        int i = 0;
        for (SimpleMove possibleMove : Objects.requireNonNull(manager.possibleMoves(manager.getInternalBoard(), GameManager.Tile.fromPoint(WHITE, clicked, pieceSize)))) {
            joiner.add(possibleMove.deepToString(manager, manager.getInternalBoard()));
            i++;
        }
        System.out.println("Available moves: [" + joiner + "] (" + i + ")");
    }

    public void mouseDragged(MouseEvent me) {
        if (chessPiece == null) return;

        int x = Math.max(Math.min(me.getX(), layeredPane.getWidth()), 0);
        int y = Math.max(Math.min(me.getY(), layeredPane.getHeight()), 0);

        chessPiece.setLocation(x - (chessPiece.getWidth() / 2), y - (chessPiece.getWidth() / 2));
    }

    public synchronized void mouseReleased(MouseEvent e) {
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
        GameManager.Tile from = GameManager.Tile.fromPoint(WHITE, clicked, pieceSize);
        GameManager.Tile to = GameManager.Tile.fromPoint(WHITE, released, pieceSize);
        if (from.equals(to)) return;
        final PieceWrapper piece = manager.getInternalBoard()[from.row()][from.column()];
        if ((to.row() == 7 || to.row() == 0) && piece instanceof Pawn) {
            manager.movePiece(from, to, manager.getInternalBoard(), getFromInteger(0 /* TODO Piece Promotion Panel */, piece.isWhite(), to));
        } else {
            manager.movePiece(from, to, manager.getInternalBoard());
            manager.endGame();
        }

        released = null;
        clicked = null;
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