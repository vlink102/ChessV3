package me.vlink102.personal.internal;

public enum PieceEnum {
    WHITE_KING("wk", "K"),
    BLACK_KING("bk", "K"),
    WHITE_QUEEN("wq", "Q"),
    BLACK_QUEEN("bq", "Q"),
    WHITE_ROOK("wr", "R"),
    BLACK_ROOK("br", "R"),
    WHITE_KNIGHT("wn", "N"),
    BLACK_KNIGHT("bn", "N"),
    WHITE_BISHOP("wb", "B"),
    BLACK_BISHOP("bb", "B"),
    WHITE_PAWN("wp", ""),
    BLACK_PAWN("bp", "");

    private final String abbr;
    private final String namedNotation;

    PieceEnum(String abbr, String namedNotation) {
        this.abbr = abbr;
        this.namedNotation = namedNotation;
    }

    public String getAbbr() {
        return abbr;
    }

    public String getNamedNotation() {
        return namedNotation;
    }

    public String getFENNotation() {
        if (this == WHITE_PAWN || this == BLACK_PAWN) {
            return (this == WHITE_PAWN ? "P" : "p");
        }
        if (this.toString().startsWith("WHITE")) {
            return getNamedNotation().toUpperCase();
        } else {
            return getNamedNotation().toLowerCase();
        }
    }
}
