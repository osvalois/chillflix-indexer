package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.VideoGame;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VideoGameRepository extends R2dbcRepository<VideoGame, UUID> {

    @Query("SELECT * FROM video_games WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(to_tsvector('english', title || ' ' || COALESCE(developer, '') || ' ' || COALESCE(publisher, '') || ' ' || " +
           "CAST(year AS TEXT) || ' ' || array_to_string(platform, ' ') || ' ' || array_to_string(genre, ' ') || ' ' || " +
           "COALESCE(description, '')) @@ " +
           "plainto_tsquery('english', :searchTerm) " +
           "OR LOWER(title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(developer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(publisher) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(year AS TEXT) = :searchTerm " +
           "OR EXISTS (SELECT 1 FROM unnest(platform) p WHERE LOWER(p) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "OR EXISTS (SELECT 1 FROM unnest(genre) g WHERE LOWER(g) LIKE LOWER(CONCAT('%', :searchTerm, '%')))) " +
           "ORDER BY ts_rank(to_tsvector('english', title || ' ' || COALESCE(developer, '') || ' ' || COALESCE(publisher, '') || ' ' || " +
           "CAST(year AS TEXT) || ' ' || array_to_string(platform, ' ') || ' ' || array_to_string(genre, ' ') || ' ' || COALESCE(description, '')), " +
           "plainto_tsquery('english', :searchTerm)) DESC, updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<VideoGame> searchVideoGames(@Param("searchTerm") String searchTerm, 
                              @Param("limit") int limit, 
                              @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:developer IS NULL OR LOWER(developer) LIKE LOWER(CONCAT('%', :developer, '%'))) " +
           "AND (:publisher IS NULL OR LOWER(publisher) LIKE LOWER(CONCAT('%', :publisher, '%'))) " +
           "AND (:year IS NULL OR year = :year) " +
           "AND (:platform IS NULL OR :platform = ANY(platform)) " +
           "AND (:genre IS NULL OR :genre = ANY(genre)) " +
           "ORDER BY updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<VideoGame> advancedSearch(@Param("title") String title,
                               @Param("developer") String developer,
                               @Param("publisher") String publisher,
                               @Param("year") Integer year,
                               @Param("platform") String platform,
                               @Param("genre") String genre,
                               @Param("limit") int limit,
                               @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByYear(@Param("year") int year, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(developer) LIKE LOWER(CONCAT('%', :developer, '%')) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByDeveloper(@Param("developer") String developer, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(publisher) LIKE LOWER(CONCAT('%', :publisher, '%')) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByPublisher(@Param("publisher") String publisher, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND :platform = ANY(platform) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByPlatform(@Param("platform") String platform, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND :genre = ANY(genre) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByGenre(@Param("genre") String genre, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findAllVideoGamesPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query("INSERT INTO video_games (id, title, year, developer, publisher, platform, magnet, quality, file_type, size, sha256_hash, seeds, peers, cover_path, description, system_requirements, genre, screenshot_paths, rating, release_date, torrent_url, esrb_rating, multiplayer, is_deleted, created_at, updated_at, search_vector) " +
           "VALUES (:#{#game.id}, :#{#game.title}, :#{#game.year}, :#{#game.developer}, :#{#game.publisher}, :#{#game.platform}, " +
           ":#{#game.magnet}, :#{#game.quality}, :#{#game.fileType}, :#{#game.size}, :#{#game.sha256Hash}, :#{#game.seeds}, " +
           ":#{#game.peers}, :#{#game.coverPath}, :#{#game.description}, :#{#game.systemRequirements}, :#{#game.genre}, " +
           ":#{#game.screenshotPaths}, :#{#game.rating}, :#{#game.releaseDate}, :#{#game.torrentUrl}, :#{#game.esrbRating}, " +
           ":#{#game.multiplayer}, :#{#game.isDeleted}, :#{#game.createdAt}, :#{#game.updatedAt}, :#{#game.searchVector}) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "title = EXCLUDED.title, year = EXCLUDED.year, developer = EXCLUDED.developer, publisher = EXCLUDED.publisher, " +
           "platform = EXCLUDED.platform, magnet = EXCLUDED.magnet, quality = EXCLUDED.quality, file_type = EXCLUDED.file_type, " +
           "size = EXCLUDED.size, sha256_hash = EXCLUDED.sha256_hash, seeds = EXCLUDED.seeds, peers = EXCLUDED.peers, " +
           "cover_path = EXCLUDED.cover_path, description = EXCLUDED.description, system_requirements = EXCLUDED.system_requirements, " +
           "genre = EXCLUDED.genre, screenshot_paths = EXCLUDED.screenshot_paths, rating = EXCLUDED.rating, " +
           "release_date = EXCLUDED.release_date, torrent_url = EXCLUDED.torrent_url, esrb_rating = EXCLUDED.esrb_rating, " +
           "multiplayer = EXCLUDED.multiplayer, is_deleted = EXCLUDED.is_deleted, updated_at = EXCLUDED.updated_at, " +
           "search_vector = EXCLUDED.search_vector " +
           "RETURNING *")
    Mono<VideoGame> saveOrUpdate(VideoGame game);

    @Query("SELECT COUNT(*) FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year")
    Mono<Long> countByYear(@Param("year") int year);

    @Query("SELECT unnest(genre) as genre, COUNT(*) as count FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY genre ORDER BY count DESC LIMIT :limit")
    Flux<GenreCount> getTopGenres(@Param("limit") int limit);

    @Query("SELECT unnest(platform) as platform, COUNT(*) as count FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY platform ORDER BY count DESC LIMIT :limit")
    Flux<PlatformCount> getTopPlatforms(@Param("limit") int limit);

    @Query("UPDATE video_games SET is_deleted = true WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT * FROM video_games WHERE (is_deleted = false OR is_deleted IS NULL) AND updated_at > :lastUpdateTime ORDER BY updated_at ASC LIMIT :limit OFFSET :offset")
    Flux<VideoGame> findByUpdatedAtAfterOrderByUpdatedAtAsc(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, 
                                                        @Param("limit") int limit, 
                                                        @Param("offset") long offset);

    interface GenreCount {
        String getGenre();
        Long getCount();
    }

    interface PlatformCount {
        String getPlatform();
        Long getCount();
    }
}