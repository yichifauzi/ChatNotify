package notryken.chatnotify.gui.listwidget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import notryken.chatnotify.client.ChatNotifyClient;
import notryken.chatnotify.config.Notification;
import org.jetbrains.annotations.NotNull;

public class AdvancedConfigListWidget extends ConfigListWidget
{
    private final Notification notif;

    public AdvancedConfigListWidget(MinecraftClient client,
                                    int width, int height,
                                    int top, int bottom, int itemHeight,
                                    Screen parent, Text title,
                                    Notification notif)
    {
        super(client, width, height, top, bottom, itemHeight, parent, title);
        this.notif = notif;

        this.addEntry(new ConfigListWidget.Entry.Header(width, this, client,
                Text.literal("WARNING"), 16711680));
        this.addEntry(new ConfigListWidget.Entry.MultiLineHeader(width, this,
                client, Text.literal("These settings allow you to break " +
                    "ChatNotify and crash Minecraft. Use with caution.")));

        this.addEntry(new ConfigListWidget.Entry.Header(width, this, client,
                Text.literal("Regex in Notification Triggers"), Text.literal(
                        "Enables/Disables use of regex notation in trigger " +
                                "strings. Note: If using regex, " +
                                "double-escapes must be used ('\\\\' instead " +
                                "of the normal '\\').")));
        this.addEntry(new Entry.RegexToggleButton(width, notif, this));

        this.addEntry(new ConfigListWidget.Entry.Header(width, this, client,
                Text.literal("Exclusion Triggers"),
                Text.literal("Exclusion triggers are strings (or regex) that," +
                        " if detected in a message, will prevent this " +
                        "notification from activating when it otherwise " +
                        "would.")));
        for (int idx = 0; idx < notif.getExclusionTriggers().size(); idx ++) {
            this.addEntry(new Entry.ExclusionTriggerField(width, notif, client,
                    this, idx));
        }
        this.addEntry(new Entry.ExclusionTriggerField(width, notif, client,
                this, -1));

        this.addEntry(new ConfigListWidget.Entry.Header(width, this,
                client, Text.literal("Auto Response Messages"), Text.literal(
                        "Auto Response Messages are sent by the client in " +
                                "chat immediately when this notification is " +
                                "activated. Use only with extreme caution.")));
        for (int idx = 0; idx < notif.getResponseMessages().size(); idx ++) {
            this.addEntry(new Entry.ResponseMessageField(width, notif, client,
                    this, idx));
        }
        this.addEntry(new Entry.ResponseMessageField(width, notif, client,
                this, -1));

        this.addEntry(new ConfigListWidget.Entry.Header(width, this,
                client, Text.literal("Broken Everything?")));
        this.addEntry(new Entry.ResetButton(width, notif, this));
        this.addEntry(new Entry.TotalResetButton(width, notif, this));
        this.addEntry(new Entry.NuclearResetButton(width, notif, this, client));
    }

    @Override
    public AdvancedConfigListWidget resize(int width, int height,
                                           int top, int bottom)
    {
        AdvancedConfigListWidget listWidget =
                new AdvancedConfigListWidget(client, width, height, top, bottom,
                        itemHeight, parent, title, notif);
        listWidget.setScrollAmount(this.getScrollAmount());
        return listWidget;
    }

    @Override
    protected void refreshScreen()
    {
        refreshScreen(this);
    }

    private abstract static class Entry extends ConfigListWidget.Entry
    {
        public final Notification notif;

        Entry(int width, Notification notif, AdvancedConfigListWidget listWidget)
        {
            super(width, listWidget);
            this.notif = notif;
        }

        private static class RegexToggleButton extends Entry
        {
            RegexToggleButton(int width, Notification notif,
                              AdvancedConfigListWidget listWidget)
            {
                super(width, notif, listWidget);
                options.add(CyclingButtonWidget.onOffBuilder(
                                Text.literal("Enabled"),
                                Text.literal("Disabled"))
                        .initially(notif.regexEnabled)
                        .build(this.width / 2 - 120, 0, 240, 20,
                                Text.literal("Regex"),
                                (button, status) -> {
                                    notif.regexEnabled = status;
                                    listWidget.refreshScreen();
                                }));
            }
        }

        private static class ExclusionTriggerField extends Entry
        {
            int index;

            ExclusionTriggerField(int width, Notification notif,
                                  @NotNull MinecraftClient client,
                                  AdvancedConfigListWidget listWidget,
                                  int index)
            {
                super(width, notif, listWidget);
                this.index = index;

                if (index >= 0) {
                    TextFieldWidget triggerEdit = new TextFieldWidget(
                            client.textRenderer, this.width / 2 - 120, 0, 240,
                            20, Text.literal("Exclusion Trigger"));
                    triggerEdit.setMaxLength(120);
                    triggerEdit.setText(this.notif.getExclusionTrigger(index));
                    triggerEdit.setChangedListener(this::setExclusionTrigger);

                    this.options.add(triggerEdit);

                    this.options.add(ButtonWidget.builder(Text.literal("X"),
                                    (button) -> {
                                        notif.removeExclusionTrigger(index);
                                        listWidget.refreshScreen();
                                    })
                            .size(20, 20)
                            .position(width / 2 + 120 + 5, 0)
                            .build());
                }
                else {
                    this.options.add(ButtonWidget.builder(Text.literal("+"),
                                    (button) -> {
                                        notif.addExclusionTrigger("");
                                        listWidget.refreshScreen();
                                    })
                            .size(240, 20)
                            .position(width / 2 - 120, 0)
                            .build());
                }
            }

