package me.vlink102.personal.game;

import me.vlink102.personal.game.pieces.*;
import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameManager {
    private final ChessBoard board;

    private final PieceWrapper[][] internalBoard;

    public PieceWrapper[][] getInternalBoard() {
        return internalBoard;
    }

    public GameManager(ChessBoard board) {
        this.board = board;
        this.internalBoard = new PieceWrapper[8][8];
        setupPieces();
        refreshBoard(ChessBoard.WHITE);
    }

    public void movePiece(Tile from, Tile to, PieceWrapper[][] board) {
        movePiece(board, new SimpleMove(from, to, board));
        refreshBoard(ChessBoard.WHITE);
    }

    public boolean canMove(SimpleMove move, PieceWrapper[][] board) {
        PieceWrapper target = board[move.getTo().row][move.getTo().column];
        PieceWrapper piece = move.getPiece();
        if (target == null) {
            return piece.canMove(move, false);
        } else if (target.isWhite() != move.getPiece().isWhite()) {
            return piece.canMove(move, true);
        }
        return false;
    }

    public boolean notBlocked(PieceWrapper[][] board, Tile f, Tile t) {
        int yfrom = f.row;
        int xfrom = f.column;
        int yto = t.row;
        int xto = t.column;

        PieceWrapper from = board[yfrom][xfrom];
        PieceWrapper to = board[yto][xto];

        int dx = Integer.compare(xto, xfrom);
        int dy = Integer.compare(yto, yfrom);

        int steps = Math.max(Math.abs(xfrom - xto), Math.abs(yfrom - yto));

        if (xfrom == xto || yfrom == yto || Math.abs(xfrom - xto) == Math.abs(yfrom - yto)) {
            for (int i = 1; i < steps; i++) {
                int x = xfrom + i * dx;
                int y = yfrom + i * dy;
                if ((board[y][x] != null && board[y][x] != to && board[y][x] != from)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isTileInCheck(Tile tile, boolean whiteToCheck, PieceWrapper[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper wrapper = board[i][j];
                if (wrapper == null) continue;
                if (wrapper.isWhite() != whiteToCheck) continue;
                Tile from = new Tile(i, j);
                if (from.equals(tile)) continue;
                if (canMove(new SimpleMove(from, tile, board), board) && notBlocked(board, from, tile)) {
                    return true;
                }
            }
        }
        return false;
    }

    public PieceWrapper[][] getResultBoard(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper[][] tempBoard = Arrays.stream(board).map(PieceWrapper[]::clone).toArray(PieceWrapper[][]::new);
        tempBoard = movePieceInternal(tempBoard, move);
        return tempBoard;
    }

    public Tile getKing(PieceWrapper[][] board, boolean white) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper wrapper = board[i][j];
                if (wrapper == null) continue;
                if (wrapper.isWhite() != white) continue;
                if (wrapper instanceof King) return new Tile(i, j);
            }
        }
        throw new RuntimeException("King not found");
    }

    public boolean isKingInCheck(PieceWrapper[][] board, boolean white) {
        return isTileInCheck(getKing(board, white), !white, board);
    }

    public boolean kingAvoidsCheck(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper[][] newBoard = getResultBoard(board, move);
        return !isKingInCheck(newBoard, move.getPiece().isWhite());
    }

    public void movePiece(PieceWrapper[][] board, SimpleMove move) {
        if (move.getPiece() == null) return;
        if (canMove(move, board) && notBlocked(board, move.getFrom(), move.getTo()) && kingAvoidsCheck(board, move)) {
            movePieceInternal(board, move);
            move.getPiece().incrementMoves();
        }
    }

    public PieceWrapper[][] movePieceInternal(PieceWrapper[][] board, SimpleMove move) {
        Tile from = move.getFrom();
        Tile to = move.getTo();
        if (board[from.row][from.column].equals(move.getPiece())) {
            PieceWrapper temp = board[from.row][from.column];
            board[from.row][from.column] = null;
            board[to.row][to.column] = temp;
        }
        return board;
    }

    public void setupPieces() {
        setInternalPiece(new King(true, new Tile(0, 3)), new Tile(0, 3));
        setInternalPiece(new Queen(true, new Tile(0, 4)), new Tile(0, 4));
        setInternalPiece(new Bishop(true, new Tile(0, 2)), new Tile(0, 2));
        setInternalPiece(new Bishop(true, new Tile(0, 5)), new Tile(0, 5));
        setInternalPiece(new Knight(true, new Tile(0, 1)), new Tile(0, 1));
        setInternalPiece(new Knight(true, new Tile(0, 6)), new Tile(0, 6));
        setInternalPiece(new Rook(true, new Tile(0, 0)), new Tile(0, 0));
        setInternalPiece(new Rook(true, new Tile(0, 7)), new Tile(0, 7));
        setInternalPiece(new King(false, new Tile(7, 3)), new Tile(7, 3));
        setInternalPiece(new Queen(false, new Tile(7, 4)), new Tile(7, 4));
        setInternalPiece(new Bishop(false, new Tile(7, 2)), new Tile(7, 2));
        setInternalPiece(new Bishop(false, new Tile(7, 5)), new Tile(7, 5));
        setInternalPiece(new Knight(false, new Tile(7, 1)), new Tile(7, 1));
        setInternalPiece(new Knight(false, new Tile(7, 6)), new Tile(7, 6));
        setInternalPiece(new Rook(false, new Tile(7, 0)), new Tile(7, 0));
        setInternalPiece(new Rook(false, new Tile(7, 7)), new Tile(7, 7));

        for (int i = 0; i < 8; i++) {
            setInternalPiece(new Pawn(true, new Tile(1, i)), new Tile(1, i));
            setInternalPiece(new Pawn(false, new Tile(6, i)), new Tile(6, i));
        }
    }

    public void setInternalPiece(PieceWrapper piece, Tile tile) {
        internalBoard[tile.row][tile.column] = piece;
    }

    public void refreshBoard(boolean viewFromWhite) {
        for (Component component : board.getChessBoard().getComponents()) {
            JPanel tilePanel = (JPanel) component;
            tilePanel.removeAll();
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = new Tile(i, j);
                addPiece(viewFromWhite ? internalBoard[7 - tile.row][7 - tile.column] : internalBoard[tile.row][tile.column], tile);
            }
        }
    }

    public void addPiece(PieceWrapper piece, Tile tile) {
        ImageIcon imageIcon = FileUtils.getPieces().get((piece == null) ? null : piece.getType());
        JLabel pieceLabel = new JLabel(imageIcon);
        JPanel tilePanel = (JPanel) board.getChessBoard().getComponent(tile.getComponent());
        tilePanel.add(pieceLabel);
    }

    public static class Tile {
        private final int row;
        private final int column;

        public int column() {
            return column;
        }

        public int row() {
            return row;
        }

        public int getComponent() {
            return (row * 8) + column;
        }

        public Tile(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public Tile(boolean white, int x, int y, int pieceSize) {
            this.row = white ? 7 - (y / pieceSize) : (y / pieceSize);
            this.column = white ? 7 - (x / pieceSize) : (x / pieceSize);
        }

        public static Tile fromPoint(boolean white, Point point, int pieceSize) {
            return new Tile(white, point.x, point.y, pieceSize);
        }

        /*
        @Override
        public String toString() {
            return "GameManager.Tile{row=" + row + ", column=" + column + "}";
        }
         */

        @Override
        public String toString() {
            return ((char) ((7 - column) + 'a')) + String.valueOf(row + 1);
        }
    }

    public List<SimpleMove> possibleMoves(PieceWrapper[][] board, boolean white) {
        List<SimpleMove> simpleMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper pieceWrapper = board[i][j];
                if (pieceWrapper == null) continue;
                if (pieceWrapper.isWhite() != white) continue;
                simpleMoves.addAll(possibleMoves(board, new Tile(i, j)));
            }
        }
        return simpleMoves;
    }

    public List<SimpleMove> possibleMoves(PieceWrapper[][] board, Tile tile) {
        if (board[tile.row][tile.column] == null) return null;
        return possibleMoves(board[tile.row][tile.column], tile, board);
    }

    public List<SimpleMove> possibleMoves(PieceWrapper wrapper, Tile tile, PieceWrapper[][] board) {
        if (!board[tile.row][tile.column].equals(wrapper)) return null;

        List<SimpleMove> possibleMoves = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile to = new Tile(i, j);
                SimpleMove possibleMove = new SimpleMove(tile, to, board);
                if (canMove(possibleMove, board) && notBlocked(board, tile, to) && kingAvoidsCheck(board, possibleMove)) {
                    possibleMoves.add(possibleMove);
                }
            }
        }
        return possibleMoves;
    }

    public boolean isCheckmated(PieceWrapper[][] board, boolean white) {
        return possibleMoves(board, white).isEmpty() && isKingInCheck(board, white);
    }

    public boolean isStalemate(PieceWrapper[][] board) {
        return (possibleMoves(board, true).isEmpty() && !isKingInCheck(board, true)) || (possibleMoves(board, false).isEmpty() && !isKingInCheck(board, false));
    }
}
