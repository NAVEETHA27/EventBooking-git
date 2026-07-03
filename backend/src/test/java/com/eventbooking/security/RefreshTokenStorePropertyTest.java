package com.eventbooking.security;

import com.eventbooking.security.RefreshTokenStore;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for RefreshTokenStore single-use enforcement.
 *
 * Validates: Requirements 1.7
 *
 * Property 5: For any JTI already marked used, isUsed() always returns true.
 */
class RefreshTokenStorePropertyTest {

    private final RefreshTokenStore store = new RefreshTokenStore();

    // Property 5: For any JTI already marked used, isUsed() always returns true
    // Tests 100 random UUIDs to universally verify the property holds
    @RepeatedTest(100)
    void property5_anyMarkedJtiAlwaysReturnsIsUsedTrue() {
        String jti = UUID.randomUUID().toString();
        assertFalse(store.isUsed(jti), "JTI should not be used before marking");
        store.markUsed(jti);
        assertTrue(store.isUsed(jti), "JTI must return isUsed=true after markUsed()");
    }

    @Test
    void property5_markedJtiRemainsUsedAfterMultipleChecks() {
        String jti = UUID.randomUUID().toString();
        store.markUsed(jti);
        for (int i = 0; i < 10; i++) {
            assertTrue(store.isUsed(jti),
                    "isUsed() must remain true on repeated checks, iteration " + i);
        }
    }

    @Test
    void property5_differentJtisAreIndependent() {
        String jti1 = UUID.randomUUID().toString();
        String jti2 = UUID.randomUUID().toString();
        store.markUsed(jti1);
        assertTrue(store.isUsed(jti1), "Marked JTI must be used");
        assertFalse(store.isUsed(jti2), "Unmarked JTI must not be affected by marking another JTI");
    }
}
