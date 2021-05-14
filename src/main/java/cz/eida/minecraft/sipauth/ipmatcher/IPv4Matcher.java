package cz.eida.minecraft.sipauth.ipmatcher;

import java.util.List;

/**
 * Simple IPv4 address/network matcher.
 */
public class IPv4Matcher implements IIPMatcher {

    /**
     * IPv4 address
     */
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
            this.ip += Long.parseLong(splitIP[i]) << (24 - (8 * i));
        }
    }

    /**
     * Validates input as IPv4 address.
     *
     * @param address IPv4 with optional CIDR mask.
     * @return given address is valid
     */
    public static boolean isValid(String address) {
        return address.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\/([0-9]|[1-2][0-9]|3[0-2]))?$");
    }

    /**
     * IPv4 single host mask - 32 bits.
     *
     * @return "32"
     */
    public String getSingleHostMask() {
        return "32";
    }

    /**
     * Matches IP with given network.
     *
     * @param ipNet network in CIDR format A.B.C.D/M
     * @return IP belongs to this network
     */
    @Override
    public boolean match(String ipNet) {

        try {
            long mask = 0xFFFFFFFFL << (32 - Integer.parseInt(ipNet.split("\\/")[1]));
            long base = 0x00000000L;

            String[] splitBase = ipNet.split("\\/")[0].split("\\.");

            int i;
            for (i = 0; i < 4; i++) {
                base += Long.parseLong(splitBase[i]) << (24 - (8 * i));
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
    @Override
    public boolean matchAny(String[] ipNets) {
        for (String cidr : ipNets) {
            if (match(cidr)) return true;
        }
        return false;
    }

    /**
     * Matches IP with given networks.
     *
     * @param ipNets list of networks in CIDR format A.B.C.D/M
     * @return IP belongs to some of given network
     */
    @Override
    public boolean matchAny(List<String> ipNets) {
        for (String cidr : ipNets) {
            if (match(cidr)) return true;
        }
        return false;
    }
}
