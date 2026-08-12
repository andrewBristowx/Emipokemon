package com.emipokemon.client.emote;

import com.emipokemon.Emipokemon;
import net.minecraft.text.Style;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class StreamotesBridge {
    private static Method getEmotes;
    private static Method isLoading;
    private static Method fromName;
    private static Method getName;
    private static Method getSource;
    private static Method makeEmoteStyle;
    private static Method requestTexture;
    private static Method getWidth;
    private static Method getHeight;
    private static Method getTexture;
    private static Method getLoadData;
    private static Method getLoader;

    private StreamotesBridge() {
    }

    static boolean initialize() {
        try {
            Class<?> registry = Class.forName("xeed.mc.streamotes.emoticon.EmoticonRegistry");
            Class<?> emoticon = Class.forName("xeed.mc.streamotes.emoticon.Emoticon");
            Class<?> compat = Class.forName("xeed.mc.streamotes.Compat");
            Method resolvedGetEmotes = registry.getMethod("getEmotes");
            Method resolvedIsLoading = registry.getMethod("isLoading");
            Method resolvedFromName = registry.getMethod("fromName", String.class);
            Method resolvedGetName = emoticon.getMethod("getName");
            Method resolvedGetSource = emoticon.getMethod("getSource");
            Method resolvedMakeEmoteStyle = compat.getMethod("makeEmoteStyle", emoticon);
            Method resolvedRequestTexture = emoticon.getMethod("requestTexture");
            Method resolvedGetWidth = emoticon.getMethod("getWidth");
            Method resolvedGetHeight = emoticon.getMethod("getHeight");
            Method resolvedGetTexture = emoticon.getMethod("getTexture");
            Method resolvedGetLoadData = emoticon.getMethod("getLoadData");
            Method resolvedGetLoader = emoticon.getMethod("getLoader");

            // Publish the bridge only after every required method was resolved.
            // This avoids exposing a half-initialized bridge when Streamotes
            // changes one method or the hologram renderer starts very early.
            getEmotes = resolvedGetEmotes;
            isLoading = resolvedIsLoading;
            fromName = resolvedFromName;
            getName = resolvedGetName;
            getSource = resolvedGetSource;
            makeEmoteStyle = resolvedMakeEmoteStyle;
            requestTexture = resolvedRequestTexture;
            getWidth = resolvedGetWidth;
            getHeight = resolvedGetHeight;
            getTexture = resolvedGetTexture;
            getLoadData = resolvedGetLoadData;
            getLoader = resolvedGetLoader;
            return true;
        } catch (ReflectiveOperationException exception) {
            Emipokemon.LOGGER.warn("Streamotes public emote API was not found; picker disabled", exception);
            return false;
        }
    }

    static List<Object> emotes() {
        Method method = getEmotes;
        if (method == null) return List.of();
        try {
            Object value = method.invoke(null);
            if (value instanceof Collection<?> collection) return List.copyOf(collection);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Emipokemon.LOGGER.warn("Could not read Streamotes catalog", exception);
        }
        return List.of();
    }

    static EmoteEntry lookup(String name) {
        Method method = fromName;
        if (method == null || name == null || name.isBlank()) return null;
        try {
            Object emoticon = method.invoke(null, name);
            if (emoticon == null) return null;
            String resolvedName = StreamotesBridge.name(emoticon);
            if (resolvedName == null || resolvedName.isBlank()) return null;
            return new EmoteEntry(emoticon, resolvedName, source(emoticon), style(emoticon));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    static boolean isLoading() {
        Method method = isLoading;
        if (method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    static String name(Object emoticon) {
        return invokeString(getName, emoticon);
    }

    static String source(Object emoticon) {
        return invokeString(getSource, emoticon);
    }

    static Style style(Object emoticon) {
        Method method = makeEmoteStyle;
        if (method == null) return Style.EMPTY;
        try {
            Object value = method.invoke(null, emoticon);
            return value instanceof Style style ? style : Style.EMPTY;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Style.EMPTY;
        }
    }

    static void requestPreview(Object emoticon) {
        invoke(requestTexture, emoticon);
    }

    static boolean isPreviewDecoded(Object emoticon) {
        return invokeInt(getWidth, emoticon) > 0 && invokeInt(getHeight, emoticon) > 0;
    }

    static void uploadPreview(Object emoticon) {
        invoke(getTexture, emoticon);
    }

    static Optional<EmoteCacheIdentity> cacheIdentity(Object emoticon) {
        Object loader = invoke(getLoader, emoticon);
        Object loadData = invoke(getLoadData, emoticon);
        if (loader == null || loadData == null) return Optional.empty();

        String provider = providerFromLoader(loader.getClass().getName());
        String id = cacheId(loadData);
        if (provider.isEmpty() || !isSafeCachePart(id)) return Optional.empty();
        return Optional.of(new EmoteCacheIdentity(provider, id));
    }

    private static String providerFromLoader(String loaderClassName) {
        String normalized = loaderClassName.toLowerCase(Locale.ROOT);
        if (normalized.contains("x7tv")) return "7tv";
        if (normalized.contains("bttv")) return "bttv";
        if (normalized.contains("ffz")) return "ffz";
        if (normalized.contains("twitch")) return "twitch";
        return "";
    }

    private static String cacheId(Object loadData) {
        if (loadData instanceof String string) return string;

        for (String methodName : List.of("getLeft", "method_15442")) {
            try {
                Method method = loadData.getClass().getMethod(methodName);
                Object value = method.invoke(loadData);
                if (value instanceof String string) return string;
            } catch (ReflectiveOperationException ignored) {
                // Pair method names differ between development and production mappings.
            }
        }

        for (Field field : loadData.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(loadData);
                if (value instanceof String string) return string;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Unknown provider payload: simply let Streamotes manage it normally.
            }
        }
        return "";
    }

    private static boolean isSafeCachePart(String value) {
        if (value == null || value.isBlank()) return false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_' && character != '-') return false;
        }
        return true;
    }

    private static Object invoke(Method method, Object target) {
        if (method == null) return null;
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static int invokeInt(Method method, Object target) {
        Object value = invoke(method, target);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String invokeString(Method method, Object target) {
        if (method == null) return "";
        try {
            Object value = method.invoke(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }

    record EmoteCacheIdentity(String provider, String id) {
        String key() {
            return provider + ":" + id;
        }

        String fileName() {
            return provider + "-" + id + ".png";
        }
    }
}
