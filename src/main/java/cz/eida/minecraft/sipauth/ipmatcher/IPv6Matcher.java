package cz.eida.minecraft.sipauth.ipmatcher;

import java.util.List;

/**
 * Simple IPv6 address/network matcher.
 */
public class IPv6Matcher implements IIPMatcher {

    /**
     * IPv6 address
     */
    private int[] ip6;

    /**
     * Converts host IPv6 address to numeric array.
     *
     * @param ipString IPv6 host address in valid format.
     */
    public IPv6Matcher(String ipString) {
        this.ip6 = parseBase(ipString);
    }

    /**
     * Parse given string to IPv6 hextets.
     *
     * @param ipString IPv6 address
     * @return array of numeric hextets
     */
    private static int[] parseBase(String ipString) {

        int[] base = {
                0x0000, 0x0000, 0x0000, 0x0000,
                0x0000, 0x0000, 0x0000, 0x0000
        };
        String[] splitIP;

        if (ipString.contains("/")) {
            splitIP = ipString.split("\\/")[0].split(":");
        } else {
            splitIP = ipString.split(":");
        }

        int i;
        for (i = 0; i < splitIP.length; i++) {

            if (splitIP[i].length() == 0) {
                base[i] = 0x0;
                break;
            } else {
                base[i] = Integer.parseInt(splitIP[i], 16);
            }
        }

        if (splitIP.length != 8) {
            int r;
            for (r = 0; r < (splitIP.length - i); r++) {

                if (splitIP[(splitIP.length - 1) - r].length() == 0) {
                    break;
                }

                base[7 - r] = Integer.parseInt(splitIP[(splitIP.length - 1) - r], 16);
            }
        }

        return base;
    }

    /**
     * Parse given string to binary prefix mask.
     *
     * @param ipString IPv6 address
     * @return array of prefix mask hextets
     */
    private static int[] parsePrefix(String ipString) {

        int[] prefixmask = new int[]{
                0x0, 0x0, 0x0, 0x0,
                0x0, 0x0, 0x0, 0x0
        };

        if (ipString.contains("/")) {

            int bits = Integer.parseInt(ipString.split("\\/")[1]);
            int hextetsFull = bits / 16;
            int hextetsPartial = bits % 16;

            if (hextetsPartial != 0) {
                prefixmask[hextetsFull] = ((0xFFFF) << (16 - hextetsPartial)) & 0xFFFF0000;
            }

            int i;
            for (i = 0; i < hextetsFull; i++) {
                prefixmask[0] = 0xFFFF;
            }

        } else {
            // no mask; assume /128 - single host
            prefixmask = new int[]{
                    0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF,
                    0xFFFF, 0xFFFF, 0xFFFF, 0xFFFF
            };
        }

        return prefixmask;
    }

    // TODO create regexp
    public static boolean isValid(String address) {
        return true;
    }

    /**
     * IPv6 single host prefix - 128 bits.
     *
     * @return "128"
     */
    public String getSingleHostMask() {
        return "128";
    }

    /**
     * Matches IPv6 with given network.
     *
     * @param network IPv6 network address in valid format
     * @return IPv6 belongs to this network
     */
    @Override
    public boolean match(String network) {

        int[] base = parseBase(network);
        int[] prefix = parsePrefix(network);

        int i;
        for (i = 0; i < 8; i++) {
            if ((base[i] & prefix[i]) != (this.ip6[i] & prefix[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Matches IPv6 with given networks.
     *
     * @param networks array of IPv6 networks in valid format
     * @return IP belongs to any of given network
     */
    @Override
    public boolean matchAny(String[] networks) {
        for (String network : networks) {
            if (match(network)) return true;
        }
        return false;
    }

    /**
     * Matches IPv6 with given networks.
     *
     * @param networks list of IPv6 networks in valid format
     * @return IP belongs to any of given network
     */
    @Override
    public boolean matchAny(List<String> networks) {
        for (String network : networks) {
            if (match(network)) return true;
        }
        return false;
    }

    /**
     * Print current IPv6.
     *
     * @return loaded and parsed IPv6 address
     */
    @Override
    public String toString() {
        return toString(false);
    }

    // TODO 🦤 compress zeros to ::
    public String toString(boolean compressed) {
        StringBuilder sb = new StringBuilder();

        int i;
        for (i = 0; i < 8; i++) {
            sb.append(String.format("%04x", this.ip6[i]));
            sb.append((i < 7) ? ":" : "");
        }

        return sb.toString();
    }
}