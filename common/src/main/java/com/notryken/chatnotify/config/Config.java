/*
 * Copyright 2023, 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package com.notryken.chatnotify.config;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.notryken.chatnotify.ChatNotify;
import com.notryken.chatnotify.config.util.GhettoAsciiWriter;
import com.notryken.chatnotify.config.util.JsonRequired;
import com.notryken.chatnotify.config.util.JsonValidator;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * ChatNotify configuration options class with default values and validation.
 *
 * <p><b>Note:</b> The list of notifications is required to maintain a
 * notification at index 0 for the user's name. This notification is handled
 * differently in several ways.
 *
 * <p>In versions prior to and including v1.2.0, serialization of config is done
 * automatically by gson, and deserialization used a single custom deserializer.
 * Starting in v1.3.0 beta versions, each configuration class requiring custom
 * serialization and/or deserialization has its own serializer and deserializer.
 *
 * <p>Every configuration class has a final int field "version" which can be
 * used by the class deserializer to determine how to interpret the json.
 *
 * <p>Config files generated by versions 1.2.0-pre.3 to 1.2.0 are deserialized
 * by {@link IntermediaryConfigDeserializer}. Files generated by v1.2.0-pre.2
 * and earlier versions are deserialized by {@link LegacyConfigDeserializer}.
 */
