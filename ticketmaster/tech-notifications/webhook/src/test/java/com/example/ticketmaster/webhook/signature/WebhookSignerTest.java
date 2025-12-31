package com.example.ticketmaster.webhook.signature;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignerTest {

    /**
     * Basic regression test for the signing algorithm.
     * Purpose: keep the signature deterministic and ensure mismatched inputs are rejected.
     */

    @Test
    void signatureMatchesForSameInputs() {
        WebhookSigner signer = new WebhookSigner("secret");
        long ts = 1_700_000_000L;
        String key = "evt_wr_123_active";

        String signature = signer.signature(ts, key);

        assertTrue(signer.matches(signature, ts, key));
        assertFalse(signer.matches(signature, ts, "other"));
        assertFalse(signer.matches(signature, ts + 1, key));
    }
}
