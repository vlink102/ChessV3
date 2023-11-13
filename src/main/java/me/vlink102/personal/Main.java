package me.vlink102.personal;

import me.vlink102.personal.internal.ChessBoard;
import me.vlink102.personal.internal.FileUtils;

import javax.swing.*;

public class Main {
    public static FileUtils fileUtils;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            fileUtils = new FileUtils(75);
            new ChessBoard(600);
        });
    }
}