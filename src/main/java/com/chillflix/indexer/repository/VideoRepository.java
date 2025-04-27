package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.Video;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends R2dbcRepository<Video, UUID> {

    @Query("SELECT * FROM videos WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(to_tsvector('english', title || ' ' || COALESCE(creator, '') || ' ' || CAST(year AS TEXT) || ' ' || " +
           "COALESCE(category, '') || ' ' || array_to_string(tags, ' ') || ' ' || COALESCE(description, '')) @@ " +
           "plainto_tsquery('english', :searchTerm) " +
           "OR LOWER(title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(creator) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(year AS TEXT) = :searchTerm " +
           "OR LOWER(category) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR EXISTS (SELECT 1 FROM unnest(tags) t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) " +
           "ORDER BY ts_rank(to_tsvector('english', title || ' ' || COALESCE(creator, '') || ' ' || CAST(year AS TEXT) || ' ' || " +
           "COALESCE(category, '') || ' ' || array_to_string(tags, ' ') || ' ' || COALESCE(description, '')), " +
           "plainto_tsquery('english', :searchTerm)) DESC, updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Video> searchVideos(@Param("searchTerm") String searchTerm, 
                          @Param("limit") int limit, 
                          @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:creator IS NULL OR LOWER(creator) LIKE LOWER(CONCAT('%', :creator, '%'))) " +
           "AND (:year IS NULL OR year = :year) " +
           "AND (:category IS NULL OR LOWER(category) = LOWER(:category)) " +
           "AND (:tag IS NULL OR :tag = ANY(tags)) " +
           "AND (:quality IS NULL OR LOWER(quality) = LOWER(:quality)) " +
           "ORDER BY updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Video> advancedSearch(@Param("title") String title,
                            @Param("creator") String creator,
                            @Param("year") Integer year,
                            @Param("category") String category,
                            @Param("tag") String tag,
                            @Param("quality") String quality,
                            @Param("limit") int limit,
                            @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Video> findByYear(@Param("year") int year, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(creator) LIKE LOWER(CONCAT('%', :creator, '%')) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Video> findByCreator(@Param("creator") String creator, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(category) = LOWER(:category) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Video> findByCategory(@Param("category") String category, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND :tag = ANY(tags) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Video> findByTag(@Param("tag") String tag, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Video> findAllVideosPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query("INSERT INTO videos (id, title, creator, year, duration, category, magnet, quality, file_type, size, sha256_hash, seeds, peers, thumbnail_path, description, tags, torrent_url, source, is_deleted, created_at, updated_at, search_vector) " +
           "VALUES (:#{#video.id}, :#{#video.title}, :#{#video.creator}, :#{#video.year}, :#{#video.duration}, :#{#video.category}, " +
           ":#{#video.magnet}, :#{#video.quality}, :#{#video.fileType}, :#{#video.size}, :#{#video.sha256Hash}, :#{#video.seeds}, " +
           ":#{#video.peers}, :#{#video.thumbnailPath}, :#{#video.description}, :#{#video.tags}, :#{#video.torrentUrl}, " +
           ":#{#video.source}, :#{#video.isDeleted}, :#{#video.createdAt}, :#{#video.updatedAt}, :#{#video.searchVector}) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "title = EXCLUDED.title, creator = EXCLUDED.creator, year = EXCLUDED.year, duration = EXCLUDED.duration, " +
           "category = EXCLUDED.category, magnet = EXCLUDED.magnet, quality = EXCLUDED.quality, file_type = EXCLUDED.file_type, " +
           "size = EXCLUDED.size, sha256_hash = EXCLUDED.sha256_hash, seeds = EXCLUDED.seeds, peers = EXCLUDED.peers, " +
           "thumbnail_path = EXCLUDED.thumbnail_path, description = EXCLUDED.description, tags = EXCLUDED.tags, " +
           "torrent_url = EXCLUDED.torrent_url, source = EXCLUDED.source, is_deleted = EXCLUDED.is_deleted, " +
           "updated_at = EXCLUDED.updated_at, search_vector = EXCLUDED.search_vector " +
           "RETURNING *")
    Mono<Video> saveOrUpdate(Video video);

    @Query("SELECT COUNT(*) FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year")
    Mono<Long> countByYear(@Param("year") int year);

    @Query("SELECT category, COUNT(*) as count FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND category IS NOT NULL GROUP BY category ORDER BY count DESC LIMIT :limit")
    Flux<CategoryCount> getTopCategories(@Param("limit") int limit);

    @Query("SELECT unnest(tags) as tag, COUNT(*) as count FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY tag ORDER BY count DESC LIMIT :limit")
    Flux<TagCount> getTopTags(@Param("limit") int limit);

    @Query("UPDATE videos SET is_deleted = true WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT * FROM videos WHERE (is_deleted = false OR is_deleted IS NULL) AND updated_at > :lastUpdateTime ORDER BY updated_at ASC LIMIT :limit OFFSET :offset")
    Flux<Video> findByUpdatedAtAfterOrderByUpdatedAtAsc(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, 
                                                     @Param("limit") int limit, 
                                                     @Param("offset") long offset);

    interface CategoryCount {
        String getCategory();
        Long getCount();
    }

    interface TagCount {
        String getTag();
        Long getCount();
    }
}