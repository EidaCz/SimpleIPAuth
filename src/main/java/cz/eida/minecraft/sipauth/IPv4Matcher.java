package cz.eida.minecraft.sipauth;

import java.util.List;

/**
 * Simple IPv4 address/network matcher.
 */
public class IPv4Matcher {

    /** IPv4 address */
    private long ip = 0x00000000L;

    /**
     * Converts IPv4 address string to long.
     *
     * @param ipString single IPv4 in A.B.C.D format.
     */
    public IPv4Matcher(String ipString) {

        String[] splitIP = ipString.split("\\.");

        int i;
        for (i = 0; i < 4; i++) {
            this.ip += Integer.parseInt(splitIP[i]) << (24 - (8 * i));
        }
    }

    /**
     * Matches IP with given network.
     *
     * @param ipNet network in CIDR format A.B.C.D/M
     * @return IP belongs to this network
     */
    public boolean match(String ipNet) {

        try {
            long mask = 0xFFFFFFFFL << (32 - Integer.parseInt(ipNet.split("\\/")[1]));
            long base = 0x00000000L;

            String[] splitBase = ipNet.split("\\/")[0].split("\\.");

            int i;
            for (i = 0; i < 4; i++) {
                base += Integer.parseInt(splitBase[i]) << (24 - (8 * i));
            }

            return ((base & mask) == (this.ip & mask));

        } catch (ArrayIndexOutOfBoundsException ex) {
            return match(ipNet + "/32");
        }
    }

    /**
     * Matches IP with given networks.
     *
     * @param ipNets array of networks in CIDR format A.B.C.D/M
     * @return IP belongs to some of given network
     */
    public boolean matchAny(String[] ipNets) {
        for (String cidr : ipNets) {
            if (match(cidr)) return true;
        }
        return false;
    }

    /**
     * Matches IP with given networks.
     *
     * @param ipNets array of networks in CIDR format A.B.C.D/M
     * @return IP belongs to some of given network
     */
    public boolean matchAny(List<String> ipNets) {
        for (String cidr : ipNets) {
            if (match(cidr)) return true;
        }
        return false;
    }

    /**
     * Validates input as IPv4 address.
     *
     * @param ip IPv4 with optional CIDR mask.
     * @return given address is valid
     */
    public static boolean isValid(String ip) {
        return ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\/([0-9]|[1-2][0-9]|3[0-2]))?$");
    }
}
