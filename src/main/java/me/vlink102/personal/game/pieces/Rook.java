package me.vlink102.personal.game.pieces;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.PieceEnum;

public class Rook extends PieceWrapper {
    public Rook(boolean white, GameManager.Tile startingSquare) {
        super(white ? PieceEnum.WHITE_ROOK : PieceEnum.BLACK_ROOK, white, startingSquare);
    }

    @Override
    public boolean canMove(SimpleMove move, boolean capture) {
        return isStraight(move.getFrom(), move.getTo());
    }
}
