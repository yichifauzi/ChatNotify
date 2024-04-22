/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.chatnotify.gui.component.listwidget;

import com.notryken.chatnotify.config.Config;
import com.notryken.chatnotify.config.Notification;
import com.notryken.chatnotify.config.Trigger;
import com.notryken.chatnotify.gui.screen.OptionsScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/**
 * Contains a button linking to global options, and a dynamic list of buttons
 * linking to different {@link Notification}s.
 */
public class MainListWidget extends OptionsListWidget {

    public MainListWidget(Minecraft mc, int width, int height, int top, int bottom,
                          int itemHeight, int entryRelX, int entryWidth, int entryHeight,
                          int scrollWidth) {
        super(mc, width, height, top, bottom, itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth);

        addEntry(new OptionsListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("Global Options"), null, -1, (button -> openGlobalConfig())));

        addEntry(new OptionsListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notifications \u2139"),
                Tooltip.create(Component.literal("Incoming messages will activate the first " +
                        "enabled notification that has a matching trigger.")), -1));

        List<Notification> notifs = Config.get().getNotifs();
        for (int i = 0; i < notifs.size(); i++) {
            addEntry(new Entry.NotifConfigEntry(entryX, entryWidth, entryHeight, this, notifs, i));
        }
        addEntry(new OptionsListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    Config.get().addNotif();
                    openNotificationConfig(Config.get().getNotifs().size()-1);
                }));
    }

    @Override
    public MainListWidget resize(int width, int height, int top, int bottom, int itemHeight, double scrollAmount) {
        MainListWidget newListWidget = new MainListWidget(minecraft, width, height, top, bottom, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    private void openGlobalConfig() {
        minecraft.setScreen(new OptionsScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.global"),
                new GlobalOptionsListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth)));
    }

    private void openNotificationConfig(int index) {
        minecraft.setScreen(new OptionsScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.notif"),
                new NotifOptionsListWidget(minecraft, screen.width, screen.height, y0, y1,
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth,
                        Config.get().getNotifs().get(index), index == 0)));
    }

    public static class Entry extends OptionsListWidget.Entry {

        private static class NotifConfigEntry extends Entry {
            private final int mainButtonWidth;

            NotifConfigEntry(int x, int width, int height, MainListWidget listWidget,
                             List<Notification> notifs, int index) {
                super();

                int spacing = 5;
                int statusButtonWidth = 25;
                mainButtonWidth = width - statusButtonWidth - spacing;
                int moveButtonWidth = 12;
                int removeButtonWidth = 24;
                Notification notif = notifs.get(index);

                elements.add(Button.builder(getMessage(notif),
                                (button) -> listWidget.openNotificationConfig(index))
                        .pos(x, 0)
                        .size(mainButtonWidth, height)
                        .build());

                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(notif.isEnabled())
                        .create(x + mainButtonWidth + spacing, 0, statusButtonWidth, height,
                                Component.empty(),
                                (button, status) -> notif.setEnabled(status)));

                if (index > 0) {
                    Button upButton = Button.builder(Component.literal("\u2191"),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            Config.get().toMaxPriority(index);
                                            listWidget.reload();
                                        } else {
                                            Config.get().increasePriority(index);
                                            listWidget.reload();
                                        }})
                            .pos(x - 2 * moveButtonWidth - spacing, 0)
                            .size(moveButtonWidth, height)
                            .build();
                    if (index == 1) upButton.active = false;
                    elements.add(upButton);


                    Button downButton = Button.builder(Component.literal("\u2193"),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            Config.get().toMinPriority(index);
                                            listWidget.reload();
                                        } else {
                                            Config.get().decreasePriority(index);
                                            listWidget.reload();
                                        }})
                            .pos(x - moveButtonWidth - spacing, 0)
                            .size(moveButtonWidth, height)
                            .build();
                    if (index == notifs.size() - 1) downButton.active = false;
                    elements.add(downButton);

                    elements.add(Button.builder(Component.literal("\u274C"),
                                    (button) -> {
                                        if (Config.get().removeNotif(index)) {
                                            listWidget.reload();
                                        }
                                    })
                            .pos(x + width + spacing, 0)
                            .size(removeButtonWidth, height)
                            .build());
                }
            }

            private MutableComponent getMessage(Notification notif) {
                MutableComponent message;
                int maxWidth = (int)(mainButtonWidth * 0.8);
                Font font = Minecraft.getInstance().font;

                if (notif.triggers.isEmpty() || notif.triggers.get(0).string.isBlank()) {
                    message = Component.literal("> Click to Configure <");
                }
                else {
                    StringBuilder messageBuilder = new StringBuilder();

                    for (int i = 0; i < notif.triggers.size(); i++) {
                        Trigger trigger = notif.triggers.get(i);
                        String triggerStr;
                        if (trigger.isKey()) {
                            triggerStr = "[Key] " + (trigger.string.equals(".") ? "Any Message" : trigger.string);
                        }
                        else {
                            triggerStr = trigger.string;
                        }

                        if (i == 0) {
                            messageBuilder.append(triggerStr);
                        }
                        else if (font.width(messageBuilder.toString()) + font.width(triggerStr) <= maxWidth) {
                            messageBuilder.append(", ");
                            messageBuilder.append(triggerStr);
                        }
                        else {
                            messageBuilder.append(" [+");
                            messageBuilder.append(notif.triggers.size() - i);
                            messageBuilder.append("]");
                            break;
                        }
                    }

                    message = Component.literal(messageBuilder.toString())
                            .setStyle(notif.textStyle.getStyle());
                }
                return message;
            }
        }
    }
}