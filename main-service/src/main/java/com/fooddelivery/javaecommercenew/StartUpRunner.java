package com.fooddelivery.javaecommercenew;

import com.fooddelivery.javaecommercenew.configuration.DataLoader;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartUpRunner implements CommandLineRunner {
    private final DataLoader dataLoader;

    public StartUpRunner(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        dataLoader.runTasks(args);
    }
}
