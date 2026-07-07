package fi.dy.masa.litematica.compat.mixin;

import java.util.List;
import java.util.Set;
import me.fallenbreath.conditionalmixin.api.mixin.RestrictiveMixinConfigPlugin;

import fi.dy.masa.litematica.Litematica;

public class LitematicaMixinConfigPlugin extends RestrictiveMixinConfigPlugin
{
	@Override
	protected void onRestrictionCheckFailed(String mixinClassName, String reason)
	{
		Litematica.LOGGER.warn("Disabled mixin '{}' due to: '{}'", mixinClassName, reason);
	}

	@Override
	public String getRefMapperConfig()
	{
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
	}

	@Override
	public List<String> getMixins()
	{
		return null;
	}
}
