package me.vlink102.personal.game;

import me.vlink102.personal.game.pieces.King;
import me.vlink102.personal.game.pieces.Pawn;
import me.vlink102.personal.internal.ChessBoard;

public class SimpleMove {
    private final GameManager.Tile from;
    private final GameManager.Tile to;
    private final PieceWrapper piece;
    private final PieceWrapper[] promotionPieces;

    public SimpleMove(GameManager.Tile from, GameManager.Tile to, final PieceWrapper piece, PieceWrapper... promotionPieces) {
        this.from = from;
        this.to = to;
        this.piece = piece;
        this.promotionPieces = promotionPieces;
    }

    public SimpleMove(GameManager.Tile from, GameManager.Tile to, final PieceWrapper[][] board, PieceWrapper... promotionPieces) {
        this.piece = board[from.row()][from.column()];
        this.from = from;
        this.to = to;
        this.promotionPieces = promotionPieces;
    }

    /**
     * 0    1    2    3
     * e    2    e    4
     */
    public static boolean validateUCIMove(String uciString) {
        if (uciString.length() == 4 || uciString.length() == 5) {
            String tile1 = uciString.substring(0, 2); // TODO Maybe fixed 
            String tile2 = uciString.substring(2, 4);
            if (GameManager.Tile.isValidTile(8, tile1) && GameManager.Tile.isValidTile(8, tile2)) {
                if (uciString.length() == 4) {
                    return true;
                } else {
                    switch (uciString.substring(3, 4)) {
                        case "r", "n", "b", "q" -> {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static SimpleMove parseStockFishMove(PieceWrapper[][] board, String stockFishMove) {
        if (!validateUCIMove(stockFishMove)) {
            throw new IllegalArgumentException("Invalid UCI Move received from engine: " + stockFishMove);
        }
        String fromString = stockFishMove.substring(0, 2);
        String toString = stockFishMove.substring(2, 4);
        GameManager.Tile from = GameManager.Tile.parseTile(fromString);
        GameManager.Tile to = GameManager.Tile.parseTile(toString);
        if (stockFishMove.length() == 4) {
            return new SimpleMove(from, to, board);
        } else {
            String promotionPieceString = stockFishMove.substring(4, 5).toLowerCase();
            int pieceNum = switch (promotionPieceString) {
                case "q" -> 0;
                case "r" -> 1;
                case "b" -> 2;
                case "n" -> 3;
                default -> -1;
            };
            boolean isWhite = board[from.row()][from.column()].isWhite();
            return new SimpleMove(from, to, board, ChessBoard.getFromInteger(pieceNum, isWhite, to));
        }
    }

    public PieceWrapper getPromotionPiece() {
        if (promotionPieces.length == 0) return null;
        return promotionPieces[0];
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
        return ((!(piece instanceof Pawn)) ? piece.getType().getAbbr().substring(1, 2).toUpperCase() : "") + to.toString();
    }

    public String toUCI() {
        return from.toString() + to.toString() + ((promotionPieces != null && promotionPieces.length != 0) ? (promotionPieces[0] == null ? "" : promotionPieces[0].getType().getNamedNotation().toLowerCase()) : "");
    }

    public String deepToString(GameManager manager, PieceWrapper[][] board) {
        PieceWrapper[][] result = manager.getResultBoard(board, this);
        StringBuilder builder = new StringBuilder();
        boolean isCastle = (piece instanceof King && Math.abs(to.column() - from.column()) == 2);
        boolean isEnPassant = (piece instanceof Pawn && manager.getEnPassant() != null && manager.getEnPassant().equals(to));
        boolean capture = (board[to.row()][to.column()] != null);
        boolean isPromotion = (piece instanceof Pawn && (to.row() == 7 || to.row() == 0));
        String abbreviation = piece.getType().getNamedNotation();


        if (isCastle) {
            if (to.column() > from.column()) {
                builder.append("O-O-O");
            } else {
                builder.append("O-O");
            }
        }

        if (!isCastle) builder.append(abbreviation);
        GameManager.AmbiguityLevel level = manager.isTileAmbiguous(board, to, piece.getType(), piece.isWhite());
        if (level != null && !(piece instanceof Pawn)) {
            switch (level) {
                case A1 -> builder.append(from.getColumnLetter());
                case A2 -> builder.append(from.toString());
            }
        }
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

        if (isPromotion) {
            builder.append("=");
            builder.append(promotionPieces[0].getType().getNamedNotation());
        }

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
