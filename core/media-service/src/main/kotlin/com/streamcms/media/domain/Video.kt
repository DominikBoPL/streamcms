package com.streamcms.media.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

// VideoStatus represents the lifecycle of a video
// "enum class" in Kotlin works like Java enum
enum class VideoStatus {
    PENDING,      // just uploaded, waiting for processing
    PROCESSING,   // FFmpeg is currently transcoding
    READY,        // transcoding done, video is watchable
    FAILED        // something went wrong during processing
}

// @Entity tells JPA/Hibernate this class maps to a database table
// @Table specifies the table name and schema
// schema = "media" matches what we created in 01-init.sql
@Entity
@Table(name = "videos", schema = "media")
class Video(

    // @Column maps field to database column
    // nullable = false means NOT NULL in database
    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val description: String = "",

    // Original filename as uploaded by user
    @Column(name = "original_filename", nullable = false)
    val originalFilename: String,

    // Path in MinIO where the raw uploaded file is stored
    // e.g. "raw/uuid/filename.mp4"
    @Column(name = "storage_key")
    var storageKey: String? = null,

    // Duration in seconds - we don't know this until FFmpeg processes the file
    @Column
    var duration: Long? = null,

    // "var" because status changes as video is processed
    // @Enumerated tells JPA to store enum as String ("PENDING")
    // not as integer (0) - much easier to read in database
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: VideoStatus = VideoStatus.PENDING,

    // Timestamp when record was created
    // Instant is the modern Java time API - always UTC, no timezone confusion
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    // @Id marks the primary key field
    // @GeneratedValue with UUID strategy - we generate UUID in application
    // not in database - this is better for distributed systems
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null

) {
    // @PreUpdate is a JPA lifecycle hook
    // This method runs automatically before every UPDATE SQL statement
    // Keeps updatedAt in sync without manual calls
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
