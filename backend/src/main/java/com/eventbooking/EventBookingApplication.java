package com.eventbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ╔══════════════════════════════════════════════════════════╗
 *  Online Event Ticket Booking System – Spring Boot Entry Point
 *  Online Event Ticket Booking System - Spring Boot Entry Point
 * ╚══════════════════════════════════════════════════════════╝
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class EventBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventBookingApplication.class, args);
    }
}
