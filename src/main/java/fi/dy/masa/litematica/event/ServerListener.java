package fi.dy.masa.litematica.event;

import fi.dy.masa.malilib.interfaces.IServerListener;
import net.minecraft.client.server.IntegratedServer;
import fi.dy.masa.litematica.data.DataManager;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        DataManager.getInstance().setHasIntegratedServer(true);
    }
}
