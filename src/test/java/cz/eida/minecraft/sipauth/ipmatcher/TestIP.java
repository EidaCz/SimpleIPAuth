package cz.eida.minecraft.sipauth.ipmatcher;

import org.junit.Test;

import static org.junit.Assert.*;

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
        assertTrue(ipv6Matcher.match("a000::/4"));
        assertTrue(ipv6Matcher.match("a001::/1"));
        assertFalse(ipv6Matcher.match("cafe::/64"));
        assertFalse(ipv6Matcher.match("a000::/5"));
        assertFalse(ipv6Matcher.match("abcd:0200::/23"));
        assertFalse(ipv6Matcher.match("abcd:0100:000a:0000:0000:0000:0000:00d2/128"));

        assertTrue(ipv6Matcher.matchAny(new String[]{"::1", "fe80:1a1b:2c2d::/64", "abcd:100:a::d1/128"}));
    }

    @Test
    public void ipv6Parse() {
        IPv6Matcher ipv6Matcher = new IPv6Matcher("abcd:100:a::d1");
        assertEquals("abcd:100:a::d1", ipv6Matcher.toString(true));

        ipv6Matcher = new IPv6Matcher("fc00:0::10:100");
        assertEquals("fc00::10:100", ipv6Matcher.toString(true));

        assertTrue(IPv6Matcher.isValid("fe00:100::/64"));
        assertTrue(IPv6Matcher.isValid("0:0:0:0:0:0:0:1/128"));
        assertTrue(IPv6Matcher.isValid("2001:db8:123:abcd::12/99"));
        assertTrue(IPv6Matcher.isValid("2001:db8:123:abcd::/99"));
        assertFalse(IPv6Matcher.isValid("127.0.0.1/32"));
        assertFalse(IPv6Matcher.isValid("fe80:932::x11"));
        assertFalse(IPv6Matcher.isValid("8:8:8:8:8:8:8:8:8"));
    }

    @Test
    public void ipFamilyCheck() {
        IPMatcher matcher = new IPMatcher("fc00:1234::100/128");
        assertTrue(matcher.match("fc00:1234::/64"));

        matcher = new IPMatcher("127.0.0.1");
        assertTrue(matcher.match("127.0.0.1/8"));
    }


}
