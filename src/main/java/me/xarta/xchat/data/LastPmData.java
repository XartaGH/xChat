package me.xarta.xchat.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;
import net.minecraft.MethodsReturnNonnullByDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LastPmData extends SavedData {

    private static final String NAME = "xchat_last_pm";
    private final Map<UUID, Entry> lastFrom = new HashMap<>();

    public static LastPmData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        SavedData.Factory<LastPmData> factory = new SavedData.Factory<>(
                LastPmData::new,
                LastPmData::load
        );
        return overworld.getDataStorage().computeIfAbsent(factory, NAME);
    }

    public LastPmData() {}

    public static LastPmData load(CompoundTag tag, HolderLookup.Provider provider) {
        LastPmData data = new LastPmData();
        ListTag list = tag.getList("pm", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            UUID receiver = e.getUUID("receiver");
            UUID sender = e.getUUID("sender");
            long time = e.getLong("time");
            data.lastFrom.put(receiver, new Entry(sender, time));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> e : lastFrom.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("receiver", e.getKey());
            c.putUUID("sender", e.getValue().sender);
            c.putLong("time", e.getValue().time);
            list.add(c);
        }
        tag.put("pm", list);
        return tag;
    }

    public void markReceived(UUID receiver, UUID sender, long nowMillis) {
        lastFrom.put(receiver, new Entry(sender, nowMillis));
        setDirty();
    }

    @Nullable
    public UUID getLastSenderIfActive(UUID receiver, long nowMillis, long activeMillis) {
        Entry e = lastFrom.get(receiver);
        if (e == null) return null;
        if (nowMillis - e.time <= activeMillis) {
            return e.sender;
        }
        return null;
    }

    private record Entry(UUID sender, long time) {}
}