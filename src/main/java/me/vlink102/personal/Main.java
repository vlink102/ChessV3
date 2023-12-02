package me.vlink102.personal;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import me.vlink102.personal.internal.*;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static FileUtils fileUtils;
    public static StockFish stockFish = null;
    public static Evaluation evaluation = null;
    public static SyzygyTableBases syzygyTableBases = null;
    public static MoveType SELF = MoveType.PLAYER;
    public static MoveType OPPONENT = MoveType.COMPUTER;

    public static final int CPU_CORES = getNumberOfCPUCores();
    public static int PRINCIPAL_VARIATION = 1;

    public enum MoveType {
        COMPUTER,
        RANDOM,
        PLAYER
    }

    public static void main(String[] args) {
        LafManager.install(new DarculaTheme());
        SwingUtilities.invokeLater(() -> {
            fileUtils = new FileUtils(75);
            ChessBoard board = new ChessBoard(600);
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
                    stockFish.sendCommand("setoption name Threads value " + (Main.CPU_CORES - 2));
                    stockFish.sendCommand("setoption name MultiPV value " + Main.PRINCIPAL_VARIATION);
                    stockFish.sendCommand("setoption name SyzygyPath value ");
                } else {
                    System.out.println("Something went wrong...");
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

        public static boolean isSolaris() {
            return (OS.contains("sunos"));
        }

        @SuppressWarnings("unused")
        public static String getOS() {
            if (isWindows()) {
                return "win";
            } else if (isMac()) {
                return "osx";
            } else if (isUnix()) {
                return "uni";
            } else if (isSolaris()) {
                return "sol";
            } else {
                return "err";
            }
        }
    }
}