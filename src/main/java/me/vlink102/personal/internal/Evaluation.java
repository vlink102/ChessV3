package me.vlink102.personal.internal;

import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.util.concurrent.CompletableFuture;

public class Evaluation {
    public static final String PREFIX = "stream2file";
    public static final String SUFFIX = ".tmp";
    private final GameManager manager;
    private Process engineProcess;
    private BufferedReader processReader;
    private OutputStreamWriter processWriter;
    private boolean IS_RUNNING;

    public Evaluation(GameManager manager) {
        this.manager = manager;
        this.IS_RUNNING = true;
    }

    public static File stream2file(InputStream in) throws IOException {
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
            processReader = new BufferedReader(new InputStreamReader(engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(engineProcess.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause().toString().contains("error=13, Permission denied")) {
                Main.evaluation = null;
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

    public void getOutput(int waitTime) {
        try {
            Thread.sleep(waitTime);
            sendCommand("isready");
            while (true) {
                String text = processReader.readLine();
                if (text.equals("readyok")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getBestMoveOutput() {
        boolean whiteToMove = manager.getGamePlay().isWhiteToMove();
        String currentFEN = manager.generateCurrentFEN();
        try {
            float eval = 0.0f;
            int c = 0;
            StringBuilder builder = new StringBuilder();
            while (IS_RUNNING) {
                String textLine = processReader.readLine();
                if (c >= 10) {
                    String[] texts = builder.toString().split("\n");
                    for (String text : texts) {
                        if (text.startsWith("bestmove ")) return;

                        if (text.startsWith("info depth ")) {
                            int depth = Integer.parseInt(text.split(" ")[2]);
                            if (depth <= 15) continue;
                            if (text.contains("cp")) {
                                String scoreCp = text.split("score cp ")[1];
                                String truncatedScoreCp = scoreCp.split(" nodes")[0];
                                if (truncatedScoreCp.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                    eval = Float.parseFloat(truncatedScoreCp);
                                } else if (scoreCp.contains("upperbound")) {
                                    String upperboundNodesScore = scoreCp.split(" upperbound nodes")[0];
                                    if (upperboundNodesScore.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                        eval = Float.parseFloat(upperboundNodesScore);
                                    } else {
                                        System.out.println("SECOND ORDER=" + text + "\n     " + upperboundNodesScore);
                                    }
                                }
                            }/* else {
                        if (text.contains("score")) {
                            if (text.split("score ")[1].contains("mate")) {
                                String dumpMate = text.split("score mate ")[1].split(" nodes")[0];
                                if (dumpMate.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                    mateNo = Integer.parseInt(dumpMate);
                                } else {
                                    System.out.println("THIRD ORDER=" + text);
                                }
                            }
                        }

                    }*/

                        }
                    }
                    double finalEval;
                    if (whiteToMove) {
                        finalEval = eval;
                    } else {
                        finalEval = -eval;
                    }

                    manager.getBoard().getEvalBoard().updateEval(finalEval / 100, currentFEN);
                    builder = new StringBuilder();
                }
                builder.append(textLine).append("\n");
                c++;


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void moveMade(String fen) {
        sendCommand("stop");

        EventQueue.invokeLater(() -> {
            //System.out.println(StockFish.getWinningChances(manager.getBoard().getEvalBoard().getLastEvaluation());

            updateEvaluation(fen);
        });
    }


    public void updateEvaluation(String fen) {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go infinite");
        CompletableFuture.runAsync(this::getBestMoveOutput);
    }
}