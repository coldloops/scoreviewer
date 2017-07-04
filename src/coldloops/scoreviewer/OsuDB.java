package coldloops.scoreviewer;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

class OsuDB {
    int version;
    int folder_count;
    byte acc_unlocked;
    long unlock_time;
    String player_name;
    int n_beatmaps;
    Map<String, BeatmapInfo> beatmaps;
    int unknown;

    static OsuDB readOsuDB(ByteBuffer buf) {
        OsuDB o = new OsuDB();
        o.version = buf.getInt();
        o.folder_count = buf.getInt();
        o.acc_unlocked = buf.get();
        o.unlock_time = buf.getLong();
        o.player_name = readULEBString(buf);
        o.n_beatmaps = buf.getInt();
        o.beatmaps = new HashMap<>();
        for (int i = 0; i < o.n_beatmaps; i++) {
            BeatmapInfo bi = readBeatmapInfo(buf);
            o.beatmaps.put(bi.beatmap_hash, bi);
        }
        o.unknown = buf.getInt();
        return o;
    }

    private static BeatmapInfo readBeatmapInfo(ByteBuffer buf) {
        BeatmapInfo b = new BeatmapInfo();
        b.entry_size = buf.getInt();
        b.artist_name = readULEBString(buf);
        b.artist_name_unicode = readULEBString(buf);
        b.song_title = readULEBString(buf);
        b.song_title_unicode = readULEBString(buf);
        b.creator = readULEBString(buf);
        b.diff_name = readULEBString(buf);
        b.audio_name = readULEBString(buf);
        b.beatmap_hash = readULEBString(buf);
        b.osu_filename = readULEBString(buf);
        b.rank_status = buf.get();
        b.n_hitcircles = buf.getShort();
        b.n_sliders = buf.getShort();
        b.n_spinners = buf.getShort();
        b.last_modified = buf.getLong();
        b.app_rate = buf.getFloat();
        b.circle_size = buf.getFloat();
        b.hp_drain = buf.getFloat();
        b.overall_diff = buf.getFloat();
        b.slider_speed = buf.getDouble();
        b.std_star_rating = readIntDoublePairs(buf);
        b.taiko_star_rating = readIntDoublePairs(buf);
        b.ctb_star_rating = readIntDoublePairs(buf);
        b.mania_star_rating = readIntDoublePairs(buf);
        b.drain_time = buf.getInt();
        b.total_time = buf.getInt();
        b.preview_time = buf.getInt();
        b.timing_points = readTimingPoints(buf);
        b.beatmap_id = buf.getInt();
        b.beatmapset_id = buf.getInt();
        b.thread_id = buf.getInt();
        b.std_grade = buf.get();
        b.taiko_grade = buf.get();
        b.ctb_grade = buf.get();
        b.mania_grade = buf.get();
        b.local_beatmap_offset = buf.getShort();
        b.stack_leniency = buf.getFloat();
        b.gameplay_mode = buf.get();
        b.song_source = readULEBString(buf);
        b.song_tags = readULEBString(buf);
        b.online_offset = buf.getShort();
        b.font_used = readULEBString(buf);
        b.is_unplayed = buf.get();
        b.last_played = buf.getLong();
        b.is_osz2 = buf.get();
        b.folder_name = readULEBString(buf);
        b.last_checked = buf.getLong();
        b.ignore_sounds = buf.get();
        b.ignore_skin = buf.get();
        b.disable_sb = buf.get();
        b.disable_video = buf.get();
        b.visual_override = buf.get();
        b.last_modified2 = buf.getInt();
        b.mania_scroll_speed = buf.get();
        return b;
    }

    private static TimingPoint[] readTimingPoints(ByteBuffer buf) {
        int n = buf.getInt();
        TimingPoint[] tps = new TimingPoint[n];
        for (int i = 0; i < n; i++) {
            tps[i] = new TimingPoint();
            tps[i].bpm = buf.getDouble();
            tps[i].offset = buf.getDouble();
            tps[i].is_not_inherited = buf.get();
        }
        return tps;
    }

    private static Map<Integer, Double> readIntDoublePairs(ByteBuffer buf) {
        HashMap<Integer, Double> m = new HashMap<>();
        int n = buf.getInt();
        for (int i = 0; i < n; i++) {
            byte b0 = buf.get(); // 0x08
            int k = buf.getInt();
            byte b1 = buf.get(); // 0x0d
            double v = buf.getDouble();
            m.put(k, v);
        }
        return m;
    }

    static String readULEBString(ByteBuffer buf) {
        int s = buf.get();
        if (s == 0x0b) {
            int l = readULEBInt(buf);
            byte str[] = new byte[l];
            buf.get(str);
            try {
                return new String(str, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return new String(str);
            }
        }
        return "";
    }

    // https://android.googlesource.com/platform/libcore/+/a7752f4d22097346dd7849b92b9f36d0a0a7a8f3/dex/src/main/java/com/android/dex/Leb128.java
    private static int readULEBInt(ByteBuffer buf) {
        int result = 0;
        int cur;
        int count = 0;
        do {
            cur = buf.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        }
        while (((cur & 0x80) == 0x80) && count < 5);
        if ((cur & 0x80) == 0x80) {
            throw new RuntimeException("invalid LEB128 sequence");
        }
        return result;
    }

    static class TimingPoint {
        double bpm;
        double offset;
        byte is_not_inherited;
    }

    static class BeatmapInfo {
        int entry_size;
        String artist_name;
        String artist_name_unicode;
        String song_title;
        String song_title_unicode;
        String creator;
        String diff_name;
        String audio_name;
        String beatmap_hash;
        String osu_filename;
        byte rank_status;
        short n_hitcircles;
        short n_sliders;
        short n_spinners;
        long last_modified;
        float app_rate;
        float circle_size;
        float hp_drain;
        float overall_diff;
        double slider_speed;
        Map<Integer, Double> std_star_rating;
        Map<Integer, Double> taiko_star_rating;
        Map<Integer, Double> ctb_star_rating;
        Map<Integer, Double> mania_star_rating;
        int drain_time;
        int total_time;
        int preview_time;
        TimingPoint[] timing_points;
        int beatmap_id;
        int beatmapset_id;
        int thread_id;
        byte std_grade;
        byte taiko_grade;
        byte ctb_grade;
        byte mania_grade;
        short local_beatmap_offset;
        float stack_leniency;
        byte gameplay_mode;
        String song_source;
        String song_tags;
        short online_offset;
        String font_used;
        byte is_unplayed;
        long last_played;
        byte is_osz2;
        String folder_name;
        long last_checked;
        byte ignore_sounds;
        byte ignore_skin;
        byte disable_sb;
        byte disable_video;
        byte visual_override;
        int last_modified2;
        byte mania_scroll_speed;
    }

    enum RankStatus {
        NotSubmitted(1),
        Pending(2),
        Ranked(4),
        Qualified(6),
        Loved(7),
        Unknown(-1);

        int value;
        RankStatus(int v) { value = v; }

        static RankStatus fromByte(byte status) {
            for(RankStatus rs : values()) {
                if(rs.value == status) return rs;
            }
            RankStatus unk = Unknown;
            unk.value = status;
            return unk;
        }

        public String toString() {
            if(this == Unknown) {
                return "Unknown ("+value+")";
            }else {
                return super.toString();
            }
        }
    }
}
