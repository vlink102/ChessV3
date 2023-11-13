package me.vlink102.personal.game.pieces;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.PieceEnum;

public class Bishop extends PieceWrapper {
    public Bishop(boolean white, GameManager.Tile startingSquare) {
        super(white ? PieceEnum.WHITE_BISHOP : PieceEnum.BLACK_BISHOP, white, startingSquare);
    }

    @Override
    public boolean canMove(SimpleMove move, boolean capture) {
        return isDiagonal(move.getFrom(), move.getTo());
    }
}
