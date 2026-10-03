package com.heimdall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Single artifact, two process roles (docs/01-c4-architecture.md, Level 2).
 *
 * <pre>
 *   java -jar heimdall.jar                              # api role (default profile)
 *   java -jar heimdall.jar --spring.profiles.active=worker   # worker role
 * </pre>
 *
 * The "worker" profile activates {@link com.heimdall.orchestrator.OrchestratorScheduler},
 * which polls analysis_jobs. The "api" role serves ingestion, console-api and identity.
 * Both roles share the same Spring Modulith module boundaries so module tests
 * (see src/test) verify the architecture regardless of which role is running.
 */
@SpringBootApplication
@Modulith(systemName = "Heimdall")
@EnableScheduling
public class HeimdallApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeimdallApplication.class, args);
    }
}
