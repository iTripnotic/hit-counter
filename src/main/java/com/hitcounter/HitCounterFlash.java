package net.runelite.client.plugins.hitcounter;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class HitCounterFlash extends Overlay
{
    @Nullable
    private Color flashColor = null;

    private final Client client;
    private Instant flashStart;
    private int durationTicks;

    private static final int DEFAULT_FLASH_ALPHA = 80;

    private static final long TICK_DURATION_MS = 600L;

    @Inject
    public HitCounterFlash(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    public void trigger(Color color, int durationTicks)
    {
        this.flashColor = color;
        this.durationTicks = durationTicks;
        this.flashStart = Instant.now();
    }

    public void clear()
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

        long elapsed = Duration.between(flashStart, Instant.now()).toMillis();
        if (elapsed > durationTicks * TICK_DURATION_MS)
        {
            clear();
            return null;
        }

        int cycle = client.getGameCycle();
        boolean shouldRender = cycle % 40 >= 20;

        if (!shouldRender)
        {
            return null;
        }

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