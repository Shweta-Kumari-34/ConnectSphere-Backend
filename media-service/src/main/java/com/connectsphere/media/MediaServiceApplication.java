package com.connectsphere.media;
import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.Story;
import com.connectsphere.media.repository.MediaRepository;
import com.connectsphere.media.repository.StoryRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EntityScan(basePackageClasses = {Media.class, Story.class})
@EnableJpaRepositories(basePackageClasses = {MediaRepository.class, StoryRepository.class})
public class MediaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
