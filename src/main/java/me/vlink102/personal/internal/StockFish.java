package me.vlink102.personal.internal;

import chariot.model.CloudEvalCacheEntry;
import me.vlink102.personal.Main;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.SimpleMove;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
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

            EventQueue.invokeLater(() -> {
                manager.recursiveMoves(manager.getInternalBoard());
            });
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

    public String getBestMoveOutput() {
        StringBuilder buffer = new StringBuilder();
        try {
            while (true) {
                String text = processReader.readLine();
                buffer.append(text).append("\n");
                if (text != null && text.startsWith("bestmove ")) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public String generateMoves() {
        StringJoiner joiner = new StringJoiner(" ");
        for (String s : manager.uciHistory) {
            joiner.add(s);
        }
        return (joiner.toString().equalsIgnoreCase("") ? "" : " " + joiner);
    }

    public void getBestMove(String fen) {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go depth 245 movetime 3000");
        CompletableFuture<String> move = CompletableFuture.supplyAsync(this::getBestMoveOutput);
        move.thenAccept((data) -> {
            float eval = 0.0f;
            int mateNo = 0;
            String[] dump = data.split("\n");
            for (int i = dump.length - 1; i >= 0; i--) {
                if (dump[i].startsWith("info depth ")) {
                    if (dump[i].contains("cp")) {
                        if (dump[i].split("score cp ")[1].split(" nodes")[0].matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                            eval = Float.parseFloat(dump[i].split("score cp ")[1].split(" nodes")[0]);
                        } else if (dump[i].split("score cp ")[1].contains("upperbound")){
                            if (dump[i].split("score cp ")[1].split(" upperbound nodes")[0].matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                                eval = Float.parseFloat(dump[i].split("score cp ")[1].split(" upperbound nodes")[0]);
                            } else {
                                System.out.println("SECOND ORDER=" + dump[i] + "\n     " + dump[i].split("score cp ")[1].split(" upperbound nodes")[0]);
                            }
                        }
                    } else if (dump[i].split("score ")[1].contains("mate")) {
                        if (dump[i].split("score mate ")[1].split(" nodes")[0].matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                            mateNo = Integer.parseInt(dump[i].split("score mate ")[1].split(" nodes")[0]);
                        } else {
                            System.out.println("THIRD ORDER=" + dump[i]);
                        }
                    }
                }
            }
            String newData = data.split("bestmove ")[1];
            if (newData.contains(" ")) {
                newData = newData.split(" ")[0];
            }
            SimpleMove parsedMove = SimpleMove.parseStockFishMove(manager.getInternalBoard(), newData);
            String parsedMoveString = parsedMove.deepToString(manager, manager.getInternalBoard());

            manager.movePiece(manager.getInternalBoard(), parsedMove);
            //drawBoard(manager.generateCurrentFEN());

            try {
                float finalEval = eval;
                int finalMateNo = mateNo;
                EventQueue.invokeAndWait(() -> {
                    Integer dtzam = finalMateNo == 0 ? null : finalMateNo;
                    manager.getBoard().getEvalBoard().updateEval(finalEval / 100f, dtzam, dtzam);
                    manager.refreshBoard(ChessBoard.WHITE_VIEW);
                    String currentFEN = manager.generateCurrentFEN();
                    manager.history.add(currentFEN);
                    manager.getBoard().getEvalBoard().addHistory(parsedMoveString, currentFEN);
                    manager.uciHistory.add(parsedMove.toUCI());
                    manager.getBoard().getContentPane().paintComponents(manager.getBoard().getContentPane().getGraphics());
                    if (!manager.endGame()) {
                        manager.recursiveMoves(manager.getInternalBoard());
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
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