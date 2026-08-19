package com.xcess.ocs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
/**
 * Configuration for async processing, particularly for Trie initialization.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Task executor for async operations like Trie initialization
     * 
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "trieInitializationExecutor")
    public Executor trieInitializationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TrieInit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("Configured async executor for Trie initialization");
        return executor;
    }

}
