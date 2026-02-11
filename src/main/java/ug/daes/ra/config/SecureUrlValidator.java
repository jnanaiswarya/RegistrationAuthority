package ug.daes.ra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

@Component
public class SecureUrlValidator {

    @Value("${security.allowed-hosts}")
    private List<String> allowedHosts;

    public void validate(String url) {

        try {
            URI uri = new URI(url);

            // Allow only HTTP/HTTPS
            String scheme = uri.getScheme();
            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http") &&
                            !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Invalid scheme");
            }

            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null) {
                throw new IllegalArgumentException("Invalid host");
            }

            String hostWithPort = (port == -1) ? host : host + ":" + port;

            if (!allowedHosts.contains(hostWithPort) &&
                    !allowedHosts.contains(host)) {
                throw new IllegalArgumentException("Host not allowed");
            }

            // DNS resolution check
            InetAddress address = InetAddress.getByName(host);

            if (address.isAnyLocalAddress() ||
                    address.isLoopbackAddress() ||
                    address.isSiteLocalAddress()) {

                throw new IllegalArgumentException("Internal access blocked");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Unsafe URL");
        }
    }
}
