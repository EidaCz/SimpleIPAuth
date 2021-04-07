package cz.eida.minecraft;

import cz.eida.minecraft.sipauth.IPv4Matcher;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestIP {

    @Test
    public void ipMatch() {

        IPv4Matcher ipmatcher = new IPv4Matcher("10.5.5.5");

        assertTrue(ipmatcher.match("10.5.5.5"));
        assertFalse(ipmatcher.match("10.5.5.7"));

        assertTrue(ipmatcher.match("10.5.5.0/24"));
        assertTrue(ipmatcher.matchAny(new String[]{"10.5.5.5/32", "10.5.0.0/16", "10.0.0.0/8", "10.5.0.0/9"}));

        assertFalse(ipmatcher.match("172.29.0.0/16"));
        assertFalse(ipmatcher.matchAny(new String[]{"10.5.6.0/24", "10.5.6.0/25"}));
    }

    @Test
    public void ipParse() {
        assertTrue(IPv4Matcher.isValid("127.0.0.1"));
        assertTrue(IPv4Matcher.isValid("127.0.0.1/32"));
        assertTrue(IPv4Matcher.isValid("127.0.0.1/8"));
        assertTrue(IPv4Matcher.isValid("192.168.12.0/25"));
        assertFalse(IPv4Matcher.isValid("127.0.0.1/33"));
        assertFalse(IPv4Matcher.isValid("127.0.384.1"));
        assertFalse(IPv4Matcher.isValid("256.192.168.1/15"));
    }

}
