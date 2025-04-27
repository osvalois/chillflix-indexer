package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.SeriesDTO;
import com.chillflix.indexer.exception.SeriesNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.SeriesMapper;
import com.chillflix.indexer.repository.SeriesRepository;
import com.chillflix.indexer.util.SeriesValidationUtil;

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
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeriesMapper seriesMapper;
    private final Validator validator;
    private final SeriesValidationUtil seriesValidationUtil;

    @CircuitBreaker(name = "searchSeries", fallbackMethod = "searchSeriesFallback")
    @RateLimiter(name = "searchSeries")
    public Flux<SeriesDTO> searchSeries(String searchTerm, Pageable pageable) {
        log.debug("Searching series with term: {}", searchTerm);
        return seriesRepository.searchSeries(searchTerm, pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }

    public Flux<SeriesDTO> searchSeriesFallback(String searchTerm, Pageable pageable, Throwable t) {
        log.error("Fallback: Error searching series", t);
        return Flux.empty();
    }

    @Cacheable(cacheNames = "seriesCache", key = "#id")
    public Mono<SeriesDTO> getSeriesById(UUID id) {
        log.debug("Fetching series with id: {}", id);
        return seriesRepository.findById(id)
                .map(seriesMapper::toDto)
                .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + id)));
    }

    public Flux<SeriesDTO> getSeriesByTmdbId(Integer tmdbId) {
        log.debug("Fetching series with TMDB id: {}", tmdbId);
        return seriesRepository.findByTmdbId(tmdbId)
                .map(seriesMapper::toDto);
    }
    
    public Flux<SeriesDTO> getSeriesByImdbId(String imdbId) {
        log.debug("Fetching series with IMDB id: {}", imdbId);
        return seriesRepository.findByImdbId(imdbId)
                .map(seriesMapper::toDto);
    }

    public Mono<SeriesDTO> saveSeries(Mono<SeriesDTO> seriesDTO) {
        log.debug("Saving new series");
        return seriesDTO
                .flatMap(this::validateSeries)
                .map(seriesMapper::toEntity)
                .flatMap(series -> {
                    series.setCreatedAt(LocalDateTime.now());
                    series.setUpdatedAt(LocalDateTime.now());
                    return seriesRepository.saveOrUpdate(series);
                })
                .map(seriesMapper::toDto)
                .doOnSuccess(savedSeries -> log.info("Successfully saved series with id: {}", savedSeries.id()))
                .doOnError(error -> log.error("Error saving series", error));
    }

    public Mono<SeriesDTO> updateSeries(UUID id, Mono<SeriesDTO> seriesDTO) {
        log.debug("Updating series with id: {}", id);
        return seriesRepository.findById(id)
                .flatMap(existingSeries ->
                        seriesDTO.flatMap(this::validateSeries)
                                .map(dto -> {
                                    seriesMapper.updateEntityFromDto(dto, existingSeries);
                                    existingSeries.setUpdatedAt(LocalDateTime.now());
                                    return existingSeries;
                                })
                )
                .flatMap(seriesRepository::saveOrUpdate)
                .map(seriesMapper::toDto)
                .switchIfEmpty(Mono.error(new SeriesNotFoundException("Series not found with id: " + id)))
                .doOnSuccess(updatedSeries -> log.info("Successfully updated series with id: {}", updatedSeries.id()))
                .doOnError(error -> log.error("Error updating series with id: {}", id, error));
    }

    public Mono<Void> deleteSeries(UUID id) {
        log.debug("Deleting series with id: {}", id);
        return seriesRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted series with id: {}", id))
                .doOnError(error -> log.error("Error deleting series with id: {}", id, error));
    }

    public Mono<SeriesDTO> createOrUpdateSeries(Mono<SeriesDTO> seriesDTO) {
        return seriesDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateSeries(dto.id(), Mono.just(dto));
            } else {
                return saveSeries(Mono.just(dto));
            }
        });
    }

    @Cacheable(cacheNames = "allSeriesCache", key = "#pageable")
    public Flux<SeriesDTO> getAllSeries(Pageable pageable) {
        log.debug("Fetching all series with pagination");
        return seriesRepository.findAllSeriesPaginated(pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }

    public Mono<Long> countSeries() {
        return seriesRepository.count();
    }

    public Flux<SeriesDTO> getSeriesByYear(int year, Pageable pageable) {
        log.debug("Fetching series for year: {}", year);
        return seriesRepository.findByYear(year, pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }

    public Flux<SeriesDTO> getSeriesByLanguage(String language, Pageable pageable) {
        log.debug("Fetching series in language: {}", language);
        return seriesRepository.findByLanguage(language, pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }

    public Flux<SeriesDTO> getSeriesByNetwork(String network, Pageable pageable) {
        log.debug("Fetching series by network: {}", network);
        return seriesRepository.findByNetwork(network, pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }

    public Mono<Void> deleteSeriesByIds(List<UUID> ids) {
        log.debug("Deleting series with ids: {}", ids);
        return seriesRepository.deleteAllByIdIn(ids)
                .doOnSuccess(__ -> log.info("Successfully deleted series with ids: {}", ids))
                .doOnError(error -> log.error("Error deleting series with ids: {}", ids, error));
    }

    public Mono<Long> countSeriesByYear(int year) {
        return seriesRepository.countByYear(year)
                .doOnSuccess(count -> log.debug("Count of series for year {}: {}", year, count))
                .doOnError(error -> log.error("Error counting series for year: {}", year, error));
    }

    public Flux<SeriesRepository.LanguageCount> getTopLanguages(int limit) {
        return seriesRepository.getTopLanguages(limit)
                .doOnComplete(() -> log.debug("Fetched top {} languages", limit))
                .doOnError(error -> log.error("Error fetching top languages", error));
    }

    public Flux<SeriesRepository.YearCount> getSeriesCountByYear(int limit) {
        return seriesRepository.getSeriesCountByYear(limit)
                .doOnComplete(() -> log.debug("Fetched series count by year, limit: {}", limit))
                .doOnError(error -> log.error("Error fetching series count by year", error));
    }

    public Flux<SeriesRepository.NetworkCount> getTopNetworks(int limit) {
        return seriesRepository.getTopNetworks(limit)
                .doOnComplete(() -> log.debug("Fetched top {} networks", limit))
                .doOnError(error -> log.error("Error fetching top networks", error));
    }

    @Transactional
    public Mono<SeriesDTO> saveOrUpdateSeries(SeriesDTO seriesDTO) {
        log.debug("Saving or updating series: {}", seriesDTO);
        return Mono.just(seriesDTO)
                .flatMap(this::validateSeries)
                .map(seriesMapper::toEntity)
                .flatMap(series -> {
                    series.setUpdatedAt(LocalDateTime.now());
                    if (series.getCreatedAt() == null) {
                        series.setCreatedAt(LocalDateTime.now());
                    }
                    return seriesRepository.saveOrUpdate(series);
                })
                .map(seriesMapper::toDto)
                .doOnSuccess(savedSeries -> log.info("Successfully saved/updated series with id: {}", savedSeries.id()))
                .doOnError(error -> log.error("Error saving/updating series", error));
    }

    private Mono<SeriesDTO> validateSeries(SeriesDTO seriesDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(seriesDTO, "seriesDTO");
            validator.validate(seriesDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return seriesDTO;
        });
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteSeriesFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteSeries(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} series", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteSeries)
                .then();
    }

    public Mono<Void> bulkDeleteSeriesFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateSeriesFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<SeriesDTO> bulkUpdateSeries(List<SeriesDTO> seriesDTOs) {
        log.debug("Performing bulk update operation for {} series", seriesDTOs.size());
        return Flux.fromIterable(seriesDTOs)
                .flatMap(dto -> saveOrUpdateSeries(dto));
    }

    public Flux<SeriesDTO> bulkUpdateSeriesFallback(List<SeriesDTO> seriesDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Flux<SeriesDTO> advancedSearch(String title, Integer year, String language, String quality, String network, String fileType, Pageable pageable) {
        return seriesRepository.advancedSearch(title, year, language, quality, network, fileType, pageable.getPageSize(), pageable.getOffset())
                .map(seriesMapper::toDto);
    }
}