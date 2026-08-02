package com.example.birdgame3;

/**
 * A validated direct-connect network endpoint.
 *
 * <p>Accepted forms are {@code host}, {@code host:port}, an unbracketed IPv6
 * literal using the default port, and {@code [IPv6]:port}.</p>
 */
record NetworkEndpoint(String host, int port) {
    NetworkEndpoint {
        host = validateHost(host);
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
    }

    static NetworkEndpoint parse(String value, int defaultPort) {
        validatePort(defaultPort);
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter a host name or IP address.");
        }
        if (text.length() > 300 || containsEndpointSeparator(text)) {
            throw new IllegalArgumentException("Enter only a host name or IP address, optionally followed by :port.");
        }

        if (text.charAt(0) == '[') {
            int closing = text.indexOf(']');
            if (closing < 0) {
                throw new IllegalArgumentException("IPv6 addresses with a port must use [address]:port.");
            }
            String host = text.substring(1, closing).trim();
            String suffix = text.substring(closing + 1).trim();
            int port = defaultPort;
            if (!suffix.isEmpty()) {
                if (!suffix.startsWith(":") || suffix.length() == 1) {
                    throw new IllegalArgumentException("Use [IPv6-address]:port.");
                }
                port = parsePort(suffix.substring(1));
            }
            return new NetworkEndpoint(validateHost(host), port);
        }

        long colonCount = text.chars().filter(ch -> ch == ':').count();
        if (colonCount == 1) {
            int colon = text.lastIndexOf(':');
            String host = text.substring(0, colon).trim();
            String portText = text.substring(colon + 1).trim();
            if (portText.isEmpty()) {
                throw new IllegalArgumentException("Enter a port after the colon.");
            }
            return new NetworkEndpoint(validateHost(host), parsePort(portText));
        }

        return new NetworkEndpoint(validateHost(text), defaultPort);
    }

    static int parsePort(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter a TCP port.");
        }
        try {
            int port = Integer.parseInt(text);
            validatePort(port);
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port must be a number between 1 and 65535.");
        }
    }

    String display() {
        String displayHost = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
        return displayHost + ":" + port;
    }

    private static String validateHost(String value) {
        String host = value == null ? "" : value.trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Enter a host name or IP address.");
        }
        if (host.chars().anyMatch(Character::isWhitespace) || containsEndpointSeparator(host)) {
            throw new IllegalArgumentException("The host name or IP address is not valid.");
        }
        return host;
    }

    private static boolean containsEndpointSeparator(String value) {
        return value.contains("/") || value.contains("\\") || value.contains("?") || value.contains("#")
                || value.contains("@") || value.contains("://");
    }

    private static void validatePort(int port) {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
    }
}
