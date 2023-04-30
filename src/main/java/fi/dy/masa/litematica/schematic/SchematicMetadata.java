package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.HashSet;
import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.NbtUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NBTUtils;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "Unknown";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.ZERO;
    private long timeCreated;
    private long timeModified;
    private int regionCount;
    private int totalVolume;
    private int totalBlocks;
    private int[] thumbnailPixelData;
    private boolean modifiedSinceSaved;

    public String getName()
    {
        return this.name;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getDescription()
    {
        return this.description;
    }

    @Nullable
    public int[] getPreviewImagePixelData()
    {
        return this.thumbnailPixelData;
    }

    public int getRegionCount()
    {
        return this.regionCount;
    }

    public int getTotalVolume()
    {
        return this.totalVolume;
    }

    public int getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    public long getTimeModified()
    {
        return this.timeModified;
    }

    public boolean hasBeenModified()
    {
        return this.timeCreated != this.timeModified;
    }

    public boolean wasModifiedSinceSaved()
    {
        return this.modifiedSinceSaved;
    }

    public void setModifiedSinceSaved()
    {
        this.modifiedSinceSaved = true;
    }

    public void clearModifiedSinceSaved()
    {
        this.modifiedSinceSaved = false;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPreviewImagePixelData(int[] pixelData)
    {
        this.thumbnailPixelData = pixelData;
    }

    public void setRegionCount(int regionCount)
    {
        this.regionCount = regionCount;
    }

    public void setTotalVolume(int totalVolume)
    {
        this.totalVolume = totalVolume;
    }

    public void setTotalBlocks(int totalBlocks)
    {
        this.totalBlocks = totalBlocks;
    }

    public void setEnclosingSize(Vec3i enclosingSize)
    {
        this.enclosingSize = enclosingSize;
    }

    public void setTimeCreated(long timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    public void setTimeModified(long timeModified)
    {
        this.timeModified = timeModified;
    }

    public void setTimeModifiedToNow()
    {
        this.timeModified = System.currentTimeMillis();
    }

    public NbtCompound writeToNBT()
    {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("Name", this.name);
        nbt.putString("Author", this.author);
        nbt.putString("Description", this.description);
        nbt.putInt("RegionCount", this.regionCount);
        nbt.putInt("TotalVolume", this.totalVolume);
        nbt.putInt("TotalBlocks", this.totalBlocks);
        nbt.putLong("TimeCreated", this.timeCreated);
        nbt.putLong("TimeModified", this.timeModified);
        nbt.put("EnclosingSize", NBTUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            nbt.putIntArray("PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void readFromNBT(NbtCompound nbt)
    {
        this.name = nbt.getString("Name");
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.regionCount = nbt.getInt("RegionCount");
        this.totalVolume = nbt.getInt("TotalVolume");
        this.totalBlocks = nbt.getInt("TotalBlocks");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        Vec3i size = NBTUtils.readBlockPos(nbt.getCompound("EnclosingSize"));
        this.enclosingSize = size != null ? size : BlockPos.ORIGIN;

        if (nbt.contains("PreviewImageData", Constants.NBT.TAG_INT_ARRAY))
        {
            this.thumbnailPixelData = nbt.getIntArray("PreviewImageData");
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }

    public static SchematicMetadata readMetadataFromFile(FileType fileType, File file) {
        switch (fileType) {
            case SPONGE_SCHEMATIC -> {
                NbtCompound nbt = NbtUtils.readNbtFromFile(file);
                if (nbt == null) return null;
                SchematicMetadata metadata = new SchematicMetadata();

                var fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                metadata.setName(fileName);

                if (nbt.contains("Metadata", Constants.NBT.TAG_COMPOUND)) {
                    NbtCompound spongeMetadata = nbt.getCompound("Metadata");
                    if (spongeMetadata.contains("Name", Constants.NBT.TAG_STRING))
                        metadata.setName(spongeMetadata.getString("Name"));

                    if (spongeMetadata.contains("Author", Constants.NBT.TAG_STRING))
                        metadata.setAuthor(spongeMetadata.getString("Author"));

                    if (spongeMetadata.contains("Date", Constants.NBT.TAG_LONG)) {
                        var date = spongeMetadata.getLong("Date");
                        metadata.setTimeCreated(date);
                        metadata.setTimeModified(date);
                    }
                }

                metadata.setRegionCount(0);

                int width = 0;
                int height = 0;
                int length = 0;
                if (nbt.contains("Width", Constants.NBT.TAG_SHORT))
                    width = nbt.getShort("Width") & 0xFFFF;
                if (nbt.contains("Height", Constants.NBT.TAG_SHORT))
                    height = nbt.getShort("Height") & 0xFFFF;
                if (nbt.contains("Length", Constants.NBT.TAG_SHORT))
                    length = nbt.getShort("Length") & 0xFFFF;

                metadata.setEnclosingSize(new Vec3i(width, height, length));
                int volume = width * height * length;
                metadata.setTotalVolume(volume);

                var paletteToIgnore = new HashSet<Integer>();

                if (nbt.contains("PaletteMax", Constants.NBT.TAG_INT)) {
                    int paletteMaxIdx = nbt.getInt("PaletteMax");
                    if (nbt.contains("Palette", Constants.NBT.TAG_COMPOUND)) {
                        var palette = nbt.getCompound("Palette");

                        for (var id : palette.getKeys()) {
                            if (id.endsWith("air") && palette.contains(id, Constants.NBT.TAG_INT))
                                paletteToIgnore.add(palette.getInt(id));
                        }
                    }
                }

                if (paletteToIgnore.size() == 0) {
                    metadata.setTotalBlocks(volume);
                } else {
                    if (nbt.contains("BlockData", Constants.NBT.TAG_BYTE_ARRAY)) {
                        var blockData = nbt.getByteArray("BlockData"); //varint[] format

                        int totalBlocks = 0;
                        var i = 0;
                        boolean error = false;

                        while (i < blockData.length) {
                            var paletteIdx = 0;
                            var varintLength = 0;

                            while (true) {
                                paletteIdx |= (blockData[i] & 127) << (varintLength++ * 7);
                                if (varintLength > 5) {
                                    //Corrupted data?
                                    error = true;
                                    break;
                                }

                                if ((blockData[i] & 128) != 128) {
                                    i++;
                                    break;
                                }
                                i++;
                            }

                            if (error)
                                break;

                            if (!paletteToIgnore.contains(paletteIdx))
                                totalBlocks++;
                        }

                        if (!error)
                            metadata.setTotalBlocks(totalBlocks);
                    }
                }

                return metadata;
            }
            case VANILLA_STRUCTURE -> {
                NbtCompound nbt = NbtUtils.readNbtFromFile(file);
                if (nbt == null) return null;
                SchematicMetadata metadata = new SchematicMetadata();

                var fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                metadata.setName(fileName);

                if (nbt.contains("author", Constants.NBT.TAG_STRING))
                    metadata.setAuthor(nbt.getString("author"));

                metadata.setRegionCount(0);

                int x = 0;
                int y = 0;
                int z = 0;

                if (nbt.contains("size", Constants.NBT.TAG_LIST)) {
                    var size = nbt.getList("size", Constants.NBT.TAG_INT);
                    if (size.size() == 3) {
                        x = size.getInt(0);
                        y = size.getInt(1);
                        z = size.getInt(2);
                    }
                }
                metadata.setEnclosingSize(new Vec3i(x, y, z));
                int volume = x * y * z;
                metadata.setTotalVolume(volume);

                if (nbt.contains("blocks", Constants.NBT.TAG_LIST)) {
                    var blocks = nbt.getList("blocks", Constants.NBT.TAG_COMPOUND);
                    metadata.setTotalBlocks(blocks.size());
                }

                return metadata;
            }
        }

        return null;
    }
}
