package net.runelite.client.plugins.hitcounter;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("attackcounter")
public interface HitCounterConfig extends Config {

    @ConfigSection(
            name = "Reset Options",
            description = "Settings for resetting the attack counter.",
            position = 0
    )
    String resetSection = "resetSection";

    @ConfigSection(
            name = "Display Options",
            description = "Settings for how the overlay is displayed.",
            position = 1
    )
    String displaySection = "displaySection";

    @ConfigSection(
            name = "Hit Triggers",
            description = "Trigger effects on specific hit counts.",
            position = 2
    )
    String hitTriggerSection = "hitTriggerSection";

    @ConfigSection(
            name = "Keybinds",
            description = "Settings for keybinds.",
            position = 3
    )
    String keybindSection = "keybindSection";

    /**
     * ======================
     *       Reset Options
     * ======================
     */
    @ConfigItem(
            keyName = "resetOnNpcDespawn",
            name = "Reset on Kill",
            description = "Resets the counter when the target NPC dies.",
            position = 0,
            section = resetSection
    )
    default boolean resetOnNpcDespawn()
    {
        return true;
    }

    @ConfigItem(
            keyName = "resetAfterXHits",
            name = "Reset on Hit",
            description = "Resets the counter after the specified number of hits.",
            position = 1,
            section = resetSection
    )
    default boolean resetAfterXHits() {
        return false;
    }

    @ConfigItem(
            keyName = "resetHitCount",
            name = "Hits to Reset Counter",
            description = "Number of hits to reset the counter.",
            position = 2,
            section = resetSection
    )
    default int resetHitCount() {
        return 10; // Default reset threshold
    }

    /**
     * ======================
     *       Display Options
     * ======================
     */
    @ConfigItem(
            keyName = "displayMode",
            name = "Display Mode",
            description = "Choose how to display the hit counter.",
            position = 0,
            section = displaySection
    )
    default DisplayMode displayMode()
    {
        return DisplayMode.OVERLAY;
    }

    @ConfigItem(
            keyName = "displayOnlyOnUpdate",
            name = "Display Only in Combat",
            description = "Only display the overlay when attacking NPC's.",
            position = 1,
            section = displaySection
    )
    default boolean displayOnlyOnUpdate() {
        return false;
    }

    @ConfigItem(
            keyName = "onlyCountSuccessfulHits",
            name = "Successful Hits Only",
            description = "Only increase the counter when a hit does damage.",
            position = 2,
            section = displaySection
    )
    default boolean onlyCountSuccessfulHits()
    {
        return false;
    }

    @ConfigItem(
            keyName = "excludedNpcNames",
            name = "Ignored NPCs",
            description = "List of NPC names that will not increase the attack count. (Comma-separated)",
            position = 3,
            section = displaySection
    )
    default String excludedNpcNames()
    {
        return "";
    }

    /**
     * ======================
     *       Trigger Options
     * ======================
     */
    @ConfigItem(
            keyName = "triggerHitCount",
            name = "Trigger Hit Count",
            description = "The hit count at which the effect will trigger (0 = disabled).",
            position = 0,
            section = hitTriggerSection
    )
    default int triggerHitCount()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "triggerEffectMode",
            name = "Trigger Effect",
            description = "Choose what effect to apply on the trigger hit.",
            position = 1,
            section = hitTriggerSection
    )
    default TriggerEffectMode triggerEffectMode()
    {
        return TriggerEffectMode.NONE;
    }

    @ConfigItem(
            keyName = "triggerColor",
            name = "Highlight / Flash Color",
            description = "Color used for flash or highlight effect.",
            position = 2,
            section = hitTriggerSection
    )
    default Color triggerColor()
    {
        return Color.RED;
    }

    @ConfigItem(
            keyName = "triggerMode",
            name = "Trigger Mode",
            description = "Choose whether effects trigger once or every X hits.",
            position = 3,
            section = hitTriggerSection
    )
    default AudioTriggerMode triggerMode()
    {
        return AudioTriggerMode.EXACT;
    }

    @ConfigItem(
            keyName = "defaultColor",
            name = "Counter Color",
            description = "Color of the text when trigger condition is not met.",
            position = 4,
            section = hitTriggerSection
    )
    default Color defaultColor()
    {
        return Color.WHITE;
    }

    /**
     * ======================
     *       Keybind Options
     * ======================
     */
    @ConfigItem(
            keyName = "resetCounterKey",
            name = "Reset Counter Key",
            description = "Keybind to reset the attack counter.",
            position = 0,
            section = keybindSection
    )
    default Keybind resetCounterKey() {
        return Keybind.NOT_SET;
    }

    /**
     * Trigger
     */
    enum TriggerEffectMode
    {
        NONE,
        HIGHLIGHT,
        SOUND,
        FLASH
    }

    /**
     * Display
     */
    enum DisplayMode
    {
        OVERLAY,
        INFOBOX
    }

    /**
     * Mode
     */
    enum AudioTriggerMode
    {
        EXACT,
        EVERY
    }
}