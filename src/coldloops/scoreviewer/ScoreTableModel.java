package coldloops.scoreviewer;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;

class ScoreTableModel extends AbstractTableModel {

    static class Col {
        final String name;
        final String tooltip;
        final Class<?> type;

        Col(String name, String tooltip, Class<?> type) {
            this.name = name;
            this.tooltip = tooltip;
            this.type = type;
        }
    }

    private final Col[] headers = new Col[]{
        new Col("artist", null, String.class),
        new Col("title", null, String.class),
        new Col("diff", null, String.class),
        new Col("stars", "star rating", Double.class),
        new Col("score", null, Double.class),
        new Col("ratio", "rainbow ratio / 300", Double.class),
        new Col("acc", "accuracy", Double.class),
        new Col("mods", null, String.class),
        new Col("status", "ranked status", OsuDB.RankStatus.class),
        new Col("date", "score date", Date.class),
        new Col("keys", null, Double.class),
        new Col("length", "total length (seconds)", Double.class),
        new Col("density", "average notes per second", Double.class),
        new Col("LN%", "long note percentage", Double.class),
        new Col("perfect", "perfect combo", Boolean.class),
        new Col("collections", null, List.class),
        new Col("MAX%", "max percentage / total", Double.class),
        new Col("judgments", "max,300,200,100,50,miss", ScoreDB.Judgment.class),
        new Col("cor", "score/time correlation", Double.class),
        new Col("N", "number of scores", Integer.class),
    };

    // columns hidden by default
    static final String[] hiddenColumns = new String[] {
        "perfect",
        "collections",
        "MAX%",
        "judgments",
    };

    private final ArrayList<Object[]> data = new ArrayList<>();

    void readScoreTable(File osuDB, File scoreDB, File collecDB) {
        OsuDB o = OsuDB.readOsuDB(readBufferFromFile(osuDB));
        ScoreDB s = ScoreDB.readScoreDB(readBufferFromFile(scoreDB));
        CollectionDB c = CollectionDB.readCollectionDB(readBufferFromFile(collecDB));
        readScoreTable(o, s, c);
    }

    private void readScoreTable(OsuDB o, ScoreDB s, CollectionDB c) {
        data.clear();
        for (Map.Entry<String, OsuDB.BeatmapInfo> e : o.beatmaps.entrySet()) {
            OsuDB.BeatmapInfo b = e.getValue();
            if (b.gameplay_mode != 3) continue; // only mania beatmaps
            ScoreDB.BeatmapScores bs = s.beatmaps.get(e.getKey());
            if (bs == null) continue;
            if (bs.scores.length == 0) continue;

            HashMap<Integer,List<ScoreDB.Score>> modsets = new HashMap<>();
            for(ScoreDB.Score si : bs.scores) {
                if(!modsets.containsKey(si.mods)) modsets.put(si.mods, new ArrayList<ScoreDB.Score>());
                modsets.get(si.mods).add(si);
            }
            for(List<ScoreDB.Score> sl : modsets.values()){
                addRow(b, sl, c);
            }
        }
        fireTableDataChanged();
    }

    private void addRow(OsuDB.BeatmapInfo b, List<ScoreDB.Score> sl, CollectionDB c) {

        // scores are ordered max->min
        // first is the highest
        ScoreDB.Score s = sl.get(0);
        Double cor = null;
        if(sl.size() >= 2) {
            cor = Math.round(scoreTimeCorrelation(sl) * 1000.0) / 1000.0;
        }

        double diff = b.mania_star_rating.get(0);
        if (s.mods > 0 && b.mania_star_rating.containsKey(s.mods)) {
            diff = b.mania_star_rating.get(s.mods);
        }
        List<ScoreDB.Mod> l = ScoreDB.Mod.fromInt(s.mods);
        double time_mult = 1;
        // halftime
        if(l.contains(ScoreDB.Mod.HT)) {
            time_mult = 4 / 3d;
        }
        // doubletime
        if(l.contains(ScoreDB.Mod.DT)) {
            time_mult = 2 / 3d;
        }
        double ratio = ScoreDB.rratio(s);
        double acc = 100 * ScoreDB.acc(s);
        int total_obj = b.n_hitcircles + b.n_sliders;
        double time = time_mult * ((double) b.total_time) / 1000d;
        double ln_perc = 100 * b.n_sliders / (double) total_obj;
        double den = ((double) total_obj) / time;
        double maxp = 100 * ScoreDB.rratioMax(s);

        List<String> collecs = c.findCollections(b.beatmap_hash);

        String mods = "--";
        if(l.size() > 0) mods = joinStrings(", ", l);

        data.add(new Object[]{
                b.artist_name,
                b.song_title,
                b.diff_name,
                diff,
                (double) s.score,
                ratio,
                acc,
                mods,
                OsuDB.RankStatus.fromByte(b.rank_status),
                ScoreDB.wticksToDate(s.timestamp),
                (double) b.circle_size,
                time,
                den,
                ln_perc,
                s.perfect!=0,
                collecs,
                maxp,
                new ScoreDB.Judgment(s),
                cor,
                sl.size(),
        });
    }

    // https://stackoverflow.com/a/28428582
    private static double scoreTimeCorrelation(List<ScoreDB.Score> s) {
        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;
        int n = s.size();

        for (ScoreDB.Score value : s) {
            double x = value.score;
            double y = ScoreDB.wticksToDate(value.timestamp).getTime();
            sx += x;
            sy += y;
            sxx += x * x;
            syy += y * y;
            sxy += x * y;
        }

        // covariation
        double cov = sxy / n - sx * sy / n / n;
        // standard error of x
        double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
        // standard error of y
        double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);
        // correlation is just a normalized covariation
        return cov / sigmax / sigmay;
    }

    private static String joinStrings(String sep, List<?> l) {
        StringBuilder sb = new StringBuilder(l.get(0).toString());
        for(int i = 1; i < l.size(); i++) {
            sb.append(sep).append(l.get(i));
        }
        return sb.toString();
    }

    private static ByteBuffer readBufferFromFile(File f) {
        try {
            ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(f.toPath()));
            b.order(ByteOrder.LITTLE_ENDIAN);
            return b;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Object getValueAt(int row, int col) { return data.get(row)[col]; }

    public int getColumnCount() { return headers.length; }

    public int getRowCount() { return data.size(); }

    public String getColumnName(int col) { return headers[col].name; }

    public String getColumnTooltip(int col) { return headers[col].tooltip; }

    public Class<?> getColumnClass(int col) { return headers[col].type; }
}
