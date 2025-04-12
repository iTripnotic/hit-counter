package com.hitcounter;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;

@Singleton
public class HitCounterOverlay extends OverlayPanel
{
    private final HitCounterPlugin plugin;
    private final HitCounterConfig config;

    private static final Dimension PANEL_SIZE = new Dimension(130, 0);

    @Inject
    public HitCounterOverlay(HitCounterPlugin plugin, HitCounterConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        this.config = config;
        addMenuEntry(RUNELITE_OVERLAY, "Reset", "Hit counter", e -> plugin.resetCounter());
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (config.displayMode() != HitCounterConfig.DisplayMode.OVERLAY)
        {
            /**
             * Exit early if display mode is not set to overlay
             */
            return null;
        }

        if (config.displayOnlyOnUpdate() && plugin.isOutOfCombat())
        {
            /**
             * Only show if user is in combat
             */
            return null;
        }

        panelComponent.setPreferredSize(PANEL_SIZE);

        int attackCount = plugin.getAttackCount();

        Color numberColor = config.defaultColor();
        if (config.triggerEffectMode() == HitCounterConfig.TriggerEffectMode.HIGHLIGHT &&
                config.triggerHitCount() > 0 &&
                attackCount == config.triggerHitCount())
        {
            numberColor = config.triggerColor();
        }

        panelComponent.getChildren().add(TitleComponent.builder().text("Hit Counter").build());
        panelComponent.getChildren().add(LineComponent.builder().left("Hits:").right(String.valueOf(attackCount)).rightColor(numberColor).build());

        return super.render(graphics);
    }
}