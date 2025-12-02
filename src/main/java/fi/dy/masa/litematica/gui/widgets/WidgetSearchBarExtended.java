package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;

public class WidgetSearchBarExtended extends WidgetSearchBar
{
    public WidgetSearchBarExtended(int x, int y, int width, int height,
            int searchBarOffsetX, IGuiIcon iconSearch, LeftRight iconAlignment)
    {
        super(x, y, width, height, searchBarOffsetX, iconSearch, iconAlignment);
    }

    public void setText(String text)
    {
        this.searchBox.setText(text);
    }

    public String getText()
    {
        return this.searchBox.getText();
    }
}
