package me.vlink102.personal.internal;

import chariot.Client;
import chariot.model.TablebaseResult;
import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.PieceWrapper;
import me.vlink102.personal.game.SimpleMove;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;

/**
 * 8/5P2/8/8/p5r1/1p6/3R4/k1K5 w - - 0 1
 * 4k3/8/8/8/8/8/8/4KBN1 w - - 0 1
 * 8/8/7B/K7/8/7p/4Bkp1/6Rb w - - 0 1
 * 5q2/n2P1k2/2b5/8/8/3N4/4BK2/6Q1 w - - 0 1
 * 8/1p6/1P1p4/1K1p2B1/P2P4/6pp/1P6/5k2 w - - 0 1
 * 8/8/5k2/8/p7/8/1PK5/8 w - - 0 1
 * 5Q2/8/8/8/p7/1p6/6r1/k1K2R2 w - - 0 1
 *
 * 4k2r/8/8/7P/7P/6KP/7P/7R w k - 0 1
 * 7k/r6P/6K1/7R/8/8/P7/8 w - - 0 1
 * r2qk3/8/8/8/8/8/8/3QK2R w Kq - 0 1
 * 8/pQp2p1k/7p/6pK/6P1/6P1/8/5q2 b - - 0 1
 * 1q6/p1p2Q2/8/3p4/4p3/3qk3/8/B5K1 w - - 0 1
 * 6k1/P3Q3/6K1/8/8/8/5pq1/7q w - - 0 1
 * 1k6/8/pp6/6B1/3P4/2P5/r3r2P/1KR4R b - - 0 1
 * 8/8/1rk5/KR6/8/8/1P6/8 w - - 0 1
 *
 * 8/8/8/8/2N5/6p1/k1K3N1/8 w - - 0 1
 * 8/8/8/2kpp3/8/8/1K1NN3/8 w - - 0 1
 *
 * 6k1/8/1r2n3/2b5/K7/8/8/1N5Q w - - 0 1
 * 8/3r4/8/6n1/3K1k2/1b6/7N/7Q w - - 0 1
 *
 * rr4k1/p2b2p1/2p4p/3pRp2/2pP4/2P2PqN/PPQ3P1/2K1R3 w - - 0 25
 *
 * 8/8/8/8/6k1/6P1/r4PK1/1R6 w - - 0 1
 *
 * 5qk1/6p1/6P1/8/PP6/KP6/8/QRRRRRRR w - - 0 1
 * 1N6/1RK5/5n2/8/8/8/5n2/6k1 w - - 0 1
 *
 * 8/8/pp4p1/k1p3Pb/p1RB2pK/PpP5/8/8 w - - 0 1
 *
 * 8/4N3/8/8/3pN3/1p6/p2R4/k5K1 w - - 0 1
 *
 * 5k1B/2q2P2/1p1p4/pPpPpN1p/P1P1P1p1/3B2P1/8/4K3 w - - 0 1
 * nqrb4/p1p1p3/KpP1P1P1/1P6/6k1/8/8/8 w - - 0 1
 *
 * R7/8/8/8/7q/2K1B2p/7P/2Bk4 w - - 0 1
 */
public class SyzygyTableBases {
    private final GameManager manager;
    private final Client client;
    public SyzygyTableBases(GameManager manager) throws IOException {
        this.manager = manager;
        this.client = Client.auth("lip_DIhiyPV8deCMIaktZX8E");
    }

    public Client getClient() {
        return client;
    }

