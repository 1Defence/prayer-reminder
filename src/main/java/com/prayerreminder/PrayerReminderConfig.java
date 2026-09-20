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
}
