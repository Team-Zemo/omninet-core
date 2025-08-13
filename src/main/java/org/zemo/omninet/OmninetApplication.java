package org.zemo.omninet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditAware")   // bean ka name
public class OmninetApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmninetApplication.class, args);
    }

}
