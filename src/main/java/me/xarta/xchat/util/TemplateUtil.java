
package me.xarta.xchat.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;
import java.util.Set;

public final class TemplateUtil {
    private TemplateUtil() {}

    public static Component render(String template, Map<String, Component> inserts) {
        if (template == null) template = "";
        if (inserts == null || inserts.isEmpty()) {
            return LegacyFormatter.parse(template);
        }
        MutableComponent out = Component.empty();
        int pos = 0;
        Style current = Style.EMPTY;

        Result nextRes;
        while (pos < template.length() && (nextRes = findNextMarker(template, inserts.keySet(), pos)) != null) {
            int next = nextRes.index();
            String marker = nextRes.marker();

            if (next > pos) {
                String chunk = template.substring(pos, next);
                Component parsed = LegacyFormatter.parse(chunk);
                out.append(parsed);
                current = LegacyFormatter.endStyleFromLegacy(chunk, current);
            }

            Component insert = inserts.get(marker);
            if (insert != null) {
                Component styledInsert = applyBaseStyle(insert, current);
                out.append(styledInsert);
            }

            pos = next + marker.length();
        }

        if (pos < template.length()) {
            String tail = template.substring(pos);
            Component parsed = LegacyFormatter.parse(tail);
            out.append(parsed);
        }

        return out;
    }

    private static Result findNextMarker(String s, Set<String> markers, int start) {
        int min = -1;
        String chosen = null;
        for (String m : markers) {
            if (m == null || m.isEmpty()) continue;
            int idx = s.indexOf(m, start);
            if (idx >= 0 && (min == -1 || idx < min)) {
                min = idx;
                chosen = m;
            }
        }
        return (min >= 0) ? new Result(min, chosen) : null;
    }

    private static Component applyBaseStyle(Component c, Style base) {
        if (base == null) base = Style.EMPTY;
        Style merged = c.getStyle().applyTo(base);
        return c.copy().setStyle(merged);
    }

    private record Result(int index, String marker) {}
}