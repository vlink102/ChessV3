package me.vlink102.personal.game;

import me.vlink102.personal.Main;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.IOException;


// TODO
public class PromotionPanel extends JPanel {
    public PromotionPanel(GameManager manager) {
        int pieceSize = manager.getBoard().getPieceSize();
        Dimension size = new Dimension(pieceSize * 2, pieceSize * 2);
        this.setLayout(new GridLayout(2, 2));
        this.setPreferredSize(size);
        this.setBounds(0, 0, size.width, size.height);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                JPanel tile = new JPanel(new BorderLayout());
                tile.setBackground(Color.LIGHT_GRAY);
                tile.setBorder(new LineBorder(Color.DARK_GRAY, 3));
                this.add(tile);
            }
        }
    }


    // TODO
    public void open() {
        JFrame frame = new JFrame();
        frame.getContentPane().add(this);
        frame.setTitle("Piece Promotion");
        try {
            frame.setIconImage(Main.fileUtils.getImage("wq"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);


    }
}
