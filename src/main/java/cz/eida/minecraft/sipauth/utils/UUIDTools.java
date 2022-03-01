package cz.eida.minecraft.sipauth.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * UUID handling helper.
 *
 * @author Eida_cz
 */
public class UUIDTools {

    private static final Pattern UUID_FULL = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    /**
     * Generates player UUID based on given nickname.
     * For use in offline/insecure server mode.
     *
     * @param nickName case-sensitive player nickname
     * @return UUID of given nickname
     */
    public static UUID getOfflineModePlayerUUID(String nickName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickName).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Performs lookup for Mojang-registered UUID of given player name.
     *
     * @param nickName case-insensitive player nickname
     * @return online UUID of player
     */
    public static UUID getOnlineModePlayerUUID(String nickName) throws IOException {

        UUID result;
        String apiURL = "https://api.mojang.com/users/profiles/minecraft/" + nickName;

        //try {
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

            result = UUID.fromString(UUID_FULL.matcher(uuidString.replace("-", "")).replaceAll("$1-$2-$3-$4-$5"));

        /*} catch (Exception e) {
            // TODO catch
        }*/

        return result;
    }
}
