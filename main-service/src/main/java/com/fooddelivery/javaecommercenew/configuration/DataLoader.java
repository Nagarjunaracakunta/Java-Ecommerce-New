package com.fooddelivery.javaecommercenew.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataLoader {

    public void runTasks(String... args) {
        log.info("DataLoader is running on startup....{}", args.length > 0 ? String.join(", ", args) : "no args");
    }
}
