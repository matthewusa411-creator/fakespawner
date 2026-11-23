package dev.fakespawner;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.LongConsumer;

/** Right Shift menu: turn the fake features on/off and change the worth values. */
public class MenuScreen extends Screen {
    private int cx;
    private int top;

    public MenuScreen() {
        super(Component.literal("Fake Client"));
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        cx = width / 2;
        top = height / 2 - 80;

        addRenderableWidget(Button.builder(label("Fake Spawner", FakeConfig.spawnerEnabled), b -> {
            FakeConfig.spawnerEnabled = !FakeConfig.spawnerEnabled;
            b.setMessage(label("Fake Spawner", FakeConfig.spawnerEnabled));
            FakeConfig.save();
        }).bounds(cx - 100, top, 200, 20).build());

        addRenderableWidget(Button.builder(label("Fake Elytra", FakeConfig.elytraEnabled), b -> {
            FakeConfig.elytraEnabled = !FakeConfig.elytraEnabled;
            b.setMessage(label("Fake Elytra", FakeConfig.elytraEnabled));
            FakeConfig.save();
        }).bounds(cx - 100, top + 24, 200, 20).build());

        addRenderableWidget(Button.builder(label("Elytra flight (desync)", FakeConfig.flightEnabled), b -> {
            FakeConfig.flightEnabled = !FakeConfig.flightEnabled;
            b.setMessage(label("Elytra flight (desync)", FakeConfig.flightEnabled));
            FakeConfig.save();
        }).bounds(cx - 100, top + 48, 200, 20).build());

        numberField(top + 82, FakeConfig.spawnerWorth, v -> FakeConfig.spawnerWorth = v);
        numberField(top + 108, FakeConfig.elytraWorth, v -> FakeConfig.elytraWorth = v);
        numberField(top + 134, FakeConfig.lootSeconds, v -> FakeConfig.lootSeconds = (int) Math.max(1, Math.min(v, 100_000)));
    }

    private void numberField(int y, long initial, LongConsumer onChange) {
        EditBox box = new EditBox(font, cx + 20, y, 80, 20, Component.empty());
        box.setMaxLength(9);
        box.setFilter(s -> s.matches("\\d*"));
        box.setValue(Long.toString(initial));
        box.setResponder(s -> {
            if (s.isEmpty()) return;
            try {
                onChange.accept(Long.parseLong(s));
                FakeConfig.save();
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(box);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, title, cx, top - 20, 0xFFFFFFFF);
        g.drawString(font, "Spawner worth ($ each)", cx - 100, top + 88, 0xFFFFFFFF);
        g.drawString(font, "Elytra worth ($)", cx - 100, top + 114, 0xFFFFFFFF);
        g.drawString(font, "Seconds per bone/arrow", cx - 100, top + 140, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
