package com.chillflix.indexer.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chillflix.indexer.entities.Music;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MusicRepository extends R2dbcRepository<Music, UUID> {

    @Query("SELECT * FROM music WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(to_tsvector('english', title || ' ' || artist || ' ' || COALESCE(album, '') || ' ' || CAST(year AS TEXT) || ' ' || " +
           "COALESCE(genre, '') || ' ' || COALESCE(description, '')) @@ " +
           "plainto_tsquery('english', :searchTerm) " +
           "OR LOWER(title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(artist) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(album) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR CAST(year AS TEXT) = :searchTerm " +
           "OR LOWER(genre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY ts_rank(to_tsvector('english', title || ' ' || artist || ' ' || COALESCE(album, '') || ' ' || " +
           "CAST(year AS TEXT) || ' ' || COALESCE(genre, '') || ' ' || COALESCE(description, '')), " +
           "plainto_tsquery('english', :searchTerm)) DESC, updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Music> searchMusic(@Param("searchTerm") String searchTerm, 
                         @Param("limit") int limit, 
                         @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE " +
           "(is_deleted = false OR is_deleted IS NULL) AND " +
           "(:title IS NULL OR LOWER(title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:artist IS NULL OR LOWER(artist) LIKE LOWER(CONCAT('%', :artist, '%'))) " +
           "AND (:album IS NULL OR LOWER(album) LIKE LOWER(CONCAT('%', :album, '%'))) " +
           "AND (:year IS NULL OR year = :year) " +
           "AND (:genre IS NULL OR LOWER(genre) LIKE LOWER(CONCAT('%', :genre, '%'))) " +
           "ORDER BY updated_at DESC " +
           "LIMIT :limit OFFSET :offset")
    Flux<Music> advancedSearch(@Param("title") String title,
                            @Param("artist") String artist,
                            @Param("album") String album,
                            @Param("year") Integer year,
                            @Param("genre") String genre,
                            @Param("limit") int limit,
                            @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Music> findByYear(@Param("year") int year, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(artist) LIKE LOWER(CONCAT('%', :artist, '%')) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Music> findByArtist(@Param("artist") String artist, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(album) LIKE LOWER(CONCAT('%', :album, '%')) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Music> findByAlbum(@Param("album") String album, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND LOWER(genre) = LOWER(:genre) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Music> findByGenre(@Param("genre") String genre, @Param("limit") int limit, @Param("offset") long offset);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    Flux<Music> findAllMusicPaginated(@Param("limit") int limit, @Param("offset") long offset);

    @Query("INSERT INTO music (id, title, artist, album, year, genre, track_count, magnet, quality, file_type, size, sha256_hash, seeds, peers, cover_path, description, label, release_date, torrent_url, is_deleted, created_at, updated_at, search_vector) " +
           "VALUES (:#{#music.id}, :#{#music.title}, :#{#music.artist}, :#{#music.album}, :#{#music.year}, :#{#music.genre}, " +
           ":#{#music.trackCount}, :#{#music.magnet}, :#{#music.quality}, :#{#music.fileType}, :#{#music.size}, :#{#music.sha256Hash}, " +
           ":#{#music.seeds}, :#{#music.peers}, :#{#music.coverPath}, :#{#music.description}, :#{#music.label}, " +
           ":#{#music.releaseDate}, :#{#music.torrentUrl}, :#{#music.isDeleted}, :#{#music.createdAt}, :#{#music.updatedAt}, " +
           ":#{#music.searchVector}) " +
           "ON CONFLICT (id) DO UPDATE SET " +
           "title = EXCLUDED.title, artist = EXCLUDED.artist, album = EXCLUDED.album, year = EXCLUDED.year, " +
           "genre = EXCLUDED.genre, track_count = EXCLUDED.track_count, magnet = EXCLUDED.magnet, quality = EXCLUDED.quality, " +
           "file_type = EXCLUDED.file_type, size = EXCLUDED.size, sha256_hash = EXCLUDED.sha256_hash, " +
           "seeds = EXCLUDED.seeds, peers = EXCLUDED.peers, cover_path = EXCLUDED.cover_path, description = EXCLUDED.description, " +
           "label = EXCLUDED.label, release_date = EXCLUDED.release_date, torrent_url = EXCLUDED.torrent_url, " +
           "is_deleted = EXCLUDED.is_deleted, updated_at = EXCLUDED.updated_at, search_vector = EXCLUDED.search_vector " +
           "RETURNING *")
    Mono<Music> saveOrUpdate(Music music);

    @Query("SELECT COUNT(*) FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND year = :year")
    Mono<Long> countByYear(@Param("year") int year);

    @Query("SELECT genre, COUNT(*) as count FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND genre IS NOT NULL GROUP BY genre ORDER BY count DESC LIMIT :limit")
    Flux<GenreCount> getTopGenres(@Param("limit") int limit);

    @Query("SELECT artist, COUNT(*) as count FROM music WHERE (is_deleted = false OR is_deleted IS NULL) GROUP BY artist ORDER BY count DESC LIMIT :limit")
    Flux<ArtistCount> getTopArtists(@Param("limit") int limit);

    @Query("UPDATE music SET is_deleted = true WHERE id IN (:ids)")
    Mono<Void> deleteAllByIdIn(@Param("ids") List<UUID> ids);

    @Query("SELECT * FROM music WHERE (is_deleted = false OR is_deleted IS NULL) AND updated_at > :lastUpdateTime ORDER BY updated_at ASC LIMIT :limit OFFSET :offset")
    Flux<Music> findByUpdatedAtAfterOrderByUpdatedAtAsc(@Param("lastUpdateTime") LocalDateTime lastUpdateTime, 
                                                     @Param("limit") int limit, 
                                                     @Param("offset") long offset);

    interface GenreCount {
        String getGenre();
        Long getCount();
    }

    interface ArtistCount {
        String getArtist();
        Long getCount();
    }
}