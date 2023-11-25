package me.vlink102.personal.game;

import me.vlink102.personal.Main;
import me.vlink102.personal.game.pieces.*;
import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;
import me.vlink102.personal.internal.PieceEnum;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class GameManager {
    private final ChessBoard board;
    private final Random random;
    private final List<String> history;
    private GamePlay gamePlay;
    private PieceWrapper[][] internalBoard;
    private Tile enPassant;
    private Tile enPassantPiece;

    public GameManager(ChessBoard board, String fen) {
        this.board = board;
        this.internalBoard = fromFEN(fen);
        this.gamePlay = gamePlayFromFEN(fen);
        this.enPassant = Tile.parseTile(fen.split(" ")[3]);
        this.enPassantPiece = null;
        this.history = new ArrayList<>();
        refreshBoard(ChessBoard.WHITE_VIEW);
        random = ThreadLocalRandom.current();
    }

    public GameManager(ChessBoard board) {
        this.board = board;
        this.internalBoard = new PieceWrapper[8][8];
        setupPieces();
        this.gamePlay = new GamePlay(this);
        this.enPassant = null;
        this.enPassantPiece = null;
        this.history = new ArrayList<>();
        refreshBoard(ChessBoard.WHITE_VIEW);
        random = ThreadLocalRandom.current();
    }

    public static String generateFENString(PieceWrapper[][] board, boolean whiteToMove, CastleState state, Tile enPassant, int fullMoveCount, int halfMoveCount) {
        StringBuilder builder = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            int empty = 0;
            for (int j = 7; j >= 0; j--) {
                PieceWrapper wrapper = board[i][j];
                if (wrapper == null) {
                    empty++;
                } else {
                    if (empty != 0) {
                        builder.append(empty);
                        empty = 0;
                    }
                    builder.append(wrapper.getType().getFENNotation());
                }
            }
            if (empty != 0) {
                builder.append(empty);
            }
            if (i != 0) builder.append("/");
        }

        builder.append(" ");
        builder.append(whiteToMove ? "w" : "b");
        builder.append(" ");
        String castleState = state.toString();
        builder.append(castleState);
        if (castleState.length() == 0) {
            builder.append("- ");
        } else {
            builder.append(" ");
        }
        if (enPassant == null) {
            builder.append("- ");
        } else {
            builder.append(enPassant).append(" ");
        }
        builder.append(halfMoveCount).append(" ");
        builder.append(fullMoveCount);

        return builder.toString();
    }

    public List<String> getHistory() {
        return history;
    }

    public void reset(String fen) {
        this.gamePlay = null;
        this.internalBoard = null;
        this.enPassantPiece = null;
        this.enPassant = null;
        this.history.clear();

        this.internalBoard = fromFEN(fen);
        this.gamePlay = gamePlayFromFEN(fen);
        this.enPassant = Tile.parseTile(fen.split(" ")[3]);
        this.enPassantPiece = null;
        this.board.getEvalBoard().clearHistory();
        refreshBoard(ChessBoard.WHITE_VIEW);
        EventQueue.invokeLater(() -> {
            computerMove(internalBoard);
            Container panel = board.getContentPane();
            panel.paintComponents(panel.getGraphics());
        });
    }

    public void reset() {
        this.internalBoard = new PieceWrapper[8][8];
        setupPieces();
        this.gamePlay = new GamePlay(this);
        this.enPassant = null;
        this.enPassantPiece = null;
        this.history.clear();
        this.board.getEvalBoard().clearHistory();
        refreshBoard(ChessBoard.WHITE_VIEW);
        EventQueue.invokeLater(() -> {
            computerMove(internalBoard);
            Container panel = board.getContentPane();
            panel.paintComponents(panel.getGraphics());
        });
    }

    public ChessBoard getBoard() {
        return board;
    }

    public PieceWrapper[][] getInternalBoard() {
        return internalBoard;
    }

    public PieceWrapper[][] fromFEN(String fen) {
        PieceWrapper[][] board = new PieceWrapper[8][8];
        String newFen = computedFEN(fen.split(" ")[0]);
        String[] rows = newFen.split("/");
        for (int i = 0; i < 8; i++) {
            String row = rows[i];
            System.out.println(row);
            for (int j = 0; j < 8; j++) {
                board[7 - i][7 - j] = fromString(new Tile(i, j), row.substring(j, j + 1), fen);
            }
        }
        return board;
    }

    public String computedFEN(String fen) {
        StringBuilder builder = new StringBuilder();
        for (String s : fen.split("")) {
            if (s.matches("\\d")) {
                builder.append("#".repeat(Integer.parseInt(s)));
            } else {
                builder.append(s);
            }
        }
        return builder.toString();
    }

    public PieceWrapper fromString(Tile tile, String piece, String fen) {
        String castleState = fen.split(" ")[2];
        boolean K = castleState.contains("K");
        boolean Q = castleState.contains("Q");
        boolean k = castleState.contains("k");
        boolean q = castleState.contains("q");
        return switch (piece) {
            case "R" -> {
                if (tile.equals(new Tile(7, 0))) {
                    yield new Rook(true, tile, PieceWrapper.RookSide.QUEENSIDE);
                }
                if (tile.equals(new Tile(7, 7))) {
                    yield new Rook(true, tile, PieceWrapper.RookSide.KINGSIDE);
                }
                yield new Rook(true, tile);
            }
            case "N" -> new Knight(true, tile);
            case "B" -> {
                if (tile.row + tile.column % 2 == 0) {
                    yield new Bishop(true, new Tile(7, 5));
                }
                yield new Bishop(true, new Tile(7, 2));
            }
            case "Q" -> new Queen(true, tile);
            case "K" -> {
                if (K && Q && tile.equals(new Tile(7, 3))) {
                    yield new King(true, tile);
                }
                yield new King(true, tile);
            }
            case "P" -> {
                Pawn p = new Pawn(true, new Tile(6, tile.column));
                if (tile.row != 6) {
                    p.incrementMoves();
                }
                yield p;
            }
            case "r" -> {
                if (tile.equals(new Tile(0, 0))) {
                    yield new Rook(false, tile, PieceWrapper.RookSide.QUEENSIDE);
                }
                if (tile.equals(new Tile(0, 7))) {
                    yield new Rook(false, tile, PieceWrapper.RookSide.KINGSIDE);
                }
                yield new Rook(false, tile);
            }
            case "n" -> new Knight(false, tile);
            case "b" -> {
                if (tile.row + tile.column % 2 == 0) {
                    yield new Bishop(false, new Tile(0, 2));
                }
                yield new Bishop(false, new Tile(0, 5));
            }
            case "q" -> new Queen(false, tile);
            case "k" -> {
                if (k && q && tile.equals(new Tile(0, 3))) {
                    yield new King(false, tile);
                }
                yield new King(false, tile);
            }
            case "p" -> {
                Pawn p = new Pawn(false, new Tile(1, tile.column));
                if (tile.row != 1) {
                    p.incrementMoves();
                }
                yield p;
            }
            default -> null;
        };
    }

    public GamePlay gamePlayFromFEN(String fen) {
        GamePlay play = new GamePlay(this);
        String castles = fen.split(" ")[2];
        play.setCastleWhiteKing(castles.contains("K"));
        play.setCastleBlackKing(castles.contains("k"));
        play.setCastleWhiteQueen(castles.contains("Q"));
        play.setCastleBlackQueen(castles.contains("q"));
        play.setWhiteToMove(fen.split(" ")[1].equalsIgnoreCase("w"));
        play.setHalfMoveCounter(Integer.parseInt(fen.split(" ")[4]));
        play.setFullMoveCounter(Integer.parseInt(fen.split(" ")[5]));
        return play;
    }

    public void playerMovePiece(Tile from, Tile to, PieceWrapper[][] board, PieceWrapper... promotionPiece) {
        SimpleMove move = new SimpleMove(from, to, board, promotionPiece);
        String moveString = move.deepToString(this, board);
        movePiece(board, move);
        refreshBoard(ChessBoard.WHITE_VIEW);
        String currentFEN = generateCurrentFEN();
        if (history.size() == 0 || !history.get(history.size() - 1).equalsIgnoreCase(currentFEN)) {
            history.add(currentFEN);
            this.board.getEvalBoard().addHistory(moveString, currentFEN);
        }
        if (!endGame()) {
            recursiveMoves(board);
        }
    }

    public void computerMove(PieceWrapper[][] board) {
        if (getPiecesOnBoard() <= 7) {
            // run through 14 terabyte syzygy table base
            try {
                Main.syzygyTableBases.computerMove(board, generateCurrentFEN());
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            Main.stockFish.getBestMove(generateFENString(board, gamePlay.isWhiteToMove(), new CastleState(gamePlay.isCastleWhiteKing(), gamePlay.isCastleBlackKing(), gamePlay.isCastleWhiteQueen(), gamePlay.isCastleBlackQueen()), enPassant, gamePlay.getFullMoveCounter(), gamePlay.getHalfMoveCounter()), (int) (ChessBoard.COMPUTER_WAIT_TIME * 1000));
        }
    }

    public int getPiecesOnBoard() {
        int c = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (internalBoard[i][j] != null) {
                    c++;
                }
            }
        }
        return c;
    }

    public void recursiveMoves(PieceWrapper[][] board) {
        if (gamePlay.isWhiteToMove()) {
            if (ChessBoard.WHITE_VIEW) {
                switch (Main.SELF) {
                    case COMPUTER -> computerMove(board);
                    case RANDOM -> randomMove(board);
                }
            } else {
                switch (Main.OPPONENT) {
                    case COMPUTER -> computerMove(board);
                    case RANDOM -> randomMove(board);
                }
            }
        } else {
            if (ChessBoard.WHITE_VIEW) {
                switch (Main.OPPONENT) {
                    case COMPUTER -> computerMove(board);
                    case RANDOM -> randomMove(board);
                }
            } else {
                switch (Main.SELF) {
                    case COMPUTER -> computerMove(board);
                    case RANDOM -> randomMove(board);
                }
            }
        }
    }


    public void randomMove(PieceWrapper[][] board) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SimpleMove move = getRandomMove(board, gamePlay.isWhiteToMove());
                assert move != null;
                String moveString = move.deepToString(GameManager.this, board);
                movePiece(board, move);
                try {
                    EventQueue.invokeAndWait(() -> {
                        refreshBoard(ChessBoard.WHITE_VIEW);
                        String currentFEN = generateCurrentFEN();
                        history.add(currentFEN);
                        GameManager.this.board.getEvalBoard().addHistory(moveString, currentFEN);
                        GameManager.this.board.getContentPane().paintComponents(GameManager.this.board.getContentPane().getGraphics());
                        if (!endGame()) {
                            recursiveMoves(board);
                        }
                    });
                } catch (InterruptedException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

            }
        }, 500);
    }

    public boolean endGame() {
        if (isCheckmated(internalBoard, gamePlay.isWhiteToMove())) {
            JOptionPane.showMessageDialog(null, (gamePlay.isWhiteToMove() ? "Black" : "White") + " wins by checkmate", "Game over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        if (isStalemate(internalBoard)) {
            JOptionPane.showMessageDialog(null, "Game drawn by stalemate", "Game over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        if (gamePlay.getFiftyMoveRule() >= 50) {
            JOptionPane.showMessageDialog(null, "Game draw by fifty-move rule", "Game over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        if (isThreeFoldRepetition()) {
            JOptionPane.showMessageDialog(null, "Game draw by three-fold repetition", "Game over", JOptionPane.INFORMATION_MESSAGE);
            return true;
        }
        return false;
    }

    public List<String> truncatedHistory() {
        List<String> truncated = new ArrayList<>();
        for (String s : history) {
            truncated.add(s.split(" ")[0]);
        }
        return truncated;
    }

    public boolean isThreeFoldRepetition() {
        List<String> truncatedHistory = truncatedHistory();
        for (String s : truncatedHistory) {
            if (Collections.frequency(truncatedHistory, s) >= 3) {
                return true;
            }
        }
        return false;
    }

    public boolean canLegallyMove(SimpleMove move) {
        return move.getPiece().isWhite() == gamePlay.isWhiteToMove();
    }

    public boolean canMove(SimpleMove move, PieceWrapper[][] board) {
        PieceWrapper target = board[move.getTo().row][move.getTo().column];
        PieceWrapper piece = move.getPiece();
        if (ChessBoard.IGNORE_RULES) {
            if (!(target instanceof King)) {
                return true;
            }
        }
        if (target == null) {
            if (!canMoveToEnPassant(move)) {
                CastleSide castleSide = canCastle(board, move);
                if (castleSide == null) {
                    return piece.canMove(move, false);
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else if (target.isWhite() != move.getPiece().isWhite()) {
            return piece.canMove(move, true);
        }
        return false;
    }

    public boolean canMoveToEnPassant(SimpleMove move) {
        if (enPassant != null) {
            return enPassant.equals(move.getTo()) && move.getPiece() instanceof Pawn pawn && pawn.canMove(move, true);
        }
        return false;
    }

    public @Nullable AmbiguityLevel isTileAmbiguous(PieceWrapper[][] board, Tile tile, PieceEnum pieceType, boolean white) {
        List<Tile> ambiguousTiles = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile iter = new Tile(i, j);
                if (iter.equals(tile)) continue;
                PieceWrapper piece = board[i][j];
                if (piece == null) continue;
                if (piece.isWhite() != white || piece.getType() != pieceType) continue;
                SimpleMove move = new SimpleMove(iter, tile, board);
                if (canMove(move, board) && notBlocked(board, iter, tile) && kingAvoidsCheck(board, move)) {
                    ambiguousTiles.add(iter);
                }
            }
        }
        for (int i = 0; i < ambiguousTiles.size(); i++) {
            for (Tile ambiguousTile : ambiguousTiles) {
                if (ambiguousTiles.get(i).equals(ambiguousTile)) continue;
                if (ambiguousTiles.get(i).column == ambiguousTile.column) {
                    return AmbiguityLevel.A2;
                }
            }
        }
        if (ambiguousTiles.size() > 1) {
            return AmbiguityLevel.A1;
        }

        return null;
    }

    public boolean canCastleThroughCheckedTiles(PieceWrapper[][] board, SimpleMove move) {
        if (ChessBoard.IGNORE_RULES) {
            return true;
        }
        Tile kingTile = getKing(board, move.getPiece().isWhite());
        PieceWrapper king = board[kingTile.row][kingTile.column];
        int startingCol = move.getFrom().column;
        int toCol = move.getTo().column;
        if (toCol > startingCol) {
            if (Math.abs(toCol - startingCol) == 3) {
                for (int i = 0; i < 3; i++) {
                    Tile affected = new Tile(kingTile.row, kingTile.column + (i + 1));
                    if (isTileInCheck(affected, !king.isWhite(), board)) {
                        return false;
                    }
                }
            }
        } else if (toCol < startingCol) {
            if (Math.abs(toCol - startingCol) == 2) {
                for (int i = 0; i < 2; i++) {
                    Tile affected = new Tile(kingTile.row, kingTile.column - (i + 1));
                    if (isTileInCheck(affected, !king.isWhite(), board)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void cleanUpCastleMove(PieceWrapper[][] board, SimpleMove move, boolean tempo) {
        if (move.isCapture(board) && !ChessBoard.IGNORE_RULES) return;
        if (move.getPiece() instanceof King king) {
            int startingCol = move.getFrom().column;
            int toCol = move.getTo().column;
            if (Math.abs(toCol - startingCol) == 2) {
                if (toCol > startingCol) {
                    Tile rook = getRook(board, PieceWrapper.RookSide.QUEENSIDE, king.isWhite());
                    if (rook == null) return;
                    PieceWrapper temp = board[rook.row][rook.column];
                    board[rook.row][rook.column] = null;
                    board[rook.row][rook.column - 3] = temp;
                    if (tempo) return;
                    if (king.isWhite()) {
                        gamePlay.setCastleWhiteQueen(false);
                    } else {
                        gamePlay.setCastleBlackQueen(false);
                    }
                } else if (toCol < startingCol) {
                    Tile rook = getRook(board, PieceWrapper.RookSide.KINGSIDE, king.isWhite());
                    if (rook == null) return;
                    PieceWrapper temp = board[rook.row][rook.column];
                    board[rook.row][rook.column] = null;
                    board[rook.row][rook.column + 2] = temp;
                    if (tempo) return;
                    if (king.isWhite()) {
                        gamePlay.setCastleWhiteKing(false);
                    } else {
                        gamePlay.setCastleBlackKing(false);
                    }
                }
            }

        }
    }

    public @Nullable Tile getRook(PieceWrapper[][] board, PieceWrapper.RookSide side, boolean white) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = new Tile(i, j);
                if (board[i][j] instanceof Rook rook) {
                    PieceWrapper.RookSide rookSide = rook.getSide();
                    if (rookSide == null) continue;
                    if (rookSide.equals(side) && rook.isWhite() == white) {
                        return tile;
                    }
                }
            }
        }
        return null;
    }

    public @Nullable CastleSide canCastle(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper piece = move.getPiece();
        if (piece instanceof King king && move.getTo().row == move.getFrom().row && Math.abs(move.getTo().column - move.getFrom().column) == 2) {
            if (isKingInCheck(board, king.isWhite())) return null;
            if (!canCastleThroughCheckedTiles(board, move)) return null;
            int startingCol = move.getFrom().column;
            int toCol = move.getTo().column;
            if (Math.abs(toCol - startingCol) == 2) {
                if (toCol > startingCol) {
                    if (getRook(board, PieceWrapper.RookSide.QUEENSIDE, king.isWhite()) == null) return null;
                    return (king.isWhite() ? gamePlay.isCastleWhiteQueen() : gamePlay.isCastleBlackQueen()) ? CastleSide.QUEENSIDE : null;
                } else if (toCol < startingCol) {
                    if (getRook(board, PieceWrapper.RookSide.KINGSIDE, king.isWhite()) == null) return null;
                    return (king.isWhite() ? gamePlay.isCastleWhiteKing() : gamePlay.isCastleBlackKing()) ? CastleSide.KINGSIDE : null;
                }
            }

        }
        return null;
    }

    public boolean notBlocked(PieceWrapper[][] board, Tile f, Tile t) {
        int yfrom = f.row;
        int xfrom = f.column;
        int yto = t.row;
        int xto = t.column;

        PieceWrapper from = board[yfrom][xfrom];
        PieceWrapper to = board[yto][xto];

        int dx = Integer.compare(xto, xfrom);
        int dy = Integer.compare(yto, yfrom);

        int steps = Math.max(Math.abs(xfrom - xto), Math.abs(yfrom - yto));

        if (xfrom == xto || yfrom == yto || Math.abs(xfrom - xto) == Math.abs(yfrom - yto)) {
            for (int i = 1; i < steps; i++) {
                int x = xfrom + i * dx;
                int y = yfrom + i * dy;
                if ((board[y][x] != null && board[y][x] != to && board[y][x] != from)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isTileInCheck(Tile tile, boolean whiteToCheck, PieceWrapper[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper wrapper = board[i][j];
                if (wrapper == null) continue;
                if (ChessBoard.IGNORE_RULES && !(wrapper instanceof King)) return false;
                if (wrapper.isWhite() != whiteToCheck) continue;
                Tile from = new Tile(i, j);
                if (from.equals(tile)) continue;
                if (canMove(new SimpleMove(from, tile, board), board) && notBlocked(board, from, tile)) {
                    return true;
                }
            }
        }
        return false;
    }

    public PieceWrapper[][] getResultBoard(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper[][] tempBoard = Arrays.stream(board).map(PieceWrapper[]::clone).toArray(PieceWrapper[][]::new);
        tempBoard = movePieceInternal(tempBoard, move);
        return tempBoard;
    }

    public Tile getKing(PieceWrapper[][] board, boolean white) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper wrapper = board[i][j];
                if (wrapper == null) continue;
                if (wrapper.isWhite() != white) continue;
                if (wrapper instanceof King) return new Tile(i, j);
            }
        }
        throw new RuntimeException("King not found");
    }

    public boolean isKingInCheck(PieceWrapper[][] board, boolean white) {
        return isTileInCheck(getKing(board, white), !white, board);
    }

    public boolean kingAvoidsCheck(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper[][] newBoard = getResultBoard(board, move);
        return !isKingInCheck(newBoard, move.getPiece().isWhite());
    }

    public void cleanUpEnPassantCapture(PieceWrapper[][] board, SimpleMove move) {
        cleanUpEnPassantCapture(board, move, enPassant, enPassantPiece);
    }

    public void cleanUpEnPassantCapture(PieceWrapper[][] board, SimpleMove move, Tile ep, Tile epP) {
        if (move.isCapture(board)) return;
        if (move.getPiece() instanceof Pawn) {
            if (move.getTo().equals(ep) && canMove(move, board)) {
                board[epP.row][epP.column] = null;
            }
        }
    }

    public void movePiece(PieceWrapper[][] board, SimpleMove move) {
        if (move.getPiece() == null) return;
        if (!canLegallyMove(move)) return;
        if (canMove(move, board) && notBlocked(board, move.getFrom(), move.getTo()) && kingAvoidsCheck(board, move)) {
            movePieceInternal(board, move);
            generateEnPassantTile(move);
            move.getPiece().incrementMoves();
            gamePlay.movePiece(move, board);
        }
    }

    public void cleanUpPawnPromotion(PieceWrapper[][] board, SimpleMove move) {
        PieceWrapper piece = move.getPiece();
        Tile to = move.getTo();
        if (piece instanceof Pawn && (to.row == 7 || to.row == 0) && move.getPromotionPiece() != null) {
            board[to.row][to.column] = move.getPromotionPiece();
        }
    }

    public PieceWrapper[][] movePieceInternal(PieceWrapper[][] board, SimpleMove move) {
        cleanUpCastleMove(board, move, true);
        cleanUpEnPassantCapture(board, move);

        Tile from = move.getFrom();
        Tile to = move.getTo();
        if (board[from.row][from.column] != null) {
            if (board[from.row][from.column].equals(move.getPiece())) {
                PieceWrapper temp = board[from.row][from.column];
                board[from.row][from.column] = null;
                board[to.row][to.column] = temp;
            }
        }

        cleanUpPawnPromotion(board, move);
        return board;
    }

    public void setupPieces() {
        setInternalPiece(new King(true, new Tile(0, 3)), new Tile(0, 3));
        setInternalPiece(new Queen(true, new Tile(0, 4)), new Tile(0, 4));
        setInternalPiece(new Bishop(true, new Tile(0, 2)), new Tile(0, 2));
        setInternalPiece(new Bishop(true, new Tile(0, 5)), new Tile(0, 5));
        setInternalPiece(new Knight(true, new Tile(0, 1)), new Tile(0, 1));
        setInternalPiece(new Knight(true, new Tile(0, 6)), new Tile(0, 6));
        setInternalPiece(new Rook(true, new Tile(0, 0), PieceWrapper.RookSide.KINGSIDE), new Tile(0, 0));
        setInternalPiece(new Rook(true, new Tile(0, 7), PieceWrapper.RookSide.QUEENSIDE), new Tile(0, 7));
        setInternalPiece(new King(false, new Tile(7, 3)), new Tile(7, 3));
        setInternalPiece(new Queen(false, new Tile(7, 4)), new Tile(7, 4));
        setInternalPiece(new Bishop(false, new Tile(7, 2)), new Tile(7, 2));
        setInternalPiece(new Bishop(false, new Tile(7, 5)), new Tile(7, 5));
        setInternalPiece(new Knight(false, new Tile(7, 1)), new Tile(7, 1));
        setInternalPiece(new Knight(false, new Tile(7, 6)), new Tile(7, 6));
        setInternalPiece(new Rook(false, new Tile(7, 0), PieceWrapper.RookSide.KINGSIDE), new Tile(7, 0));
        setInternalPiece(new Rook(false, new Tile(7, 7), PieceWrapper.RookSide.QUEENSIDE), new Tile(7, 7));

        for (int i = 0; i < 8; i++) {
            setInternalPiece(new Pawn(true, new Tile(1, i)), new Tile(1, i));
            setInternalPiece(new Pawn(false, new Tile(6, i)), new Tile(6, i));
        }
    }

    public void setInternalPiece(PieceWrapper piece, Tile tile) {
        internalBoard[tile.row][tile.column] = piece;
    }

    public void printBoardRaw(PieceWrapper[][] board) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper wrapper = board[i][j];
                System.out.print(wrapper == null ? " " : wrapper.getType().getFENNotation());
            }
            System.out.print("\n");
        }
    }

    public void refreshBoard(boolean viewFromWhite) {
        for (Component component : board.getChessBoard().getComponents()) {
            JPanel tilePanel = (JPanel) component;
            tilePanel.removeAll();
        }
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = new Tile(i, j);
                addPiece(viewFromWhite ? internalBoard[7 - tile.row][7 - tile.column] : internalBoard[tile.row][tile.column], tile);
            }
        }
    }

    public void addPiece(PieceWrapper piece, Tile tile) {
        ImageIcon imageIcon = FileUtils.getPieces().get((piece == null) ? null : piece.getType());
        JLabel pieceLabel = new JLabel(imageIcon);
        JPanel tilePanel = (JPanel) board.getChessBoard().getComponent(tile.getComponent());
        tilePanel.add(pieceLabel);
    }

    public List<SimpleMove> possibleMoves(PieceWrapper[][] board, boolean white) {
        List<SimpleMove> simpleMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                PieceWrapper pieceWrapper = board[i][j];
                if (pieceWrapper == null) continue;
                if (pieceWrapper.isWhite() != white) continue;
                List<SimpleMove> moves = possibleMoves(board, new Tile(i, j));
                if (moves == null) continue;
                simpleMoves.addAll(moves);
            }
        }
        return simpleMoves;
    }

    public @Nullable List<SimpleMove> possibleMoves(PieceWrapper[][] board, Tile tile) {
        if (board[tile.row][tile.column] == null) return null;
        return possibleMoves(board[tile.row][tile.column], tile, board);
    }

    public @Nullable List<SimpleMove> possibleMoves(PieceWrapper wrapper, Tile tile, PieceWrapper[][] board) {
        if (!board[tile.row][tile.column].equals(wrapper)) return null;

        List<SimpleMove> possibleMoves = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile to = new Tile(i, j);
                SimpleMove possibleMove = new SimpleMove(tile, to, board);
                if ((to.row == 7 || to.row == 0) && possibleMove.getPiece() instanceof Pawn) {
                    if (canMove(possibleMove, board) && notBlocked(board, tile, to) && kingAvoidsCheck(board, possibleMove)) {
                        possibleMoves.add(new SimpleMove(tile, to, board, new Queen(possibleMove.getPiece().isWhite(), to)));
                        possibleMoves.add(new SimpleMove(tile, to, board, new Rook(possibleMove.getPiece().isWhite(), to)));
                        possibleMoves.add(new SimpleMove(tile, to, board, new Bishop(possibleMove.getPiece().isWhite(), to)));
                        possibleMoves.add(new SimpleMove(tile, to, board, new Knight(possibleMove.getPiece().isWhite(), to)));
                    }
                } else if (canMove(possibleMove, board) && notBlocked(board, tile, to) && kingAvoidsCheck(board, possibleMove)) {
                    possibleMoves.add(possibleMove);
                }
            }
        }
        return possibleMoves;
    }


    public @Nullable SimpleMove getRandomMove(PieceWrapper[][] board, boolean white) {
        List<SimpleMove> possibleMoves = possibleMoves(board, white);
        return possibleMoves.get(random.nextInt(possibleMoves.size()));
    }

    public boolean isCheckmated(PieceWrapper[][] board, boolean white) {
        return possibleMoves(board, white).isEmpty() && isKingInCheck(board, white);
    }

    public boolean isStalemate(PieceWrapper[][] board) {
        return (possibleMoves(board, true).isEmpty() && !isKingInCheck(board, true) && gamePlay.isWhiteToMove()) || (possibleMoves(board, false).isEmpty() && !isKingInCheck(board, false) && !gamePlay.isWhiteToMove());
    }

    public Tile getEnPassant() {
        return enPassant;
    }

    public Tile getEnPassantPiece() {
        return enPassantPiece;
    }

    public void generateEnPassantTile(SimpleMove move) {
        PieceWrapper piece = move.getPiece();
        if (piece.getMoves() == 0 && piece instanceof Pawn) {
            if (move.getFrom().row - move.getTo().row == 2) {
                // black
                enPassant = new Tile(move.getTo().row + 1, move.getTo().column);
                enPassantPiece = move.getTo();
            } else if (move.getTo().row - move.getFrom().row == 2) {
                // white
                enPassant = new Tile(move.getTo().row - 1, move.getTo().column);
                enPassantPiece = move.getTo();
            }
        } else {
            enPassantPiece = null;
            enPassant = null;
        }
    }

    public String generateCurrentFEN() {
        return generateFENString(internalBoard, gamePlay.isWhiteToMove(), new CastleState(gamePlay.isCastleWhiteKing(), gamePlay.isCastleBlackKing(), gamePlay.isCastleWhiteQueen(), gamePlay.isCastleBlackQueen()), enPassant, gamePlay.getFullMoveCounter(), gamePlay.getHalfMoveCounter());
    }

    public enum CastleSide {
        KINGSIDE,
        QUEENSIDE
    }

    public enum AmbiguityLevel {
        A1,
        A2
    }

    public static class Tile {
        private final int row;
        private final int column;

        public Tile(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public Tile(boolean white, int x, int y, int pieceSize) {
            this.row = white ? 7 - (y / pieceSize) : (y / pieceSize);
            this.column = white ? 7 - (x / pieceSize) : (x / pieceSize);
        }

        public static Tile fromPoint(boolean white, Point point, int pieceSize) {
            return new Tile(white, point.x, point.y, pieceSize);
        }

        public static Tile parseTile(String tileString) {
            if (tileString.equals("-")) return null;
            int col = 'a' - (tileString.charAt(0) - 7);
            int row = Integer.parseInt(tileString.substring(1)) - 1;
            return new Tile(row, col);
        }

        public int column() {
            return column;
        }

        public int row() {
            return row;
        }

        public int getComponent() {
            return (row * 8) + column;
        }

        @Override
        public String toString() {
            return getColumnLetter() + getRowNumber();
        }

        public String getColumnLetter() {
            return String.valueOf(((char) ((7 - column) + 'a')));
        }

        public String getRowNumber() {
            return String.valueOf(row + 1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Tile tile = (Tile) o;
            return row == tile.row && column == tile.column;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, column);
        }
    }

    public static class CastleState {
        private final boolean K, k, Q, q;

        public CastleState(boolean K, boolean k, boolean Q, boolean q) {
            this.K = K;
            this.k = k;
            this.Q = Q;
            this.q = q;
        }

        public static CastleState getState(boolean K, boolean k, boolean Q, boolean q) {
            return new CastleState(K, k, Q, q);
        }

        public boolean whiteKing() {
            return K;
        }

        public boolean blackKing() {
            return k;
        }

        public boolean whiteQueen() {
            return Q;
        }

        public boolean blackQueen() {
            return q;
        }

        @Override
        public String toString() {
            return (K ? "K" : "") + (Q ? "Q" : "") + (k ? "k" : "") + (q ? "q" : "");
        }

        public CastleState getState() {
            return new CastleState(K, k, Q, q);
        }
    }
}
