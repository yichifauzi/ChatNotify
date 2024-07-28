package dev.terminalmc.chatnotify.gui.widget;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class OverlayWidget extends AbstractWidget {
    private final Consumer<OverlayWidget> close;
    public final double nominalWidthRatio;
    public final double nominalHeightRatio;

    public OverlayWidget(int x, int y, int width, int height, Component msg,
                         Consumer<OverlayWidget> close) {
        super(x, y, width, height, msg);
        checkWidth(width);
        checkHeight(height);
        this.close = close;
        Window window = Minecraft.getInstance().getWindow();
        this.nominalWidthRatio = width / (double)window.getGuiScaledWidth();
        this.nominalHeightRatio = height / (double)window.getGuiScaledHeight();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return true;
    }

    /**
     * Creates (or re-creates) all sub-widgets and adjusts their sizes and
     * positions based on the current positional and dimensional values of the
     * {@link OverlayWidget}.
     *
     * <p>To be called on initial build and whenever the position or dimensions
     * of the {@link OverlayWidget} are changed.</p>
     */
    protected abstract void init();

    public void onClose() {
        close.accept(this);
    }

    public abstract int getMinWidth();

    public abstract int getMaxWidth();

    public abstract int getMinHeight();

    public abstract int getMaxHeight();

    /*
     * Min dimensions are respected by ALL resizing operations, max dimensions
     * are only respected by nominal resizing.
     */

    public int getNominalWidth(int screenWidth) {
        return Math.min(Math.max(getMinWidth(), (int)(screenWidth * nominalWidthRatio)), getMaxWidth());
    }

    public int getNominalHeight(int screenHeight) {
        return Math.min(Math.max(getMinHeight(), (int)(screenHeight * nominalHeightRatio)), getMaxHeight());
    }

    // Re-init on reposition or resize to maintain sub-widget position and size

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        init();
    }

    public void setX(int x) {
        super.setX(x);
        init();
    }

    public void setY(int y) {
        super.setY(y);
        init();
    }

    /**
     * @throws IllegalArgumentException if {@code width} or {@code height} is
     * out of range.
     * @see OverlayWidget#checkWidth
     * @see OverlayWidget#checkHeight
     */
    @Override
    public void setSize(int width, int height) {
        checkWidth(width);
        checkHeight(height);
        setWidth(width);
        setHeight(height);
        init();
    }

    /**
     * @throws IllegalArgumentException if {@code width} is out of range.
     * @see OverlayWidget#checkWidth
     */
    @Override
    public void setWidth(int width) {
        checkWidth(width);
        super.setWidth(width);
        init();
    }

    /**
     * @throws IllegalArgumentException if {@code height} is out of range.
     * @see OverlayWidget#checkHeight
     */
    @Override
    public void setHeight(int height) {
        checkHeight(height);
        super.setHeight(height);
        init();
    }

    /**
     * @return {@code width}, if it is valid.
     * @throws IllegalArgumentException if {@code width} is less than
     * {@link OverlayWidget#getMinWidth}
     */
    protected int checkWidth(int width) {
        if (width < getMinWidth()) throw new IllegalArgumentException(
                "Width cannot be less than " + getMinWidth() + ", got " + width);
        return width;
    }

    /**
     * @return {@code height}, if it is valid.
     * @throws IllegalArgumentException if {@code height} is less than
     * {@link OverlayWidget#getMinHeight}
     */
    protected int checkHeight(int height) {
        if (height < getMinHeight()) throw new IllegalArgumentException(
                "Height cannot be less than " + getMinHeight() + ", got " + height);
        return height;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narration) {}
}