package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.MusicDTO;
import com.chillflix.indexer.exception.MusicNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.MusicMapper;
import com.chillflix.indexer.repository.MusicRepository;
import com.chillflix.indexer.util.MusicValidationUtil;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MusicService {

    private final MusicRepository musicRepository;
    private final MusicMapper musicMapper;
    private final Validator validator;
    private final MusicValidationUtil musicValidationUtil;

    @CircuitBreaker(name = "searchMusic", fallbackMethod = "searchMusicFallback")
    @RateLimiter(name = "searchMusic")
    public Flux<MusicDTO> searchMusic(String searchTerm, Pageable pageable) {
        log.debug("Searching music with term: {}", searchTerm);
        return musicRepository.searchMusic(searchTerm, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Flux<MusicDTO> searchMusicFallback(String searchTerm, Pageable pageable, Throwable t) {
        log.error("Fallback: Error searching music", t);
        return Flux.empty();
    }

    @Cacheable(cacheNames = "musicCache", key = "#id")
    public Mono<MusicDTO> getMusicById(UUID id) {
        log.debug("Fetching music with id: {}", id);
        return musicRepository.findById(id)
                .map(musicMapper::toDto)
                .switchIfEmpty(Mono.error(new MusicNotFoundException("Music not found with id: " + id)));
    }

    public Flux<MusicDTO> getMusicByArtist(String artist, Pageable pageable) {
        log.debug("Fetching music by artist: {}", artist);
        return musicRepository.findByArtist(artist, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Flux<MusicDTO> getMusicByAlbum(String album, Pageable pageable) {
        log.debug("Fetching music by album: {}", album);
        return musicRepository.findByAlbum(album, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Flux<MusicDTO> getMusicByGenre(String genre, Pageable pageable) {
        log.debug("Fetching music by genre: {}", genre);
        return musicRepository.findByGenre(genre, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Mono<MusicDTO> saveMusic(Mono<MusicDTO> musicDTO) {
        log.debug("Saving new music");
        return musicDTO
                .flatMap(this::validateMusic)
                .map(musicMapper::toEntity)
                .flatMap(music -> {
                    music.setCreatedAt(LocalDateTime.now());
                    music.setUpdatedAt(LocalDateTime.now());
                    return musicRepository.saveOrUpdate(music);
                })
                .map(musicMapper::toDto)
                .doOnSuccess(savedMusic -> log.info("Successfully saved music with id: {}", savedMusic.id()))
                .doOnError(error -> log.error("Error saving music", error));
    }

    public Mono<MusicDTO> updateMusic(UUID id, Mono<MusicDTO> musicDTO) {
        log.debug("Updating music with id: {}", id);
        return musicRepository.findById(id)
                .flatMap(existingMusic ->
                        musicDTO.flatMap(this::validateMusic)
                                .map(dto -> {
                                    musicMapper.updateEntityFromDto(dto, existingMusic);
                                    existingMusic.setUpdatedAt(LocalDateTime.now());
                                    return existingMusic;
                                })
                )
                .flatMap(musicRepository::saveOrUpdate)
                .map(musicMapper::toDto)
                .switchIfEmpty(Mono.error(new MusicNotFoundException("Music not found with id: " + id)))
                .doOnSuccess(updatedMusic -> log.info("Successfully updated music with id: {}", updatedMusic.id()))
                .doOnError(error -> log.error("Error updating music with id: {}", id, error));
    }

    public Mono<Void> deleteMusic(UUID id) {
        log.debug("Deleting music with id: {}", id);
        return musicRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted music with id: {}", id))
                .doOnError(error -> log.error("Error deleting music with id: {}", id, error));
    }

    public Mono<MusicDTO> createOrUpdateMusic(Mono<MusicDTO> musicDTO) {
        return musicDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateMusic(dto.id(), Mono.just(dto));
            } else {
                return saveMusic(Mono.just(dto));
            }
        });
    }

    @Cacheable(cacheNames = "allMusicCache", key = "#pageable")
    public Flux<MusicDTO> getAllMusic(Pageable pageable) {
        log.debug("Fetching all music with pagination");
        return musicRepository.findAllMusicPaginated(pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Mono<Long> countMusic() {
        return musicRepository.count();
    }

    public Flux<MusicDTO> getMusicByYear(int year, Pageable pageable) {
        log.debug("Fetching music for year: {}", year);
        return musicRepository.findByYear(year, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }

    public Mono<Void> deleteMusicByIds(List<UUID> ids) {
        log.debug("Deleting music with ids: {}", ids);
        return musicRepository.deleteAllByIdIn(ids)
                .doOnSuccess(__ -> log.info("Successfully deleted music with ids: {}", ids))
                .doOnError(error -> log.error("Error deleting music with ids: {}", ids, error));
    }

    public Mono<Long> countMusicByYear(int year) {
        return musicRepository.countByYear(year)
                .doOnSuccess(count -> log.debug("Count of music for year {}: {}", year, count))
                .doOnError(error -> log.error("Error counting music for year: {}", year, error));
    }

    public Flux<MusicRepository.GenreCount> getTopGenres(int limit) {
        return musicRepository.getTopGenres(limit)
                .doOnComplete(() -> log.debug("Fetched top {} genres", limit))
                .doOnError(error -> log.error("Error fetching top genres", error));
    }

    public Flux<MusicRepository.ArtistCount> getTopArtists(int limit) {
        return musicRepository.getTopArtists(limit)
                .doOnComplete(() -> log.debug("Fetched top {} artists", limit))
                .doOnError(error -> log.error("Error fetching top artists", error));
    }

    @Transactional
    public Mono<MusicDTO> saveOrUpdateMusic(MusicDTO musicDTO) {
        log.debug("Saving or updating music: {}", musicDTO);
        return Mono.just(musicDTO)
                .flatMap(this::validateMusic)
                .map(musicMapper::toEntity)
                .flatMap(music -> {
                    music.setUpdatedAt(LocalDateTime.now());
                    if (music.getCreatedAt() == null) {
                        music.setCreatedAt(LocalDateTime.now());
                    }
                    return musicRepository.saveOrUpdate(music);
                })
                .map(musicMapper::toDto)
                .doOnSuccess(savedMusic -> log.info("Successfully saved/updated music with id: {}", savedMusic.id()))
                .doOnError(error -> log.error("Error saving/updating music", error));
    }

    private Mono<MusicDTO> validateMusic(MusicDTO musicDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(musicDTO, "musicDTO");
            validator.validate(musicDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return musicDTO;
        });
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteMusicFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteMusic(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} music items", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteMusic)
                .then();
    }

    public Mono<Void> bulkDeleteMusicFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateMusicFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<MusicDTO> bulkUpdateMusic(List<MusicDTO> musicDTOs) {
        log.debug("Performing bulk update operation for {} music items", musicDTOs.size());
        return Flux.fromIterable(musicDTOs)
                .flatMap(dto -> saveOrUpdateMusic(dto));
    }

    public Flux<MusicDTO> bulkUpdateMusicFallback(List<MusicDTO> musicDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Flux<MusicDTO> advancedSearch(String title, Integer year, String language, String quality, String fileType, Pageable pageable) {
        return musicRepository.advancedSearch(title, null, null, year, null, pageable.getPageSize(), pageable.getOffset())
                .map(musicMapper::toDto);
    }
    
    public Flux<MusicDTO> getMusicByLanguage(String language, Pageable pageable) {
        log.debug("Fetching music by language: {}", language);
        return musicRepository.findAllMusicPaginated(pageable.getPageSize(), pageable.getOffset())
                // Implementar el filtro basado en los campos disponibles en Music
                .filter(music -> music.getArtist() != null && music.getArtist().contains(language))
                .map(musicMapper::toDto);
    }
    
    public Flux<MusicRepository.LanguageCount> getTopLanguages(int limit) {
        log.debug("Fetching top {} languages", limit);
        return musicRepository.getTopLanguages(limit)
                .doOnComplete(() -> log.debug("Fetched top languages"))
                .doOnError(error -> log.error("Error fetching top languages", error));
    }
    
    public Flux<MusicRepository.YearCount> getMusicCountByYear(int limit) {
        log.debug("Fetching music count by year, limit: {}", limit);
        return musicRepository.getMusicCountByYear(limit)
                .doOnComplete(() -> log.debug("Fetched music count by year"))
                .doOnError(error -> log.error("Error fetching music count by year", error));
    }
}