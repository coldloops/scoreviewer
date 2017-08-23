package coldloops.scoreviewer;


import java.nio.ByteBuffer;
import java.util.*;

public class CollectionDB {
    int version;
    int n_collections;
    // collection name -> set of beatmap hashes
    Map<String, Set<String>> collections;

    List<String> findCollections(String beatmapHash) {
        ArrayList<String> c = new ArrayList<>();
        for(Map.Entry<String,Set<String>> e : collections.entrySet()) {
            if(e.getValue().contains(beatmapHash)) c.add(e.getKey());
        }
        return c;
    }

    static CollectionDB readCollectionDB(ByteBuffer buf) {
        CollectionDB c = new CollectionDB();
        c.version = buf.getInt();
        c.n_collections = buf.getInt();
        c.collections = new HashMap<>();
        for(int i = 0; i < c.n_collections; i++) {
            String cn = OsuDB.readULEBString(buf);
            int n_beatmaps = buf.getInt();
            Set<String> l = new HashSet<>();
            for(int j = 0; j < n_beatmaps; j++) {
                l.add(OsuDB.readULEBString(buf));
            }
            c.collections.put(cn, l);
        }
        return c;
    }
}
