package cz.eida.minecraft.sipauth.ipmatcher;

import java.util.List;

/**
 * Generic IP address matcher.
 */
public class IPMatcher implements IIPMatcher {

    IIPMatcher instance;

    /**
     * Create a generic matcher.
     *
     * @param addressString input host or network address string
     */
    public IPMatcher(String addressString) {

        // any string containing a dot will be handled as IPv4 address
        if (addressString.contains(".")) {
            this.instance = new IPv4Matcher(addressString);
            return;
        }

        // IPv6 contains colons
        if (addressString.contains(":")) {
            this.instance = new IPv6Matcher(addressString);
        }

    }

    /**
     * Validates input as host or network address.
     *
     * @param address address to validate with an optional network mask
     * @return address is valid
     */
    public static boolean isValid(String address) {

        // IPv4
        if (address.contains(".")) {
            return IPv4Matcher.isValid(address);
        }

        // IPv6
        if (address.contains(":")) {
            return IPv6Matcher.isValid(address);
        }

        // fallback - invalid input
        return false;
    }

    @Override
    public String getSingleHostMask() {
        return instance.getSingleHostMask();
    }

    /**
     * Matches current address with given network address.
     *
     * @param network network address in valid format
     * @return address belongs to given network
     */
    @Override
    public boolean match(String network) {
        return instance.match(network);
    }

    /**
     * Matches current address with given array of network addresses.
     *
     * @param networks array of networks in valid format
     * @return address belongs to one or more given networks
     */
    @Override
    public boolean matchAny(String[] networks) {
        return instance.matchAny(networks);
    }

    /**
     * Matches current address with given list of network addresses.
     *
     * @param networks list of networks in valid format
     * @return address belongs to one or more given networks
     */
    @Override
    public boolean matchAny(List<String> networks) {
        return instance.matchAny(networks);
    }
}
