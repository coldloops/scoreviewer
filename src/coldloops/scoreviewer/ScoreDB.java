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

    // rainbow ratio max / 300
    static double rratio(Score s) {
        return (double) s.cmax / s.c300;
    }

    // rainbow ratio max / total
    static double rratio2(Score s) {
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
        // extra target practice mod stuff
        if(Mod.TP.isEnabled(s.mods)) {
            buf.getLong();
        }
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

        NF(1),         // no fail
        EZ(2),         // easy
        HD(8),         // hidden
        HR(16),        // hard rock
        SD(32),        // sudden death
        DT(64),        // double time
        RL(128),       // relax, std only
        HT(256),       // half time
        NC(512),       // night core
        FL(1024),      // flash light
        AP(2048),      // auto play
        SO(4096),      // spun out, std only
        RL2(8192),     // relax2, std only
        PF(16384),     // perfect
        K4(32768),     // key 4
        K5(65536),     // key 5
        K6(131072),    // key 6
        K7(262144),    // key 7
        K8(524288),    // key 8
        FI(1048576),   // fade in
        RD(2097152),   // random
        TP(8388608),   // target-practice, std only
        K9(16777216),  // key 9
        K10(33554432), // key 10
        K1(67108864),  // key 1
        K3(134217728), // key 3
        K2(268435456); // key 2

        final int value;
        Mod(int v) { this.value = v; }

        static List<Mod> fromInt(int mods) {
            ArrayList<Mod> l = new ArrayList<>();
            for (Mod m : values()) {
                if(m.isEnabled(mods)) {
                    l.add(m);
                }
            }
            return l;
        }

        boolean isEnabled(int mods) {
            return ((mods & value) == value);
        }
    }
}
