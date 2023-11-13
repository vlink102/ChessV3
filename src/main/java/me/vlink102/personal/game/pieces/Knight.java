package me.vlink102.personal.game.pieces;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.PieceEnum;

public class Knight extends PieceWrapper {

    public Knight(boolean white, GameManager.Tile startingSquare) {
        super(white ? PieceEnum.WHITE_KNIGHT : PieceEnum.BLACK_KNIGHT, white, startingSquare);
    }

    @Override
    public boolean canMove(SimpleMove move, boolean capture) {
        int x0 = move.getFrom().column();
        int y0 = move.getFrom().row();
        int x1 = move.getTo().column();
        int y1 = move.getTo().row();

        return (Math.abs(x1 - x0) == 1 && Math.abs(y1 - y0) == 2) || (Math.abs(x1 - x0) == 2 && Math.abs(y1 - y0) == 1);
    }
}
