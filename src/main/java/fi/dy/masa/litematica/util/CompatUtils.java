package fi.dy.masa.litematica.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/**
 * Post Re-Write code
 */
public class CompatUtils
{
    public static boolean isKeyHeld(InputConstants.Key key)
    {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key.getValue());
    }
}
