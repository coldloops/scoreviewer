package coldloops.scoreviewer;

import java.nio.ByteBuffer;
import java.util.*;

class ScoreDB {
    int version;
    int n_beatmaps;
    Map<String, BeatmapScores> beatmaps;

    static ScoreDB readScoreDB(ByteBuffer buf) {
        ScoreDB s = new ScoreDB();
        s.version = buf.getInt();
        s.n_beatmaps = buf.getInt();
        s.beatmaps = new HashMap<>();
        for (int i = 0; i < s.n_beatmaps; i++) {
            BeatmapScores bs = readBeatmapScores(buf);
            s.beatmaps.put(bs.beatmap_hash, bs);
        }
        return s;
    }

    // convert windows ticks to Date object
    static Date wticksToDate(long wticks) {
        wticks -= 621355968000000000L;
        return new Date(wticks / 10000);
    }

    // accuracy
    static double acc(Score s) {
        int points = s.c50 * 50 + s.c100 * 100 + s.c200 * 200 + s.c300 * 300 + s.cmax * 300;
        int total = s.c0 + s.c50 + s.c100 + s.c200 + s.c300 + s.cmax;
        return (double) points / (total * 300);
    }

    // rainbow ratio
    static double rratio(Score s) {
        int total = s.c0 + s.c50 + s.c100 + s.c200 + s.c300 + s.cmax;
        return (double) s.cmax / total;
    }

    private static BeatmapScores readBeatmapScores(ByteBuffer buf) {
        BeatmapScores b = new BeatmapScores();
        b.beatmap_hash = OsuDB.readULEBString(buf);
        b.n_scores = buf.getInt();
        b.scores = new Score[b.n_scores];
        for (int j = 0; j < b.n_scores; j++) {
            b.scores[j] = readScore(buf);
        }
        return b;
    }

    private static Score readScore(ByteBuffer buf) {
        Score s = new Score();
        s.mode = buf.get();
        s.version = buf.getInt();
        s.beatmap_hash = OsuDB.readULEBString(buf);
        s.player_name = OsuDB.readULEBString(buf);
        s.replay_hash = OsuDB.readULEBString(buf);
        s.c300 = buf.getShort();
        s.c100 = buf.getShort();
        s.c50 = buf.getShort();
        s.cmax = buf.getShort();
        s.c200 = buf.getShort();
        s.c0 = buf.getShort();
        s.score = buf.getInt();
        s.maxcombo = buf.getShort();
        s.perfect = buf.get();
        s.mods = buf.getInt();
        s.empty = OsuDB.readULEBString(buf);
        s.timestamp = buf.getLong();
        s.empty2 = buf.getInt();
        s.score_id = buf.getLong();
        return s;
    }

    static class BeatmapScores {
        String beatmap_hash;
        int n_scores;
        Score[] scores;
    }

    static class Score {
        byte mode;
        int version;
        String beatmap_hash;
        String player_name;
        String replay_hash;
        int c0, c50, c100, c200, c300, cmax;
        int score;
        short maxcombo;
        byte perfect;
        int mods;
        String empty;
        long timestamp;
        int empty2;
        long score_id;
    }

    enum Mod {

        NF(1),
        EZ(2),
        HD(8),
        HR(16),
        SD(32),
        DT(64),
        HT(256),
        NC(512),
        FL(1024),
        PF(16384),
        FI(1048576),
        RD(2097152);

        final int value;
        Mod(int v) { this.value = v; }

        static List<Mod> fromInt(int mods) {
            ArrayList<Mod> l = new ArrayList<>();
            for (Mod m : values()) {
                if ((mods & m.value) == m.value) {
                    l.add(m);
                }
            }
            return l;
        }
    }
}
