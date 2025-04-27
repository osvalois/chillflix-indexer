package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.MusicTrackDTO;
import com.chillflix.indexer.exception.MusicNotFoundException;
import com.chillflix.indexer.exception.MusicTrackNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.MusicTrackMapper;
import com.chillflix.indexer.repository.MusicRepository;
import com.chillflix.indexer.repository.MusicTrackRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class MusicTrackService {

    private final MusicTrackRepository trackRepository;
    private final MusicRepository musicRepository;
    private final MusicTrackMapper trackMapper;
    private final Validator validator;

    public Mono<MusicTrackDTO> getTrackById(UUID id) {
        log.debug("Fetching track with id: {}", id);
        return trackRepository.findById(id)
                .map(trackMapper::toDto)
                .switchIfEmpty(Mono.error(new MusicTrackNotFoundException("Track not found with id: " + id)));
    }

    public Flux<MusicTrackDTO> getTracksByAlbumId(UUID albumId) {
        log.debug("Fetching tracks for album with id: {}", albumId);
        return musicRepository.findById(albumId)
                .switchIfEmpty(Mono.error(new MusicNotFoundException("Album not found with id: " + albumId)))
                .thenMany(trackRepository.findByAlbumId(albumId))
                .map(trackMapper::toDto);
    }

    public Mono<MusicTrackDTO> getTrackByAlbumIdAndTrackNumber(UUID albumId, Integer trackNumber) {
        log.debug("Fetching track for album id: {} and track number: {}", albumId, trackNumber);
        return musicRepository.findById(albumId)
                .switchIfEmpty(Mono.error(new MusicNotFoundException("Album not found with id: " + albumId)))
                .then(trackRepository.findByAlbumIdAndTrackNumber(albumId, trackNumber))
                .map(trackMapper::toDto)
                .switchIfEmpty(Mono.error(new MusicTrackNotFoundException(
                        "Track not found for album id: " + albumId + 
                        ", track number: " + trackNumber)));
    }

    public Mono<MusicTrackDTO> saveTrack(Mono<MusicTrackDTO> trackDTO) {
        log.debug("Saving new track");
        return trackDTO
                .flatMap(this::validateTrack)
                .flatMap(dto -> 
                    musicRepository.findById(dto.albumId())
                        .switchIfEmpty(Mono.error(new MusicNotFoundException("Album not found with id: " + dto.albumId())))
                        .thenReturn(dto)
                )
                .map(trackMapper::toEntity)
                .flatMap(track -> {
                    track.setCreatedAt(LocalDateTime.now());
                    track.setUpdatedAt(LocalDateTime.now());
                    return trackRepository.saveOrUpdate(track);
                })
                .map(trackMapper::toDto)
                .doOnSuccess(savedTrack -> log.info("Successfully saved track with id: {}", savedTrack.id()))
                .doOnError(error -> log.error("Error saving track", error));
    }

    public Mono<MusicTrackDTO> updateTrack(UUID id, Mono<MusicTrackDTO> trackDTO) {
        log.debug("Updating track with id: {}", id);
        return trackRepository.findById(id)
                .flatMap(existingTrack ->
                        trackDTO.flatMap(this::validateTrack)
                                .flatMap(dto -> 
                                    musicRepository.findById(dto.albumId())
                                        .switchIfEmpty(Mono.error(new MusicNotFoundException("Album not found with id: " + dto.albumId())))
                                        .thenReturn(dto)
                                )
                                .map(dto -> {
                                    trackMapper.updateEntityFromDto(dto, existingTrack);
                                    existingTrack.setUpdatedAt(LocalDateTime.now());
                                    return existingTrack;
                                })
                )
                .flatMap(trackRepository::saveOrUpdate)
                .map(trackMapper::toDto)
                .switchIfEmpty(Mono.error(new MusicTrackNotFoundException("Track not found with id: " + id)))
                .doOnSuccess(updatedTrack -> log.info("Successfully updated track with id: {}", updatedTrack.id()))
                .doOnError(error -> log.error("Error updating track with id: {}", id, error));
    }

    public Mono<Void> deleteTrack(UUID id) {
        log.debug("Deleting track with id: {}", id);
        return trackRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted track with id: {}", id))
                .doOnError(error -> log.error("Error deleting track with id: {}", id, error));
    }

    public Mono<Void> deleteTracksByAlbumId(UUID albumId) {
        log.debug("Deleting all tracks for album with id: {}", albumId);
        return trackRepository.deleteByAlbumId(albumId)
                .doOnSuccess(__ -> log.info("Successfully deleted all tracks for album with id: {}", albumId))
                .doOnError(error -> log.error("Error deleting tracks for album with id: {}", albumId, error));
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteTracksFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteTracks(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} tracks", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteTrack)
                .then();
    }

    public Mono<Void> bulkDeleteTracksFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkSaveTracksFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<MusicTrackDTO> bulkSaveTracks(List<MusicTrackDTO> trackDTOs) {
        log.debug("Performing bulk save operation for {} tracks", trackDTOs.size());
        return Flux.fromIterable(trackDTOs)
                .flatMap(dto -> saveTrack(Mono.just(dto)));
    }

    public Flux<MusicTrackDTO> bulkSaveTracksFallback(List<MusicTrackDTO> trackDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk save operation", t);
        return Flux.empty();
    }

    @Transactional
    public Mono<MusicTrackDTO> saveOrUpdateTrack(MusicTrackDTO trackDTO) {
        log.debug("Saving or updating track: {}", trackDTO);
        return Mono.just(trackDTO)
                .flatMap(this::validateTrack)
                .flatMap(dto -> 
                    musicRepository.findById(dto.albumId())
                        .switchIfEmpty(Mono.error(new MusicNotFoundException("Album not found with id: " + dto.albumId())))
                        .thenReturn(dto)
                )
                .map(trackMapper::toEntity)
                .flatMap(track -> {
                    track.setUpdatedAt(LocalDateTime.now());
                    if (track.getCreatedAt() == null) {
                        track.setCreatedAt(LocalDateTime.now());
                    }
                    return trackRepository.saveOrUpdate(track);
                })
                .map(trackMapper::toDto)
                .doOnSuccess(savedTrack -> log.info("Successfully saved/updated track with id: {}", savedTrack.id()))
                .doOnError(error -> log.error("Error saving/updating track", error));
    }

    private Mono<MusicTrackDTO> validateTrack(MusicTrackDTO trackDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(trackDTO, "trackDTO");
            validator.validate(trackDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return trackDTO;
        });
    }
}