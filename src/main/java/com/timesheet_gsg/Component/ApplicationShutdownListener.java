package com.timesheet_gsg.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownListener.class);

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("Spring Boot application is shutting down.");
    }
}

