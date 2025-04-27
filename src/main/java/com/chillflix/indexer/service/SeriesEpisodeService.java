package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.SeriesEpisodeDTO;
import com.chillflix.indexer.exception.EpisodeNotFoundException;
import com.chillflix.indexer.exception.SeriesNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.SeriesEpisodeMapper;
import com.chillflix.indexer.repository.SeriesEpisodeRepository;
import com.chillflix.indexer.repository.SeriesRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SeriesEpisodeService {

    private final SeriesEpisodeRepository episodeRepository;
    private final SeriesRepository seriesRepository;
    private final SeriesEpisodeMapper episodeMapper;
    private final Validator validator;

    public Mono<SeriesEpisodeDTO> getEpisodeById(UUID id) {
        log.debug("Fetching episode with id: {}", id);
        return episodeRepository.findById(id)
                .map(episodeMapper::toDto)
                .switchIfEmpty(Mono.error(new EpisodeNotFoundException("Episode not found with id: " + id)));
    }

    public Flux<SeriesEpisodeDTO> getEpisodesBySeriesId(UUID seriesId, Pageable pageable) {
        log.debug("Fetching episodes for series with id: {}", seriesId);
        return seriesRepository.findById(seriesId)
                .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + seriesId)))
                .thenMany(episodeRepository.findBySeriesId(seriesId))
                .map(episodeMapper::toDto);
    }

    public Flux<SeriesEpisodeDTO> getEpisodesBySeriesIdAndSeason(UUID seriesId, Integer seasonNumber, Pageable pageable) {
        log.debug("Fetching episodes for series id: {} and season: {}", seriesId, seasonNumber);
        return seriesRepository.findById(seriesId)
                .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + seriesId)))
                .thenMany(episodeRepository.findBySeriesIdAndSeasonNumber(seriesId, seasonNumber))
                .map(episodeMapper::toDto);
    }

    public Mono<SeriesEpisodeDTO> saveEpisode(Mono<SeriesEpisodeDTO> episodeDTO) {
        log.debug("Saving new episode");
        return episodeDTO
                .flatMap(this::validateEpisode)
                .flatMap(dto -> 
                    seriesRepository.findById(dto.seriesId())
                        .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + dto.seriesId())))
                        .thenReturn(dto)
                )
                .map(episodeMapper::toEntity)
                .flatMap(episode -> {
                    episode.setCreatedAt(LocalDateTime.now());
                    episode.setUpdatedAt(LocalDateTime.now());
                    return episodeRepository.saveOrUpdate(episode);
                })
                .map(episodeMapper::toDto)
                .doOnSuccess(savedEpisode -> log.info("Successfully saved episode with id: {}", savedEpisode.id()))
                .doOnError(error -> log.error("Error saving episode", error));
    }

    public Mono<SeriesEpisodeDTO> updateEpisode(UUID id, Mono<SeriesEpisodeDTO> episodeDTO) {
        log.debug("Updating episode with id: {}", id);
        return episodeRepository.findById(id)
                .flatMap(existingEpisode ->
                        episodeDTO.flatMap(this::validateEpisode)
                                .flatMap(dto -> 
                                    seriesRepository.findById(dto.seriesId())
                                        .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + dto.seriesId())))
                                        .thenReturn(dto)
                                )
                                .map(dto -> {
                                    episodeMapper.updateEntityFromDto(dto, existingEpisode);
                                    existingEpisode.setUpdatedAt(LocalDateTime.now());
                                    return existingEpisode;
                                })
                )
                .flatMap(episodeRepository::saveOrUpdate)
                .map(episodeMapper::toDto)
                .switchIfEmpty(Mono.error(new EpisodeNotFoundException("Episode not found with id: " + id)))
                .doOnSuccess(updatedEpisode -> log.info("Successfully updated episode with id: {}", updatedEpisode.id()))
                .doOnError(error -> log.error("Error updating episode with id: {}", id, error));
    }

    public Mono<Void> deleteEpisode(UUID id) {
        log.debug("Deleting episode with id: {}", id);
        return episodeRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted episode with id: {}", id))
                .doOnError(error -> log.error("Error deleting episode with id: {}", id, error));
    }

    public Mono<SeriesEpisodeDTO> createOrUpdateEpisode(Mono<SeriesEpisodeDTO> episodeDTO) {
        return episodeDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateEpisode(dto.id(), Mono.just(dto));
            } else {
                return saveEpisode(Mono.just(dto));
            }
        });
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteEpisodesFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteEpisodes(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} episodes", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteEpisode)
                .then();
    }

    public Mono<Void> bulkDeleteEpisodesFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateEpisodesFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<SeriesEpisodeDTO> bulkUpdateEpisodes(List<SeriesEpisodeDTO> episodeDTOs) {
        log.debug("Performing bulk update operation for {} episodes", episodeDTOs.size());
        return Flux.fromIterable(episodeDTOs)
                .flatMap(dto -> createOrUpdateEpisode(Mono.just(dto)));
    }

    public Flux<SeriesEpisodeDTO> bulkUpdateEpisodesFallback(List<SeriesEpisodeDTO> episodeDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Mono<Long> countEpisodes() {
        return episodeRepository.count();
    }

    private Mono<SeriesEpisodeDTO> validateEpisode(SeriesEpisodeDTO episodeDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(episodeDTO, "episodeDTO");
            validator.validate(episodeDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return episodeDTO;
        });
    }

    // Additional methods for the controller - placeholder implementations
    
    public Flux<SeriesEpisodeDTO> getAllEpisodes(Pageable pageable) {
        // Implement pagination logic
        return episodeRepository.findAll()
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(episodeMapper::toDto);
    }

    public Flux<SeriesEpisodeDTO> searchEpisodes(String term, Pageable pageable) {
        // Implement search logic - this is a placeholder
        return episodeRepository.findAll()
                .filter(episode -> episode.getTitle() != null && episode.getTitle().toLowerCase().contains(term.toLowerCase()))
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(episodeMapper::toDto);
    }

    public Flux<SeriesEpisodeDTO> advancedSearch(String title, UUID seriesId, Integer seasonNumber, 
                                              Integer episodeNumber, String language, String quality, 
                                              String fileType, Pageable pageable) {
        // Implement more advanced search logic - this is a placeholder
        return episodeRepository.findAll()
                .filter(episode -> 
                    (title == null || (episode.getTitle() != null && episode.getTitle().toLowerCase().contains(title.toLowerCase()))) &&
                    (seriesId == null || episode.getSeriesId().equals(seriesId)) &&
                    (seasonNumber == null || episode.getSeasonNumber().equals(seasonNumber)) &&
                    (episodeNumber == null || episode.getEpisodeNumber().equals(episodeNumber)) &&
                    (language == null || (episode.getTitle() != null && episode.getTitle().toLowerCase().contains(language.toLowerCase()))) &&
                    (quality == null || (episode.getQuality() != null && episode.getQuality().toLowerCase().contains(quality.toLowerCase()))) &&
                    (fileType == null || (episode.getFileType() != null && episode.getFileType().toLowerCase().contains(fileType.toLowerCase())))
                )
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(episodeMapper::toDto);
    }

    public Flux<SeriesEpisodeDTO> getEpisodesByLanguage(String language, Pageable pageable) {
        // Placeholder implementation - in real application, you would query the database
        return episodeRepository.findAll()
                // In reality, we would filter by a language field, but SeriesEpisode doesn't have one
                // This is just a placeholder implementation
                .skip(pageable.getOffset())
                .take(pageable.getPageSize())
                .map(episodeMapper::toDto);
    }

    public Flux<SeriesEpisodeDTO> getEpisodesByTmdbId(Integer tmdbId) {
        // Placeholder implementation - in real application, you would query the database
        return Flux.empty();
    }
}