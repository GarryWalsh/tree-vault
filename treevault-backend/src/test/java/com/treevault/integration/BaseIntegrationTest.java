package com.treevault.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that provides common Spring Boot test configuration
 * and utilities for testing REST endpoints.
 * 
 * <p>All integration tests should extend this class to inherit:
 * <ul>
 *   <li>Spring Boot test context with random port</li>
 *   <li>Test profile activation</li>
 *   <li>TestRestTemplate for HTTP requests</li>
 *   <li>JdbcTemplate for database operations</li>
 *   <li>Helper method to get base URL</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    /**
     * Gets the base URL for API endpoints.
     * 
     * @return the base URL in the format "http://localhost:{port}/api/v1"
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }
}

