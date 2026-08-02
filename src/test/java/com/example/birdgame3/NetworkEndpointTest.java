package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkEndpointTest {
    @Test
    void parsesHostNamesAndOptionalPorts() {
        assertEquals(new NetworkEndpoint("games.example.com", 28999),
                NetworkEndpoint.parse("games.example.com", 28999));
        assertEquals(new NetworkEndpoint("203.0.113.7", 30100),
                NetworkEndpoint.parse("203.0.113.7:30100", 28999));
    }

    @Test
    void parsesIpv6WithAndWithoutExplicitPort() {
        assertEquals(new NetworkEndpoint("2001:db8::7", 28999),
                NetworkEndpoint.parse("2001:db8::7", 28999));
        NetworkEndpoint endpoint = NetworkEndpoint.parse("[2001:db8::7]:30100", 28999);
        assertEquals(new NetworkEndpoint("2001:db8::7", 30100), endpoint);
        assertEquals("[2001:db8::7]:30100", endpoint.display());
    }

    @Test
    void rejectsUrlsAndInvalidPorts() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkEndpoint.parse("https://games.example.com", 28999));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkEndpoint.parse("games.example.com:0", 28999));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkEndpoint.parse("games.example.com:70000", 28999));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkEndpoint.parse("games.example.com:", 28999));
    }
}
