package com.streamcms.media.config

import io.minio.MinioClient
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// @ConfigurationProperties maps application.yml properties to this class
// prefix = "minio" means it reads the "minio:" block from application.yml
// This is much cleaner than @Value("${minio.url}") on every field
@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    val url: String,
    val accessKey: String,
    val secretKey: String,
    val bucketName: String
)

// @Configuration marks this as a Spring configuration class
// @EnableConfigurationProperties activates our MinioProperties class
@Configuration
@EnableConfigurationProperties(MinioProperties::class)
class MinioConfig {

    // @Bean tells Spring to manage this object as a Spring bean
    // MinioClient will be injectable anywhere with @Autowired or constructor injection
    @Bean
    fun minioClient(properties: MinioProperties): MinioClient {
        // Builder pattern - same concept as in Java
        return MinioClient.builder()
            .endpoint(properties.url)
            .credentials(properties.accessKey, properties.secretKey)
            .build()
    }
}
