package me.vlink102.personal.game;

import me.vlink102.personal.game.pieces.*;
import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;
import me.vlink102.personal.internal.PieceEnum;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GameManager {
    private final ChessBoard board;
    private final GamePlay gamePlay;

    private Tile enPassant;
    private Tile enPassantPiece;

    private final PieceWrapper[][] internalBoard;

    public PieceWrapper[][] getInternalBoard() {
        return internalBoard;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public GameManager(ChessBoard board) {
        this.board = board;
        this.internalBoard = new PieceWrapper[8][8];
        setupPieces();
        refreshBoard(ChessBoard.WHITE);
        this.gamePlay = new GamePlay();
        this.enPassant = null;
        this.enPassantPiece = null;
    }

    public void movePiece(Tile from, Tile to, PieceWrapper[][] board) {
        movePiece(board, new SimpleMove(from, to, board));
        refreshBoard(ChessBoard.WHITE);
    }

    public boolean canLegallyMove(SimpleMove move) {
        return move.getPiece().isWhite() == gamePlay.isWhiteToMove();
    }

    public boolean canMove(SimpleMove move, PieceWrapper[][] board) {
        PieceWrapper target = board[move.getTo().row][move.getTo().column];
        PieceWrapper piece = move.getPiece();
        if (target == null) {
            if (!canMoveToEnPassant(move)) {
                CastleSide castleSide = canCastle(board, move);
                if (castleSide == null) {
                    return piece.canMove(move, false);
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else if (target.isWhite() != move.getPiece().isWhite()) {
            return piece.canMove(move, true);
        }
        return false;
    }

    public boolean canMoveToEnPassant(SimpleMove move) {
        if (enPassant != null) {
            return enPassant.equals(move.getTo()) && move.getPiece() instanceof Pawn pawn && pawn.canMove(move, true);
        }
        return false;
    }

    public enum CastleSide {
        KINGSIDE,
        QUEENSIDE
    }

    public enum AmbiguityLevel {
        A1,
        A2
    }

    public @Nullable AmbiguityLevel isTileAmbiguous(PieceWrapper[][] board, Tile tile, PieceEnum pieceType, boolean white) {
        List<Tile> ambiguousTiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile iter = new Tile(i, j);
                if (iter.equals(tile)) continue;
                PieceWrapper piece = board[i][j];
                if (piece == null) continue;
                if (piece.isWhite() != white || piece.getType() != pieceType) continue;
                SimpleMove move = new SimpleMove(iter, tile, board);
                if (canMove(move, board) && notBlocked(board, iter, tile) && kingAvoidsCheck(board, move)) {
                    ambiguousTiles.add(iter);
                }
            }
        }
        for (int i = 0; i < ambiguousTiles.size(); i++) {
            for (Tile ambiguousTile : ambiguousTiles) {
                if (ambiguousTiles.get(i).equals(ambiguousTile)) continue;
                if (ambiguousTiles.get(i).column == ambiguousTile.column) {
                    return AmbiguityLevel.A2;
                }
            }
        }
        if (ambiguousTiles.size() > 1) {
            return AmbiguityLevel.A1;
        }

        return null;
    }

    public boolean canCastleThroughCheckedTiles(PieceWrapper[][] board, SimpleMove move) {
        Tile kingTile = getKing(board, move.getPiece().isWhite());
        PieceWrapper king = board[kingTile.row][kingTile.column];
        int startingCol = move.getFrom().column;
        int toCol = move.getTo().column;
        if (toCol > startingCol) {
            if (Math.abs(toCol - startingCol) == 3) {
                for (int i = 0; i < 3; i++) {
                    Tile affected = new Tile(kingTile.row, kingTile.column + (i + 1));
                    if (isTileInCheck(affected, !king.isWhite(), board)) {
                        return false;
                    }
                }
            }
        } else if (toCol < startingCol) {
            if (Math.abs(toCol - startingCol) == 2) {
                for (int i = 0; i < 2; i++) {
                    Tile affected = new Tile(kingTile.row, kingTile.column - (i + 1));
                    if (isTileInCheck(affected, !king.isWhite(), board)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void cleanUpCastleMove(PieceWrapper[][] board, SimpleMove move, boolean tempo) {
        if (move.isCapture(board)) return;
        if (move.getPiece() instanceof King king) {
            int startingCol = move.getFrom().column;
            int toCol = move.getTo().column;
            if (Math.abs(toCol - startingCol) == 2) {
                if (toCol > startingCol) {
                    Tile rook = getRook(board, PieceWrapper.RookSide.QUEENSIDE, king.isWhite());
                    assert rook != null;
                    PieceWrapper temp = board[rook.row][rook.column];
                    board[rook.row][rook.column] = null;
                    board[rook.row][rook.column - 3] = temp;
                    if (tempo) return;
                    if (king.isWhite()) {
                        gamePlay.setCastleWhiteQueen(false);
                    } else {
                        gamePlay.setCastleBlackQueen(false);
                    }
                } else if (toCol < startingCol) {
                    Tile rook = getRook(board, PieceWrapper.RookSide.KINGSIDE, king.isWhite());
                    assert rook != null;
                    PieceWrapper temp = board[rook.row][rook.column];
                    board[rook.row][rook.column] = null;
                    board[rook.row][rook.column + 2] = temp;
                    if (tempo) return;
                    if (king.isWhite()) {
                        gamePlay.setCastleWhiteKing(false);
                    } else {
                        gamePlay.setCastleBlackKing(false);
                    }
                }
            }

        }
    }

    public @Nullable Tile getRook(PieceWrapper[][] board, PieceWrapper.RookSide side, boolean white) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = new Tile(i, j);
                if (board[i][j] instanceof Rook rook) {
                    if (rook.getSide().equals(side) && rook.isWhite() == white) {
                        return tile;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable CastleSide canCastle(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper piece = move.getPiece();
        if (piece instanceof King king && move.getTo().row == move.getFrom().row && Math.abs(move.getTo().column - move.getFrom().column) == 2) {
            if (!canCastleThroughCheckedTiles(board, move)) return null;
            int startingCol = move.getFrom().column;
            int toCol = move.getTo().column;
            if (Math.abs(toCol - startingCol) == 2) {
                if (toCol > startingCol) {
                    return (king.isWhite() ? gamePlay.isCastleWhiteQueen() : gamePlay.isCastleBlackQueen()) ? CastleSide.QUEENSIDE : null;
                } else if (toCol < startingCol) {
                    return (king.isWhite() ? gamePlay.isCastleWhiteKing() : gamePlay.isCastleBlackKing()) ? CastleSide.KINGSIDE : null;
                }
            }

        }
        return null;
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

    public void cleanUpEnPassantCapture(PieceWrapper[][] board, SimpleMove move) {
        cleanUpEnPassantCapture(board, move, enPassant, enPassantPiece);
    }

    public void cleanUpEnPassantCapture(PieceWrapper[][] board, SimpleMove move, Tile ep, Tile epP) {
        if (move.isCapture(board)) return;
        if (move.getPiece() instanceof Pawn) {
            if (move.getTo().equals(ep) && canMove(move, board)) {
                board[epP.row][epP.column] = null;
            }
        }
    }

    public void movePiece(PieceWrapper[][] board, SimpleMove move) {
        if (move.getPiece() == null) return;
        if (!canLegallyMove(move)) return;
        if (canMove(move, board) && notBlocked(board, move.getFrom(), move.getTo()) && kingAvoidsCheck(board, move)) {
            movePieceInternal(board, move);
            generateEnPassantTile(move);
            move.getPiece().incrementMoves();

            gamePlay.movePiece(move, board);
        }
    }

    public void managePromotion(PieceWrapper[][] board, SimpleMove move) {
        if (!(move.getPiece() instanceof Pawn)) return;
        if (move.getTo().row == 7) {
            // black promotion

        } else if (move.getTo().row == 0) {
            // white promotion
        }
    }


    public PieceWrapper[][] movePieceInternal(PieceWrapper[][] board, SimpleMove move) {

        cleanUpCastleMove(board, move, true);
        cleanUpEnPassantCapture(board, move);

        Tile from = move.getFrom();
        Tile to = move.getTo();
        if (board[from.row][from.column] != null) {
            if (board[from.row][from.column].equals(move.getPiece())) {
                PieceWrapper temp = board[from.row][from.column];
                board[from.row][from.column] = null;
                board[to.row][to.column] = temp;
            }
        }
        return board;
    }

    public void setupPieces() {
        setInternalPiece(new King(true, new Tile(0, 3)), new Tile(0, 3));
        //setInternalPiece(new Queen(true, new Tile(0, 4)), new Tile(0, 4));
        //setInternalPiece(new Bishop(true, new Tile(0, 2)), new Tile(0, 2));
        //setInternalPiece(new Bishop(true, new Tile(0, 5)), new Tile(0, 5));
        //setInternalPiece(new Knight(true, new Tile(0, 1)), new Tile(0, 1));
        //setInternalPiece(new Knight(true, new Tile(0, 6)), new Tile(0, 6));
        //setInternalPiece(new Rook(true, new Tile(0, 0), PieceWrapper.RookSide.KINGSIDE), new Tile(0, 0));
        //setInternalPiece(new Rook(true, new Tile(0, 7), PieceWrapper.RookSide.QUEENSIDE), new Tile(0, 7));
        setInternalPiece(new King(false, new Tile(7, 3)), new Tile(7, 3));
        //setInternalPiece(new Queen(false, new Tile(7, 4)), new Tile(7, 4));
        //setInternalPiece(new Bishop(false, new Tile(7, 2)), new Tile(7, 2));
        //setInternalPiece(new Bishop(false, new Tile(7, 5)), new Tile(7, 5));
        //setInternalPiece(new Knight(false, new Tile(7, 1)), new Tile(7, 1));
        //setInternalPiece(new Knight(false, new Tile(7, 6)), new Tile(7, 6));
        //setInternalPiece(new Rook(false, new Tile(7, 0), PieceWrapper.RookSide.KINGSIDE), new Tile(7, 0));
        //setInternalPiece(new Rook(false, new Tile(7, 7), PieceWrapper.RookSide.QUEENSIDE), new Tile(7, 7));

        for (int i = 0; i < 8; i++) {
            //setInternalPiece(new Pawn(true, new Tile(1, i)), new Tile(1, i));
            //setInternalPiece(new Pawn(false, new Tile(6, i)), new Tile(6, i));
        }
        setInternalPiece(new Pawn(true, new Tile(1, 0)), new Tile(1, 0));
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

        @Override
        public String toString() {
            return getColumnLetter() + getRowNumber();
        }

        public String getColumnLetter() {
            return String.valueOf(((char) ((7 - column) + 'a')));
        }

        public String getRowNumber() {
            return String.valueOf(row + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tile tile = (Tile) o;
            return row == tile.row && column == tile.column;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, column);
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

    public @Nullable List<SimpleMove> possibleMoves(PieceWrapper[][] board, Tile tile) {
        if (board[tile.row][tile.column] == null) return null;
        return possibleMoves(board[tile.row][tile.column], tile, board);
    }

    public @Nullable List<SimpleMove> possibleMoves(PieceWrapper wrapper, Tile tile, PieceWrapper[][] board) {
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

    public Tile getEnPassant() {
        return enPassant;
    }

    public Tile getEnPassantPiece() {
        return enPassantPiece;
    }

    public void generateEnPassantTile(SimpleMove move) {
        PieceWrapper piece = move.getPiece();
        if (piece.getMoves() == 0 && piece instanceof Pawn) {
            if (move.getFrom().row - move.getTo().row == 2) {
                // black
                enPassant = new Tile(move.getTo().row + 1, move.getTo().column);
                enPassantPiece = move.getTo();
            } else if (move.getTo().row - move.getFrom().row == 2) {
                // white
                enPassant = new Tile(move.getTo().row - 1, move.getTo().column);
                enPassantPiece = move.getTo();
            }
        } else {
            enPassantPiece = null;
            enPassant = null;
        }
    }
}
