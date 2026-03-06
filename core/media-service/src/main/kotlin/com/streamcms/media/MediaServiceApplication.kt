package com.streamcms.media

// Spring Boot imports - same as Java, just different syntax
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// @SpringBootApplication is the same as in Java - it enables:
// - @Configuration: this class can define Spring beans
// - @EnableAutoConfiguration: Spring Boot auto-configures based on classpath
// - @ComponentScan: scans this package and subpackages for Spring components
@SpringBootApplication
class MediaServiceApplication

// In Kotlin, "fun main" is a top-level function - it doesn't need a class
// runApplication<T> is a Kotlin extension function provided by Spring Boot
// It's equivalent to SpringApplication.run(MediaServiceApplication::class.java, *args)
fun main(args: Array<String>) {
    runApplication<MediaServiceApplication>(*args)
    // "*args" is the "spread operator" - unpacks array into varargs
    // Same as Java's "args" in SpringApplication.run(App.class, args)
}
