package io.carpets.util.json;

import java.util.HashMap;
import java.util.Map;

public class JSONObject {
    private final Map<String, Object> data = new HashMap<>();

    public JSONObject() {
    }

    public JSONObject(Map<String, ?> map) {
        if (map != null) {
            data.putAll(map);
        }
    }

    public void put(String key, Object value) {
        if (key != null) {
            data.put(key, value);
        }
    }

    public Object get(String key) {
        return data.get(key);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else {
                sb.append(val);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
