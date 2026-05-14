package com.connectsphere.auth.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class that enables asynchronous processing in the application.
 * <p>
 * This configuration explicitly defines a {@link ThreadPoolTaskExecutor} to handle
 * background tasks (e.g., email sending, async events) without blocking the main HTTP request threads.
 * </p>
 *
 * <h3>Execution Flow</h3>
 * <pre class="mermaid">
 * graph TD;
 *     A[HTTP Request Thread] -->|Submits Async Task| B(Task Executor Pool);
 *     B --> C[Background Thread 1];
 *     B --> D[Background Thread 2];
 *     C --> E[Email Sent / Process Complete];
 * </pre>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configures and provides the core thread pool executor for asynchronous tasks.
     * 
     * @return a configured {@link Executor} instance for async processing
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ConnectSphereAsync-");
        executor.initialize();
        return executor;
    }
}
