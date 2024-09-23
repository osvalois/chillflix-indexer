package com.chillflix.indexer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.Movie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends R2dbcRepository<Movie, UUID> {

    @Query("SELECT * FROM movies WHERE " +
           "to_tsvector('english', title || ' ' || COALESCE(description, '') || ' ' || CAST(year AS TEXT) || ' ' || " +
           "language || ' ' || original_language || ' ' || quality || ' ' || file_type) @@ " +
           "plainto_tsquery('english', :searchTerm) " +
           "OR LOWER(title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(year AS TEXT) = :searchTerm " +
           "OR LOWER(language) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(original_language) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(quality) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(file_type) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY ts_rank(to_tsvector('english', title || ' ' || COALESCE(description, '') || ' ' || " +
           "CAST(year AS TEXT) || ' ' || language || ' ' || original_language || ' ' || quality || ' ' || file_type), " +
           "plainto_tsquery('english', :searchTerm)) DESC, updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Movie> searchMovies(@Param("searchTerm") String searchTerm, 
                             @Param("limit") int limit, 
                             @Param("offset") long offset);

    @Query("SELECT * FROM movies WHERE " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:year IS NULL OR year = :year) " +
           "AND (:language IS NULL OR LOWER(language) = LOWER(:language)) " +
           "AND (:quality IS NULL OR LOWER(quality) = LOWER(:quality)) " +
           "AND (:fileType IS NULL OR LOWER(file_type) = LOWER(:fileType)) " +
           "ORDER BY updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Movie> advancedSearch(@Param("title") String title,
                               @Param("year") Integer year,
                               @Param("language") String language,
                               @Param("quality") String quality,
                               @Param("fileType") String fileType,
                               @Param("limit") int limit,
                               @Param("offset") long offset);

    @Query("SELECT * FROM movies WHERE tmdb_id = :tmdbId")
    Flux<Movie> findByTmdbId(@Param("tmdbId") Integer tmdbId);

    @Query("SELECT * FROM movies WHERE imdb_id = :imdbId")
    Flux<Movie> findByImdbId(@Param("imdbId") String imdbId);

    @Query("SELECT * FROM movies WHERE year = :year ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Movie> findByYear(@Param("year") int year, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM movies WHERE LOWER(language) = LOWER(:language) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Movie> findByLanguage(@Param("language") String language, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM movies ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Movie> findAllMoviesPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query("INSERT INTO movies (id, title, year, magnet, tmdb_id, imdb_id, language, original_language, quality, file_type, sha256_hash, created_at, updated_at, version) " +
           "VALUES (:#{#movie.id}, :#{#movie.title}, :#{#movie.year}, :#{#movie.magnet}, :#{#movie.tmdbId}, :#{#movie.imdbId}, " +
           ":#{#movie.language}, :#{#movie.originalLanguage}, :#{#movie.quality}, :#{#movie.fileType}, :#{#movie.sha256Hash}, " +
           ":#{#movie.createdAt}, :#{#movie.updatedAt}, :#{#movie.version}) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "title = EXCLUDED.title, year = EXCLUDED.year, magnet = EXCLUDED.magnet, tmdb_id = EXCLUDED.tmdb_id, " +
           "imdb_id = EXCLUDED.imdb_id, language = EXCLUDED.language, original_language = EXCLUDED.original_language, " +
           "quality = EXCLUDED.quality, file_type = EXCLUDED.file_type, sha256_hash = EXCLUDED.sha256_hash, " +
           "updated_at = EXCLUDED.updated_at, version = EXCLUDED.version + 1 " +
           "WHERE movies.version = :#{#movie.version} " +
           "RETURNING *")
    Mono<Movie> saveOrUpdate(Movie movie);

    @Query("SELECT COUNT(*) FROM movies WHERE year = :year")
    Mono<Long> countByYear(@Param("year") int year);

    @Query("SELECT language, COUNT(*) as count FROM movies GROUP BY language ORDER BY count DESC LIMIT :limit")
    Flux<LanguageCount> getTopLanguages(@Param("limit") int limit);

    @Query("SELECT year, COUNT(*) as count FROM movies GROUP BY year ORDER BY year DESC LIMIT :limit")
    Flux<YearCount> getMovieCountByYear(@Param("limit") int limit);

    @Query("DELETE FROM movies WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT * FROM movies WHERE updated_at > :lastUpdateTime ORDER BY updated_at ASC LIMIT :limit OFFSET :offset")
    Flux<Movie> findByUpdatedAtAfterOrderByUpdatedAtAsc(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, 
                                                        @Param("limit") int limit, 
                                                        @Param("offset") long offset);

    interface LanguageCount {
        String getLanguage();
        Long getCount();
    }

    interface YearCount {
        Integer getYear();
        Long getCount();
    }
}