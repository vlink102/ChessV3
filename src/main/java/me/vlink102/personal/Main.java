package me.vlink102.personal;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import me.vlink102.personal.internal.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URISyntaxException;
import java.util.StringJoiner;

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

    public static void generateBat() {
        JFrame frame = new JFrame();
        frame.setBounds(0, 0, 600, 400);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(0, 20, 0, 20));

        JCheckBox showErrScreen = new JCheckBox();
        showErrScreen.setSelected(true);
        JPanel errScreen = getCouplePanel(new JLabel("Display Error Screen"), showErrScreen, true);
        JCheckBox showConsoleScreen = new JCheckBox();
        showConsoleScreen.setSelected(true);
        JPanel consoleScreen = getCouplePanel(new JLabel("Display Console Screen"), showConsoleScreen, true);
        JSpinner boardSizeSpinner = new JSpinner(new SpinnerNumberModel(600, 400, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getHeight(), 50));
        JPanel boardSize = getCouplePanel(new JLabel("Board Size"), boardSizeSpinner, true);
        JSpinner engineCoresSpinner = new JSpinner(new SpinnerNumberModel(CPU_CORES, 1, Runtime.getRuntime().availableProcessors(), 1));
        JPanel engineCores = getCouplePanel(new JLabel("Engine Cores"), engineCoresSpinner, true);
        JSpinner multiPVSpinner = new JSpinner(new SpinnerNumberModel(PRINCIPAL_VARIATION, 1, 50, 1));
        JPanel multiPV = getCouplePanel(new JLabel("Principal Variation"), multiPVSpinner, true);

        JTextField fenField = new JTextField("XXXXXXXXXXXX");
        final Dimension fenFieldSize = fenField.getPreferredSize();
        fenField.setMinimumSize(fenFieldSize);
        fenField.setPreferredSize(fenFieldSize);
        fenField.setText("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        JPanel fen = getCouplePanel(new JLabel("FEN String"), fenField, true);

        JCheckBox playAsWhite = new JCheckBox();
        playAsWhite.setSelected(WHITE_TO_MOVE);
        JPanel play = getCouplePanel(new JLabel("Play as white?"), playAsWhite, true);

        JSpinner engineTimeSpinner = new JSpinner(new SpinnerNumberModel(ENGINE_THINKING_TIME, 0, 20000, 100));
        JPanel engineTime = getCouplePanel(new JLabel("Engine Thinking Time"), engineTimeSpinner, true);

        JComboBox<MoveType> opponentComboBox = new JComboBox<>(MoveType.values());
        opponentComboBox.setSelectedItem(OPPONENT);
        JPanel opponentPanel = getCouplePanel(new JLabel("Opponent Type"), opponentComboBox, true);

        JComboBox<MoveType> selfComboBox = new JComboBox<>(MoveType.values());
        selfComboBox.setSelectedItem(SELF);
        JPanel selfPanel = getCouplePanel(new JLabel("Self Type"), selfComboBox, true);


        JFileChooser jdk = new JFileChooser();
        jdk.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jdk.setDialogTitle("Choose JDK 'java.exe'");
        jdk.setApproveButtonText("Use JDK");
        jdk.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().equalsIgnoreCase("java.exe");
            }

            @Override
            public String getDescription() {
                return "java.exe files";
            }
        });

        panel.add(errScreen);
        panel.add(consoleScreen);
        panel.add(boardSize);
        panel.add(engineCores);
        panel.add(multiPV);
        panel.add(fen);
        panel.add(engineTime);
        panel.add(play);
        panel.add(opponentPanel);
        panel.add(selfPanel);
        panel.add(new JSeparator());

        JButton submit = new JButton();
        submit.setText("Generate");
        submit.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                ExtensionFileFilter filter = new ExtensionFileFilter(new LayoutFileFilter("Batch executable", ".bat", true));
                chooser.setFileFilter(filter);
                chooser.setDialogTitle("Save batch file");
                try {
                    chooser.setCurrentDirectory(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile());
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
                int status = chooser.showSaveDialog(frame);
                if (status == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();

                    try {
                        String fileName = selected.getCanonicalPath();
                        if (!fileName.endsWith(".bat")) {
                            selected = new File(fileName + ".bat");
                        }

                        //String batContents = "powershell -Command \"& \"{JAVA_EXE}\" -jar \"{CHESS_JAR}\" {JVM_ARGUMENTS}\"";
                        String batContents = "powershell -Command \"& '{JAVA_EXE}' -jar '{CHESS_JAR}' {JVM_ARGUMENTS}\"";
                        StringJoiner jvmArgs = new StringJoiner(" ");
                        if (!showErrScreen.isSelected()) jvmArgs.add("--noerr");
                        if (!showConsoleScreen.isSelected()) jvmArgs.add("--nogui");
                        jvmArgs.add("-Board.Size=" + boardSizeSpinner.getValue());
                        jvmArgs.add("-Engine.Cores=" + engineCoresSpinner.getValue());
                        jvmArgs.add("-Engine.PV=" + multiPVSpinner.getValue());
                        jvmArgs.add("'-FEN=" + fenField.getText() + "'");
                        jvmArgs.add("-PlayAs=" + playAsWhite.isSelected());
                        jvmArgs.add("-Engine.Time=" + engineTimeSpinner.getValue());
                        jvmArgs.add("-Game.Opponent=" + opponentComboBox.getSelectedItem());
                        jvmArgs.add("-Game.Self=" + selfComboBox.getSelectedItem());
                        batContents = batContents.replace("{JVM_ARGUMENTS}", jvmArgs.toString());
                        batContents = batContents.replace("{JAVA_EXE}", jdk.getSelectedFile().getAbsolutePath());
                        batContents = batContents.replace("{CHESS_JAR}", Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                        org.apache.commons.io.FileUtils.writeStringToFile(selected, batContents);
                    } catch (IOException | URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    } finally {
                        JOptionPane.showMessageDialog(frame, "Successfully saved .bat file", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }

                }
            }
        });

        JButton jdkButton = new JButton("Choose .exe");
        jdkButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = jdk.showOpenDialog(panel);

                submit.setEnabled(result == JFileChooser.APPROVE_OPTION);
            }
        });

        JPanel jdkPanel = getCouplePanel(new JLabel("Executable Environment"), jdkButton, true);

        panel.add(jdkPanel);

        panel.add(Box.createRigidArea(new Dimension(-1, 20)));
        panel.add(submit);

        frame.getContentPane().add(panel);

        frame.pack();
        frame.setVisible(true);
    }

    public static JPanel getCouplePanel(JComponent left, JComponent right, boolean align) {
        JPanel inner = new JPanel(new BorderLayout());
        if (align) {
            left.setAlignmentX(Component.LEFT_ALIGNMENT);
            right.setAlignmentX(Component.RIGHT_ALIGNMENT);
        }
        inner.add(left, BorderLayout.WEST);
        inner.add(right, BorderLayout.EAST);

        return inner;
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        LafManager.install(new DarculaTheme());
        generateBat();
        if (true)return;
        int boardSize = 600;
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        boolean openConsole = true;
        boolean openErr = true;
        if (args.length != 0) {
            for (String arg : args) {
                if (!arg.startsWith("-")) continue;
                if (arg.startsWith("--generate-bat")) {
                    generateBat();
                    return;
                }
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

    public static class ExtensionFileFilter extends FileFilter {

        protected LayoutFileFilter filter;

        protected String description;
        protected String[] extensions;

        public ExtensionFileFilter(LayoutFileFilter filter) {
            this(filter.getDescription(), filter.getExtension());
            this.filter = filter;
        }

        public ExtensionFileFilter(String description, String extension) {
            this(description, new String[]{extension});
        }

        public ExtensionFileFilter(String description, String[] extensions) {
            if ((description == null) || (description.equals(""))) {
                this.description = extensions[0] + " {" + extensions.length + "}";
            } else {
                this.description = description;
            }
            this.extensions = extensions.clone();
            toLower(this.extensions);
        }

        private void toLower(String[] extensions) {
            for (int i = 0, n = extensions.length; i < n; i++) {
                extensions[i] = extensions[i].toLowerCase();
            }
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            } else {
                String path = file.getAbsolutePath().toLowerCase();
                for (String extension : extensions) {
                    if (path.endsWith(extension)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return description;
        }

    }

    public static class LayoutFileFilter {

        boolean isDefault;

        String description;
        String extension;

        public LayoutFileFilter(String description, String extension,
                                boolean isDefault) {
            this.description = description;
            this.extension = extension;
            this.isDefault = isDefault;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public void setDefault(boolean isDefault) {
            this.isDefault = isDefault;
        }

        public String getDescription() {
            return description;
        }

        public String getExtension() {
            return extension;
        }

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