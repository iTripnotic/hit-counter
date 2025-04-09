package net.runelite.client.plugins.attackcounter;

import net.runelite.client.plugins.attackcounter.AttackCounterConfig.DisplayMode;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.plugins.attackcounter.AttackCounterConfig.TriggerEffectMode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY;

@Singleton
public class AttackCounterOverlay extends OverlayPanel
{

    private final AttackCounterPlugin plugin;
    private final AttackCounterConfig config;

    @Inject
    public AttackCounterOverlay(AttackCounterPlugin plugin, AttackCounterConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        this.config = config;
        addMenuEntry(RUNELITE_OVERLAY, "Reset", "Hit counter", e -> plugin.resetCounter());
    }

    //TODO
    // 5. BUG - Duplicate infoboxes

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (config.displayMode() != DisplayMode.OVERLAY)
        {
            return null;
        }

        if (config.displayOnlyOnUpdate() && plugin.isOutOfCombat())
        {
            return null;
        }

        panelComponent.setPreferredSize(new Dimension(130, 0));

        int attackCount = plugin.getAttackCount();

        Color numberColor = config.defaultColor();
        if (config.triggerEffectMode() == TriggerEffectMode.HIGHLIGHT &&
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