    /**
     * {@link StockFish#getBestMove(String)}
     */
    public void computerMove(String FEN) throws InterruptedException, InvocationTargetException {

        /*
        JSONObject result;
        try {
            result = getData(FEN);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (result == null) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        computerMove(board, FEN);
                    } catch (InterruptedException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 3600);
        } else {
            */
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                TablebaseResult result = client.tablebase().standard(FEN).get();
                TablebaseResult.Move moveResult = result.moves().get(0);
                SimpleMove move = SimpleMove.parseStockFishMove(manager.getInternalBoard(), moveResult.uci());
                String parsedMoveString = move.deepToString(manager, manager.getInternalBoard());
                manager.movePiece(manager.getInternalBoard(), move);
                String currentFEN = manager.generateCurrentFEN();

                //Float eval = Main.stockFish.getEvaluation(currentFEN);
                //printTableBaseInfo(result);
                EventQueue.invokeLater(() -> {
                    manager.refreshBoard(ChessBoard.WHITE_VIEW);
                    manager.getBoard().getEvalBoard().updateEvalDTZDTM( result.dtz(), result.dtm());

                    manager.history.add(currentFEN);
                    manager.uciHistory.add(move.toUCI());
                    manager.getBoard().getEvalBoard().addHistory(parsedMoveString, currentFEN);
                    manager.getBoard().getContentPane().paintComponents(manager.getBoard().getContentPane().getGraphics());
                    Main.evaluation.moveMade(currentFEN);
                    manager.recursiveMoves();
                });
            }
        });

        /*}*/
    }

    public void printTableBaseInfo(TablebaseResult result) {
        TablebaseResult.Move move = result.moves().get(0);
        System.out.println("==============================");
        System.out.println(" |  DTZ: " + result.dtz());
        System.out.println(" |  DTZ_PRECISE: " + result.precise_dtz());
        System.out.println(" |  DEPTH_TO_MATE: " + result.dtm());
        System.out.println(" |  CHECKMATE: " + result.checkmate());
        System.out.println(" |  STALEMATE: " + result.stalemate());
        System.out.println(" |  INSUFFICIENT_MATERIAL: " + result.insufficient_material());
        System.out.println(" |  CATEGORY: " + result.category());
        System.out.println(" |  --------------------");
        System.out.println(" >   |  UCI: " + move.uci());
        System.out.println(" >   |  SAN: " + move.san());
        System.out.println(" >   |  DTZ: " + move.dtz());
        System.out.println(" >   |  DTZ_PRECISE: " + move.precise_dtz());
        System.out.println(" >   |  DEPTH_TO_MATE: " + move.dtm());
        System.out.println(" >   |  ZEROING: " + move.zeroing());
        System.out.println(" >   |  CHECKMATE: " + move.checkmate());
        System.out.println(" >   |  STALEMATE: " + move.stalemate());
        System.out.println(" >   |  INSUFFICIENT_MATERIAL: " + move.insufficient_material());
        System.out.println(" >   |  CATEGORY: " + move.category());
        System.out.println(" |  --------------------");
        System.out.println("==============================");
    }

    /*
    public void printTableBaseInfo(JSONObject object) {
        System.out.println("--------------------");
        System.out.println("DTZ: " + object.getInt("dtz"));
        System.out.println("DTZ_PRECISE: " + object.getInt("precise_dtz"));
        System.out.println("DEPTH_TO_MATE: " + object.getInt("dtm"));
        moveStats(object);
        JSONObject move = object.getJSONArray("moves").getJSONObject(0);
        System.out.println("UCI: " + move.getString("uci"));
        System.out.println("SAN: " + move.getString("san"));
        System.out.println("DTZ: " + move.getInt("dtz"));
        System.out.println("DTZ_PRECISE: " + move.getInt("precise_dtz"));
        System.out.println("DEPTH_TO_MATE: " + move.getInt("dtm"));
        System.out.println("ZEROING: " + move.getBoolean("zeroing"));
        moveStats(move);
    }

    private void moveStats(JSONObject object) {
        System.out.println("CHECKMATE: " + object.getBoolean("checkmate"));
        System.out.println("STALEMATE: " + object.getBoolean("stalemate"));
        System.out.println("INSUFFICIENT_MATERIAL: " + object.getBoolean("insufficient_material"));
        System.out.println("CATEGORY: " + object.getString("category"));
        System.out.println("--------------------");
    }

     */
    /*
    public JSONObject getData(String FEN) throws IOException {
        String reformattedFEN = FEN.replaceAll(" ", "_");
        URL url = new URL("http://tablebase.lichess.ovh/standard?fen=" + reformattedFEN);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        int status = connection.getResponseCode();
        if (status == 429) {
            return null;
        } else {
            System.out.println("Status given=" + status);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();
            return new JSONObject(content.toString());
        }
    }

     */
}
