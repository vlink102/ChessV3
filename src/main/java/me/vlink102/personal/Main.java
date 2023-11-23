package me.vlink102.personal;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;
import me.vlink102.personal.internal.StockFish;

import javax.swing.*;

public class Main {
    public static FileUtils fileUtils;
    public static StockFish stockFish = null;
    public static Opponent OPPONENT = Opponent.COMPUTER;

    public enum Opponent {
        COMPUTER,
        RANDOM,
        PLAYER
    }

    public static void main(String[] args) {
        LafManager.install(new DarculaTheme());
        SwingUtilities.invokeLater(() -> {
            fileUtils = new FileUtils(75);
            ChessBoard board = new ChessBoard(600);
            if (OPPONENT.equals(Opponent.COMPUTER)) {
                stockFish = new StockFish(board.getManager());

                if (stockFish.startEngine()) {
                    System.out.println("Engine started");

                    stockFish.sendCommand("uci");
                    System.out.println(stockFish.getOutput(0));
                } else {
                    System.out.println("Something went wrong...");
                }
            }
        });

    }
}