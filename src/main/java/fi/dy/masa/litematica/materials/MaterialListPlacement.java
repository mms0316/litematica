package fi.dy.masa.litematica.materials;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.StringUtils;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacementPersistent;

public class MaterialListPlacement extends MaterialListBase
{
    private final SchematicPlacement placement;

    public MaterialListPlacement(SchematicPlacement placement)
    {
        this(placement, false);
    }

    public MaterialListPlacement(SchematicPlacement placement, boolean reCreate)
    {
        super();

        this.placement = placement;

        if (reCreate)
        {
            this.reCreateMaterialList();
        }
    }

    @Override
    public boolean supportsRenderLayers()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return this.placement.getName();
    }

    @Override
    public String getTitle()
    {
        return StringUtils.translate("litematica.gui.title.material_list.placement", this.getName());
    }

    @Override
    public void reCreateMaterialList()
    {
        //Custom Additions (easier to resolve future merge conflicts)
        TaskScheduler.getInstanceClient().removeTasks(TaskCountBlocksPlacement.class);
        TaskScheduler.getInstanceClient().removeTasks(TaskCountBlocksPlacementPersistent.class);

        boolean ignoreState = Configs.Generic.MATERIAL_LIST_IGNORE_STATE.getBooleanValue();

        //Custom Additions (easier to resolve future merge conflicts)
        TaskCountBlocksPlacement task;

        if (Configs.Generic.MATERIAL_LIST_PLACEMENT_PERSISTENT.getBooleanValue())
        {
            task = new TaskCountBlocksPlacementPersistent(this.placement, this, ignoreState);
        }
        else
        {
            task = new TaskCountBlocksPlacement(this.placement, this, ignoreState);
        }

        TaskScheduler.getInstanceClient().scheduleTask(task, 20);
    }
}
