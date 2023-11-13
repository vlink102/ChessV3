package me.vlink102.personal.game.pieces;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.PieceEnum;

public class King extends PieceWrapper {
    public King(boolean white, GameManager.Tile startingSquare) {
        super(white ? PieceEnum.WHITE_KING : PieceEnum.BLACK_KING, white, startingSquare);
    }

    @Override
    public boolean canMove(SimpleMove move, boolean capture) {
        int x0 = move.getFrom().column();
        int y0 = move.getFrom().row();
        int x1 = move.getTo().column();
        int y1 = move.getTo().row();

        return Math.abs(x0 - x1) <= 1 && Math.abs(y0 - y1) <= 1;
    }
}
