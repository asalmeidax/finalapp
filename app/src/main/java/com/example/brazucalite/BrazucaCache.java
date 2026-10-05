package com.example.brazucalite;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class BrazucaCache {
    private BrazucaCache() {}

    public static String encode(List<MediaItemModel> items) throws Exception {
        JSONArray a = new JSONArray();
        for (MediaItemModel m : items) {
            JSONObject o = new JSONObject();
            o.put("id", m.id); o.put("title", m.title); o.put("year", m.year);
            o.put("category", m.category); o.put("poster", m.poster); o.put("streamUrl", m.streamUrl);
            a.put(o);
        }
        JSONObject root = new JSONObject(); root.put("items", a); return root.toString();
    }
}
