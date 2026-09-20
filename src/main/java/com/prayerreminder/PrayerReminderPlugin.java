package com.prayerreminder;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Prayer Reminder",
        description = "Reminds you quick prayer is no longer on.",
        tags = {"prayer","alting","hopping","quick"}
)
public class PrayerReminderPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private PrayerReminderConfig config;

    @Override
    protected void startUp() throws Exception
    {

    }

    @Override
    protected void shutDown() throws Exception
    {

    }

    @Provides
	PrayerReminderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PrayerReminderConfig.class);
    }
}
