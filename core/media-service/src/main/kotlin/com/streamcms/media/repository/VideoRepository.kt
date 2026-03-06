package com.streamcms.media.repository

import com.streamcms.media.domain.Video
import com.streamcms.media.domain.VideoStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

// JpaRepository<Video, UUID> means:
// - Entity type: Video
// - Primary key type: UUID
// Spring Data automatically generates SQL for standard operations:
// save(), findById(), findAll(), deleteById(), etc.
// No implementation needed - Spring generates it at startup
@Repository
interface VideoRepository : JpaRepository<Video, UUID> {

    // Spring Data reads method names and generates SQL automatically
    // "findByStatus" → SELECT * FROM videos WHERE status = ?
    fun findByStatus(status: VideoStatus): List<Video>

    // "findByTitleContainingIgnoreCase" →
    // SELECT * FROM videos WHERE LOWER(title) LIKE LOWER('%query%')
    fun findByTitleContainingIgnoreCase(query: String): List<Video>
}
