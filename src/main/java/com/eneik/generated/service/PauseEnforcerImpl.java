package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PauseEnforcerImpl implements PauseEnforcer {
    private static final Logger log = LoggerFactory.getLogger(PauseEnforcerImpl.class);

    @Override
    public void pause(int seconds) throws InterruptedException {
        log.info("Pausing for {} seconds", seconds);
        if (seconds > 0) {
            Thread.sleep(seconds * 1000L);
        }
    }
}
