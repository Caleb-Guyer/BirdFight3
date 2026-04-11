package com.example.birdgame3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UpnpPortMapper {
    private static final InetSocketAddress SSDP_ADDRESS = new InetSocketAddress("239.255.255.250", 1900);
    private static final String[] SEARCH_TARGETS = {
            "urn:schemas-upnp-org:service:WANIPConnection:1",
            "urn:schemas-upnp-org:service:WANPPPConnection:1",
            "urn:schemas-upnp-org:device:InternetGatewayDevice:1"
    };
    private static final Pattern SERVICE_BLOCK_PATTERN = Pattern.compile("(?is)<service>.*?</service>");

    record PortMappingLease(GatewayService service, int externalPort, String protocol) {
        void close() {
            if (service == null) return;
            try {
                service.deletePortMapping(externalPort, protocol == null ? "TCP" : protocol);
            } catch (IOException ignored) {
            }
        }
    }

    record PortMappingResult(boolean mapped, String externalAddress, String statusMessage, PortMappingLease lease) {
    }

    static PortMappingResult openTcpPort(int port, String internalClient, String description) {
        String client = internalClient == null ? "" : internalClient.trim();
        if (client.isBlank()) {
            return new PortMappingResult(false, "", "No local network address was available for UPnP port mapping.", null);
        }

        String externalAddress = "";
        String status = "Router did not expose a usable UPnP gateway.";
        for (URI location : discoverGatewayDescriptions()) {
            try {
                GatewayService service = discoverGatewayService(location);
                if (service == null) {
                    continue;
                }

                String gatewayExternalIp = service.getExternalIPAddress();
                if (gatewayExternalIp != null && !gatewayExternalIp.isBlank()) {
                    externalAddress = gatewayExternalIp.trim();
                }

                service.addPortMapping(port, client, port, "TCP", description == null ? "Bird Fight 3 Online" : description);
                return new PortMappingResult(true, externalAddress,
                        "Internet relay port opened automatically via UPnP.", new PortMappingLease(service, port, "TCP"));
            } catch (IOException e) {
                status = normalizeFailureMessage(e.getMessage(), status);
            }
        }

        return new PortMappingResult(false, externalAddress, status, null);
    }

    private static List<URI> discoverGatewayDescriptions() {
        Set<URI> locations = new LinkedHashSet<>();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setReuseAddress(true);
            socket.setSoTimeout(700);
            for (String searchTarget : SEARCH_TARGETS) {
                byte[] request = buildDiscoveryRequest(searchTarget);
                DatagramPacket packet = new DatagramPacket(request, request.length, SSDP_ADDRESS);
                socket.send(packet);

                long deadline = System.currentTimeMillis() + 1400L;
                while (System.currentTimeMillis() < deadline) {
                    byte[] responseBuffer = new byte[2048];
                    DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
                    try {
                        socket.receive(response);
                    } catch (SocketTimeoutException ignored) {
                        break;
                    }
                    URI location = parseLocationHeader(responseBuffer, response.getLength());
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return new ArrayList<>(locations);
    }

    private static byte[] buildDiscoveryRequest(String searchTarget) {
        String request = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: 239.255.255.250:1900\r\n"
                + "MAN: \"ssdp:discover\"\r\n"
                + "MX: 2\r\n"
                + "ST: " + searchTarget + "\r\n\r\n";
        return request.getBytes(StandardCharsets.US_ASCII);
    }

    private static URI parseLocationHeader(byte[] payload, int length) {
        String response = new String(payload, 0, Math.max(0, length), StandardCharsets.US_ASCII);
        for (String line : response.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon).trim();
            if (!"location".equalsIgnoreCase(key)) continue;
            String value = line.substring(colon + 1).trim();
            if (value.isBlank()) {
                return null;
            }
            try {
                return URI.create(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static GatewayService discoverGatewayService(URI descriptionUri) throws IOException {
        String xml = readText(descriptionUri.toURL());
        Matcher matcher = SERVICE_BLOCK_PATTERN.matcher(xml);
        while (matcher.find()) {
            String block = matcher.group();
            String serviceType = tagValue(block, "serviceType");
            if (serviceType == null || serviceType.isBlank()) {
                continue;
            }
            if (!serviceType.contains("WANIPConnection") && !serviceType.contains("WANPPPConnection")) {
                continue;
            }
            String controlUrlText = tagValue(block, "controlURL");
            if (controlUrlText == null || controlUrlText.isBlank()) {
                continue;
            }
            URI controlUri = descriptionUri.resolve(controlUrlText.trim());
            return new GatewayService(serviceType.trim(), controlUri);
        }
        return null;
    }

    private static String tagValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile("(?is)<" + Pattern.quote(tagName) + ">\\s*(.*?)\\s*</" + Pattern.quote(tagName) + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String normalizeFailureMessage(String rawMessage, String fallback) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isBlank()) {
            return fallback;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("401") || lower.contains("403")) {
            return "Router rejected automatic UPnP setup.";
        }
        if (lower.contains("718") || lower.contains("conflict")) {
            return "Router already has another mapping on TCP port " + OnlineRelayProtocol.DEFAULT_PORT + '.';
        }
        return message;
    }

    private static String readText(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("User-Agent", "BirdFight3/1.0");
        try (InputStream input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static final class GatewayService {
        private final String serviceType;
        private final URI controlUri;

        private GatewayService(String serviceType, URI controlUri) {
            this.serviceType = serviceType;
            this.controlUri = controlUri;
        }

        private void addPortMapping(int externalPort, String internalClient, int internalPort,
                                    String protocol, String description) throws IOException {
            String body = soapEnvelope("AddPortMapping",
                    "<NewRemoteHost></NewRemoteHost>"
                            + "<NewExternalPort>" + externalPort + "</NewExternalPort>"
                            + "<NewProtocol>" + protocol + "</NewProtocol>"
                            + "<NewInternalPort>" + internalPort + "</NewInternalPort>"
                            + "<NewInternalClient>" + escapeXml(internalClient) + "</NewInternalClient>"
                            + "<NewEnabled>1</NewEnabled>"
                            + "<NewPortMappingDescription>" + escapeXml(description) + "</NewPortMappingDescription>"
                            + "<NewLeaseDuration>0</NewLeaseDuration>");
            callSoap("AddPortMapping", body);
        }

        private void deletePortMapping(int externalPort, String protocol) throws IOException {
            String body = soapEnvelope("DeletePortMapping",
                    "<NewRemoteHost></NewRemoteHost>"
                            + "<NewExternalPort>" + externalPort + "</NewExternalPort>"
                            + "<NewProtocol>" + protocol + "</NewProtocol>");
            callSoap("DeletePortMapping", body);
        }

        private String getExternalIPAddress() throws IOException {
            String body = soapEnvelope("GetExternalIPAddress", "");
            String response = callSoap("GetExternalIPAddress", body);
            String ip = tagValue(response, "NewExternalIPAddress");
            return ip == null ? "" : ip.trim();
        }

        private String callSoap(String action, String body) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) controlUri.toURL().openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            connection.setRequestProperty("SOAPAction", '"' + serviceType + '#' + action + '"');
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }

            int status = connection.getResponseCode();
            InputStream responseStream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = responseStream == null
                    ? ""
                    : new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) {
                String fault = tagValue(response, "errorDescription");
                if (fault == null || fault.isBlank()) {
                    fault = "UPnP action " + action + " failed with HTTP " + status + '.';
                }
                throw new IOException(fault);
            }
            return response;
        }

        private String soapEnvelope(String action, String innerXml) {
            return "<?xml version=\"1.0\"?>"
                    + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                    + "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                    + "<s:Body>"
                    + "<u:" + action + " xmlns:u=\"" + serviceType + "\">"
                    + innerXml
                    + "</u:" + action + ">"
                    + "</s:Body>"
                    + "</s:Envelope>";
        }

        private String escapeXml(String value) {
            if (value == null) return "";
            return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
        }
    }

    private UpnpPortMapper() {
    }
}
