package com.example.shorturl.service;

import com.example.shorturl.model.ShortUrl;
import com.example.shorturl.repository.ShortUrlRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UrlService {

    private final ShortUrlRepository repository;

    public UrlService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    /**
     * Shortens a given URL.
     * 
     * @param originalUrl the full URL to shorten
     * @return the generated short ID
     */
    public String shorten(String originalUrl) {
        String shortId;
        do {
            shortId = generateShortId();
        } while (repository.existsById(shortId));

        repository.save(new ShortUrl(shortId, originalUrl));
        return shortId;
    }

    /**
     * Resolves a short ID to the original URL.
     * Uses caching to check memory first.
     * 
     * @param shortId the ID to look up
     * @return Optional containing the URL if found
     */
    @Cacheable(value = "urls", key = "#shortId")
    public Optional<String> resolve(String shortId) {
        return repository.findById(shortId)
                .map(ShortUrl::getOriginalUrl);
    }

    /**
     * Deletes a short URL.
     * Evicts the corresponding entry from the cache.
     * 
     * @param shortId the ID to delete
     */
    @CacheEvict(value = "urls", key = "#shortId")
    public void delete(String shortId) {
        repository.deleteById(shortId);
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
