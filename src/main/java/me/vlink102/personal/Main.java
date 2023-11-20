package me.vlink102.personal;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import me.vlink102.personal.game.GameManager;
import me.vlink102.personal.game.SimpleMove;
import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;
import me.vlink102.personal.internal.StockFish;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class Main {
    public static FileUtils fileUtils;
    public static StockFish stockFish = null;
    public static final boolean COMPUTER = true;

    public static void main(String[] args) {
        LafManager.install(new DarculaTheme());
        SwingUtilities.invokeLater(() -> {
            fileUtils = new FileUtils(75);
            ChessBoard board = new ChessBoard(600);
            if (COMPUTER) {
                stockFish = new StockFish(board.getManager());

                if (stockFish.startEngine()) {
                    System.out.println("Engine started");
                } else {
                    System.out.println("Something went wrong... Aborting program");
                    System.exit(0);
                }

                stockFish.sendCommand("uci");
                System.out.println(stockFish.getOutput(0));
            }
        });

    }
}