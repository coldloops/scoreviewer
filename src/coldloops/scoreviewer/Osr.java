package coldloops.scoreviewer;

import org.knowm.xchart.SwingWrapper;
import org.tukaani.xz.LZMAInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

class Osr {

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
    String life_bar_graph;
    long timestamp;
    int replay_data_length;
    List<ReplayFrame> replay_data;
    long unknown;

    public static void main(String[] args) throws Exception {
        // osr name is
        // <beatmap_hash>-<xticks>.osr
        // xticks is (score.timestamp - 504911232000000000)

        File f = new File("testdata/b.osr");
        File f2 = new File("testdata/b.osu");
        ByteBuffer b = readBufferFromFile(f);
        Osr r = readOsr(b);
        OsuMap m = readOsuObjs(f2);

        List<TimingError> tes = calcTimingErrors(m, r.replay_data);
        new SwingWrapper<>(Chart.makeChart(tes)).displayChart();
    }

    static List<TimingError> calcTimingErrors(OsuMap map, List<ReplayFrame> replay) {
        int curtime = 0;
        List<Integer> times = new ArrayList<>();
        times.addAll(map.objs.keySet());
        Collections.sort(times);
        int [] idx = new int[map.keys];
        boolean [] holds = new boolean[map.keys];
        int gate_limit = 120;
        ArrayList<TimingError> tes = new ArrayList<>();
        for(ReplayFrame rf : replay) {
            curtime += rf.w;
            for(int k = 1; k <= map.keys; k++) {
                int t = times.get(idx[k-1]);
                boolean is_pressed = rf.isPressed(k);
                boolean is_held = is_pressed && holds[k-1];
                holds[k-1] = is_pressed;
                while(!(idx[k-1]+1 >= times.size() ||
                    (map.objs.containsKey(t) && map.objs.get(t).containsKey(k) &&
                    t >= curtime - 2.4*gate_limit))) {
                    idx[k-1]++;
                    t = times.get(idx[k-1]);
                }
                if(!map.objs.containsKey(t)) continue;
                if(!map.objs.get(t).containsKey(k)) continue;
                int obj = map.objs.get(t).get(k);
                if(is_pressed && (!is_held) && obj == 1 && checkRange(t, curtime-gate_limit, curtime+gate_limit)) {
                    int delta = curtime-t;
                    tes.add(new TimingError(delta,curtime,k,"SN"));
                    map.objs.get(t).remove(k);
                }
                else if(is_pressed && (!is_held) && obj == 2 && checkRange(t, curtime-1.2*gate_limit, curtime+1.2*gate_limit)) {
                    int delta = curtime-t;
                    tes.add(new TimingError(delta,curtime,k,"LN-start"));
                    map.objs.get(t).remove(k);
                }
                else if(!is_pressed && obj == 3 && checkRange(t, curtime-2.4*gate_limit, curtime+2.4*gate_limit)) {
                    int delta = curtime-t;
                    tes.add(new TimingError(delta, curtime, k, "LN-end"));
                    map.objs.get(t).remove(k);
                }
            }
        }
        return tes;
    }

    private static boolean checkRange(double x, double lo, double hi) {
        return x >= lo && x <= hi;
    }

    private static OsuMap readOsuObjs(File f) throws Exception {
        List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        boolean read_hit_objs = false;
        OsuMap map = new OsuMap();
        for (String s : lines) {
            if (s.startsWith("CircleSize")) {
                String[] parts = s.split(":");
                map.keys = Integer.parseInt(parts[1]);
            }
            if (s.equals("[HitObjects]")) {
                read_hit_objs = true;
                continue;
            }
            if (read_hit_objs) {
                String[] parts = s.split(",");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int time = Integer.parseInt(parts[2]);
                int type = Integer.parseInt(parts[3]);
                // hitsound = parts[4]
                // add = parts[5]
                if (!map.objs.containsKey(time)) {
                    map.objs.put(time, new TreeMap<Integer, Integer>());
                }
                int spc = 512/map.keys;
                int k = (x/spc)+1;
                // LN
                if((type & 128) == 128) {
                    String rel[] = parts[5].split(":");
                    int rel0 = Integer.parseInt(rel[0]);
                    map.objs.get(time).put(k, 2);
                    if(!map.objs.containsKey(rel0)) map.objs.put(rel0, new TreeMap<Integer, Integer>());
                    map.objs.get(rel0).put(k, 3);
                }else {
                    map.objs.get(time).put(k, 1);
                }
            }
        }
        return map;
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

    private static Osr readOsr(ByteBuffer buf) throws Exception {
        Osr o = new Osr();
        o.mode = buf.get();
        o.version = buf.getInt();
        o.beatmap_hash = readULEBString(buf);
        o.player_name = readULEBString(buf);
        o.replay_hash = readULEBString(buf);
        o.c300 = buf.getShort();
        o.c100 = buf.getShort();
        o.c50 = buf.getShort();
        o.cmax = buf.getShort();
        o.c200 = buf.getShort();
        o.c0 = buf.getShort();
        o.score = buf.getInt();
        o.maxcombo = buf.getShort();
        o.perfect = buf.get();
        o.mods = buf.getInt();
        o.life_bar_graph = readULEBString(buf);
        o.timestamp = buf.getLong();
        o.replay_data_length = buf.getInt();
        byte [] replay_data = new byte[o.replay_data_length];
        buf.get(replay_data);
        o.unknown = buf.getLong();

        // decode replay_data
        o.replay_data = new ArrayList<>();
        Scanner s = new Scanner(new LZMAInputStream(new ByteArrayInputStream(replay_data))).useDelimiter(",");
        while(s.hasNext()) {
            String parts [] = s.next().split("\\|");
            int w = Integer.parseInt(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            int z = Integer.parseInt(parts[3]);
            o.replay_data.add(new ReplayFrame(w,x,y,z));
        }
        return o;
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

    static class ReplayFrame {
        final int w; // delta time mills
        final double x; // keys bitarray
        final double y; // ??
        final int z; // ??
        ReplayFrame(int w, double x, double y, int z) {
            this.w=w;
            this.x=x;
            this.y=y;
            this.z=z;
        }

        boolean isPressed(int k) {
            int v = (int)Math.pow(2, k-1);
            return (((int)x & v) == v);
        }

        public String toString() { return w+"|"+x+"|"+y+"|"+z;}
    }

    static class OsuMap {
        int keys;
        TreeMap<Integer, TreeMap<Integer, Integer>> objs = new TreeMap<>();
    }

    static class TimingError {
        int delta;
        int curtime;
        int key;
        String tag;
        TimingError(int delta, int curtime, int key, String tag) {
            this.delta = delta;
            this.curtime = curtime;
            this.key = key;
            this.tag = tag;
        }
    }
}