            private void setExclusionTrigger(String exclusionTrigger)
            {
                this.notif.setExclusionTrigger(this.index,
                        exclusionTrigger.strip());
            }
        }

        private static class ResponseMessageField extends Entry
        {
            int index;

            ResponseMessageField(int width, Notification notif,
                                 @NotNull MinecraftClient client,
                                 AdvancedConfigListWidget listWidget,
                                 int index)
            {
                super(width, notif, listWidget);
                this.index = index;

                if (index >= 0) {
                    TextFieldWidget messageEdit = new TextFieldWidget(
                            client.textRenderer, this.width / 2 - 120, 0, 240,
                            20, Text.literal("Response Message"));
                    messageEdit.setMaxLength(120);
                    messageEdit.setText(this.notif.getResponseMessage(index));
                    messageEdit.setChangedListener(this::setResponseMessage);

                    this.options.add(messageEdit);

                    this.options.add(ButtonWidget.builder(Text.literal("X"),
                                    (button) -> {
                                        notif.removeResponseMessage(index);
                                        listWidget.refreshScreen();
                                    })
                            .size(20, 20)
                            .position(width / 2 + 120 + 5, 0)
                            .build());
                }
                else {
                    this.options.add(ButtonWidget.builder(Text.literal("+"),
                                    (button) -> {
                                        notif.addResponseMessage("");
                                        listWidget.refreshScreen();
                                    })
                            .size(240, 20)
                            .position(width / 2 - 120, 0)
                            .build());
                }
            }

            private void setResponseMessage(String exclusionTrigger)
            {
                this.notif.setResponseMessage(this.index,
                        exclusionTrigger.strip());
            }
        }

        private static class ResetButton extends Entry
        {
            ResetButton(int width, Notification notif,
                        AdvancedConfigListWidget listWidget)
            {
                super(width, notif, listWidget);

                ButtonWidget resetButton = ButtonWidget.builder(
                        Text.literal("Reset"), (button) -> {
                            notif.regexEnabled = false;
                            for (int i = 0;
                                 i < notif.getExclusionTriggers().size();
                                 i++)
                            {
                                notif.removeExclusionTrigger(0);
                            }
                            for (int i = 0;
                                 i < notif.getResponseMessages().size();
                                 i++)
                            {
                                notif.removeResponseMessage(0);
                            }
                            listWidget.refreshScreen();
                        })
                        .size(240, 20)
                        .position(width / 2 - 120, 0)
                        .build();
                resetButton.setTooltip(Tooltip.of(Text.literal("Resets all " +
                        "advanced settings for THIS notification.")));

                this.options.add(resetButton);
            }
        }

        private static class TotalResetButton extends Entry
        {
            TotalResetButton(int width, Notification notif,
                            AdvancedConfigListWidget listWidget)
            {
                super(width, notif, listWidget);

                ButtonWidget totalResetButton = ButtonWidget.builder(
                        Text.literal("Reset All"), (button) -> {
                            for (Notification notif2 :
                                    ChatNotifyClient.config.getNotifs())
                            {
                                notif2.regexEnabled = false;
                                for (int i = 0;
                                     i < notif2.getExclusionTriggers().size();
                                     i++)
                                {
                                    notif2.removeExclusionTrigger(0);
                                }
                                for (int i = 0;
                                     i < notif2.getResponseMessages().size();
                                     i++)
                                {
                                    notif2.removeResponseMessage(0);
                                }
                            }
                            listWidget.refreshScreen();

                        })
                        .size(240, 20)
                        .position(width / 2 - 120, 0)
                        .build();
                totalResetButton.setTooltip(Tooltip.of(Text.literal("Resets " +
                        "all advanced settings for ALL notifications.")));

                this.options.add(totalResetButton);
            }
        }

        private static class NuclearResetButton extends Entry
        {
            NuclearResetButton(int width, Notification notif,
                               AdvancedConfigListWidget listWidget,
                               MinecraftClient client)
            {
                super(width, notif, listWidget);

                ButtonWidget nuclearResetButton = ButtonWidget.builder(
                        Text.literal("Nuclear Reset"), (button) -> {
                            client.setScreen(new ConfirmScreen((value) -> {
                                if (value) {
                                    ChatNotifyClient.deleteConfigFile();
                                    ChatNotifyClient.config = null;
                                    ChatNotifyClient.loadConfig();
                                    client.setScreen(null);
                                }
                                else {
                                    listWidget.refreshScreen();
                                }
                            }, Text.literal("Nuclear Reset"), Text.literal(
                                    "Are you sure you want to delete all " +
                                            "ChatNotify notifications and " +
                                            "reset all settings?")));
                        })
                        .size(240, 20)
                        .position(width / 2 - 120, 0)
                        .build();

                nuclearResetButton.setTooltip(Tooltip.of(Text.literal(
                        "Deletes all ChatNotify notifications and resets all " +
                                "settings.")));

                this.options.add(nuclearResetButton);
            }
        }
    }
}