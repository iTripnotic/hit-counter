package net.runelite.client.plugins.attackcounter;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class ScreenFlashOverlay extends Overlay
{
    private static final int DEFAULT_FLASH_ALPHA = 80;

    private final Client client;
    private final AttackCounterPlugin plugin;

    private Color flashColor;
    private Instant flashStart;
    private int durationTicks;

    @Inject
    public ScreenFlashOverlay(Client client, AttackCounterPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    public void triggerFlash(Color color, int durationTicks)
    {
        this.flashColor = color;
        this.durationTicks = durationTicks;
        this.flashStart = Instant.now();
    }

    public void clearFlash()
    {
        this.flashStart = null;
        this.flashColor = null;
        this.durationTicks = 0;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (flashStart == null || flashColor == null)
        {
            return null;
        }

        // Calculate elapsed ticks (1 tick = 600ms)
        long elapsed = Duration.between(flashStart, Instant.now()).toMillis();
        if (elapsed > durationTicks * 600L)
        {
            clearFlash();
            return null;
        }

        // Flash effect: blink every 20 game cycles
        int cycle = client.getGameCycle();
        boolean shouldRender = cycle % 40 >= 20;

        if (!shouldRender)
        {
            return null;
        }

        // Render the semi-transparent screen
        final int width = client.getCanvas().getWidth();
        final int height = client.getCanvas().getHeight();

        Color translucent = new Color(
                flashColor.getRed(),
                flashColor.getGreen(),
                flashColor.getBlue(),
                DEFAULT_FLASH_ALPHA
        );

        graphics.setColor(translucent);
        graphics.fillRect(0, 0, width, height);

        return null;
    }
}