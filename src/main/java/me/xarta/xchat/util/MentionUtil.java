package me.xarta.xchat.util;

import me.xarta.xchat.config.ConfigHandler;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionUtil {
    private MentionUtil() {}

    private static Pattern buildMentionPattern(String symbol, Collection<String> names) {
        if (names.isEmpty()) {
            return Pattern.compile("(?!x)x");
        }
        if (names.size() == 1) {
            String only = names.iterator().next();
            return Pattern.compile(Pattern.quote(symbol) + Pattern.quote(only), Pattern.CASE_INSENSITIVE);
        }
        String joined = String.join("|", names.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(n -> Pattern.quote(symbol) + Pattern.quote(n))
                .toList());
        return Pattern.compile(joined, Pattern.CASE_INSENSITIVE);
    }

    public static Component buildMentionsComponent(ServerPlayer sender, String text, boolean inChat) {
        if (!ConfigHandler.MENTION_ENABLED.get()) {
            return LegacyFormatter.parse(text);
        }

        boolean permRequired = ConfigHandler.MENTION_PERMISSION_REQUIRED.get();
        boolean has = !permRequired || LuckPermsHelper.hasPermission(sender, "xchat.mention");
        if (!has) {
            return LegacyFormatter.parse(text);
        }

        String symbol = ConfigHandler.MENTION_SYMBOL.get();
        if (symbol.isEmpty()) symbol = "@";

        Style mentionStyle = LegacyFormatter.styleFromCodes(ConfigHandler.MENTION_FORMAT.get());
        boolean tooltipEnabled = ConfigHandler.MENTION_TOOLTIP_ENABLED.get();
        String tooltipTmpl = ConfigHandler.MENTION_TOOLTIP_FORMAT.get();
        boolean soundEnabled = inChat && ConfigHandler.MENTION_SOUND.get();

        PlayerList pl = sender.server.getPlayerList();
        Map<String, ServerPlayer> byLower = new HashMap<>();
        for (ServerPlayer p : pl.getPlayers()) {
            byLower.put(p.getGameProfile().getName().toLowerCase(Locale.ROOT), p);
        }

        Pattern pattern = buildMentionPattern(symbol, byLower.keySet());
        MutableComponent out = Component.empty();
        Matcher m = pattern.matcher(text);
        int last = 0;
        Style current = Style.EMPTY;
        Set<UUID> sounded = new HashSet<>();

        while (m.find()) {
            if (m.start() > last) {
                String gap = text.substring(last, m.start());
                Component gapComp = LegacyFormatter.parse(gap);
                out.append(applyBaseStyle(gapComp, current));
                current = LegacyFormatter.endStyleFromLegacy(gap, current);
            }

            String token = text.substring(m.start(), m.end());
            String name = token.substring(symbol.length());
            ServerPlayer target = byLower.get(name.toLowerCase(Locale.ROOT));
            if (target != null) {
                Style applied = mentionStyle.applyTo(current);
                MutableComponent mention = Component.literal(token).setStyle(
                        applied.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pm " + target.getGameProfile().getName() + " "))
                );
                if (tooltipEnabled) {
                    String tt = tooltipTmpl.replace("%mentioned_player%", target.getGameProfile().getName());
                    mention = mention.setStyle(mention.getStyle().withHoverEvent(
                            new HoverEvent(HoverEvent.Action.SHOW_TEXT, LegacyFormatter.parse(tt))
                    ));
                }
                out.append(mention);
                if (soundEnabled && sounded.add(target.getUUID())) {
                    target.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 1.0f, 1.0f);
                }
            } else {
                Component plainComp = LegacyFormatter.parse(token);
                out.append(applyBaseStyle(plainComp, current));
                current = LegacyFormatter.endStyleFromLegacy(token, current);
            }
            last = m.end();
        }

        if (last < text.length()) {
            String tail = text.substring(last);
            Component tailComp = LegacyFormatter.parse(tail);
            out.append(applyBaseStyle(tailComp, current));
        }

        return out;
    }

    private static Component applyBaseStyle(Component c, Style base) {
        if (base == null) base = Style.EMPTY;
        Style merged = c.getStyle().applyTo(base);
        return c.copy().setStyle(merged);
    }
}