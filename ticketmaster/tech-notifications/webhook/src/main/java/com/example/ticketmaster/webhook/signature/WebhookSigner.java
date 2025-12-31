package com.example.ticketmaster.webhook.signature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class WebhookSigner {

    /**
     * Minimal webhook signing helper (HMAC SHA-256).
     * <p>
     * Why: the receiver needs a way to reject spoofed webhooks.
     * This demo uses a shared secret (sender + receiver both know it) and signs
     * a stable message: {@code <timestampSeconds> + '.' + <idempotencyKey>}.
     */

    private final String sharedSecret;

    public WebhookSigner(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public String signature(long timestampSeconds, String idempotencyKey) {
        String message = timestampSeconds + "." + idempotencyKey;
        return "hmac-sha256=" + hmacSha256Hex(sharedSecret, message);
    }

    public boolean matches(String providedSignature, long timestampSeconds, String idempotencyKey) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        return providedSignature.equals(signature(timestampSeconds, idempotencyKey));
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return toHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
