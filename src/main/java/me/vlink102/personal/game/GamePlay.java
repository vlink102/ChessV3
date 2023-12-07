package me.vlink102.personal.game;

import me.vlink102.personal.game.pieces.King;
import me.vlink102.personal.game.pieces.Pawn;
import me.vlink102.personal.game.pieces.Rook;
import me.vlink102.personal.internal.ChessBoard;

public class GamePlay {
    private final GameManager manager;
    private volatile boolean whiteToMove;
    private int halfMoveCounter;
    private int fullMoveCounter;
    private int fiftyMoveRule;
    private boolean castleWhiteKing, castleWhiteQueen, castleBlackKing, castleBlackQueen;

    public GamePlay(GameManager manager) {
        reset();
        this.manager = manager;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void setWhiteToMove(boolean whiteToMove) {
        this.whiteToMove = whiteToMove;
    }

    public int getHalfMoveCounter() {
        return halfMoveCounter;
    }

    public void setHalfMoveCounter(int halfMoveCounter) {
        this.halfMoveCounter = halfMoveCounter;
    }

    public int getFullMoveCounter() {
        return fullMoveCounter;
    }

    public void setFullMoveCounter(int fullMoveCounter) {
        this.fullMoveCounter = fullMoveCounter;
    }

    public boolean isCastleBlackKing() {
        return castleBlackKing;
    }

    public void setCastleBlackKing(boolean castleBlackKing) {
        this.castleBlackKing = castleBlackKing;
    }

    public boolean isCastleBlackQueen() {
        return castleBlackQueen;
    }

    public void setCastleBlackQueen(boolean castleBlackQueen) {
        this.castleBlackQueen = castleBlackQueen;
    }

    public boolean isCastleWhiteKing() {
        return castleWhiteKing;
    }

    public void setCastleWhiteKing(boolean castleWhiteKing) {
        this.castleWhiteKing = castleWhiteKing;
    }

    public boolean isCastleWhiteQueen() {
        return castleWhiteQueen;
    }

    public void setCastleWhiteQueen(boolean castleWhiteQueen) {
        this.castleWhiteQueen = castleWhiteQueen;
    }

    public int getFiftyMoveRule() {
        return fiftyMoveRule;
    }

    public void movePiece(SimpleMove move, PieceWrapper[][] board) {
        if (!whiteToMove) {
            fullMoveCounter++;
        }
        halfMoveCounter++;
        if (!(move.getPiece() instanceof Pawn) && !move.isCapture(board)) {
            fiftyMoveRule++;
        }
        if (board[move.getTo().row()][move.getTo().column()] != null) {
            if (board[move.getTo().row()][move.getTo().column()] instanceof Rook rook) {
                disableRook(rook);
            }
        }
        if (move.getPiece() instanceof Rook rook) {
            disableRook(rook);
        }
        if (move.getPiece() instanceof King king) {
            if (king.isWhite()) {
                castleWhiteKing = false;
                castleWhiteQueen = false;
            } else {
                castleBlackQueen = false;
                castleBlackKing = false;
            }
        }
        whiteToMove = !whiteToMove;
        manager.refreshBoard(ChessBoard.WHITE_VIEW);
    }

    private void disableRook(Rook rook) {
        if (rook.getSide() == null) {
            return;
        }
        if (rook.isWhite()) {
            switch (rook.getSide()) {
                case KINGSIDE -> castleWhiteKing = false;
                case QUEENSIDE -> castleWhiteQueen = false;
            }
        } else {
            switch (rook.getSide()) {
                case KINGSIDE -> castleBlackKing = false;
                case QUEENSIDE -> castleBlackQueen = false;
            }
        }
    }

    public void reset() {
        this.whiteToMove = true;
        this.halfMoveCounter = 0;
        this.fullMoveCounter = 1;

        this.castleWhiteKing = true;
        this.castleWhiteQueen = true;
        this.castleBlackKing = true;
        this.castleBlackQueen = true;

        this.fiftyMoveRule = 0;

    }
}
