package me.vlink102.personal.game;

import me.vlink102.personal.internal.PieceEnum;

public abstract class PieceWrapper {
    private final PieceEnum type;
    private final boolean white;
    private final GameManager.Tile startingSquare;
    private final RookSide side;
    private int moves;

    public PieceWrapper(PieceEnum type, boolean white, final GameManager.Tile startingSquare, RookSide... rookSides) {
        this.type = type;
        this.white = white;
        this.moves = 0;
        this.startingSquare = startingSquare;
        this.side = (rookSides.length != 0) ? rookSides[0] : null;
    }

    public int getMoves() {
        return moves;
    }

    public void incrementMoves() {
        this.moves++;
    }

    public GameManager.Tile getStartingSquare() {
        return startingSquare;
    }

    public PieceEnum getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }

    public boolean canMove(SimpleMove move, boolean capture) {
        return false;
    }

    public RookSide getSide() {
        return side;
    }

    public boolean isDiagonal(GameManager.Tile from, GameManager.Tile to) {
        return Math.abs(from.column() - to.column()) == Math.abs(from.row() - to.row());
    }

    public boolean isStraight(GameManager.Tile from, GameManager.Tile to) {
        return (from.column() == to.column() && from.row() != to.row()) || (from.column() != to.column() && from.row() == to.row());
    }

    public enum RookSide {
        KINGSIDE,
        QUEENSIDE
    }
}

