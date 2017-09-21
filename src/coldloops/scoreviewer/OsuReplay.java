package coldloops.scoreviewer;

import org.tukaani.xz.LZMAInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

class OsuReplay {
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
    int replay_seed;
    long unknown;

    public static void main(String[] args) {
        File replay = new File("testdata/a.osr");
        File osu = new File("testdata/a.osu");
        new ChartDialog(null, "test", osu, replay).display();
        System.out.println("test");
    }

    static List<TimingDelta> calcTimingDeltas(File map, File replay) {
        OsuReplay r = readOsr(readBufferFromFile(replay));
        OsuMap m = readOsuObjs(map);
        return calcTimingDeltas(m, r.replay_data);
    }

    static List<TimingDelta> calcTimingDeltas(OsuMap map, List<ReplayFrame> replay) {
        int curtime = 0;
        List<Integer> times = new ArrayList<>();
        times.addAll(map.objs.keySet());
        Collections.sort(times);
        int [] idx = new int[map.keys];
        boolean [] holds = new boolean[map.keys];
        int gate_limit = 120;
        ArrayList<TimingDelta> tes = new ArrayList<>();
        for(ReplayFrame rf : replay) {
            curtime += rf.w;
            for(int k = 1; k <= map.keys; k++) {
                int t = times.get(idx[k-1]);
                boolean is_pressed = rf.isPressed(k);
                boolean is_held = is_pressed && holds[k-1];
                holds[k-1] = is_pressed;

                // find the next obj
                while(idx[k-1] < times.size()-1 && ((!map.objs.containsKey(t) || !map.objs.get(t).containsKey(k)) || t < curtime-2.4*gate_limit)) {
                    idx[k-1]++;
                    t = times.get(idx[k-1]);
                }
                if(!map.objs.containsKey(t)) continue;
                if(!map.objs.get(t).containsKey(k)) continue;
                int obj = map.objs.get(t).get(k);
                int delta = curtime-t;
                if(is_pressed && (!is_held) && obj == 1 && checkRange(t, curtime-gate_limit, curtime+gate_limit)) {
                    tes.add(new TimingDelta(delta,curtime,k,"SN"));
                    map.objs.get(t).remove(k);
                }
                else if(is_pressed && (!is_held) && obj == 2 && checkRange(t, curtime-1.2*gate_limit, curtime+1.2*gate_limit)) {
                    tes.add(new TimingDelta(delta,curtime,k,"LN"));
                    map.objs.get(t).remove(k);
                }
                else if(!is_pressed && obj == 3 && checkRange(t, curtime-2.4*gate_limit, curtime+2.4*gate_limit)) {
                    tes.add(new TimingDelta(delta, curtime, k, "LN"));
                    map.objs.get(t).remove(k);
                }
            }
        }
        for(Integer t : map.objs.keySet()) {
            for(Integer k : map.objs.get(t).keySet()) {
                tes.add(new TimingDelta(gate_limit, t, k, "MISS"));
            }
        }
        return tes;
    }

    private static boolean checkRange(double x, double lo, double hi) {
        return x >= lo && x <= hi;
    }

    private static OsuMap readOsuObjs(File f) {
        List<String> lines;
        try {
            lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static OsuReplay readOsr(ByteBuffer buf) {
        OsuReplay o = new OsuReplay();
        o.mode = buf.get();
        o.version = buf.getInt();
        o.beatmap_hash = OsuDB.readULEBString(buf);
        o.player_name = OsuDB.readULEBString(buf);
        o.replay_hash = OsuDB.readULEBString(buf);
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
        o.life_bar_graph = OsuDB.readULEBString(buf);
        o.timestamp = buf.getLong();
        o.replay_data_length = buf.getInt();
        byte [] replay_data = new byte[o.replay_data_length];
        buf.get(replay_data);
        o.unknown = buf.getLong();

        // decode replay_data
        o.replay_data = new ArrayList<>();
        Scanner s;
        try {
            s = new Scanner(new LZMAInputStream(new ByteArrayInputStream(replay_data))).useDelimiter(",");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while(s.hasNext()) {
            String frame = s.next();
            if(frame.isEmpty()) continue;
            String parts [] = frame.split("\\|");
            if(parts.length < 4) continue;
            if(parts[0].equals("-12345")) {
                o.replay_seed = Integer.parseInt(parts[3]);
            }
            else {
                int w = Integer.parseInt(parts[0]);
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                int z = Integer.parseInt(parts[3]);
                o.replay_data.add(new ReplayFrame(w, x, y, z));
            }
        }
        return o;
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

    static class TimingDelta {
        int delta;
        int curtime;
        int key;
        String type;
        TimingDelta(int delta, int curtime, int key, String type) {
            this.delta = delta;
            this.curtime = curtime;
            this.key = key;
            this.type = type;
        }
    }
}
