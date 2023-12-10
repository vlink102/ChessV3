package me.vlink102.personal;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import me.vlink102.personal.internal.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Main {
    public static FileUtils fileUtils;
    public static StockFish stockFish = null;
    public static Evaluation evaluation = null;
    public static SyzygyTableBases syzygyTableBases = null;
    public static MoveType SELF = MoveType.PLAYER;
    public static MoveType OPPONENT = MoveType.COMPUTER;

    public static int CPU_CORES = getNumberOfCPUCores();
    public static int PRINCIPAL_VARIATION = 1;

    public static boolean WHITE_TO_MOVE = true;
    public static int ENGINE_THINKING_TIME = 3000;

    public static void main(String[] args) throws IOException {
        LafManager.install(new DarculaTheme());
        /*
        Console console = System.console();
        if (console == null && !GraphicsEnvironment.isHeadless()) {
            String fileName = Main.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            Runtime.getRuntime().exec(new String[]{"cmd","/c","start","cmd","/k","java -jar \"" + fileName + "\""});
        } else {
            Main.main(new String[0]);
            System.out.println("Program has ended, please type 'exit' to close the console");
        }
         */
        int boardSize = 600;
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        boolean openConsole = true;
        boolean openErr = true;
        if (args.length != 0) {
            for (String arg : args) {
                if (!arg.startsWith("-")) continue;
                if (arg.startsWith("--noerr")) {
                    openErr = false;
                }
                if (arg.startsWith("--nogui")) {
                    openConsole = false;
                }
                if (arg.startsWith("-Board.Size=")) {
                    boardSize = Integer.parseInt(arg.split("=")[1]);
                }
                if (arg.startsWith("-Engine.Cores=")) {
                    CPU_CORES = Integer.parseInt(arg.split("=")[1]);
                }
                if (arg.startsWith("-Engine.PV=")) {
                    PRINCIPAL_VARIATION = Integer.parseInt(arg.split("=")[1]);
                }
                if (arg.startsWith("-FEN=")) {
                    fen = arg.split("=")[1];
                }
                if (arg.startsWith("-PlayAs=")) {
                    WHITE_TO_MOVE = Boolean.parseBoolean(arg.split("=")[1]);
                }
                if (arg.startsWith("-Engine.Time=")) {
                    ENGINE_THINKING_TIME = Integer.parseInt(arg.split("=")[1]);
                }
                if (arg.startsWith("-Game.Opponent=")) {
                    OPPONENT = MoveType.valueOf(arg.split("=")[1]);
                }
                if (arg.startsWith("-Game.Self=")) {
                    SELF = MoveType.valueOf(arg.split("=")[1]);
                }
            }
        }
        if (openErr) {
            new ConsoleGUI(true);
        }
        if (openConsole) {
            new ConsoleGUI(false);
        }
        int finalBoardSize = boardSize;
        String finalFen = fen;
        SwingUtilities.invokeLater(() -> {
            fileUtils = new FileUtils(finalBoardSize / 8);
            ChessBoard board = new ChessBoard(finalBoardSize, finalFen);
            if (OPPONENT.equals(MoveType.COMPUTER)) {
                try {
                    syzygyTableBases = new SyzygyTableBases(board.getManager());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                stockFish = new StockFish(board.getManager());
                evaluation = new Evaluation(board.getManager());

                if (stockFish.startEngine()) {
                    System.out.println("Engine started");

                    stockFish.sendCommand("uci");
                    System.out.println(stockFish.getOutput(0));
                    stockFish.sendCommand("setoption name Threads value " + Main.CPU_CORES);
                    stockFish.sendCommand("setoption name MultiPV value " + Main.PRINCIPAL_VARIATION);
                    stockFish.sendCommand("setoption name SyzygyPath value ");
                } else {
                    System.out.println("Something went wrong...");
                    return;
                }
                if (evaluation.startEngine()) {
                    System.out.println("Evaluation engine started");

                    evaluation.sendCommand("uci");
                    evaluation.getOutput(0);
                    evaluation.sendCommand("setoption name SyzygyPath value ");
                } else {
                    System.out.println("Something went wrong...");
                }
            }
        });

    }

    private static int getNumberOfCPUCores() {
        String command = "";
        if (OSValidator.isMac()) {
            command = "sysctl -n machdep.cpu.core_count";
        } else if (OSValidator.isUnix()) {
            command = "lscpu";
        } else if (OSValidator.isWindows()) {
            command = "cmd /C WMIC CPU Get /Format:List";
        }
        Process process = null;
        int numberOfCores = 0;
        int sockets = 0;
        try {
            if (OSValidator.isMac()) {
                String[] cmd = {"/bin/sh", "-c", command};
                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert process != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                if (OSValidator.isMac()) {
                    numberOfCores = line.length() > 0 ? Integer.parseInt(line) : 0;
                } else if (OSValidator.isUnix()) {
                    if (line.contains("Core(s) per socket:")) {
                        numberOfCores = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                    if (line.contains("Socket(s):")) {
                        sockets = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                } else if (OSValidator.isWindows()) {
                    if (line.contains("NumberOfCores")) {
                        numberOfCores = Integer.parseInt(line.split("=")[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (OSValidator.isUnix()) {
            return numberOfCores * sockets;
        }
        return numberOfCores;
    }

    public enum MoveType {
        COMPUTER,
        RANDOM,
        PLAYER
    }

    public static class ConsoleGUI {
        public ConsoleGUI(boolean err) {
            ScrollingTextGUI scrollingTextGui;
            if (err) {
                scrollingTextGui = new ScrollingTextGUI("Console Errors");
                System.setErr(new PrintStream(new RedirectingOutputStream(scrollingTextGui), true));
            } else {
                scrollingTextGui = new ScrollingTextGUI("Console Output");
                System.setOut(new PrintStream(new RedirectingOutputStream(scrollingTextGui), true));
            }
            scrollingTextGui.start();
        }
    }

    public static class ScrollingTextGUI {
        private final JTextArea textArea;
        private final JFrame frame;
        private final JScrollPane pane;

        public ScrollingTextGUI(String title) {
            frame = new JFrame(title);
            frame.setBounds(0, 0, 600, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            textArea = new JTextArea();

            pane = new JScrollPane();
            pane.setViewportView(textArea);
            pane.setPreferredSize(new Dimension(600, 400));
            pane.getVerticalScrollBar().setBlockIncrement(20);
            frame.getContentPane().add(pane, BorderLayout.CENTER);
        }

        public void start() {
            frame.setVisible(true);
        }

        public void appendText(String text) {
            textArea.append(text);
            frame.validate();
            JScrollBar bar = pane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        }
    }

    public static class RedirectingOutputStream extends OutputStream {
        private final ScrollingTextGUI scrollingTextGui;

        public RedirectingOutputStream(ScrollingTextGUI scrollingTextGui) {
            this.scrollingTextGui = scrollingTextGui;
        }

        @Override
        public void write(int b) throws IOException {
            scrollingTextGui.appendText(String.valueOf((char) b));
        }
    }

    public static class OSValidator {
        private static final String OS = System.getProperty("os.name").toLowerCase();

        public static boolean isWindows() {
            return (OS.contains("win"));
        }

        public static boolean isMac() {
            return (OS.contains("mac"));
        }

        public static boolean isUnix() {
            return (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
        }
    }
}