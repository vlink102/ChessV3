package me.vlink102.personal.internal;

import me.vlink102.personal.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class FileUtils {
    private static HashMap<PieceEnum, ImageIcon> PIECES = null;

    public static HashMap<PieceEnum, ImageIcon> getPieces() {
        return PIECES;
    }

    public FileUtils(int size) {
        PIECES = new HashMap<>();
        try {
            loadPieces(size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadPieces(int size) throws IOException {
        for (PieceEnum value : PieceEnum.values()) {
            PIECES.put(value, new ImageIcon(getImage(value.getAbbr()).getScaledInstance(size, size, Image.SCALE_SMOOTH)));
        }
    }

    public Image getImage(String name) throws IOException {
        return ImageIO.read(Objects.requireNonNull(Main.class.getResource("/" + name + ".png")));
    }
}
