package com.prayerreminder;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.util.ArrayList;

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
    private ChatMessageManager chatMessageManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Gson gson;

    @Inject
    private MenuManager menuManager;

    @Inject
    private PrayerReminderConfig config;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PrayerReminderOverlay overlay;

    private static final int FIXED_PRAYER_TAB_ID = InterfaceID.Toplevel.STONE5;
    private static final int RESIZABLE_A_PRAYER_TABID = InterfaceID.ToplevelOsrsStretch.STONE5;
    private static final int RESIZABLE_B_PRAYER_TABID = InterfaceID.ToplevelPreEoc.STONE5;

    private static final String TOGGLE_MENU_LABEL = "Toggle region warning";

    private static final WidgetMenuOption TOGGLE_MENU_FIXED = new WidgetMenuOption(TOGGLE_MENU_LABEL, "", FIXED_PRAYER_TAB_ID);
    private static final WidgetMenuOption TOGGLE_MENU_RESIZE_A = new WidgetMenuOption(TOGGLE_MENU_LABEL, "", RESIZABLE_A_PRAYER_TABID);
    private static final WidgetMenuOption TOGGLE_MENU_RESIZE_B = new WidgetMenuOption(TOGGLE_MENU_LABEL, "", RESIZABLE_B_PRAYER_TABID);

    private int ticksInactive = -1;
    private int ticksReminderPresent = -1;
    public boolean reminderActive = false;

    ArrayList<Integer> allowedRegions = new ArrayList<>();

    boolean playerInAllowedRegion = false;

    boolean pendingPlayer = false;

    Color configReminderColor;
    int configTicksToDisplay,configTicksUntilDisplay;
    boolean configAlwaysCheck,configEnableMenu;

    @Override
    protected void startUp() throws Exception
    {
        CacheConfigs();
        ticksInactive = -1;
        ticksReminderPresent = -1;
        reminderActive = false;
        clientThread.invokeLater(() -> {
            allowedRegions = GetRegions();
            RefreshToggleMenus();
            StatusChanged(client.getVarbitValue(VarbitID.QUICKPRAYER_ACTIVE));
            CheckRegionWhenValid();
        });
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        ClearToggleMenus();
    }

    public void CacheConfigs(){
        configTicksToDisplay = config.ticksToDisplay();
        configTicksUntilDisplay = config.ticksUntilDisplay();
        configReminderColor = config.reminderColor();
        configAlwaysCheck = config.alwaysCheck();
        configEnableMenu = config.enableMenu();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged)
    {
        if (!configChanged.getGroup().equals(PrayerReminderConfig.CONFIG_GROUP))
        {
            return;
        }

        CacheConfigs();

        if(configChanged.getKey().equals("enableMenu")){
            RefreshToggleMenus();
        }
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
        if(pendingPlayer){
            if(client.getLocalPlayer() != null){
                CheckRegion();
                pendingPlayer = false;
            }
        }

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
    public void onGameStateChanged(GameStateChanged e){
        GameState state = e.getGameState();
        if (state == GameState.LOADING || state == GameState.LOGGED_IN)
        {
            CheckRegionWhenValid();
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

    /**
     * If valid gets the current world point of the local player.
     */
    WorldPoint GetCurrentWorldPoint()
    {
        Player localPlayer;
        if((localPlayer = client.getLocalPlayer()) == null)
            return null;

        WorldPoint worldPoint;
        if((worldPoint = localPlayer.getWorldLocation()) == null)
            return null;

        return worldPoint;
    }

    /**
     * Determine and return the current region accounting for instances such as the POH
     */
    int GetCurrentRegion()
    {
        WorldPoint worldPoint = GetCurrentWorldPoint();
        if(worldPoint == null)
            return -1;

        LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);

        if(localPoint == null)
            return -1;

        return WorldPoint.fromLocalInstance(client, localPoint).getRegionID();
    }

    /**
     * Convenience function to send a game message.
     */
    void SendGameMessage(String contents)
    {
        String message = new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("[Prayer Reminder] ")
                .append(contents)
                .build();
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(message)
                .build());
    }


    /**Gets saved regions that the overlay should occur in*/
    public ArrayList<Integer> GetRegions()
    {

        String json = configManager.getConfiguration(PrayerReminderConfig.CONFIG_GROUP, PrayerReminderConfig.CONFIG_REGIONS);
        if (Strings.isNullOrEmpty(json))
        {
            return new ArrayList<>();
        }
        return gson.fromJson(json, new TypeToken<ArrayList<Integer>>(){}.getType());
    }


    /**Converts collection of regions to json*/
    public void ToggleRegion()
    {
        int region = GetCurrentRegion();
        if(region == -1)
            return;

        boolean saving = true;
        ArrayList<Integer> data = GetRegions();
        if(data.contains(region))
            saving = false;

        if(saving)
            data.add(region);
        else
            data.remove(Integer.valueOf(region));

        String json = gson.toJson(data);
        configManager.setConfiguration(PrayerReminderConfig.CONFIG_GROUP, PrayerReminderConfig.CONFIG_REGIONS , json);
        allowedRegions = data;
        CheckRegionWhenValid();
        SendGameMessage((saving ? "Saved" : "Removed") + (" region...(" + region + ")"));
    }

    /**Typically only on first login, but the player can sometimes be null, in said case we keep trying until valid*/
    public void CheckRegionWhenValid(){
        pendingPlayer = client.getLocalPlayer() == null;
        if(!pendingPlayer){
            CheckRegion();
        }
    }

    /**Checks if current region is allowed.*/
    public void CheckRegion()
    {
        int region = GetCurrentRegion();
        if(region == -1)
            return;
        playerInAllowedRegion = allowedRegions.contains(region);
    }

    /** Removes custom menus
     * Used to toggle the regions status on displaying the overlay */
    private void ClearToggleMenus()
    {
        menuManager.removeManagedCustomMenu(TOGGLE_MENU_FIXED);
        menuManager.removeManagedCustomMenu(TOGGLE_MENU_RESIZE_A);
        menuManager.removeManagedCustomMenu(TOGGLE_MENU_RESIZE_B);
    }

    /** Adds custom menus if config allows it
     * Used to toggle the regions status on displaying the overlay */
    private void RefreshToggleMenus()
    {
        ClearToggleMenus();
        if(!configEnableMenu)
            return;
        menuManager.addManagedCustomMenu(TOGGLE_MENU_FIXED, e -> ToggleRegion());
        menuManager.addManagedCustomMenu(TOGGLE_MENU_RESIZE_A, e -> ToggleRegion());
        menuManager.addManagedCustomMenu(TOGGLE_MENU_RESIZE_B, e -> ToggleRegion());
    }

}
