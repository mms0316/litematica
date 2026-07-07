package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.time.TimeFormat;

public class WidgetSchematicVersion extends WidgetListEntryBase<SchematicVersion>
{
    private final SchematicProject project;
    private final boolean isOdd;

    public WidgetSchematicVersion(int x, int y, int width, int height, boolean isOdd,
            SchematicVersion entry, int listIndex, SchematicProject project)
    {
        super(x, y, width, height, entry, listIndex);

        this.project = project;
        this.isOdd = isOdd;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        boolean versionSelected = this.project.getCurrentVersion() == this.entry;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || versionSelected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (versionSelected)
        {
            RenderUtils.drawOutline(ctx, this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        String str = StringUtils.translate("litematica.gui.label.schematic_projects.version_entry", this.entry.getVersion(), this.entry.getName());
        this.drawString(ctx, this.x + 4, this.y + 4, 0xFFFFFFFF, str);
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (this.entry != null &&
            GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, this.height))
        {
            List<String> text = new ArrayList<>();

            /*
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_name", this.placement.getSchematic().getMetadata().getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_file", fileName));
            BlockPos o = this.placement.getOrigin();
            String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.origin", strOrigin));
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.sub_region_count", String.valueOf(this.placement.getSubRegionCount())));

            Vec3i size = this.placement.getSchematic().getTotalSize();
            String strSize = String.format("%d x %d x %d", size.getX(), size.getY(), size.getZ());
            text.add(StringUtils.translate("litematica.gui.label.schematic_placement.enclosing_size", strSize));
            */

            text.add(StringUtils.translate("litematica.gui.label.schematic_projects.version_hover.entry", this.entry.getVersion(), this.entry.getName()));
            text.add(StringUtils.translate("litematica.gui.label.schematic_projects.version_hover.timestamp", TimeFormat.REGULAR.formatTo(this.entry.getTimeStamp())));

            if (this.entry.getDescription() != null && !this.entry.getDescription().isEmpty())
            {
                List<String> lines = new ArrayList<>();
                StringUtils.splitTextToLines(lines, this.entry.getDescription(), SchematicVersion.MAX_DESCRIPTION_LENGTH);
                text.add(StringUtils.translate("litematica.gui.label.schematic_projects.version_hover.description"));

                for (String entry : lines)
                {
                    text.add("  §f"+entry);
                }
            }

            RenderUtils.drawHoverText(ctx, mouseX, mouseY, text);
        }

        super.postRenderHovered(ctx, mouseX, mouseY, selected);
    }
}
