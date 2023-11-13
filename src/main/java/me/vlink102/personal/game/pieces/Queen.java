package me.vlink102.personal.game.pieces;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.PieceEnum;

public class Queen extends PieceWrapper {
    public Queen(boolean white, GameManager.Tile startingSquare) {
        super(white ? PieceEnum.WHITE_QUEEN : PieceEnum.BLACK_QUEEN, white, startingSquare);
    }

    @Override
    public boolean canMove(SimpleMove move, boolean capture) {
        GameManager.Tile from = move.getFrom();
        GameManager.Tile to = move.getTo();
        return isDiagonal(from, to) || isStraight(from, to);
    }
}
