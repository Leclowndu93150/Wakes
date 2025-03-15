package com.leclowndu93150.wakes.config.gui;

import com.leclowndu93150.wakes.WakesClient;
import com.leclowndu93150.wakes.render.WakeColor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ColorPicker extends AbstractWidget {
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.withDefaultNamespace("widget/slot_frame");
    private static final ResourceLocation PICKER_KNOB_TEXTURE = ResourceLocation.fromNamespaceAndPath(WakesClient.MOD_ID, "textures/picker_knob.png");
    private static final int pickerKnobDim = 7;

    private final Map<String, Bounded> widgets = new HashMap<>();
    private final AABB bounds;
    private Consumer<WakeColor> changedColorListener;

    private final Vector2f pickerPos = new Vector2f();

    public ColorPicker(ColorPickerScreen screenContext, int x, int y, int width, int height) {
        super(x, y, width, height, Component.nullToEmpty(""));

        this.bounds = new AABB(0, 0, 1f, 2f / 3f, x, y, width, height);

        this.widgets.put("hueSlider", new GradientSlider(new AABB(0f, 4f / 6f, 1f, 5f / 6f, x, y, width, height), "Hue", this,true));
        this.widgets.put("alphaSlider", new GradientSlider(new AABB(3f / 6f, 5f / 6f, 1f, 1f, x, y, width, height), "Opacity", this, false));
        this.widgets.put("hexInputField", new HexInputField(new AABB(0f, 5f / 6f, 3f / 6f, 1f, x, y, width, height), this, Minecraft.getInstance().font));

        screenContext.addWidget(this);
        for (var widget : this.widgets.values()) {
            screenContext.addWidget(widget.getWidget());
        }
        this.setActive(false);
    }

    public void setActive(boolean active) {
        this.active = this.visible = active;
        for (var widget : this.widgets.values()) {
            widget.setActive(active);
        }
    }

    public void setColor(WakeColor currentColor, WidgetUpdateFlag updateFlag) {
        if (updateFlag.equals(WidgetUpdateFlag.ONLY_HEX)) {
            this.widgets.get("hexInputField").setColor(currentColor);
            return;
        }
        float[] hsv = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null);
        this.pickerPos.set(
                this.bounds.x + hsv[1] * this.bounds.width,
                this.bounds.y + (1 - hsv[2]) * this.bounds.height);
        for (var widgetKey : this.widgets.keySet()) {
            if (updateFlag.equals(WidgetUpdateFlag.IGNORE_HEX) && widgetKey.equals("hexInputField")) {
                this.changedColorListener.accept(currentColor);
                continue;
            }
            widgets.get(widgetKey).setColor(currentColor);
        }
    }

    public void registerListener(Consumer<WakeColor> changedListener) {
        this.changedColorListener = changedListener;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        AbstractWidget hexInput = this.widgets.get("hexInputField").getWidget();
        if (hexInput.isFocused()) {
            return hexInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        AbstractWidget hexInput = this.widgets.get("hexInputField").getWidget();
        if (hexInput.isFocused()) {
            return hexInput.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        AbstractWidget focusedWidget = null;
        for (var widget : this.widgets.values()) {
            widget.getWidget().setFocused(false);
            if (widget.getBounds().contains((int) mouseX, (int) mouseY)) {
                focusedWidget = widget.getWidget();
            }
        }
        if (focusedWidget != null) {
            focusedWidget.setFocused(true);
            focusedWidget.onClick(mouseX, mouseY);
            return;
        }
        this.updatePickerPos(mouseX, mouseY);
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        mouseX = Math.min(this.getX() + width, Math.max(this.getX(), mouseX));
        mouseY = Math.min(this.getY() + height, Math.max(this.getY(), mouseY));
        for (var widget : this.widgets.values()) {
            if (widget.getWidget().isFocused()) {
                widget.getWidget().onDrag(mouseX, mouseY, deltaX, deltaY);
                return;
            }
        }
        this.updatePickerPos(mouseX, mouseY);
        super.onDrag(mouseX, mouseY, deltaX, deltaY);
    }

    public void updatePickerPos(double mouseX, double mouseY) {
        mouseX = Math.min(this.bounds.x + this.bounds.width, Math.max(this.bounds.x, mouseX));
        mouseY = Math.min(this.bounds.y + this.bounds.height, Math.max(this.bounds.y, mouseY));
        this.pickerPos.set(mouseX, mouseY);
        this.updateColor();
    }

    public void updateColor() {
        float hue = ((GradientSlider) this.widgets.get("hueSlider").getWidget()).getValue();
        float saturation = (pickerPos.x - this.bounds.x) / this.bounds.width;
        float value = 1f - (pickerPos.y - this.bounds.y) / this.bounds.height;
        float opacity = ((GradientSlider) this.widgets.get("alphaSlider").getWidget()).getValue();
        WakeColor newColor = new WakeColor(hue, saturation, value, opacity);
        this.setColor(newColor, WidgetUpdateFlag.ONLY_HEX);
        this.changedColorListener.accept(newColor);
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!active) return;

        // Draw color spectrum
        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;

        RenderSystem.setShader(WakesClient.POSITION_TEXTURE_HSV::getProgram);
        RenderSystem.setShaderTexture(0, GradientSlider.BLANK_SLIDER_TEXTURE);
        float hue = ((GradientSlider) widgets.get("hueSlider").getWidget()).getValue();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = context.pose().last().pose();
        buffer.addVertex(matrix, x, y, 0).setUv(0, 0).setColor(hue, 0f, 1f, 1f);
        buffer.addVertex(matrix, x, y + h, 0).setUv(0, 1).setColor(hue, 0f, 0f, 1f);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(1, 1).setColor(hue, 1f, 0f, 1f);
        buffer.addVertex(matrix, x + w, y, 0).setUv(1, 0).setColor(hue, 1f, 1f, 1f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // Draw frame
        context.blitSprite(FRAME_TEXTURE, x, y, w, h);

        // Draw picker knob
        int d = pickerKnobDim;
        int pickerX = (int) Math.min(bounds.x + bounds.width - d, Math.max(bounds.x, pickerPos.x - 3));
        int pickerY = (int) Math.min(bounds.y + bounds.height - d, Math.max(bounds.y, pickerPos.y - 3));
        context.blit(PICKER_KNOB_TEXTURE, pickerX, pickerY, 0, 0, d, d, d, d);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

    public interface Bounded {
        AABB getBounds();
        AbstractWidget getWidget();

        void setActive(boolean active);

        void setColor(WakeColor currentColor);
    }

    public static class AABB {
        public int x;
        public int y;
        public int width;
        public int height;

        public AABB(float fracX1, float fracY1, float fracX2, float fracY2, int globX, int globY, int totWidth, int totHeight) {
            this.x = Math.round(fracX1 * totWidth) + globX + 1;
            this.y = Math.round(fracY1 * totHeight) + globY + 1;
            this.width = Math.round((fracX2 - fracX1) * totWidth) - 2;
            this.height = Math.round((fracY2 - fracY1) * totHeight) - 2;
        }

        public boolean contains(int x, int y) {
            return this.x <= x && x < this.x + this.width &&
                    this.y <= y && y < this.y + this.height;
        }
    }

    private static class HexInputField extends EditBox implements Bounded {
        protected AABB bounds;
        private final ColorPicker colorPicker;
        private final Pattern hexColorRegex;
        private boolean autoUpdate = false;

        public HexInputField(AABB bounds, ColorPicker colorPicker, Font textRenderer) {
            super(textRenderer, bounds.x, bounds.y, bounds.width, bounds.height, Component.empty());
            this.setMaxLength(9); // #AARRGGBB
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.setFilter(HexInputField::validHex);
            this.hexColorRegex = Pattern.compile("#[a-f0-9]{7,9}", Pattern.CASE_INSENSITIVE);
        }

        @Override
        public void onValueChange(String newText) {
            if (autoUpdate) {
                // Ensures color picker doesn't update itself when updating hex string
                return;
            }
            // Only manual edits to the hex field should update the color picker
            if (hexColorRegex.matcher(newText).find()) {
                this.colorPicker.setColor(new WakeColor(newText), WidgetUpdateFlag.IGNORE_HEX);
            }
            super.onValueChange(newText);
        }

        private static boolean validHex(String text) {
            if (text.length() > 9) {
                return false;
            }
            for (char c : text.toLowerCase().toCharArray()) {
                if (Character.digit(c, 16) == -1 && c != '#') {
                    return false;
                }
            }
            return true;
        }

        public void setActive(boolean active) {
            this.active = this.visible = active;
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            this.autoUpdate = false;
            return super.charTyped(chr, modifiers);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            this.autoUpdate = false;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void setColor(WakeColor currentColor) {
            this.autoUpdate = true;
            this.setValue(currentColor.toHex());
        }

        @Override
        public AABB getBounds() {
            return this.bounds;
        }

        @Override
        public AbstractWidget getWidget() {
            return this;
        }
    }

    private static class GradientSlider extends AbstractSliderButton implements Bounded {
        private static final ResourceLocation TRANSPARENT_SLIDER_TEXTURE = ResourceLocation.fromNamespaceAndPath("wakes", "textures/transparent_slider.png");
        private static final ResourceLocation BLANK_SLIDER_TEXTURE = ResourceLocation.fromNamespaceAndPath("wakes", "textures/blank_slider.png");

        protected AABB bounds;
        private final boolean colored;

        private final ColorPicker colorPicker;

        public GradientSlider(AABB bounds, String text, ColorPicker colorPicker, boolean colored) {
            super(bounds.x, bounds.y, bounds.width, bounds.height, Component.nullToEmpty(text), 1f);
            this.bounds = bounds;
            this.colorPicker = colorPicker;
            this.colored = colored;
        }

        public void setActive(boolean active) {
            this.active = this.visible = active;
        }

        @Override
        public void setColor(WakeColor currentColor) {
            if (colored) {
                this.value = Color.RGBtoHSB(currentColor.r, currentColor.g, currentColor.b, null)[0];
            } else {
                this.value = currentColor.a / 255f;
            }
        }

        public float getValue() {
            return (float) this.value;
        }

        @Override
        public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();

            context.blitSprite(this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
            int leftCol, rightCol;
            if (colored) {
                context.setColor(1.0F, 1.0F, 1.0F, 0.3f);
                RenderSystem.setShader(WakesClient.POSITION_TEXTURE_HSV::getProgram);
                RenderSystem.setShaderTexture(0, BLANK_SLIDER_TEXTURE);

                // AAHHSSVV
                leftCol = 0xFF00FFFF;
                rightCol = 0xFFFFFFFF;

            } else {
                context.setColor(1.0f, 1.0f, 1.0f, 0.6f);
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderTexture(0, TRANSPARENT_SLIDER_TEXTURE);

                // AARRGGBB
                leftCol = 0xFFFFFFFF;
                rightCol = 0x00FFFFFF;
            }

            int x = bounds.x;
            int y = bounds.y;
            int w = bounds.width;
            int h = bounds.height;

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f matrix = context.pose().last().pose();

            buffer.addVertex(matrix, x, y, 5).setUv(0, 0).setColor(leftCol);
            buffer.addVertex(matrix, x, y + h, 5).setUv(0, 1).setColor(leftCol);
            buffer.addVertex(matrix, x + w, y + h, 5).setUv(1, 1).setColor(rightCol);
            buffer.addVertex(matrix, x + w, y, 5).setUv(1, 0).setColor(rightCol);

            BufferUploader.drawWithShader(buffer.buildOrThrow());


            context.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.blitSprite(this.getHandleSprite(), this.getX() + (int)(this.value * (double)(this.width - 8)), this.getY(), 8, this.getHeight());
            int i = this.active ? 0xFFFFFF : 0xA0A0A0;
            this.renderScrollingString(context, Minecraft.getInstance().font, 2, i | Mth.ceil((float)(this.alpha * 255.0f)) << 24);
        }

        @Override
        protected void updateMessage() {

        }

        @Override
        protected void applyValue() {
            colorPicker.updateColor();
        }

        @Override
        public AABB getBounds() {
            return this.bounds;
        }

        @Override
        public AbstractWidget getWidget() {
            return this;
        }
    }

    public enum WidgetUpdateFlag {
        ALL,
        ONLY_HEX,
        IGNORE_HEX
    }
}
