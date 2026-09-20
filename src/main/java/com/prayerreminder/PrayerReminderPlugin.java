package com.prayerreminder;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;

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

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PrayerReminderOverlay overlay;

    private int ticksInactive = -1;
    private int ticksReminderPresent = -1;
    public boolean reminderActive = false;

    Color configReminderColor;
    int configTicksToDisplay,configTicksUntilDisplay;

    @Override
    protected void startUp() throws Exception
    {
        CacheConfigs();
        ticksInactive = -1;
        ticksReminderPresent = -1;
        reminderActive = false;
        clientThread.invokeLater(() -> {
            StatusChanged(client.getVarbitValue(VarbitID.QUICKPRAYER_ACTIVE));
        });
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
    }

    public void CacheConfigs(){
        configTicksToDisplay = config.ticksToDisplay();
        configTicksUntilDisplay = config.ticksUntilDisplay();
        configReminderColor = config.reminderColor();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged)
    {
        if (!configChanged.getGroup().equals(PrayerReminderConfig.CONFIG_GROUP))
        {
            return;
        }

        CacheConfigs();
    }

    public void StatusChanged(int value){
        boolean enabled = value == 1;
        if(!enabled){
            if(configTicksUntilDisplay == 0){
                EnableReminder();
            }else{
                ticksInactive = 0;
            }
        }else if(enabled){
            DisableReminder();
        }
    }

    void EnableReminder(){
        ticksInactive = -1;
        ticksReminderPresent = 0;
        reminderActive = true;
    }

    void DisableReminder(){
        ticksInactive = -1;
        ticksReminderPresent = -1;
        reminderActive = false;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if(ticksReminderPresent != -1 && configTicksToDisplay != 0){
            if(++ticksReminderPresent >= configTicksToDisplay){
                DisableReminder();
                return;
            }
        }

        if(ticksInactive == -1)
            return;

        if (++ticksInactive >= configTicksUntilDisplay)
        {
            EnableReminder();
        }

    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e){
        if(e.getVarbitId() == VarbitID.QUICKPRAYER_ACTIVE){
            int value = e.getValue();
            StatusChanged(value);
        }
    }

    @Provides
	PrayerReminderConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PrayerReminderConfig.class);
    }
}