public class Config {
    public final int version = 1;
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = ChatNotify.MOD_ID + ".json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new Config.Deserializer())
            .registerTypeAdapter(Notification.class, new Notification.Deserializer())
            .registerTypeAdapter(Sound.class, new Sound.Deserializer())
            .registerTypeAdapter(TextStyle.class, new TextStyle.Deserializer())
            .registerTypeAdapter(Trigger.class, new Trigger.Deserializer())
            .registerTypeAdapter(ResponseMessage.class, new ResponseMessage.Deserializer())
            .setPrettyPrinting()
            .create();
    public static final Gson INTERMEDIARY_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new IntermediaryConfigDeserializer())
            .create();
    public static final Gson LEGACY_GSON = new GsonBuilder()
            .registerTypeAdapter(Config.class, new LegacyConfigDeserializer())
            .create();

    // Options

    public static final int DEFAULT_COLOR = 16761856; // #FFC400
    public static final Sound DEFAULT_SOUND = new Sound();
    public static final SoundSource DEFAULT_SOUND_SOURCE = SoundSource.PLAYERS;
    public static final List<String> DEFAULT_PREFIXES = List.of("/shout", "!");

    @JsonRequired public boolean mixinEarly;
    @JsonRequired public boolean debugShowKey;
    @JsonRequired public boolean checkOwnMessages;
    @JsonRequired public SoundSource soundSource;
    @JsonRequired public boolean allowRegex;
    @JsonRequired public int defaultColor;
    @JsonRequired public Sound defaultSound;
    @JsonRequired public final List<String> prefixes;
    @JsonRequired private final List<Notification> notifications;

    private Config() {
        this.mixinEarly = false;
        this.debugShowKey = false;
        this.checkOwnMessages = true;
        this.soundSource = DEFAULT_SOUND_SOURCE;
        this.allowRegex = false;
        this.defaultColor = DEFAULT_COLOR;
        this.defaultSound = DEFAULT_SOUND;
        this.prefixes = new ArrayList<>(DEFAULT_PREFIXES);
        this.notifications = new ArrayList<>();
        this.notifications.add(Notification.createUser());
    }

    /**
     * Not validated, only for use by self-validating deserializer.
     */
    Config(boolean mixinEarly, boolean debugShowKey, boolean checkOwnMessages,
           SoundSource soundSource, boolean allowRegex, int defaultColor, Sound defaultSound,
           List<String> prefixes, List<Notification> notifications) {
        this.mixinEarly = mixinEarly;
        this.debugShowKey = debugShowKey;
        this.checkOwnMessages = checkOwnMessages;
        this.soundSource = soundSource;
        this.allowRegex = allowRegex;
        this.defaultColor = defaultColor;
        this.defaultSound = defaultSound;
        this.prefixes = prefixes;
        this.notifications = notifications;
    }

    // Username

    public Notification getUserNotif() {
        return notifications.get(0);
    }

    public void setProfileName(String name) {
       getUserNotif().triggers.get(0).string = name;
    }

    public void setDisplayName(String name) {
        getUserNotif().triggers.get(1).string = name;
    }

    // Notifications

    /**
     * @return an unmodifiable view of the notification list.
     */
    public List<Notification> getNotifs() {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * Adds a new notification with default values.
     */
    public void addNotif() {
        notifications.add(Notification.createBlank(new Sound(defaultSound), new TextStyle(defaultColor)));
    }

    /**
     * Removes the notification at the specified index, if possible.
     * <p>
     * <b>Note:</b> Will fail without error if the specified index is 0.
     * @param index the index of the notification.
     * @return {@code true} if a notification was removed, {@code false}
     * otherwise.
     */
    public boolean removeNotif(int index) {
        if (index != 0) {
            notifications.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Swaps the notification at the specified index with the one at
     * the specified index minus one, if possible.
     * <p>
     * <b>Note:</b> Will fail without error if the index is valid but swapping
     * is not possible.
     * @param index the current index of the notification.
     */
    public void increasePriority(int index) {
        if (index > 1) {
            Notification temp = notifications.get(index);
            notifications.set(index, notifications.get(index - 1));
            notifications.set(index - 1, temp);
        }
    }

    /**
     * Moves the notification at the specified index to index 1, if possible,
     * shuffling other notifications as required.
     * <p>
     * <b>Note:</b> Will fail without error if the index is valid but moving
     * is not possible.
     * @param index the current index of the notification.
     */
    public void toMaxPriority(int index) {
        if (index > 1) {
            notifications.add(1, notifications.get(index));
            notifications.remove(index + 1);
        }
    }

    /**
     * Swaps the notification at the specified index with the one at
     * the specified index plus one, if possible.
     * <p>
     * <b>Note:</b> Will fail without error if the index is valid but swapping
     * is not possible.
     * @param index the current index of the notification.
     */
    public void decreasePriority(int index) {
        if (index < notifications.size() - 1) {
            Notification temp = notifications.get(index);
            notifications.set(index, notifications.get(index + 1));
            notifications.set(index + 1, temp);
        }
    }

    /**
     * Moves the notification at the specified index to the highest index, if
     * possible, shuffling other notifications as required.
     * <p>
     * <b>Note:</b> Will fail without error if the index is valid but moving
     * is not possible.
     * @param index the current index of the notification.
     */
    public void toMinPriority(int index) {
        if (index < notifications.size() - 1) {
            notifications.add(notifications.get(index));
            notifications.remove(index);
        }
    }

    // Validation

    /**
     * Cleanup and validate all settings and notifications.
     */
    public void validate() {
        // Remove blank prefixes and sort by decreasing length
        prefixes.removeIf(String::isBlank);
        prefixes.sort(Comparator.comparingInt(String::length).reversed());

        Notification notif;
        Iterator<Notification> iterNotifs = notifications.iterator();

        // Username notification (cannot be removed)
        notif = iterNotifs.next();
        notif.purgeTriggers();
        notif.purgeExclusionTriggers();
        notif.purgeResponseMessages();
        notif.autoDisable();

        // All other notifications
        while (iterNotifs.hasNext()) {
            notif = iterNotifs.next();
            notif.purgeTriggers();
            notif.purgeExclusionTriggers();
            notif.purgeResponseMessages();

            if (notif.triggers.isEmpty() && notif.exclusionTriggers.isEmpty()
                    && notif.responseMessages.isEmpty()) {
                iterNotifs.remove();
            }
            else {
                notif.autoDisable();
            }
        }
    }


    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }


    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) { // Fallback to intermediary
                ChatNotify.LOG.info("Attempting deserialization using intermediary deserializer.");
                config = load(file, INTERMEDIARY_GSON);
            }
            if (config == null) { // Fallback to legacy
                ChatNotify.LOG.info("Attempting deserialization using legacy deserializer.");
                config = load(file, LEGACY_GSON);
            }
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            ChatNotify.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        try {
            if (!Files.isDirectory(DIR_PATH)) {
                Files.createDirectories(DIR_PATH);
            }
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                GSON.toJson(instance, new GhettoAsciiWriter(writer));
            }
            catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            ChatNotify.LOG.error("Unable to save config.", e);
        }
    }

    // Deserialization

    public static class Deserializer implements JsonDeserializer<Config> {
        @Override
        @SuppressWarnings("unchecked")
        public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            boolean mixinEarly = obj.get("mixinEarly").getAsBoolean();
            boolean debugShowKey = obj.get("debugShowKey").getAsBoolean();
            boolean checkOwnMessages = obj.get("checkOwnMessages").getAsBoolean();
            SoundSource soundSource = SoundSource.valueOf(obj.get("soundSource").getAsString());
            boolean allowRegex = obj.get("allowRegex").getAsBoolean();
            int defaultColor = obj.get("defaultColor").getAsInt();
            Sound defaultSound = ctx.deserialize(obj.get("defaultSound"), Sound.class);
            List<String> prefixes = new ArrayList<>(
                    obj.getAsJsonArray("prefixes")
                            .asList().stream().map(JsonElement::getAsString).toList());
            List<Notification> notifications = new ArrayList<>((List<Notification>) (List<?>)
                    obj.getAsJsonArray("notifications")
                            .asList().stream().map(je -> ctx.deserialize(je, Notification.class))
                            .filter(Objects::nonNull).toList());

            // Validate username notification
            if (notifications.isEmpty()) {
                notifications.add(Notification.createUser());
            }
            else if (notifications.get(0).triggers.size() < 2) {
                notifications.set(0, Notification.createUser());
            }

            return new JsonValidator<Config>().validateNonNull(
                    new Config(mixinEarly, debugShowKey, checkOwnMessages, soundSource,
                            allowRegex, defaultColor, defaultSound, prefixes, notifications));
        }
    }
}
