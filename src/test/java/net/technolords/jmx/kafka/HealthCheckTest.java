package net.technolords.jmx.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class HealthCheckTest {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Test (enabled = false, description = "Integration test, which requires a running Kafka instance")
    public void testBrokerStatus() {
        LOGGER.info("About to test...");
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.reportBrokerStatus();
    }
}