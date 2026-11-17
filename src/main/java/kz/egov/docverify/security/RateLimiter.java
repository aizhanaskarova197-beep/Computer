package kz.egov.docverify.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Temporary lockout against repeated failed checks.
 * After {@code maxFailures} failures in a row the key is blocked for {@code baseBlock};
 * every next block is twice as long (5 -> 10 -> 20 minutes), up to {@code maxBlock}.
 * A successful check calls {@link #reset(String)} and clears everything.
 */
public class RateLimiter {

    public static final int DEFAULT_MAX_FAILURES = 3;
    public static final Duration DEFAULT_BASE_BLOCK = Duration.ofMinutes(5);
    public static final Duration DEFAULT_MAX_BLOCK = Duration.ofHours(1);

    private final int maxFailures;
    private final Duration baseBlock;
    private final Duration maxBlock;
    private final Clock clock;
    private final Map<String, State> states = new HashMap<>();

    private static final class State {
        int failures;
        int blocks;
        Instant blockedUntil;
    }

    public RateLimiter() {
        this(DEFAULT_MAX_FAILURES, DEFAULT_BASE_BLOCK, DEFAULT_MAX_BLOCK, Clock.systemUTC());
    }

    public RateLimiter(int maxFailures, Duration baseBlock, Duration maxBlock, Clock clock) {
        if (maxFailures <= 0) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        if (baseBlock.isZero() || baseBlock.isNegative() || maxBlock.compareTo(baseBlock) < 0) {
            throw new IllegalArgumentException("Block durations are invalid");
        }
        this.maxFailures = maxFailures;
        this.baseBlock = baseBlock;
        this.maxBlock = maxBlock;
        this.clock = clock;
    }

    public synchronized boolean isBlocked(String key) {
        State state = states.get(key);
        return state != null && state.blockedUntil != null
                && clock.instant().isBefore(state.blockedUntil);
    }

    /** Seconds until the block ends (0 if the key is not blocked). */
    public synchronized long secondsLeft(String key) {
        State state = states.get(key);
        if (state == null || state.blockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), state.blockedUntil).toSeconds();
        return Math.max(seconds, 0);
    }

    public synchronized void registerFailure(String key) {
        State state = states.computeIfAbsent(key, k -> new State());
        Instant now = clock.instant();
        if (state.blockedUntil != null && now.isBefore(state.blockedUntil)) {
            return; // already blocked, do not extend the block
        }
        state.failures++;
        if (state.failures >= maxFailures) {
            Duration length = baseBlock.multipliedBy(1L << Math.min(state.blocks, 20));
            if (length.compareTo(maxBlock) > 0) {
                length = maxBlock;
            }
            state.blockedUntil = now.plus(length);
            state.blocks++;
            state.failures = 0;
        }
    }

    public synchronized void reset(String key) {
        states.remove(key);
    }
}
