package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.VideoDTO;
import com.chillflix.indexer.exception.VideoNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.VideoMapper;
import com.chillflix.indexer.repository.VideoRepository;
import com.chillflix.indexer.util.VideoValidationUtil;

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
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final Validator validator;
    private final VideoValidationUtil videoValidationUtil;

    @CircuitBreaker(name = "searchVideos", fallbackMethod = "searchVideosFallback")
    @RateLimiter(name = "searchVideos")
    public Flux<VideoDTO> searchVideos(String searchTerm, Pageable pageable) {
        log.debug("Searching videos with term: {}", searchTerm);
        return videoRepository.searchVideos(searchTerm, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Flux<VideoDTO> searchVideosFallback(String searchTerm, Pageable pageable, Throwable t) {
        log.error("Fallback: Error searching videos", t);
        return Flux.empty();
    }

    @Cacheable(cacheNames = "videoCache", key = "#id")
    public Mono<VideoDTO> getVideoById(UUID id) {
        log.debug("Fetching video with id: {}", id);
        return videoRepository.findById(id)
                .map(videoMapper::toDto)
                .switchIfEmpty(Mono.error(new VideoNotFoundException("Video not found with id: " + id)));
    }

    public Flux<VideoDTO> getVideosByYear(int year, Pageable pageable) {
        log.debug("Fetching videos for year: {}", year);
        return videoRepository.findByYear(year, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Flux<VideoDTO> getVideosByCreator(String creator, Pageable pageable) {
        log.debug("Fetching videos by creator: {}", creator);
        return videoRepository.findByCreator(creator, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Flux<VideoDTO> getVideosByCategory(String category, Pageable pageable) {
        log.debug("Fetching videos in category: {}", category);
        return videoRepository.findByCategory(category, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Flux<VideoDTO> getVideosByTag(String tag, Pageable pageable) {
        log.debug("Fetching videos with tag: {}", tag);
        return videoRepository.findByTag(tag, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Mono<VideoDTO> saveVideo(Mono<VideoDTO> videoDTO) {
        log.debug("Saving new video");
        return videoDTO
                .flatMap(this::validateVideo)
                .map(videoMapper::toEntity)
                .flatMap(video -> {
                    video.setCreatedAt(LocalDateTime.now());
                    video.setUpdatedAt(LocalDateTime.now());
                    return videoRepository.saveOrUpdate(video);
                })
                .map(videoMapper::toDto)
                .doOnSuccess(savedVideo -> log.info("Successfully saved video with id: {}", savedVideo.id()))
                .doOnError(error -> log.error("Error saving video", error));
    }

    public Mono<VideoDTO> updateVideo(UUID id, Mono<VideoDTO> videoDTO) {
        log.debug("Updating video with id: {}", id);
        return videoRepository.findById(id)
                .flatMap(existingVideo ->
                        videoDTO.flatMap(this::validateVideo)
                                .map(dto -> {
                                    videoMapper.updateEntityFromDto(dto, existingVideo);
                                    existingVideo.setUpdatedAt(LocalDateTime.now());
                                    return existingVideo;
                                })
                )
                .flatMap(videoRepository::saveOrUpdate)
                .map(videoMapper::toDto)
                .switchIfEmpty(Mono.error(new VideoNotFoundException("Video not found with id: " + id)))
                .doOnSuccess(updatedVideo -> log.info("Successfully updated video with id: {}", updatedVideo.id()))
                .doOnError(error -> log.error("Error updating video with id: {}", id, error));
    }

    public Mono<Void> deleteVideo(UUID id) {
        log.debug("Deleting video with id: {}", id);
        return videoRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted video with id: {}", id))
                .doOnError(error -> log.error("Error deleting video with id: {}", id, error));
    }

    public Mono<VideoDTO> createOrUpdateVideo(Mono<VideoDTO> videoDTO) {
        return videoDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateVideo(dto.id(), Mono.just(dto));
            } else {
                return saveVideo(Mono.just(dto));
            }
        });
    }

    @Cacheable(cacheNames = "allVideosCache", key = "#pageable")
    public Flux<VideoDTO> getAllVideos(Pageable pageable) {
        log.debug("Fetching all videos with pagination");
        return videoRepository.findAllVideosPaginated(pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }

    public Mono<Long> countVideos() {
        return videoRepository.count();
    }

    public Mono<Long> countVideosByYear(int year) {
        return videoRepository.countByYear(year)
                .doOnSuccess(count -> log.debug("Count of videos for year {}: {}", year, count))
                .doOnError(error -> log.error("Error counting videos for year: {}", year, error));
    }

    public Flux<VideoRepository.CategoryCount> getTopCategories(int limit) {
        return videoRepository.getTopCategories(limit)
                .doOnComplete(() -> log.debug("Fetched top {} categories", limit))
                .doOnError(error -> log.error("Error fetching top categories", error));
    }

    public Flux<VideoRepository.TagCount> getTopTags(int limit) {
        return videoRepository.getTopTags(limit)
                .doOnComplete(() -> log.debug("Fetched top {} tags", limit))
                .doOnError(error -> log.error("Error fetching top tags", error));
    }

    @Transactional
    public Mono<VideoDTO> saveOrUpdateVideo(VideoDTO videoDTO) {
        log.debug("Saving or updating video: {}", videoDTO);
        return Mono.just(videoDTO)
                .flatMap(this::validateVideo)
                .map(videoMapper::toEntity)
                .flatMap(video -> {
                    video.setUpdatedAt(LocalDateTime.now());
                    if (video.getCreatedAt() == null) {
                        video.setCreatedAt(LocalDateTime.now());
                    }
                    return videoRepository.saveOrUpdate(video);
                })
                .map(videoMapper::toDto)
                .doOnSuccess(savedVideo -> log.info("Successfully saved/updated video with id: {}", savedVideo.id()))
                .doOnError(error -> log.error("Error saving/updating video", error));
    }

    private Mono<VideoDTO> validateVideo(VideoDTO videoDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(videoDTO, "videoDTO");
            validator.validate(videoDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            // Additional validation for SHA256 hash in magnet link
            String hash = videoValidationUtil.extractHashFromMagnet(videoDTO.magnet());
            if (hash != null && !videoValidationUtil.isValidSha256Hash(hash)) {
                throw new ValidationException("Invalid SHA256 hash in magnet link");
            }
            return videoDTO;
        });
    }

    public Mono<Void> deleteVideosByIds(List<UUID> ids) {
        log.debug("Deleting videos with ids: {}", ids);
        return videoRepository.deleteAllByIdIn(ids)
                .doOnSuccess(__ -> log.info("Successfully deleted videos with ids: {}", ids))
                .doOnError(error -> log.error("Error deleting videos with ids: {}", ids, error));
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteVideosFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteVideos(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} videos", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteVideo)
                .then();
    }

    public Mono<Void> bulkDeleteVideosFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateVideosFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<VideoDTO> bulkUpdateVideos(List<VideoDTO> videoDTOs) {
        log.debug("Performing bulk update operation for {} videos", videoDTOs.size());
        return Flux.fromIterable(videoDTOs)
                .flatMap(dto -> saveOrUpdateVideo(dto));
    }

    public Flux<VideoDTO> bulkUpdateVideosFallback(List<VideoDTO> videoDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Flux<VideoDTO> advancedSearch(String title, String creator, Integer year, String category, String tag, String quality, Pageable pageable) {
        return videoRepository.advancedSearch(title, creator, year, category, tag, quality, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }
    
    public Flux<VideoDTO> getRecentlyUpdatedVideos(LocalDateTime lastUpdateTime, Pageable pageable) {
        log.debug("Fetching videos updated after: {}", lastUpdateTime);
        return videoRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(lastUpdateTime, pageable.getPageSize(), pageable.getOffset())
                .map(videoMapper::toDto);
    }
}