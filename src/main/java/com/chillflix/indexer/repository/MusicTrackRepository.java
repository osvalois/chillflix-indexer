package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.MusicTrack;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface MusicTrackRepository extends R2dbcRepository<MusicTrack, UUID> {

    @Query("SELECT * FROM music_tracks WHERE album_id = :albumId ORDER BY track_number ASC")
    Flux<MusicTrack> findByAlbumId(@Param("albumId") UUID albumId);

    @Query("SELECT * FROM music_tracks WHERE album_id = :albumId AND track_number = :trackNumber")
    Mono<MusicTrack> findByAlbumIdAndTrackNumber(@Param("albumId") UUID albumId, @Param("trackNumber") Integer trackNumber);

    @Query("INSERT INTO music_tracks (id, album_id, track_number, title, artist, duration, file_path, file_type, sha256_hash, created_at, updated_at) " +
           "VALUES (:#{#track.id}, :#{#track.albumId}, :#{#track.trackNumber}, :#{#track.title}, :#{#track.artist}, " +
           ":#{#track.duration}, :#{#track.filePath}, :#{#track.fileType}, :#{#track.sha256Hash}, :#{#track.createdAt}, :#{#track.updatedAt}) " +
           "ON CONFLICT (album_id, track_number) DO UPDATE SET " +
           "title = EXCLUDED.title, artist = EXCLUDED.artist, duration = EXCLUDED.duration, " +
           "file_path = EXCLUDED.file_path, file_type = EXCLUDED.file_type, sha256_hash = EXCLUDED.sha256_hash, " +
           "updated_at = EXCLUDED.updated_at " +
           "RETURNING *")
    Mono<MusicTrack> saveOrUpdate(MusicTrack track);

    @Query("DELETE FROM music_tracks WHERE album_id = :albumId")
    Mono<Void> deleteByAlbumId(@Param("albumId") UUID albumId);

    @Query("DELETE FROM music_tracks WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);
}