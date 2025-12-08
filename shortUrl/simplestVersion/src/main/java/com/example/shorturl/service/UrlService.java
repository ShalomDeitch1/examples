package com.example.shorturl.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UrlService {

    private final Map<String, String> urlMap = new ConcurrentHashMap<>();

    /**
     * Shortens a given URL.
     * If the URL already exists in the map (as a value), we could reuse it,
     * but for simplicity and speed (O(1)), we just generate a new ID.
     * 
     * @param originalUrl the full URL to shorten
     * @return the generated short ID
     */
    public String shorten(String originalUrl) {
        String shortId;
        do {
            shortId = generateShortId();
        } while (urlMap.containsKey(shortId));

        urlMap.put(shortId, originalUrl);
        return shortId;
    }

    /**
     * Resolves a short ID to the original URL.
     * 
     * @param shortId the ID to look up
     * @return Optional containing the URL if found
     */
    public Optional<String> resolve(String shortId) {
        return Optional.ofNullable(urlMap.get(shortId));
    }

    private String generateShortId() {
        UUID uuid = UUID.randomUUID();
        // Convert the UUID to a byte array.
        // A UUID consists of two long values (mostSignificantBits and
        // leastSignificantBits).
        // Each long is 8 bytes, so we need a 16-byte array.
        byte[] uuidBytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            uuidBytes[i] = (byte) (msb >>> (8 * (7 - i)));
            uuidBytes[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }

        // Encode the 16 bytes using URL-safe Base64.
        // Base64 is more compact than hex representation.
        // The URL-safe encoder replaces '+' with '-' and '/' with '_', and omits
        // padding.
        String base64Url = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);

        // Take a substring of the Base64-encoded string.
        // A 16-byte UUID encodes to 22 characters in Base64 (without padding).
        // Taking the first 9 characters provides a good balance between brevity and
        // uniqueness.
        // The original `substring(0, 8)` from hex gave 16^8 possibilities (approx 4.3
        // billion).
        // 9 characters from Base64 (64^9 possibilities) is much more unique (approx 2.6
        // x 10^16).
        return base64Url.substring(0, 9);
    }
}
