package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NameTagHelper {

    private NameTagHelper() {}

    public record BotSkin(String texture, String signature) {}

    public record NickData(String nick, @Nullable String plainNick, @Nullable BotSkin skin) {

        public boolean canRename() {
            return plainNick != null && !plainNick.isEmpty();
        }
    }

    @Nullable
    private static Plugin nameTagPlugin() {
        Plugin p = Bukkit.getPluginManager().getPlugin("NameTag");
        return (p != null && p.isEnabled()) ? p : null;
    }

    @Nullable
    private static Object getApi() {
        try {
            Plugin nt = nameTagPlugin();
            if (nt == null) return null;
            Class<?> cls =
                    nt.getClass().getClassLoader().loadClass("gg.lode.nametagapi.NameTagAPI");
            return cls.getMethod("getApi").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    private static Class<?> getApiIface() {
        try {
            Plugin nt = nameTagPlugin();
            if (nt == null) return null;
            return nt.getClass().getClassLoader().loadClass("gg.lode.nametagapi.INameTagAPI");
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isAvailable() {
        return nameTagPlugin() != null;
    }

    public static boolean hasNick(UUID uuid) {
        return getNick(uuid) != null;
    }

    @Nullable
    public static String getNick(UUID uuid) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null) return null;
            return (String) iface.getMethod("getNick", UUID.class).invoke(api, uuid);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static String getNick(Player player) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null) return null;
            return (String) iface.getMethod("getNick", Player.class).invoke(api, player);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static BotSkin getSkin(UUID uuid) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null) return null;
            Object skin = iface.getMethod("getSkin", UUID.class).invoke(api, uuid);
            if (skin == null) return null;

            return new BotSkin(
                    (String) skin.getClass().getMethod("texture").invoke(skin),
                    (String) skin.getClass().getMethod("signature").invoke(skin));
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static NickData clearBotFromCache(UUID botUuid) {
        try {
            Plugin nt = nameTagPlugin();
            if (nt == null) return null;
            Field f = nt.getClass().getDeclaredField("playerCache");
            f.setAccessible(true);
            ConcurrentHashMap<UUID, ?> cache = (ConcurrentHashMap<UUID, ?>) f.get(nt);
            if (cache == null) return null;

            Object nickPlayer = cache.get(botUuid);
            if (nickPlayer != null) {
                try {
                    boolean hasNick =
                            (boolean) nickPlayer.getClass().getMethod("hasNick").invoke(nickPlayer);
                    if (hasNick) {

                        String rawNick = null;
                        try {
                            rawNick =
                                    (String)
                                            nickPlayer
                                                    .getClass()
                                                    .getMethod("getNickname")
                                                    .invoke(nickPlayer);
                        } catch (Throwable ignored) {
                        }

                        BotSkin skin = null;
                        try {
                            String tex =
                                    (String)
                                            nickPlayer
                                                    .getClass()
                                                    .getMethod("getTexture")
                                                    .invoke(nickPlayer);
                            String sig =
                                    (String)
                                            nickPlayer
                                                    .getClass()
                                                    .getMethod("getSignature")
                                                    .invoke(nickPlayer);
                            if (tex != null && !tex.isBlank()) skin = new BotSkin(tex, sig);
                        } catch (Throwable ignored) {
                        }

                        String plain = rawNick != null ? stripFormattingForRename(rawNick) : null;
                        me.bill.fakePlayerPlugin.config.Config.debugStartup(
                                "[NameTag] Bot "
                                        + botUuid
                                        + " has nick '"
                                        + rawNick
                                        + "' (plain='"
                                        + plain
                                        + "') — preserving cache entry.");
                        return new NickData(rawNick != null ? rawNick : "", plain, skin);
                    }
                } catch (Throwable ignored) {
                }
            }

            cache.remove(botUuid);
            me.bill.fakePlayerPlugin.config.Config.debugStartup(
                    "[NameTag] Removed bot "
                            + botUuid
                            + " from NameTag playerCache (no nick set).");
        } catch (Throwable t) {
            me.bill.fakePlayerPlugin.config.Config.debugStartup(
                    "[NameTag] clearBotFromCache skipped for " + botUuid + ": " + t.getMessage());
        }
        return null;
    }

    public static void resetBotNickname(Player bot) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null) return;
            iface.getMethod("resetNickname", Player.class).invoke(api, bot);
        } catch (Throwable t) {
            FppLogger.warn("[NameTag] resetBotNickname failed: " + t.getMessage());
        }
    }

    public static void applySkinViaNameTag(Player player, BotSkin skin) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null || skin == null) return;
            iface.getMethod(
                            "setSkinFromTextureAndSignature",
                            Player.class,
                            String.class,
                            String.class)
                    .invoke(api, player, skin.texture(), skin.signature());
        } catch (Throwable t) {
            me.bill.fakePlayerPlugin.config.Config.debugStartup(
                    "[NameTag] applySkinViaNameTag failed: " + t.getMessage());
        }
    }

    @Nullable
    public static String stripFormattingForRename(String nick) {
        if (nick == null) return null;

        String s = nick.replaceAll("<[^>]*>", "");

        s = s.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "");

        s = s.replaceAll("[^a-zA-Z0-9_]", "");

        if (s.length() > 16) s = s.substring(0, 16);
        return s.isEmpty() ? null : s;
    }

    public static boolean isNickUsedByRealPlayer(String candidateName, FakePlayerManager manager) {
        try {
            Object api = getApi();
            Class<?> iface = getApiIface();
            if (api == null || iface == null) return false;
            Method getNickByPlayer = iface.getMethod("getNick", Player.class);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (manager.getByUuid(online.getUniqueId()) != null) continue;
                String nick = (String) getNickByPlayer.invoke(api, online);
                if (nick != null && nick.equalsIgnoreCase(candidateName)) return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getDisplayNameForPlayer(Player player) {
        String nick = getNick(player);
        return nick != null ? nick : player.getName();
    }
}
