package fi.dy.masa.litematica.materials;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

import static fi.dy.masa.malilib.util.JsonUtils.blockPosToJson;

public class BeaconManager {
    private final Set<BlockPos> beaconList = new HashSet<>();

    public BeaconManager() {
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        if (!this.beaconList.isEmpty()) {
            JsonArray arr = new JsonArray();

            for (var pos : this.beaconList) {
                var posJson = blockPosToJson(pos);
                arr.add(posJson);
            }

            obj.add("beacons", arr);
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj) {
        this.beaconList.clear();

        if (JsonUtils.hasArray(obj, "beacons")) {
            JsonArray posArray = obj.get("beacons").getAsJsonArray();
            final int size = posArray.size();

            for (int i = 0; i < size; i++) {
                BlockPos blockPos = null;
                try {
                    var pos = posArray.get(i).getAsJsonArray();
                    blockPos = new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());
                } catch (Exception ignored) { }

                if (blockPos == null) {
                    Litematica.logger.warn("Failed to load position at line {}", i);
                    continue;
                }

                beaconList.add(blockPos);
            }
        }
    }

    public void register(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            final var blockPos = blockHitResult.getBlockPos();
            final var blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock() instanceof BeaconBlock) {
                beaconList.add(blockPos);
                InfoUtils.printActionbarMessage("Registered beacon");
            }
        }
    }

    public void unregister(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            final var blockPos = blockHitResult.getBlockPos();
            final var blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock() instanceof BeaconBlock) {
                beaconList.remove(blockPos);
                InfoUtils.printActionbarMessage("Unregistered beacon");
            }
        }
    }

    public void unregisterAll(MinecraftClient mc) {
        if (beaconList.isEmpty())
            InfoUtils.printActionbarMessage("Nothing to be unregistered");
        else {
            beaconList.clear();
            InfoUtils.printActionbarMessage("Unregistered all beacons");
        }
    }

    public boolean checkIfObstructs(World world, BlockPos pos, BlockState blockState) {
        for (var beaconPos : beaconList) {
            //To obstruct, (X, Z) must be the same of a beacon
            if (pos.getX() != beaconPos.getX() || pos.getZ() != beaconPos.getZ())
                continue;
            //To obstruct, block's Y is above the beacon
            if (pos.getY() < beaconPos.getY())
                continue;
            //Bedrock doesn't obstruct
            if (blockState.isOf(Blocks.BEDROCK))
                continue;
            //Visually-transparent blocks don't obstruct (from BeaconBlockEntity.java::tick)
            if (blockState.getOpacity(world, pos) < 15)
                continue;

            return true;
        }

        return false;
    }
}
