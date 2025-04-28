package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.VideoGameDTO;
import com.chillflix.indexer.exception.VideoGameNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.VideoGameMapper;
import com.chillflix.indexer.repository.VideoGameRepository;
import com.chillflix.indexer.util.VideoGameValidationUtil;

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
public class VideoGameService {

    private final VideoGameRepository videoGameRepository;
    private final VideoGameMapper videoGameMapper;
    private final Validator validator;
    private final VideoGameValidationUtil videoGameValidationUtil;

    @CircuitBreaker(name = "searchVideoGames", fallbackMethod = "searchVideoGamesFallback")
    @RateLimiter(name = "searchVideoGames")
    public Flux<VideoGameDTO> searchVideoGames(String searchTerm, Pageable pageable) {
        log.debug("Searching video games with term: {}", searchTerm);
        return videoGameRepository.searchVideoGames(searchTerm, pageable.getPageSize(), pageable.getOffset())
                .map(videoGameMapper::toDto);
    }

    public Flux<VideoGameDTO> searchVideoGamesFallback(String searchTerm, Pageable pageable, Throwable t) {
        log.error("Fallback: Error searching video games", t);
        return Flux.empty();
    }

    @Cacheable(cacheNames = "videoGameCache", key = "#id")
    public Mono<VideoGameDTO> getVideoGameById(UUID id) {
        log.debug("Fetching video game with id: {}", id);
        return videoGameRepository.findById(id)
                .map(videoGameMapper::toDto)
                .switchIfEmpty(Mono.error(new VideoGameNotFoundException("Video game not found with id: " + id)));
    }

    public Flux<VideoGameDTO> getVideoGamesByPlatform(String platform, Pageable pageable) {
        log.debug("Fetching video games with platform: {}", platform);
        return videoGameRepository.findByPlatform(platform, pageable.getPageSize(), pageable.getOffset())
                .map(videoGameMapper::toDto);
    }

    public Flux<VideoGameDTO> getVideoGamesByYear(int year, Pageable pageable) {
        log.debug("Fetching video games for year: {}", year);
        return videoGameRepository.findByYear(year, pageable.getPageSize(), pageable.getOffset())
                .map(videoGameMapper::toDto);
    }

    public Mono<VideoGameDTO> saveVideoGame(Mono<VideoGameDTO> videoGameDTO) {
        log.debug("Saving new video game");
        return videoGameDTO
                .flatMap(this::validateVideoGame)
                .map(videoGameMapper::toEntity)
                .flatMap(videoGame -> {
                    videoGame.setCreatedAt(LocalDateTime.now());
                    videoGame.setUpdatedAt(LocalDateTime.now());
                    return videoGameRepository.saveOrUpdate(videoGame);
                })
                .map(videoGameMapper::toDto)
                .doOnSuccess(savedVideoGame -> log.info("Successfully saved video game with id: {}", savedVideoGame.id()))
                .doOnError(error -> log.error("Error saving video game", error));
    }

    public Mono<VideoGameDTO> updateVideoGame(UUID id, Mono<VideoGameDTO> videoGameDTO) {
        log.debug("Updating video game with id: {}", id);
        return videoGameRepository.findById(id)
                .flatMap(existingVideoGame ->
                        videoGameDTO.flatMap(this::validateVideoGame)
                                .map(dto -> {
                                    videoGameMapper.updateEntityFromDto(dto, existingVideoGame);
                                    existingVideoGame.setUpdatedAt(LocalDateTime.now());
                                    return existingVideoGame;
                                })
                )
                .flatMap(videoGameRepository::saveOrUpdate)
                .map(videoGameMapper::toDto)
                .switchIfEmpty(Mono.error(new VideoGameNotFoundException("Video game not found with id: " + id)))
                .doOnSuccess(updatedVideoGame -> log.info("Successfully updated video game with id: {}", updatedVideoGame.id()))
                .doOnError(error -> log.error("Error updating video game with id: {}", id, error));
    }

    public Mono<Void> deleteVideoGame(UUID id) {
        log.debug("Deleting video game with id: {}", id);
        return videoGameRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted video game with id: {}", id))
                .doOnError(error -> log.error("Error deleting video game with id: {}", id, error));
    }

