package com.timesheet_gsg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimesheetSimpleloginApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TimesheetSimpleloginApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TimesheetSimpleloginApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Spring Boot application has started.");
    }
}
