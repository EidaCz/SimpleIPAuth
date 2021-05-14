package cz.eida.minecraft;

import cz.eida.minecraft.sipauth.ipmatcher.IPMatcher;
import cz.eida.minecraft.sipauth.ipmatcher.IPv4Matcher;
import cz.eida.minecraft.sipauth.ipmatcher.IPv6Matcher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestIP {

    @Test
    public void ipv4Match() {

        IPv4Matcher ipmatcher = new IPv4Matcher("10.5.5.5");

        assertTrue(ipmatcher.match("10.5.5.5"));
        assertFalse(ipmatcher.match("10.5.5.7"));

        assertTrue(ipmatcher.match("10.5.5.0/24"));
        assertTrue(ipmatcher.matchAny(new String[]{"10.5.5.5/32", "10.5.0.0/16", "10.0.0.0/8", "10.5.0.0/9"}));

        assertFalse(ipmatcher.match("172.29.0.0/16"));
        assertFalse(ipmatcher.matchAny(new String[]{"10.5.6.0/24", "10.5.6.0/25"}));
    }

    @Test
    public void ipv4Parse() {
        assertTrue(IPv4Matcher.isValid("127.0.0.1"));
        assertTrue(IPv4Matcher.isValid("127.0.0.1/32"));
        assertTrue(IPv4Matcher.isValid("127.0.0.1/8"));
        assertTrue(IPv4Matcher.isValid("192.168.12.0/25"));
        assertFalse(IPv4Matcher.isValid("127.0.0.1/33"));
        assertFalse(IPv4Matcher.isValid("127.0.384.1"));
        assertFalse(IPv4Matcher.isValid("256.192.168.1/15"));
    }

    @Test
    public void ipv6Match() {

        IPv6Matcher ipv6Matcher = new IPv6Matcher("abcd:100:a::d1");

        assertTrue(ipv6Matcher.match("::/0"));
        assertTrue(ipv6Matcher.match("abcd:100:a::/64"));
        assertTrue(ipv6Matcher.match("abcd:100:a::d1/128"));
        assertTrue(ipv6Matcher.match("abcd:0100:000a:0000:0000:0000:0000:00d1/128"));
        assertTrue(ipv6Matcher.match("a000::/8"));
        assertTrue(ipv6Matcher.match("a001::/1"));
        assertFalse(ipv6Matcher.match("cafe::/64"));
    }

    @Test
    public void ipv6Parse() {
        // TODO
    }

    @Test
    public void ipFamilyCheck() {
        IPMatcher matcher = new IPMatcher("fc00:1234::100/128");
        assertTrue(matcher.match("fc00:1234::/64"));

        matcher = new IPMatcher("127.0.0.1");
        assertTrue(matcher.match("127.0.0.1/8"));
    }


}
