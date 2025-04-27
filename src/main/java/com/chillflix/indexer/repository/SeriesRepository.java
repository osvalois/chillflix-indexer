package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.Series;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SeriesRepository extends R2dbcRepository<Series, UUID> {

    @Query("SELECT * FROM series WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(to_tsvector('english', title || ' ' || COALESCE(overview, '') || ' ' || CAST(year AS TEXT) || ' ' || " +
           "language || ' ' || original_language || ' ' || quality || ' ' || file_type || ' ' || COALESCE(network, '')) @@ " +
           "plainto_tsquery('english', :searchTerm) " +
           "OR LOWER(title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(year AS TEXT) = :searchTerm " +
           "OR LOWER(language) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(original_language) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(quality) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(network) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(file_type) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY ts_rank(to_tsvector('english', title || ' ' || COALESCE(overview, '') || ' ' || " +
           "CAST(year AS TEXT) || ' ' || language || ' ' || original_language || ' ' || quality || ' ' || file_type || ' ' || COALESCE(network, '')), " +
           "plainto_tsquery('english', :searchTerm)) DESC, updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Series> searchSeries(@Param("searchTerm") String searchTerm, 
                           @Param("limit") int limit, 
                           @Param("offset") long offset);

    @Query("SELECT * FROM series WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:year IS NULL OR year = :year) " +
           "AND (:language IS NULL OR LOWER(language) = LOWER(:language)) " +
           "AND (:quality IS NULL OR LOWER(quality) = LOWER(:quality)) " +
           "AND (:network IS NULL OR LOWER(network) LIKE LOWER(CONCAT('%', :network, '%'))) " +
           "AND (:fileType IS NULL OR LOWER(file_type) = LOWER(:fileType)) " +
           "ORDER BY updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Series> advancedSearch(@Param("title") String title,
                             @Param("year") Integer year,
                             @Param("language") String language,
                             @Param("quality") String quality,
                             @Param("network") String network,
                             @Param("fileType") String fileType,
                             @Param("limit") int limit,
                             @Param("offset") long offset);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND tmdb_id = :tmdbId")
    Flux<Series> findByTmdbId(@Param("tmdbId") Integer tmdbId);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND imdb_id = :imdbId")
    Flux<Series> findByImdbId(@Param("imdbId") String imdbId);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Series> findByYear(@Param("year") int year, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(language) = LOWER(:language) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Series> findByLanguage(@Param("language") String language, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(network) = LOWER(:network) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Series> findByNetwork(@Param("network") String network, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Series> findAllSeriesPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query("INSERT INTO series (id, title, year, magnet, tmdb_id, imdb_id, language, original_language, quality, file_type, sha256_hash, is_deleted, created_at, updated_at, search_vector, size, seeds, peers, overview, poster_path, genres, torrent_url, trailer_url, seasons, episodes, network, status, episode_runtime) " +
           "VALUES (:#{#series.id}, :#{#series.title}, :#{#series.year}, :#{#series.magnet}, :#{#series.tmdbId}, :#{#series.imdbId}, " +
           ":#{#series.language}, :#{#series.originalLanguage}, :#{#series.quality}, :#{#series.fileType}, :#{#series.sha256Hash}, " +
           ":#{#series.isDeleted}, :#{#series.createdAt}, :#{#series.updatedAt}, :#{#series.searchVector}, :#{#series.size}, " +
           ":#{#series.seeds}, :#{#series.peers}, :#{#series.overview}, :#{#series.posterPath}, :#{#series.genres}, " +
           ":#{#series.torrentUrl}, :#{#series.trailerUrl}, :#{#series.seasons}, :#{#series.episodes}, :#{#series.network}, " +
           ":#{#series.status}, :#{#series.episodeRuntime}) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "title = EXCLUDED.title, year = EXCLUDED.year, magnet = EXCLUDED.magnet, tmdb_id = EXCLUDED.tmdb_id, " +
           "imdb_id = EXCLUDED.imdb_id, language = EXCLUDED.language, original_language = EXCLUDED.original_language, " +
           "quality = EXCLUDED.quality, file_type = EXCLUDED.file_type, sha256_hash = EXCLUDED.sha256_hash, " +
           "is_deleted = EXCLUDED.is_deleted, updated_at = EXCLUDED.updated_at, search_vector = EXCLUDED.search_vector, " +
           "size = EXCLUDED.size, seeds = EXCLUDED.seeds, peers = EXCLUDED.peers, overview = EXCLUDED.overview, " +
           "poster_path = EXCLUDED.poster_path, genres = EXCLUDED.genres, torrent_url = EXCLUDED.torrent_url, " +
           "trailer_url = EXCLUDED.trailer_url, seasons = EXCLUDED.seasons, episodes = EXCLUDED.episodes, " +
           "network = EXCLUDED.network, status = EXCLUDED.status, episode_runtime = EXCLUDED.episode_runtime " +
           "RETURNING *")
    Mono<Series> saveOrUpdate(Series series);

    @Query("SELECT COUNT(*) FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year")
    Mono<Long> countByYear(@Param("year") int year);

    @Query("SELECT language, COUNT(*) as count FROM series WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY language ORDER BY count DESC LIMIT :limit")
    Flux<LanguageCount> getTopLanguages(@Param("limit") int limit);

    @Query("SELECT year, COUNT(*) as count FROM series WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY year ORDER BY year DESC LIMIT :limit")
    Flux<YearCount> getSeriesCountByYear(@Param("limit") int limit);

    @Query("UPDATE series SET is_deleted = true WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT * FROM series WHERE (is_deleted = false OR is_deleted IS NULL) AND updated_at > :lastUpdateTime ORDER BY updated_at ASC LIMIT :limit OFFSET :offset")
    Flux<Series> findByUpdatedAtAfterOrderByUpdatedAtAsc(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, 
                                                     @Param("limit") int limit, 
                                                     @Param("offset") long offset);

    @Query("SELECT network, COUNT(*) as count FROM series WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY network ORDER BY count DESC LIMIT :limit")
    Flux<NetworkCount> getTopNetworks(@Param("limit") int limit);

    interface LanguageCount {
        String getLanguage();
        Long getCount();
    }

    interface YearCount {
        Integer getYear();
        Long getCount();
    }

    interface NetworkCount {
        String getNetwork();
        Long getCount();
    }
}