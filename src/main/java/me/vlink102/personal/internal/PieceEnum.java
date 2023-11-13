package me.vlink102.personal.internal;

public enum PieceEnum {
    WHITE_KING("wk"),
    BLACK_KING("bk"),
    WHITE_QUEEN("wq"),
    BLACK_QUEEN("bq"),
    WHITE_ROOK("wr"),
    BLACK_ROOK("br"),
    WHITE_KNIGHT("wn"),
    BLACK_KNIGHT("bn"),
    WHITE_BISHOP("wb"),
    BLACK_BISHOP("bb"),
    WHITE_PAWN("wp"),
    BLACK_PAWN("bp");

    private final String abbr;

    PieceEnum(String abbr) {
        this.abbr = abbr;
    }

    public String getAbbr() {
        return abbr;
    }
}
