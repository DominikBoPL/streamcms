package com.streamcms.media.service

import com.streamcms.media.domain.Video
import com.streamcms.media.domain.VideoStatus
import com.streamcms.media.repository.VideoRepository
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

// Data class for the upload response
// "data class" gives us toString, equals, hashCode for free
data class VideoUploadResponse(
    val videoId: UUID,
    val title: String,
    val status: VideoStatus,
    val message: String
)

// @Service is a Spring stereotype annotation
// Functionally same as @Component but communicates intent:
// "this class contains business logic"
@Service
class VideoService(
    // Constructor injection - preferred over @Autowired field injection
    // Kotlin makes this clean - no need for @Autowired annotation
    // Spring sees a single constructor and injects automatically
    private val videoRepository: VideoRepository,
    private val minioClient: MinioClient,
    private val rabbitTemplate: RabbitTemplate
) {
    // Logger - in Kotlin we define it as a companion object property
    // "companion object" is Kotlin's equivalent of Java static members
    companion object {
        private val log = LoggerFactory.getLogger(VideoService::class.java)
    }

    // Exchange and routing key for RabbitMQ messages
    // We'll define the actual exchange in a future lesson
    private val exchange = "media.events"
    private val uploadRoutingKey = "video.uploaded"

    fun uploadVideo(title: String, description: String, file: MultipartFile): VideoUploadResponse {
        log.info("Starting upload for video: $title")
        // "$title" is string interpolation - Kotlin's cleaner version of
        // Java's "Starting upload for video: " + title

        // Step 1: Ensure the MinIO bucket exists
        ensureBucketExists("videos")

        // Step 2: Generate a unique storage path for this file
        // UUID ensures no filename collisions even if users upload files with same name
        val storageKey = "raw/${UUID.randomUUID()}/${file.originalFilename}"

        // Step 3: Upload file to MinIO
        log.info("Uploading file to MinIO with key: $storageKey")
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket("videos")
                .`object`(storageKey)           // backticks needed because "object" is a Kotlin keyword
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType ?: "video/mp4")
                // "?:" is the Elvis operator - use "video/mp4" if contentType is null
                .build()
        )

        // Step 4: Save video metadata to PostgreSQL
        val video = Video(
            title = title,
            description = description,
            originalFilename = file.originalFilename ?: "unknown",
            storageKey = storageKey,
            status = VideoStatus.PENDING
        )
        val savedVideo = videoRepository.save(video)
        log.info("Video saved to database with id: ${savedVideo.id}")

        // Step 5: Publish event to RabbitMQ so FFmpeg worker can pick it up
        // We send the video ID - the worker will fetch details from DB
        rabbitTemplate.convertAndSend(exchange, uploadRoutingKey, savedVideo.id.toString())
        log.info("Published VideoUploaded event for id: ${savedVideo.id}")

        return VideoUploadResponse(
            videoId = savedVideo.id!!,
            // "!!" is the "non-null assertion" operator
            // Use sparingly - it throws NullPointerException if null
            // Here it's safe because Hibernate always populates id after save()
            title = savedVideo.title,
            status = savedVideo.status,
            message = "Video uploaded successfully. Processing will begin shortly."
        )
    }

    fun getVideo(id: UUID): Video? {
        // findById returns Optional<Video> in Java
        // In Kotlin with Spring Data, we can use orElse(null)
        // which gives us Video? (nullable)
        return videoRepository.findById(id).orElse(null)
    }

    fun getAllVideos(): List<Video> = videoRepository.findAll()

    fun getVideosByStatus(status: VideoStatus): List<Video> =
        videoRepository.findByStatus(status)

    // Private helper - ensures MinIO bucket exists before uploading
    private fun ensureBucketExists(bucketName: String) {
        val exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        )
        if (!exists) {
            log.info("Bucket '$bucketName' not found, creating...")
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
        }
    }
}