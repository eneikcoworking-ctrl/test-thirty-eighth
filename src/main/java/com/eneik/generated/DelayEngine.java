package com.eneik.generated;

import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class DelayEngine {
    private Random random = new Random();

    public DelayEngine() {
    }

    public void setSeed(long seed) {
        this.random = new Random(seed);
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public int getRandomDelay(int minSeconds, int maxSeconds) {
        if (maxSeconds <= minSeconds) {
            return minSeconds;
        }
        return minSeconds + random.nextInt(maxSeconds - minSeconds + 1);
    }

    /**
     * Randomly selects a warm-up action.
     * Returns either "CHANNEL_READ" or "ONLINE_STATUS_UPDATE".
     */
    public String getRandomAction() {
        return random.nextBoolean() ? "CHANNEL_READ" : "ONLINE_STATUS_UPDATE";
    }
}
