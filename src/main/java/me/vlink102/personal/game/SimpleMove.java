package me.vlink102.personal.game;

import me.vlink102.personal.game.pieces.King;
import me.vlink102.personal.game.pieces.Pawn;

public class SimpleMove {
    private final GameManager.Tile from;
    private final GameManager.Tile to;
    private final PieceWrapper piece;

    public SimpleMove(GameManager.Tile from, GameManager.Tile to, final PieceWrapper piece) {
        this.from = from;
        this.to = to;
        this.piece = piece;
    }

    public SimpleMove(GameManager.Tile from, GameManager.Tile to, final PieceWrapper[][] board) {
        this.piece = board[from.row()][from.column()];
        this.from = from;
        this.to = to;
    }

    public GameManager.Tile getFrom() {
        return from;
    }

    public GameManager.Tile getTo() {
        return to;
    }

    public PieceWrapper getPiece() {
        return piece;
    }

    /**
     * Not color discriminatory
     */
    public boolean isCapture(PieceWrapper[][] board) {
        PieceWrapper piece = board[to.row()][to.column()];
        return piece != null;
    }

    @Override
    public String toString() {
        return ((!(piece instanceof Pawn)) ? piece.getType().getAbbr().substring(1,2).toUpperCase() : "") + to.toString();
    }

    public String deepToString(GameManager manager, PieceWrapper[][] board) {
        PieceWrapper[][] result = manager.getResultBoard(board, this);
        StringBuilder builder = new StringBuilder();
        boolean isCastle = (piece instanceof King && Math.abs(to.column() - from.column()) == 2);
        boolean isEnPassant = (piece instanceof Pawn && manager.getEnPassant() != null && manager.getEnPassant().equals(to));
        boolean capture = (board[to.row()][to.column()] != null);
        String abbreviation = piece.getType().getNamedNotation();


        if (isCastle) {
            if (to.column() > from.column()) {
                builder.append("O-O-O");
            } else {
                builder.append("O-O");
            }
        }

        if (!isCastle) builder.append(abbreviation);
        if (capture) {
            if (piece instanceof Pawn) {
                builder.append(from.getColumnLetter());
            }
            builder.append("x");
        }
        if (isEnPassant) {
            builder.append(from.getColumnLetter());
            builder.append("x");
        }

        if (!isCastle) builder.append(to);
        if (manager.isKingInCheck(result, !piece.isWhite())) {
            if (manager.isCheckmated(result, !piece.isWhite())) {
                builder.append("#");
            } else {
                builder.append("+");
            }
        }
        return builder.toString();
    }
}
