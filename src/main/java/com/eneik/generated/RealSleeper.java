package com.eneik.generated;

import org.springframework.stereotype.Component;

@Component
public class RealSleeper implements Sleeper {
    @Override
    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
