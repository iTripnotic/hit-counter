package net.runelite.client.plugins.attackcounter;

import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.plugins.attackcounter.AttackCounterConfig.DisplayMode;
import net.runelite.client.plugins.attackcounter.AttackCounterConfig.AudioTriggerMode;


import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@PluginDescriptor(
        name = "Hit Counter",
        description = "Tracks the number of attacks performed.",
        tags = {"combat", "attack", "counter", "damage", "hit", "track"},
        enabledByDefault = false
)

public class AttackCounterPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private AttackCounterConfig config;

    @Inject
    private AttackCounterOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KeyManager keyManager;

    @Getter
    private NPC lastInteractedNpc;

    @Getter
    private int attackCount = 0;

    private Instant lastAttackTime;

    @Inject
    private InfoBoxManager infoBoxManager;

    private AttackCounterInfoBox infoBox;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ScreenFlashOverlay flashOverlay;

    private static final int RED_HITSPLAT_SPRITE_ID = 1359;

    @Provides
    AttackCounterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AttackCounterConfig.class);
    }

    private final HotkeyListener resetCounterHotkey = new HotkeyListener(() -> config.resetCounterKey()) {
        @Override
        public void hotkeyPressed() {
            resetCounter();
        }
    };

    @Override
    protected void startUp() throws Exception
    {

        attackCount = 0;
        lastInteractedNpc = null;
        lastAttackTime = null;

        overlayManager.add(overlay);
        overlayManager.add(flashOverlay);
        keyManager.registerKeyListener(resetCounterHotkey);

        if (config.displayMode() == DisplayMode.INFOBOX)
        {
            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT_SPRITE_ID, 0);
            if (image != null)
            {
                infoBox = new AttackCounterInfoBox(image, this, config);
                infoBoxManager.addInfoBox(infoBox);
            }
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        lastInteractedNpc = null;
        attackCount = 0;
        lastAttackTime = null;

        overlayManager.remove(overlay);
        overlayManager.remove(flashOverlay);
        keyManager.unregisterKeyListener(resetCounterHotkey);

        forceRemoveInfobox();
    }

    /**
     * Removes the infobox regardless of the current display mode.
     * Used during plugin shutdown or when switching away from infobox display.
     */
    private void forceRemoveInfobox()
    {
        if (infoBox != null)
        {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }
    }

    /**
     * Removes the infobox only if it's currently active
     * and the display mode is set to INFOBOX.
     * Used in logic where the plugin is still running but the counter is no longer relevant,
     * such as on NPC death or when out of combat.
     */
    private void removeInfoboxIfActive()
    {
        if (config.displayMode() == DisplayMode.INFOBOX && infoBox != null)
        {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("attackcounter"))
        {
            return;
        }

        if (event.getKey().equals("displayMode"))
        {
            switch (config.displayMode())
            {
                case INFOBOX:
                    boolean shouldShow = !config.displayOnlyOnUpdate() || !isOutOfCombat();

                    if (shouldShow && infoBox == null)
                    {
                        clientThread.invokeLater(() ->
                        {
                            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT_SPRITE_ID, 0);
                            if (image != null)
                            {
                                infoBox = new AttackCounterInfoBox(image, this, config);
                                infoBoxManager.addInfoBox(infoBox);
                            }
                        });
                    }
                    overlayManager.remove(overlay);
                    break;

                case OVERLAY:
                default:
                    forceRemoveInfobox();
                    overlayManager.add(overlay);
                    break;
            }
        }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        // Check if the target is an NPC and the hitsplat is from the local player
        if (!(actor instanceof NPC) || !hitsplat.isMine()) {
            return;
        }

        NPC npc = (NPC) actor;

        // Check if the NPC is in the excluded list
        Set<String> excludedNames = getIgnoredNpcNames(config.excludedNpcNames());
        if (excludedNames.contains(Objects.requireNonNull(npc.getName()).toLowerCase()))
        {
            return;
        }

        // If config enabled, only count successful hits
        if (config.onlyCountSuccessfulHits() && hitsplat.getAmount() <= 0)
        {
            return;
        }

        attackCount++;  // Increment attack count
        lastInteractedNpc = npc; // Track the NPC
        lastAttackTime = Instant.now(); // Track the last time player hit

        int triggerCount = config.triggerHitCount();
        AudioTriggerMode mode = config.triggerMode();

        boolean shouldTrigger = triggerCount > 0 &&
                ((mode == AudioTriggerMode.EXACT && attackCount == triggerCount) ||
                        (mode == AudioTriggerMode.EVERY && attackCount % triggerCount == 0));

        if (shouldTrigger)
        {
            switch (config.triggerEffectMode())
            {
                case SOUND:
                    client.playSoundEffect(SoundEffectID.UI_BOOP, SoundEffectVolume.MEDIUM_HIGH);
                    break;

                case HIGHLIGHT:
                    // No action needed here, color will apply automatically in render method.
                    break;

                case FLASH:
                    if (flashOverlay != null)
                    {
                        flashOverlay.triggerFlash(config.triggerColor(), 2); // 2 ticks
                    }
                    break;
            }
        }

        // Reset logic for "reset after X hits"
        if (config.resetAfterXHits() && attackCount >= config.resetHitCount())
        {
            resetCounter();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned)
    {
        NPC npc = npcDespawned.getNpc();

        if (npc.equals(lastInteractedNpc))
        {
            lastInteractedNpc = null;

            if (config.resetOnNpcDespawn())
            {
                resetCounter(); // Only reset if config allows
            }
            removeInfoboxIfActive();
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() == client.getLocalPlayer())
        {
            lastInteractedNpc = null;
            resetCounter();
            removeInfoboxIfActive();
        }

    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (config.displayMode() != DisplayMode.INFOBOX)
        {
            return;
        }

        boolean shouldBeVisible = !config.displayOnlyOnUpdate() || !isOutOfCombat();

        if (!shouldBeVisible && infoBox != null)
        {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }
        else if (shouldBeVisible && infoBox == null)
        {
            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT_SPRITE_ID, 0);
            if (image != null)
            {
                infoBox = new AttackCounterInfoBox(image, this, config);
                infoBoxManager.addInfoBox(infoBox);
            }
        }
    }

    void resetCounter()
    {
        attackCount = 0;
        lastAttackTime = Instant.now();
    }

    /**
     * Parses a comma-separated list of NPC names from the config,
     * trims and lowercases them for case-insensitive comparison.
     *
     * @param names The raw string of comma-separated NPC names from config
     * @return A Set of lowercased NPC names to ignore
     */
    private Set<String> getIgnoredNpcNames(String names)
    {
        if (names == null || names.trim().isEmpty())
        {
            return Collections.emptySet();
        }

        return Arrays.stream(names.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * Determines if the player is considered "out of combat"
     * based on time elapsed since last hit.
     */
    public boolean isOutOfCombat()
    {
        if (lastInteractedNpc == null)
        {
            return true;
        }

        if (lastAttackTime == null)
        {
            return true;
        }

        return Instant.now().isAfter(lastAttackTime.plusSeconds(6));
    }

}