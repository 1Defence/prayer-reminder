package com.prayerreminder;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;

public class PrayerReminderOverlay extends Overlay
{
    private final Client client;
    private final PrayerReminderPlugin plugin;

    @Inject
    public PrayerReminderOverlay(PrayerReminderPlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if(!plugin.reminderActive)
            return null;
        Rectangle outer = new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
        Rectangle cutout = getQuickPrayerBounds();

        fillRectExcluding(g, outer, cutout, plugin.configReminderColor);
        return null;
    }

    private static void fillRectExcluding(Graphics2D g, Rectangle outer, Rectangle cutout, Color color)
    {
        Area area = new Area(outer);
        if (cutout != null)
        {
            area.subtract(new Area(cutout));
        }
        g.setColor(color);
        g.fill(area);
    }

    private Rectangle getQuickPrayerBounds()
    {
        Widget w = client.getWidget(InterfaceID.Orbs.PRAYERBUTTON);
        if (w == null || w.isHidden())
            return null;
        return w.getBounds();
    }
}