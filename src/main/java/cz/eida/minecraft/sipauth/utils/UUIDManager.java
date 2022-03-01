package cz.eida.minecraft.sipauth.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * UUID handling helper.
 *
 * @author EidaCz
 */
public class UUIDManager {

    // UUID v4 format pattern
    private static final Pattern UUID_FULL = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private final Plugin plugin;
    private File cacheFile;
    private YamlConfiguration cachedUUIDs;

    public UUIDManager(Plugin plugin) throws IOException {
        this.plugin = plugin;
        this.reloadCache();
    }

    /**
     * Online mode UUID fetcher.
     * Case-insensitive.
     *
     * @param nickName registered player nickname
     * @return Mojang-registered player UUID
     * @throws IOException online fetch failed
     */
    protected static UUID fetchPlayerUUID(String nickName) throws IOException {

        String apiURL = "https://api.mojang.com/users/profiles/minecraft/" + nickName;

        InputStream isJSON = new URL(apiURL).openStream();
        BufferedReader rdJSON = new BufferedReader(new InputStreamReader(isJSON, StandardCharsets.UTF_8));

        StringBuilder sbJSON = new StringBuilder();
        int cnt;
        while ((cnt = rdJSON.read()) != -1) {
            sbJSON.append((char) cnt);
        }

        JSONObject json = new JSONObject(sbJSON.toString());
        isJSON.close();

        String uuidString = json.getString("id");

        return UUID.fromString(UUID_FULL.matcher(uuidString.replace("-", "")).replaceAll("$1-$2-$3-$4-$5"));
    }

    /**
     * Offline/insecure server mode UUID generator.
     * Case-sensitive.
     *
     * @param nickName player nickname
     * @return generated offline UUID
     */
    protected static UUID generatePlayerUUID(String nickName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickName).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reloads configuration file.
     *
     * @throws IOException cannot create file
     */
    public void reloadCache() throws IOException {
        this.cacheFile = new File(this.plugin.getDataFolder(), "uuid-cache.yml");

        if (!cacheFile.exists()) {
            this.cacheFile.createNewFile();
        }

        this.cachedUUIDs = YamlConfiguration.loadConfiguration(this.cacheFile);
    }

    /**
     * Converts offline player nickname to UUID.
     *
     * @param nickName offline player nickname
     * @return UUID of given offline player
     * @throws IOException online-fetch failed
     */
    public UUID getOfflinePlayerUUID(String nickName) throws IOException {
        return (this.plugin.getServer().getOnlineMode()) ? getOnlineModePlayerUUID(nickName) : getOfflineModePlayerUUID(nickName);
    }

    /**
     * Generates player UUID based on given nickname.
     * For use in offline/insecure server mode.
     *
     * @param nickName case-sensitive player nickname
     * @return UUID of given nickname
     */
    private UUID getOfflineModePlayerUUID(String nickName) {
        return generatePlayerUUID(nickName);
    }

    /**
     * Performs cached lookup for Mojang-registered UUID of given player name.
     *
     * @param nickName case-insensitive player nickname
     * @return online UUID of player
     */
    private UUID getOnlineModePlayerUUID(String nickName) throws IOException {
        UUID result = cacheLookup(nickName);
        if (result != null) return result;

        result = fetchPlayerUUID(nickName);

        // save result to cache file
        cacheSave(nickName, result);

        return result;
    }

    /**
     * Save results to cache file.
     *
     * @param nickName player nickname
     * @param uuid     UUID player UUID
     */
    private void cacheSave(String nickName, UUID uuid) throws IOException {
        this.cachedUUIDs.set(nickName, uuid.toString());
        this.cachedUUIDs.save(this.cacheFile);
    }

    /**
     * Look for UUID in cache file.
     *
     * @param nickName player nickname
     * @return cached UUID
     */
    private UUID cacheLookup(String nickName) {
        String uuidString = this.cachedUUIDs.getString(nickName);
        return (uuidString != null) ? UUID.fromString(Objects.requireNonNull(this.cachedUUIDs.getString(nickName))) : null;
    }
}
