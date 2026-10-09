package com.dankscripts.sgjatlantis_cityshield.shield;

import com.dankscripts.sgjatlantis_cityshield.block.ShieldControllerBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.block.ShieldEmitterBlockEntity;
import com.dankscripts.sgjatlantis_cityshield.network.ModNetwork;
import com.dankscripts.sgjatlantis_cityshield.network.ShieldSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShieldSavedData extends SavedData {
    private static final String DATA_NAME = "sgjatlantis_cityshield_definitions";
    private final Map<String, ShieldDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, EmitterLocation> emitters = new LinkedHashMap<>();

    public static ShieldSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(ShieldSavedData::new, (tag, registries) -> load(tag), null), DATA_NAME);
    }

    public ShieldDefinition getOrCreate(ServerLevel level, BlockPos controller) {
        ShieldDefinition candidate = new ShieldDefinition(level.dimension(), controller);
        ShieldDefinition result = definitions.computeIfAbsent(candidate.key(), key -> candidate);
        setDirty();
        return result;
    }

    public ShieldDefinition find(Level level, BlockPos controller) {
        return definitions.get(level.dimension().location() + "@" + controller.asLong());
    }

    public ShieldDefinition find(String dimension, long controller) {
        return definitions.get(dimension + "@" + controller);
    }

    public ShieldDefinition findByEmitter(Level level, BlockPos emitter) {
        String dimension = level.dimension().location().toString();
        long emitterLong = emitter.asLong();
        for (ShieldDefinition definition : definitions.values()) {
            if (definition.dimension().toString().equals(dimension) && definition.hasEmitter()
                    && definition.emitterPos().asLong() == emitterLong) return definition;
        }
        return null;
    }

    public void registerEmitter(ServerLevel level, BlockPos emitter) {
        EmitterLocation location = new EmitterLocation(level.dimension().location().toString(), emitter.asLong());
        if (emitters.putIfAbsent(location.key(), location) == null) setDirty();
    }

    public void unregisterEmitter(ServerLevel level, BlockPos emitter, long gameTime) {
        if (emitters.remove(emitterKey(level, emitter)) != null) setDirty();
        boolean changedDefinition = false;
        for (ShieldDefinition definition : definitions.values()) {
            if (!definition.hasEmitter() || !definition.dimension().equals(level.dimension().location())
                    || !definition.emitterPos().equals(emitter)) continue;
            definition.setActive(false, gameTime);
            definition.clearEmitter();
            changedDefinition = true;
        }
        if (changedDefinition) changed();
    }

    public ShieldEmitterBlockEntity resolveEmitter(ServerLevel level, ShieldDefinition definition,
                                                    ShieldControllerBlockEntity controller,
                                                    boolean allowNewLink) {
        BlockPos linked = definition.emitterPos();
        if (linked != null) {
            if (!controller.canCommunicateWith(linked)) return null;
            level.getChunkAt(linked);
            if (level.getBlockEntity(linked) instanceof ShieldEmitterBlockEntity emitter) return emitter;
            definition.clearEmitter();
            changed();
        }
        if (!allowNewLink) return null;

        EmitterLocation nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        long maxDistanceSqr = controller.maxCommunicationDistanceSqr();
        for (EmitterLocation candidate : emitters.values()) {
            if (!candidate.dimension().equals(level.dimension().location().toString())) continue;
            BlockPos candidatePos = BlockPos.of(candidate.pos());
            double distance = definition.controllerPos().distSqr(candidatePos);
            if (distance > maxDistanceSqr || distance >= nearestDistance
                    || emitterClaimedByAnother(definition, candidatePos)) continue;
            nearest = candidate;
            nearestDistance = distance;
        }
        if (nearest == null) return null;

        BlockPos emitterPos = BlockPos.of(nearest.pos());
        level.getChunkAt(emitterPos);
        if (!(level.getBlockEntity(emitterPos) instanceof ShieldEmitterBlockEntity emitter)) {
            emitters.remove(nearest.key());
            setDirty();
            return null;
        }
        definition.setEmitter(emitterPos);
        changed();
        return emitter;
    }

    public ShieldDefinition linkEmitterToNearestController(ServerLevel level, BlockPos emitterPos) {
        ShieldDefinition nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        long absoluteMaxSqr = (long) ShieldControllerBlockEntity.MAX_COMMUNICATION_DISTANCE
                * ShieldControllerBlockEntity.MAX_COMMUNICATION_DISTANCE;
        for (ShieldDefinition definition : definitions.values()) {
            if (!definition.dimension().equals(level.dimension().location()) || definition.hasEmitter()) continue;
            BlockPos controllerPos = definition.controllerPos();
            double distance = controllerPos.distSqr(emitterPos);
            if (distance > absoluteMaxSqr || distance >= nearestDistance) continue;
            level.getChunkAt(controllerPos);
            if (!(level.getBlockEntity(controllerPos) instanceof ShieldControllerBlockEntity controller)
                    || !controller.canCommunicateWith(emitterPos)) continue;
            nearest = definition;
            nearestDistance = distance;
        }
        if (nearest != null) {
            nearest.setEmitter(emitterPos);
            changed();
        }
        return nearest;
    }

    public ShieldDefinition restore(ServerLevel level, BlockPos controller, CompoundTag retained,
                                    long gameTime) {
        ShieldDefinition restored = ShieldDefinition.restoreAt(level.dimension(), controller, retained, gameTime);
        definitions.put(restored.key(), restored);
        changed();
        return restored;
    }

    public void remove(Level level, BlockPos controller) {
        if (definitions.remove(level.dimension().location() + "@" + controller.asLong()) != null) changed();
    }

    public Collection<ShieldDefinition> all() { return definitions.values(); }

    public void changed() {
        setDirty();
        PacketDistributor.sendToAllPlayers(new ShieldSyncPacket(new ArrayList<>(definitions.values())));
    }

    public void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new ShieldSyncPacket(new ArrayList<>(definitions.values())));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        definitions.values().forEach(definition -> list.add(definition.save()));
        tag.put("Shields", list);
        ListTag emitterList = new ListTag();
        for (EmitterLocation emitter : emitters.values()) {
            CompoundTag emitterTag = new CompoundTag();
            emitterTag.putString("Dimension", emitter.dimension());
            emitterTag.putLong("Pos", emitter.pos());
            emitterList.add(emitterTag);
        }
        tag.put("Emitters", emitterList);
        return tag;
    }

    public static ShieldSavedData load(CompoundTag tag) {
        ShieldSavedData data = new ShieldSavedData();
        ListTag list = tag.getList("Shields", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ShieldDefinition definition = ShieldDefinition.load(list.getCompound(i));
            data.definitions.put(definition.key(), definition);
        }
        ListTag emitterList = tag.getList("Emitters", Tag.TAG_COMPOUND);
        for (int i = 0; i < emitterList.size(); i++) {
            CompoundTag emitterTag = emitterList.getCompound(i);
            EmitterLocation emitter = new EmitterLocation(
                    emitterTag.getString("Dimension"), emitterTag.getLong("Pos"));
            data.emitters.put(emitter.key(), emitter);
        }
        return data;
    }

    public List<ShieldDefinition> snapshot() { return List.copyOf(definitions.values()); }

    private boolean emitterClaimedByAnother(ShieldDefinition owner, BlockPos emitter) {
        for (ShieldDefinition definition : definitions.values()) {
            if (definition != owner && definition.hasEmitter()
                    && definition.dimension().equals(owner.dimension())
                    && definition.emitterPos().equals(emitter)) return true;
        }
        return false;
    }

    private static String emitterKey(Level level, BlockPos emitter) {
        return level.dimension().location() + "@" + emitter.asLong();
    }

    private record EmitterLocation(String dimension, long pos) {
        private String key() { return dimension + "@" + pos; }
    }
}
