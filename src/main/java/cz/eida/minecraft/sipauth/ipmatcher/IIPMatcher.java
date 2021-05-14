package cz.eida.minecraft.sipauth.ipmatcher;

import java.util.List;

/**
 * Universal IP address matcher interface.
 */
public interface IIPMatcher {

    /**
     * Matches current address with given network address.
     *
     * @param network network address in valid format
     * @return address belongs to given network
     */
    boolean match(String network);

    /**
     * Matches current address with given array of network addresses.
     *
     * @param networks array of networks in valid format
     * @return address belongs to one or more given networks
     */
    boolean matchAny(String[] networks);

    /**
     * Matches current address with given list of network addresses.
     *
     * @param networks list of networks in valid format
     * @return address belongs to one or more given networks
     */
    boolean matchAny(List<String> networks);
}
