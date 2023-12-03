package me.vlink102.personal.internal;

import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.SimpleMove;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

public class StockFish {
    private Process engineProcess;
    private BufferedReader processReader;
    private OutputStreamWriter processWriter;

    private final GameManager manager;

    public StockFish(GameManager manager) {
        this.manager = manager;
    }

    private static final String PATH = "C:\\Users\\Ethan\\Desktop\\stockfish\\stockfish\\stockfish-windows-x86-64-avx2.exe";

    public static final String PREFIX = "stream2file";
    public static final String SUFFIX = ".tmp";

    public static File stream2file (InputStream in) throws IOException {
        final File tempFile = File.createTempFile(PREFIX, SUFFIX);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }
        return tempFile;
    }


    public boolean startEngine() {
        try {
            InputStream is = Main.class.getResourceAsStream("/stockfish/stockfish-windows-x86-64-avx2.exe");
            File file = stream2file(is);
            engineProcess = Runtime.getRuntime().exec(file.getAbsolutePath());
            processReader = new BufferedReader(new InputStreamReader(
                    engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(
                    engineProcess.getOutputStream());

            EventQueue.invokeLater(manager::recursiveMoves);
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause().toString().contains("error=13, Permission denied")) {
                System.out.println("Please use a Windows OS, this program will not work on MacOS");
                System.out.println("Stockfish evaluation disabled.");
                Main.OPPONENT = Main.MoveType.PLAYER;
                Main.SELF = Main.MoveType.PLAYER;
                Main.stockFish = null;
            }
            return false;
        }
        return true;
    }

    public void sendCommand(String command) {
        try {
            processWriter.write(command + "\n");
            processWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOutput(int waitTime) {
        StringBuilder buffer = new StringBuilder();
        try {
            Thread.sleep(waitTime);
            sendCommand("isready");
            while (true) {
                String text = processReader.readLine();
                if (text.equals("readyok"))
                    break;
                else
                    buffer.append(text).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public void getBestMoveOutput() {
        StringBuilder buffer = new StringBuilder();
        try {
            while (true) {
                String text = processReader.readLine();
                //System.out.println(text);
                buffer.append(text).append("\n");
                if (text != null && text.startsWith("bestmove ")) {
                    String data = buffer.toString();
                    String newData = data.split("bestmove ")[1];
                    if (newData.contains(" ")) {
                        newData = newData.split(" ")[0];
                    }
                    if (newData.equals("(none)")) return;
                    SimpleMove parsedMove = SimpleMove.parseStockFishMove(manager.getInternalBoard(), newData);
                    String parsedMoveString = parsedMove.deepToString(manager, manager.getInternalBoard());

                    manager.movePiece(manager.getInternalBoard(), parsedMove);
                    String currentFEN = manager.generateCurrentFEN();

                    //Float eval = getEvaluation(currentFEN);

                    try {
                        EventQueue.invokeAndWait(() -> {
                            manager.refreshBoard(ChessBoard.WHITE_VIEW);
                            manager.history.add(currentFEN);
                            manager.getBoard().getEvalBoard().addHistory(parsedMoveString, currentFEN);
                            manager.uciHistory.add(parsedMove.toUCI());
                            manager.getBoard().getContentPane().paintComponents(manager.getBoard().getContentPane().getGraphics());
                            Main.evaluation.moveMade(currentFEN);
                            manager.recursiveMoves();
                        });
                    } catch (InterruptedException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String generateMoves() {
        StringJoiner joiner = new StringJoiner(" ");
        for (String s : manager.uciHistory) {
            joiner.add(s);
        }
        return (joiner.toString().equalsIgnoreCase("") ? "" : " " + joiner);
    }

    enum MoveClassification {
        BOOK(-999999, -999999),
        BRILLIANT(-999999, -999999),
        GREAT(-999999, -999999),
        BEST(0.00f, 0.00f),
        EXCELLENT(0.00f, 0.02f),
        GOOD(0.02f, 0.05f),
        INACCURACY(0.05f, 0.1f),
        MISTAKE(0.1f, 0.2f),
        BLUNDER(0.2f, 1f);

        private final float lowerLimit;
        private final float upperLimit;

        MoveClassification(float lowerLimit, float upperLimit) {
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;
        }

        public float getUpperLimit() {
            return upperLimit;
        }

        public float getLowerLimit() {
            return lowerLimit;
        }
    }

    public MoveClassification getClassification(boolean white, double prevC, double nextC) {
        double diff = white ? nextC - prevC : prevC - nextC;
        if (diff > 0) {
            if (diff > 2f) {
                return MoveClassification.BRILLIANT;
            } else if (diff > 1f) {
                return MoveClassification.GREAT;
            } else {
                return MoveClassification.BEST;
            }
        }
        double absDiff = Math.abs(diff);
        for (MoveClassification value : MoveClassification.values()) {
            float lower = value.lowerLimit;
            float upper = value.upperLimit;
            if (absDiff >= lower && absDiff <= upper) {
                return value;
            }
        }
        if (absDiff > MoveClassification.BLUNDER.upperLimit) {
            return MoveClassification.BLUNDER;
        }
        throw new IllegalArgumentException("Move did not have a correct win chance difference " + diff);
    }

    public static double getWinningChances(Double advantage) {
        return (50 + 50 * (2 / (1 + Math.exp(-0.004 * advantage)) - 1)) / 100d;
    }

    public float getEvaluation(String fen) {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        getOutput(0);

        sendCommand("eval");
        String result = getOutput(0);
        result = result.split("Final evaluation")[1];
        result = result.split("\\(")[0];
        result = result.replaceAll(" ", "");
        result = result.replaceAll("\\+", "");
        return Float.parseFloat(result);
    }


    public void getBestMove(String fen) {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go depth 245 movetime " + ChessBoard.COMPUTER_WAIT_TIME_MS);
        CompletableFuture.runAsync(this::getBestMoveOutput);


        //move.thenAccept((data) -> {
            /*
            boolean isWhiteTurn = manager.getGamePlay().isWhiteToMove();
            EventQueue.invokeLater(() -> {
                float eval = 0.0f;
                int mateNo = 0;
                String[] engineDump = data.split("\n");
                for (int i = engineDump.length - 1; i >= 0; i--) {
                    String engineDumpLine = engineDump[i];
                    if (engineDumpLine.startsWith("info depth ")) {
                        if (engineDumpLine.contains("cp")) {
                            String scoreCp = engineDumpLine.split("score cp ")[1];
                            String truncatedScoreCp = scoreCp.split(" nodes")[0];
                            if (truncatedScoreCp.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                eval = Float.parseFloat(truncatedScoreCp);

                            } else if (scoreCp.contains("upperbound")) {
                                String upperboundNodesScore = scoreCp.split(" upperbound nodes")[0];
                                if (upperboundNodesScore.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                    eval = Float.parseFloat(upperboundNodesScore);

                                } else {
                                    System.out.println("SECOND ORDER=" + engineDumpLine + "\n     " + upperboundNodesScore);
                                }
                            }
                        } else {
                            if (engineDumpLine.contains("score")) {
                                if (engineDumpLine.split("score ")[1].contains("mate")) {
                                    String dumpMate = engineDumpLine.split(" score mate")[1].split(" nodes")[0];
                                    if (dumpMate.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                        mateNo = Integer.parseInt(dumpMate);

                                    } else {
                                        System.out.println("THIRD ORDER=" + engineDumpLine);
                                    }
                                }
                            }
                        }
                    }
                }
                float finalEval = eval;
                int finalMateNo = mateNo;

                System.out.println(manager.getBoard().getEvalBoard().getPositionalChances());

                System.out.println(getClassification(isWhiteTurn, manager.getBoard().getEvalBoard().getLastEvaluation(), getWinningChances((double) finalEval)));

                if (!isWhiteTurn) {
                    finalEval = -finalEval;
                }

                Integer dtzam = finalMateNo == 0 ? null : finalMateNo;
                manager.getBoard().getEvalBoard().updateEval(finalEval / 100f, dtzam, dtzam);
            });
             */


        //});
    }

    public void stopEngine() {
        try {
            sendCommand("quit");
            processReader.close();
            processWriter.close();
        } catch (IOException ignored) {
        }
    }

    public String getLegalMoves(String fen) {
        sendCommand("position fen " + fen);
        sendCommand("d");
        return getOutput(0).split("Legal moves: ")[1];
    }

    public void drawBoard(String fen) {
        sendCommand("position fen " + fen);
        sendCommand("d");

        String[] rows = getOutput(0).split("\n");

        for (int i = 1; i < 18; i++) {
            System.out.println(rows[i]);
        }
    }

}