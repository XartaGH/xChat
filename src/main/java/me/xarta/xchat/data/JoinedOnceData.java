package me.xarta.xchat.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JoinedOnceData extends SavedData {
    private static final String NAME = "xchat_joined_once";
    private final Set<UUID> seen = new HashSet<>();

    public static JoinedOnceData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        SavedData.Factory<JoinedOnceData> factory = new SavedData.Factory<>(
                JoinedOnceData::new,
                JoinedOnceData::load
        );
        return overworld.getDataStorage().computeIfAbsent(factory, NAME);
    }

    public JoinedOnceData() {}

    public static JoinedOnceData load(CompoundTag tag, HolderLookup.Provider provider) {
        JoinedOnceData data = new JoinedOnceData();
        ListTag list = tag.getList("seen", 8);
        for (int i = 0; i < list.size(); i++) {
            data.seen.add(UUID.fromString(list.getString(i)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (UUID id : seen) list.add(StringTag.valueOf(id.toString()));
        tag.put("seen", list);
        return tag;
    }

    public boolean markAndCheckFirstJoin(UUID uuid) {
        boolean first = seen.add(uuid);
        if (first) setDirty();
        return first;
    }
}