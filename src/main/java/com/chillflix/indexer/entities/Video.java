package com.chillflix.indexer.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Table("videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @Column("id")
    private UUID id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be 255 characters or less")
    private String title;

    @Size(max = 255, message = "Creator must be 255 characters or less")
    private String creator;

    private Integer year;

    private Integer duration;

    @Size(max = 100, message = "Category must be 100 characters or less")
    private String category;

    @NotBlank(message = "Magnet link is required")
    private String magnet;

    @Size(max = 50, message = "Quality must be 50 characters or less")
    private String quality;

    @Column("file_type")
    @Size(max = 20, message = "File type must be 20 characters or less")
    private String fileType;

    private Long size;

    @Column("sha256_hash")
    @Size(min = 64, max = 64, message = "SHA256 hash must be exactly 64 characters")
    private String sha256Hash;

    private Integer seeds;

    private Integer peers;

    @Column("thumbnail_path")
    private String thumbnailPath;

    private String description;

    private List<String> tags;

    @Column("torrent_url")
    private String torrentUrl;

    private String source;

    @Column("is_deleted")
    private Boolean isDeleted;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("search_vector")
    private Object searchVector;
}