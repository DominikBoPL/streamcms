package com.streamcms.media.service

import com.streamcms.media.domain.Video
import com.streamcms.media.domain.VideoStatus
import com.streamcms.media.repository.VideoRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.mockk.clearAllMocks
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.util.Optional
import java.util.UUID

// DescribeSpec is one of several Kotest test styles
// It uses "describe/it" pattern popular in JavaScript (Jest, Jasmine)
// Good for grouping related tests together
// Other styles: FunSpec, BehaviorSpec, StringSpec - all valid choices
class VideoServiceTest : DescribeSpec({

    // ===================================
    // MOCKS SETUP
    // ===================================

    // mockk<T>() creates a fake object that pretends to be T
    // By default ALL calls to mock methods throw an exception
    // unless you explicitly define behaviour with "every { }"
    // This forces you to think about what your code actually calls
    val videoRepository = mockk<VideoRepository>()
    val minioClient = mockk<MinioClient>()
    val rabbitTemplate = mockk<RabbitTemplate>()

    // Create a fake MultipartFile for upload tests
    // mockk() without type parameter infers type from context
    val mockFile = mockk<MultipartFile>()

    // The class we're actually testing - note we pass MOCKS not real objects
    // This is "constructor injection" in action - makes testing easy
    val videoService = VideoService(videoRepository, minioClient, rabbitTemplate)

    // ===================================
    // SHARED TEST DATA
    // ===================================

    // Fixed UUID for predictable tests - random UUIDs make tests flaky
    val testVideoId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    // Helper function to create a test Video
    // Default parameters mean you only override what's relevant per test
    fun createTestVideo(
        id: UUID = testVideoId,
        title: String = "Test Video",
        status: VideoStatus = VideoStatus.PENDING
    ) = Video(
        id = id,
        title = title,
        description = "Test description",
        originalFilename = "test.mp4",
        storageKey = "raw/$id/test.mp4",
        status = status
    )

    // ===================================
    // TESTS
    // ===================================

    // "describe" groups related tests - usually one describe per method
    describe("uploadVideo") {

        // "beforeEach" runs before every "it" block inside this describe
        // We set up mock behaviour here so each test starts clean
        beforeEach {
            clearAllMocks()

            every { mockFile.originalFilename } returns "test-video.mp4"
            every { mockFile.size } returns 1024L
            every { mockFile.contentType } returns "video/mp4"
            every { mockFile.inputStream } returns ByteArrayInputStream(ByteArray(1024))

            every {
                minioClient.bucketExists(any<BucketExistsArgs>())
            } returns true

            every {
                minioClient.putObject(any<PutObjectArgs>())
            } returns mockk()

            every {
                rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<String>())
            } just runs

            // Simplified - just return the video as-is with ID already set
            // We use a Video that already has an ID so we don't need reflection tricks
            every {
                videoRepository.save(any<Video>())
            } answers {
                // Create a new Video based on what was passed in,
                // but with a guaranteed non-null ID set
                val originalVideo = firstArg<Video>()
                Video(
                    id = testVideoId,    // ← explicitly set the ID
                    title = originalVideo.title,
                    description = originalVideo.description,
                    originalFilename = originalVideo.originalFilename,
                    storageKey = originalVideo.storageKey,
                    status = originalVideo.status
                )
            }
        }

        // "it" is an individual test case
        // Use backtick strings for readable test names
        it("should return 202 response with PENDING status") {
            val response = videoService.uploadVideo("Test Video", "Description", mockFile)

            // "shouldBe" is Kotest's assertion - cleaner than assertEquals
            response.status shouldBe VideoStatus.PENDING
            response.title shouldBe "Test Video"
            response.message shouldNotBe null
        }

        it("should save video to repository") {
            videoService.uploadVideo("Test Video", "Description", mockFile)

            // verify{} checks that a mock method was called
            // exactly = 1 means it must be called exactly once
            verify(exactly = 1) { videoRepository.save(any()) }
        }

        it("should upload file to MinIO") {
            videoService.uploadVideo("Test Video", "Description", mockFile)

            verify(exactly = 1) { minioClient.putObject(any<PutObjectArgs>()) }
        }

        it("should publish event to RabbitMQ after upload") {
            videoService.uploadVideo("Test Video", "Description", mockFile)

            // Verify RabbitMQ was called with correct exchange name
            verify(exactly = 1) {
                rabbitTemplate.convertAndSend(
                    "media.events",      // exchange name
                    "video.uploaded",    // routing key
                    any<String>()        // video ID (we don't check exact value)
                )
            }
        }

        it("should generate unique storage key with raw/ prefix") {
            val videoSlot = slot<Video>()

            every { videoRepository.save(capture(videoSlot)) } answers {
                // firstArg() is the actual argument passed to save()
                // Use this INSTEAD of videoSlot.captured inside answers block
                // because the slot is populated at the same time as answers runs
                val captured = firstArg<Video>()
                Video(
                    id = testVideoId,
                    title = captured.title,
                    description = captured.description,
                    originalFilename = captured.originalFilename,
                    storageKey = captured.storageKey,
                    status = captured.status
                )
            }

            videoService.uploadVideo("Test Video", "Description", mockFile)

            // Now slot.captured is safe to use AFTER the call completes
            videoSlot.captured.storageKey shouldStartWith "raw/"
        }

        it("should create MinIO bucket if it does not exist") {
            // Override the default mock - bucket does NOT exist this time
            every {
                minioClient.bucketExists(any<BucketExistsArgs>())
            } returns false

            every {
                minioClient.makeBucket(any<MakeBucketArgs>())
            } just runs

            videoService.uploadVideo("Test Video", "Description", mockFile)

            // Verify makeBucket was called because bucket didn't exist
            verify(exactly = 1) { minioClient.makeBucket(any<MakeBucketArgs>()) }
        }
    }

    describe("getVideo") {

        it("should return video when it exists") {
            val testVideo = createTestVideo()
            // Optional.of() wraps value - this is what JPA repository returns
            every { videoRepository.findById(testVideoId) } returns Optional.of(testVideo)

            val result = videoService.getVideo(testVideoId)

            // shouldNotBe null - Kotest null check
            result shouldNotBe null
            result?.id shouldBe testVideoId
            result?.title shouldBe "Test Video"
        }

        it("should return null when video does not exist") {
            // Optional.empty() simulates "not found" in JPA
            every { videoRepository.findById(any()) } returns Optional.empty()

            val result = videoService.getVideo(UUID.randomUUID())

            // shouldBe null - clean way to assert null in Kotest
            result shouldBe null
        }
    }

    describe("getAllVideos") {

        it("should return all videos from repository") {
            val videos = listOf(
                createTestVideo(id = UUID.randomUUID(), title = "Video 1"),
                createTestVideo(id = UUID.randomUUID(), title = "Video 2"),
                createTestVideo(id = UUID.randomUUID(), title = "Video 3")
            )
            every { videoRepository.findAll() } returns videos

            val result = videoService.getAllVideos()

            result.size shouldBe 3
            // "shouldBe" works for lists too - checks structural equality
            result shouldBe videos
        }

        it("should return empty list when no videos exist") {
            every { videoRepository.findAll() } returns emptyList()

            val result = videoService.getAllVideos()

            result.size shouldBe 0
            result shouldBe emptyList()
        }
    }

    describe("getVideosByStatus") {

        it("should return only videos with matching status") {
            val readyVideos = listOf(
                createTestVideo(status = VideoStatus.READY),
                createTestVideo(id = UUID.randomUUID(), status = VideoStatus.READY)
            )
            every {
                videoRepository.findByStatus(VideoStatus.READY)
            } returns readyVideos

            val result = videoService.getVideosByStatus(VideoStatus.READY)

            result.size shouldBe 2
            // all{} is Kotest collection assertion - checks every element
            result.all { it.status == VideoStatus.READY } shouldBe true
        }

        it("should return empty list when no videos match status") {
            every {
                videoRepository.findByStatus(VideoStatus.FAILED)
            } returns emptyList()

            val result = videoService.getVideosByStatus(VideoStatus.FAILED)

            result shouldBe emptyList()
        }
    }
})