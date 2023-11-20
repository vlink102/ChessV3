package me.vlink102.personal.internal;

import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.SimpleMove;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
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

    public boolean startEngine() {
        try {
            engineProcess = Runtime.getRuntime().exec(PATH);
            processReader = new BufferedReader(new InputStreamReader(
                    engineProcess.getInputStream()));
            processWriter = new OutputStreamWriter(
                    engineProcess.getOutputStream());
        } catch (Exception e) {
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
                if (text.startsWith("bestmove ")) {
                    buffer.append(text).append("\n");
                    break;
                } else {
                    buffer.append(text).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    public void getBestMove(String fen, int waitTime) {
        sendCommand("ucinewgame");
        sendCommand("position fen " + fen);
        sendCommand("go movetime " + waitTime);
        CompletableFuture<String> move = CompletableFuture.supplyAsync(this::getBestMoveOutput);
        move.thenAccept((data) -> {
            float eval = 0.0f;
            String[] dump = data.split("\n");
            for (int i = dump.length - 1; i >= 0; i--) {
                if (dump[i].startsWith("info depth ")) {
                    if (dump[i].split("score cp ")[1].split(" nodes")[0].matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                        eval = Float.parseFloat(dump[i].split("score cp ")[1].split(" nodes")[0]);
                    } else {
                        if (dump[i].split("score cp ")[1].split(" upperbound nodes")[0].matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                            eval = Float.parseFloat(dump[i].split("score cp ")[1].split(" upperbound nodes")[0]);
                        } else {
                            System.out.println(dump[i]);
                        }
                    }
                }
            }
            String newData = data.split("bestmove ")[1].split(" ")[0];
            SimpleMove parsedMove = SimpleMove.parseStockFishMove(manager.getInternalBoard(), newData);
            manager.movePiece(manager.getInternalBoard(), parsedMove);
            drawBoard(manager.generateCurrentFEN());

            try {
                float finalEval = eval;
                EventQueue.invokeAndWait(() -> {
                    manager.getBoard().getEvalBoard().setEval(finalEval / 100);
                    manager.refreshBoard(ChessBoard.WHITE);
                    manager.getBoard().getContentPane().paintComponents(manager.getBoard().getContentPane().getGraphics());
                    manager.endGame();
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
        } catch (IOException e) {
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