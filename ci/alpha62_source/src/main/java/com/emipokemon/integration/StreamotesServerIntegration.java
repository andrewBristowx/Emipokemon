package com.emipokemon.integration;

import com.emipokemon.Emipokemon;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StreamotesServerIntegration {
    public static final String CHANNEL = "emiilyextacy";
    public static final String SUPPORTED_VERSION = "1.2.12+1.21";
    private static final Set<String> STREAMOTES_SAMPLE_CHANNELS = Set.of(
            "spookie_rose",
            "fifigoesree",
            "mifuyu"
    );

    private StreamotesServerIntegration() {
    }

    public static void ensureOfficialChannel() {
        if (!FabricLoader.getInstance().isModLoaded("streamotes")) {
            Emipokemon.LOGGER.info("Streamotes not installed; emote channel synchronization skipped");
            return;
        }

        String version = FabricLoader.getInstance().getModContainer("streamotes")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
        if (!SUPPORTED_VERSION.equals(version)) {
            Emipokemon.LOGGER.warn("Expected Streamotes {}, found {}. Automatic channel configuration skipped.",
                    SUPPORTED_VERSION, version);
            return;
        }

        try {
            Class<?> modelClass = Class.forName("xeed.mc.streamotes.ModConfigModel");
            Method getInstance = modelClass.getMethod("getInstance");
            Method save = modelClass.getMethod("save");
            Object model = getInstance.invoke(null);

            Field channelsField = modelClass.getField("emoteChannels");
            List<String> channels = new ArrayList<>();
            Object currentChannels = channelsField.get(model);
            if (currentChannels instanceof List<?> existing) {
                for (Object entry : existing) {
                    if (entry instanceof String channel
                            && !channel.isBlank()
                            && !STREAMOTES_SAMPLE_CHANNELS.contains(channel.toLowerCase(Locale.ROOT))) {
                        channels.add(channel);
                    }
                }
            }
            if (channels.stream().noneMatch(CHANNEL::equalsIgnoreCase)) channels.add(CHANNEL);
            channelsField.set(model, channels);

            enable(modelClass, model, "twitchSubscriberEmotes");
            enable(modelClass, model, "bttvChannelEmotes");
            enable(modelClass, model, "ffzChannelEmotes");
            enable(modelClass, model, "x7tvChannelEmotes");
            disable(modelClass, model, "twitchGlobalEmotes");
            disable(modelClass, model, "bttvEmotes");
            disable(modelClass, model, "ffzEmotes");
            disable(modelClass, model, "x7tvEmotes");
            disable(modelClass, model, "colorEmotes");
            requireExplicitCodes(modelClass, model);
            save.invoke(null);
            Emipokemon.LOGGER.info(
                    "Streamotes channel {} is configured with explicit codes, original colors and channel-only packs",
                    CHANNEL
            );
        } catch (ReflectiveOperationException exception) {
            Emipokemon.LOGGER.warn("Could not configure Streamotes channel automatically", exception);
        }
    }

    private static void enable(Class<?> modelClass, Object model, String fieldName) throws ReflectiveOperationException {
        modelClass.getField(fieldName).setBoolean(model, true);
    }

    private static void disable(Class<?> modelClass, Object model, String fieldName) throws ReflectiveOperationException {
        modelClass.getField(fieldName).setBoolean(model, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void requireExplicitCodes(Class<?> modelClass, Object model) throws ReflectiveOperationException {
        Class<? extends Enum> activationClass = (Class<? extends Enum>) Class.forName("xeed.mc.streamotes.ActivationOption");
        Object required = Enum.valueOf(activationClass, "Required");
        modelClass.getField("activationMode").set(model, required);
    }
}
