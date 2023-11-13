package me.vlink102.personal.game;

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

    @Override
    public String toString() {
        return ((!(piece instanceof Pawn)) ? piece.getType().getAbbr().substring(1,2).toUpperCase() : "") + to.toString();
    }

    public String deepToString(GameManager manager, PieceWrapper[][] board) {
        PieceWrapper[][] result = manager.getResultBoard(board, this);
        StringBuilder builder = new StringBuilder();
        builder.append(((!(piece instanceof Pawn)) ? piece.getType().getAbbr().substring(1,2).toUpperCase() : ""));
        if (board[to.row()][to.column()] != null) {
            builder.append("x");
        }
        builder.append(to);
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
