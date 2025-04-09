package net.runelite.client.plugins.attackcounter;

import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.plugins.attackcounter.AttackCounterConfig.TriggerEffectMode;

import java.awt.*;
import java.awt.image.BufferedImage;

public class AttackCounterInfoBox extends InfoBox {

    private final AttackCounterPlugin plugin;
    private final AttackCounterConfig config;

    public AttackCounterInfoBox(BufferedImage image, AttackCounterPlugin plugin, AttackCounterConfig config) {
        super(image, plugin);
        this.plugin = plugin;
        this.config = config;
        setPriority(InfoBoxPriority.HIGH);
    }

    @Override
    public String getText()
    {
        return String.valueOf(plugin.getAttackCount());
    }

    @Override
    public Color getTextColor()
    {
        int count = plugin.getAttackCount();

        if (config.triggerEffectMode() == TriggerEffectMode.HIGHLIGHT &&
                config.triggerHitCount() > 0 &&
                count == config.triggerHitCount())
        {
            return config.triggerColor(); // Highlight color
        }

        return config.defaultColor(); // Fallback/default color
    }

    @Override
    public String getTooltip()
    {
        StringBuilder tooltip = new StringBuilder();

        NPC npc = plugin.getLastInteractedNpc();
        if (npc != null)
        {
            tooltip.append("Target: ").append(npc.getName());
            if (npc.getCombatLevel() > 0)
            {
                tooltip.append(" (Lvl-").append(npc.getCombatLevel()).append(")");
            }
        }

        int trigger = config.triggerHitCount();
        if (trigger > 0)
        {
            if (tooltip.length() > 0)
            {
                tooltip.append(" | ");
            }
            tooltip.append("Trigger: ").append(trigger)
                    .append(" (").append(config.triggerEffectMode().name()).append(")");
        }

        return tooltip.toString();
    }
}