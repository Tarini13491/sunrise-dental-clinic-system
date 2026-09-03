package com.sunrise.clinic.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ClinicStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        SchemaBootstrap.prepare();
    }
}
