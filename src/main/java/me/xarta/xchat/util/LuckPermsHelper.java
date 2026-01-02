package me.xarta.xchat.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;

public final class LuckPermsHelper {
    private LuckPermsHelper() {}

    private static LuckPerms api() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public static String getPrefix(ServerPlayer sp) {
        LuckPerms lp = api();
        if (lp == null) return null;
        User user = lp.getPlayerAdapter(ServerPlayer.class).getUser(sp);
        ContextManager cm = lp.getContextManager();
        CachedMetaData meta = user.getCachedData().getMetaData(cm.getQueryOptions(sp));
        return meta.getPrefix();
    }

    public static String getSuffix(ServerPlayer sp) {
        LuckPerms lp = api();
        if (lp == null) return null;
        User user = lp.getPlayerAdapter(ServerPlayer.class).getUser(sp);
        ContextManager cm = lp.getContextManager();
        CachedMetaData meta = user.getCachedData().getMetaData(cm.getQueryOptions(sp));
        return meta.getSuffix();
    }

    public static String getPrimaryGroup(ServerPlayer sp) {
        LuckPerms lp = api();
        if (lp == null) return "default";
        User user = lp.getPlayerAdapter(ServerPlayer.class).getUser(sp);
        return user.getPrimaryGroup();
    }
}