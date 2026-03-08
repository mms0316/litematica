package fi.dy.masa.litematica.util;

import javax.annotation.Nonnull;
import net.minecraft.util.StringRepresentable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum DebugHudMode implements IConfigOptionListEntry, StringRepresentable
{
	DEFAULT     ("default",      "litematica.gui.label.debug_info_mode.default"),
	VANILLA     ("vanilla",      "litematica.gui.label.debug_info_mode.vanilla"),
	NONE        ("none",         "litematica.gui.label.debug_info_mode.none"),
	;

	public static final EnumCodec<DebugHudMode> CODEC = StringRepresentable.fromEnum(DebugHudMode::values);
	public static final ImmutableList<DebugHudMode> VALUES = ImmutableList.copyOf(values());
	private final String configString;
	private final String translationKey;

	DebugHudMode(String configString, String translationKey)
	{
		this.configString = configString;
		this.translationKey = translationKey;
	}

	@Override
	public @Nonnull String getSerializedName()
	{
		return this.configString;
	}

	@Override
	public String getStringValue()
	{
		return this.configString;
	}

	@Override
	public String getDisplayName()
	{
		return StringUtils.translate(this.translationKey);
	}

	@Override
	public DebugHudMode cycle(boolean forward)
	{
		int id = this.ordinal();

		if (forward)
		{
			if (++id >= values().length)
			{
				id = 0;
			}
		}
		else
		{
			if (--id < 0)
			{
				id = values().length - 1;
			}
		}

		return values()[id % values().length];
	}

	@Override
	public DebugHudMode fromString(String name)
	{
		return fromStringStatic(name);
	}

	public static DebugHudMode fromStringStatic(String name)
	{
		for (DebugHudMode val : DebugHudMode.values())
		{
			if (val.configString.equalsIgnoreCase(name))
			{
				return val;
			}
		}

		return DebugHudMode.DEFAULT;
	}
}
