package com.fooddelivery.javaecommercenew.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataLoaderTest {

    @Test
    void runTasks() {
        DataLoader dataLoader = new DataLoader();
        dataLoader.runTasks("arg1", "arg2");
    }
}