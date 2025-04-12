package net.runelite.client.plugins.hitcounter;

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
import net.runelite.client.plugins.hitcounter.HitCounterConfig.DisplayMode;
import net.runelite.client.plugins.hitcounter.HitCounterConfig.AudioTriggerMode;

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
        description = "Tracks the number of hits performed.",
        tags = {"combat", "pvm", "boss", "hit", "infobox", "monster", "overlay", "attack", "damage", "track"},
        enabledByDefault = false
)

//TODO
// 1. Make sure you can't duplicate infobox
// 2. Test what happens when local player dies
// 3. Find better sound trigger

public class HitCounterPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private HitCounterConfig config;

    @Inject
    private HitCounterOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KeyManager keyManager;

    @Getter
    private NPC lastInteractedNpc;

    private Instant lastAttackTime;

    @Inject
    private InfoBoxManager infoBoxManager;

    private HitCounterInfoBox infoBox;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private HitCounterFlash flashOverlay;

    @Getter
    private int attackCount = 0;

    private static final int RED_HITSPLAT = 1359;

    private static final int FLASH_DURATION_TICKS = 2;

    private static final int OUT_OF_COMBAT_TICKS = 6;

    @Provides
    HitCounterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HitCounterConfig.class);
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
            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT, 0);
            if (image != null)
            {
                infoBox = new HitCounterInfoBox(image, this, config);
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

        removeInfobox();
    }

    private void removeInfobox()
    {
        if (infoBox != null)
        {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }
    }

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
                            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT, 0);
                            if (image != null)
                            {
                                infoBox = new HitCounterInfoBox(image, this, config);
                                infoBoxManager.addInfoBox(infoBox);
                            }
                        });
                    }
                    overlayManager.remove(overlay);
                    break;

                case OVERLAY:
                default:
                    removeInfobox();
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

        if (!(actor instanceof NPC) || !hitsplat.isMine()) {
            return;
        }

        NPC npc = (NPC) actor;
        Set<String> excludedNames = getIgnoredNpcNames(config.excludedNpcNames());
        if (excludedNames.contains(Objects.requireNonNull(npc.getName()).toLowerCase()))
        {
            return;
        }

        if (config.onlyCountSuccessfulHits() && hitsplat.getAmount() <= 0)
        {
            return;
        }

        attackCount++;
        lastInteractedNpc = npc;
        lastAttackTime = Instant.now();

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
                    // Color will apply in render method
                    break;

                case FLASH:
                    if (flashOverlay != null)
                    {
                        flashOverlay.trigger(config.triggerColor(), FLASH_DURATION_TICKS);
                    }
                    break;
            }
        }

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
                resetCounter();
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

    /**
     * Ensures the InfoBox visibility dynamically updates based on combat state.
     */
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
            BufferedImage image = spriteManager.getSprite(RED_HITSPLAT, 0);
            if (image != null)
            {
                infoBox = new HitCounterInfoBox(image, this, config);
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

        return Instant.now().isAfter(lastAttackTime.plusSeconds(OUT_OF_COMBAT_TICKS));
    }
}