    public Mono<VideoGameDTO> createOrUpdateVideoGame(Mono<VideoGameDTO> videoGameDTO) {
        return videoGameDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateVideoGame(dto.id(), Mono.just(dto));
            } else {
                return saveVideoGame(Mono.just(dto));
            }
        });
    }

    @Cacheable(cacheNames = "allVideoGamesCache", key = "#pageable")
    public Flux<VideoGameDTO> getAllVideoGames(Pageable pageable) {
        log.debug("Fetching all video games with pagination");
        return videoGameRepository.findAllVideoGamesPaginated(pageable.getPageSize(), pageable.getOffset())
                .map(videoGameMapper::toDto);
    }

    public Mono<Long> countVideoGames() {
        return videoGameRepository.count();
    }

    public Mono<Long> countVideoGamesByYear(int year) {
        return videoGameRepository.countByYear(year)
                .doOnSuccess(count -> log.debug("Count of video games for year {}: {}", year, count))
                .doOnError(error -> log.error("Error counting video games for year: {}", year, error));
    }

    public Flux<VideoGameRepository.PlatformCount> getTopPlatforms(int limit) {
        return videoGameRepository.getTopPlatforms(limit)
                .doOnComplete(() -> log.debug("Fetched top {} platforms", limit))
                .doOnError(error -> log.error("Error fetching top platforms", error));
    }

    public Flux<VideoGameRepository.YearCount> getVideoGameCountByYear(int limit) {
        return videoGameRepository.getVideoGameCountByYear(limit)
                .doOnComplete(() -> log.debug("Fetched video game count by year, limit: {}", limit))
                .doOnError(error -> log.error("Error fetching video game count by year", error));
    }

    @Transactional
    public Mono<VideoGameDTO> saveOrUpdateVideoGame(VideoGameDTO videoGameDTO) {
        log.debug("Saving or updating video game: {}", videoGameDTO);
        return Mono.just(videoGameDTO)
                .flatMap(this::validateVideoGame)
                .map(videoGameMapper::toEntity)
                .flatMap(videoGame -> {
                    videoGame.setUpdatedAt(LocalDateTime.now());
                    if (videoGame.getCreatedAt() == null) {
                        videoGame.setCreatedAt(LocalDateTime.now());
                    }
                    return videoGameRepository.saveOrUpdate(videoGame);
                })
                .map(videoGameMapper::toDto)
                .doOnSuccess(savedVideoGame -> log.info("Successfully saved/updated video game with id: {}", savedVideoGame.id()))
                .doOnError(error -> log.error("Error saving/updating video game", error));
    }

    private Mono<VideoGameDTO> validateVideoGame(VideoGameDTO videoGameDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(videoGameDTO, "videoGameDTO");
            validator.validate(videoGameDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return videoGameDTO;
        });
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteVideoGamesFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteVideoGames(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} video games", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteVideoGame)
                .then();
    }

    public Mono<Void> bulkDeleteVideoGamesFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateVideoGamesFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<VideoGameDTO> bulkUpdateVideoGames(List<VideoGameDTO> videoGameDTOs) {
        log.debug("Performing bulk update operation for {} video games", videoGameDTOs.size());
        return Flux.fromIterable(videoGameDTOs)
                .flatMap(dto -> saveOrUpdateVideoGame(dto));
    }

    public Flux<VideoGameDTO> bulkUpdateVideoGamesFallback(List<VideoGameDTO> videoGameDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Flux<VideoGameDTO> advancedSearch(String title, Integer year, String platform, String developer, String genre, Pageable pageable) {
        return videoGameRepository.advancedSearch(title, developer, null, year, platform, genre, pageable.getPageSize(), pageable.getOffset())
                .map(videoGameMapper::toDto);
    }

    public Mono<Void> deleteVideoGamesByIds(List<UUID> ids) {
        log.debug("Deleting video games with ids: {}", ids);
        return videoGameRepository.deleteAllByIdIn(ids)
                .doOnSuccess(__ -> log.info("Successfully deleted video games with ids: {}", ids))
                .doOnError(error -> log.error("Error deleting video games with ids: {}", ids, error));
    }
}