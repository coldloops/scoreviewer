package coldloops.scoreviewer;

import net.coderazzi.filters.gui.AutoChoices;
import net.coderazzi.filters.gui.TableFilterHeader;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.util.List;

public class Interface {
    private static final String VERSION = "V. 1.2  ";
    private static final String OSU_DB = "osu!.db";
    private static final String SCORES_DB = "scores.db";
    private JButton btnOpen;
    private JTable table;
    private JPanel mainPanel;
    private JLabel lblVersion;
    private File osuDB = null;
    private File scoresDB = null;
    private final ScoreTableModel stm;

    private FileWatcher fw = null;

    public Interface() {
        lblVersion.setText(VERSION);
        lblVersion.setFont(new Font("SansSerif", Font.ITALIC, 12));
        Font f = new Font("SansSerif", Font.PLAIN, 16);
        table.setAutoCreateRowSorter(true);
        table.setFont(f);
        table.getTableHeader().setFont(f);
        stm = new ScoreTableModel();
        table.setModel(stm);
        TableFilterHeader filterHeader = new TableFilterHeader(table, AutoChoices.ENUMS);
        filterHeader.setFilterOnUpdates(true);
        TableColumnManager tcm = new TableColumnManager(table);
        for(String col : ScoreTableModel.hiddenColumns) {
            tcm.hideColumn(col);
        }
        btnOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select Osu directory:");
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int r = fc.showOpenDialog(mainPanel);
                if (r == JFileChooser.APPROVE_OPTION) {
                    File dir = fc.getSelectedFile();
                    osuDB = new File(dir, OSU_DB);
                    scoresDB = new File(dir, SCORES_DB);
                    if (osuDB.exists() && scoresDB.exists()) {
                        if (fw != null) fw.cancel(true);
                        fw = new FileWatcher(dir);
                        reloadData();
                        fw.execute();
                    } else {
                        JOptionPane.showMessageDialog(mainPanel, "Could not find db files.");
                    }
                }
            }
        });
    }

    private void reloadData() {
        if (osuDB.exists() && scoresDB.exists()) {
            stm.readScoreTable(osuDB, scoresDB);
        }
    }

    private static void setLAF() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void createUIComponents() {
        table = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return stm.getColumnTooltip(realIndex);
                    }
                };
            }
        };
    }

    class FileWatcher extends SwingWorker<Boolean, Boolean> {
        private final WatchService watchService;

        FileWatcher(File target) {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                target.toPath().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            while (true) {
                System.err.println("wait for it...");
                WatchKey key = watchService.take();
                key.pollEvents();
                key.reset();

                // wait a bit to avoid reading before osu finishes writing
                Thread.sleep(5000);

                // exaust queue, to avoid refreshing many times in a roll
                while ((key = watchService.poll()) != null) {
                    key.pollEvents();
                    key.reset();
                }
                publish(true);
            }
        }

        @Override
        protected void process(List<Boolean> list) {
            System.err.println("refreshing data");
            reloadData();
        }
    }

    public static void showExceptionDialog(Throwable e) {
        final JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 10));
        textArea.setEditable(false);
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        textArea.setText(writer.toString());
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        JOptionPane.showMessageDialog(null, scrollPane, "An Error Has Occurred", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                showExceptionDialog(e);
            }
        });
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setLAF();
                JFrame frame = new JFrame();
                frame.setTitle("OsuMania Score Viewer");
                frame.setContentPane(new Interface().mainPanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
