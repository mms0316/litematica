//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.materials;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;

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
                    Litematica.LOGGER.warn("Failed to load position at line {}", i);
                    continue;
                }

                beaconList.add(blockPos);
            }
        }
    }

    public void register(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        if (GameWrap.getHitResult() instanceof BlockHitResult blockHitResult) {
            final var blockPos = blockHitResult.getBlockPos();
            final var blockState = mc.level.getBlockState(blockPos);
            if (blockState.getBlock() instanceof BeaconBlock) {
                beaconList.add(blockPos);
                InfoUtils.printActionbarMessage("Registered beacon");
            }
        }
    }

    public void unregister(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        if (GameWrap.getHitResult() instanceof BlockHitResult blockHitResult) {
            final var blockPos = blockHitResult.getBlockPos();
            final var blockState = mc.level.getBlockState(blockPos);
            if (blockState.getBlock() instanceof BeaconBlock) {
                beaconList.remove(blockPos);
                InfoUtils.printActionbarMessage("Unregistered beacon");
            }
        }
    }

    public void unregisterAll(Minecraft mc) {
        if (beaconList.isEmpty())
            InfoUtils.printActionbarMessage("Nothing to be unregistered");
        else {
            beaconList.clear();
            InfoUtils.printActionbarMessage("Unregistered all beacons");
        }
    }

    public boolean checkIfObstructs(BlockPos pos, BlockState blockState) {
        for (var beaconPos : beaconList) {
            //To obstruct, (X, Z) must be the same of a beacon
            if (pos.getX() != beaconPos.getX() || pos.getZ() != beaconPos.getZ())
                continue;
            //To obstruct, block's Y is above the beacon
            if (pos.getY() < beaconPos.getY())
                continue;
            //Bedrock doesn't obstruct
            if (blockState.is(Blocks.BEDROCK))
                continue;
            //Visually-transparent blocks don't obstruct (from BeaconBlockEntity.java::tick)
            if (blockState.getLightBlock() < 15)
                continue;

            return true;
        }

        return false;
    }
}
