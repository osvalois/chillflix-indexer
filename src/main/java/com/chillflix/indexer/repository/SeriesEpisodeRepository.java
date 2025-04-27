package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.SeriesEpisode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeriesEpisodeRepository extends R2dbcRepository<SeriesEpisode, UUID> {

    @Query("SELECT * FROM series_episodes WHERE series_id = :seriesId ORDER BY season_number ASC, episode_number ASC")
    Flux<SeriesEpisode> findBySeriesId(@Param("seriesId") UUID seriesId);

    @Query("SELECT * FROM series_episodes WHERE series_id = :seriesId AND season_number = :seasonNumber ORDER BY episode_number ASC")
    Flux<SeriesEpisode> findBySeriesIdAndSeasonNumber(@Param("seriesId") UUID seriesId, @Param("seasonNumber") Integer seasonNumber);

    @Query("SELECT * FROM series_episodes WHERE series_id = :seriesId AND season_number = :seasonNumber AND episode_number = :episodeNumber")
    Mono<SeriesEpisode> findBySeriesIdAndSeasonNumberAndEpisodeNumber(
            @Param("seriesId") UUID seriesId, 
            @Param("seasonNumber") Integer seasonNumber, 
            @Param("episodeNumber") Integer episodeNumber);

    @Query("INSERT INTO series_episodes (id, series_id, season_number, episode_number, title, overview, air_date, runtime, magnet, quality, size, file_type, sha256_hash, created_at, updated_at) " +
           "VALUES (:#{#episode.id}, :#{#episode.seriesId}, :#{#episode.seasonNumber}, :#{#episode.episodeNumber}, " +
           ":#{#episode.title}, :#{#episode.overview}, :#{#episode.airDate}, :#{#episode.runtime}, :#{#episode.magnet}, " +
           ":#{#episode.quality}, :#{#episode.size}, :#{#episode.fileType}, :#{#episode.sha256Hash}, " +
           ":#{#episode.createdAt}, :#{#episode.updatedAt}) " +
           "ON CONFLICT (series_id, season_number, episode_number) DO UPDATE SET " +
           "title = EXCLUDED.title, overview = EXCLUDED.overview, air_date = EXCLUDED.air_date, runtime = EXCLUDED.runtime, " +
           "magnet = EXCLUDED.magnet, quality = EXCLUDED.quality, size = EXCLUDED.size, file_type = EXCLUDED.file_type, " +
           "sha256_hash = EXCLUDED.sha256_hash, updated_at = EXCLUDED.updated_at " +
           "RETURNING *")
    Mono<SeriesEpisode> saveOrUpdate(SeriesEpisode episode);

    @Query("DELETE FROM series_episodes WHERE series_id = :seriesId")
    Mono<Void> deleteBySeriesId(@Param("seriesId") UUID seriesId);

    @Query("DELETE FROM series_episodes WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT COUNT(*) FROM series_episodes WHERE series_id = :seriesId")
    Mono<Long> countBySeriesId(@Param("seriesId") UUID seriesId);

    @Query("SELECT MAX(season_number) FROM series_episodes WHERE series_id = :seriesId")
    Mono<Integer> findMaxSeasonNumberBySeriesId(@Param("seriesId") UUID seriesId);
}