package com.expense;

import java.util.*;

/** Tiny JSON writer (no external libraries needed). */
public class Json {
    public static String of(Object o) {
        if (o == null) return "null";
        if (o instanceof Number || o instanceof Boolean) return o.toString();
        if (o instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(esc(String.valueOf(e.getKey()))).append("\":").append(of(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (o instanceof Collection) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object x : (Collection<?>) o) {
                if (!first) sb.append(',');
                first = false;
                sb.append(of(x));
            }
            return sb.append(']').toString();
        }
        return "\"" + esc(o.toString()) + "\"";
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.toString();
    }
}
