package com.streamcms.media.controller

import com.streamcms.media.domain.Video
import com.streamcms.media.domain.VideoStatus
import com.streamcms.media.service.VideoService
import com.streamcms.media.service.VideoUploadResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

// @RestController = @Controller + @ResponseBody
// Every method automatically serializes return value to JSON
// @RequestMapping sets the base URL path for all methods in this class
@RestController
@RequestMapping("/api/videos")
class VideoController(
    // Constructor injection - same pattern as in VideoService
    private val videoService: VideoService
) {

    // POST /api/videos/upload
    // @RequestParam maps form fields from multipart request
    // MultipartFile is Spring's representation of an uploaded file
    @PostMapping("/upload")
    fun uploadVideo(
        @RequestParam title: String,
        @RequestParam(required = false, defaultValue = "") description: String,
        @RequestParam file: MultipartFile
    ): ResponseEntity<VideoUploadResponse> {
        // ResponseEntity lets us control HTTP status code + response body
        val response = videoService.uploadVideo(title, description, file)
        // HTTP 202 Accepted = "we got it, processing asynchronously"
        // More accurate than 200 OK since video isn't ready yet
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    // GET /api/videos/{id}
    // @PathVariable maps the {id} part of the URL
    @GetMapping("/{id}")
    fun getVideo(@PathVariable id: UUID): ResponseEntity<Video> {
        // "let" is a Kotlin scope function
        // Executes the block if value is not null, returns null otherwise
        // Equivalent to: if (video != null) { return ResponseEntity.ok(video) }
        return videoService.getVideo(id)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
        // "?:" here means: if let returned null, return 404
    }

    // GET /api/videos
    // @RequestParam(required = false) makes the query parameter optional
    // GET /api/videos              → all videos
    // GET /api/videos?status=READY → only ready videos
    @GetMapping
    fun getVideos(
        @RequestParam(required = false) status: VideoStatus?
    ): ResponseEntity<List<Video>> {
        val videos = if (status != null) {
            videoService.getVideosByStatus(status)
        } else {
            videoService.getAllVideos()
        }
        return ResponseEntity.ok(videos)
    }
}
