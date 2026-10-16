package com.prayerreminder;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("prayerreminder")
public interface PrayerReminderConfig extends Config
{
    String CONFIG_GROUP = "prayerreminder";
    String CONFIG_REGIONS = "regions_";

    @ConfigItem(
            keyName = "ticksToDisplay",
            name = "Ticks To Display",
            description = "How many ticks the reminder should be displayed for (0 to never timeout until reactivated)",
            position = 1
    )
    default int ticksToDisplay() { return 10; }

    @ConfigItem(
            keyName = "ticksUntilDisplay",
            name = "Ticks Until Display",
            description = "How many ticks quick prayers should be off for the reminder to be displayed",
            position = 2
    )
    default int ticksUntilDisplay() { return 0; }

    @Alpha
    @ConfigItem(
            position = 3,
            keyName = "reminderColor",
            name = "Reminder Color",
            description = "Reminder color of the overlay"
    )
    default Color reminderColor()
    {
        return new Color(255,122,0,122);
    }

    @ConfigItem(
            keyName = "alwaysCheck",
            name = "Always Check",
            description = "Always check if quickprayers are active, when disabled you'll need to manually set the allowed region by right clicking your prayer tab while in the desired area",
            position = 4
    )
    default boolean alwaysCheck() { return true; }

    @ConfigItem(
            keyName = "enableMenu",
            name = "Enable Menu",
            description = "Adds a menu to the prayer tab icon, clicking it will toggle the current region to enable the overlay or not. (ignored if always checking)",
            position = 5
    )
    default boolean enableMenu() { return false; }
}
