package kz.egov.docverify.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    /** Clock that the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter =
            new RateLimiter(3, Duration.ofMinutes(5), Duration.ofMinutes(60), clock);

    @Test
    void notBlockedBeforeLimit() {
        limiter.registerFailure("a");
        limiter.registerFailure("a");
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void blockedAfterThreeFailures() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        assertTrue(limiter.isBlocked("a"));
        assertEquals(300, limiter.secondsLeft("a"));
    }

    @Test
    void blockEndsAfterFiveMinutes() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        clock.advance(Duration.ofMinutes(5));
        assertFalse(limiter.isBlocked("a"));
        assertEquals(0, limiter.secondsLeft("a"));
    }

    @Test
    void secondBlockIsTwiceAsLong() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        clock.advance(Duration.ofMinutes(5));
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        assertEquals(600, limiter.secondsLeft("a"));
    }

    @Test
    void blockLengthIsCapped() {
        RateLimiter capped = new RateLimiter(1, Duration.ofMinutes(5), Duration.ofMinutes(10), clock);
        capped.registerFailure("a");
        clock.advance(Duration.ofMinutes(5));
        capped.registerFailure("a");
        clock.advance(Duration.ofMinutes(10));
        capped.registerFailure("a"); // would be 20 minutes, capped to 10
        assertEquals(600, capped.secondsLeft("a"));
    }

    @Test
    void resetClearsFailuresAndBlock() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        limiter.reset("a");
        assertFalse(limiter.isBlocked("a"));
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        assertTrue(limiter.isBlocked("a"));
        assertFalse(limiter.isBlocked("b"));
    }

    @Test
    void failuresWhileBlockedDoNotExtendBlock() {
        for (int i = 0; i < 3; i++) {
            limiter.registerFailure("a");
        }
        limiter.registerFailure("a");
        limiter.registerFailure("a");
        assertEquals(300, limiter.secondsLeft("a"));
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiter(0, Duration.ofMinutes(5), Duration.ofMinutes(60), clock));
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiter(3, Duration.ofMinutes(10), Duration.ofMinutes(5), clock));
    }
}
