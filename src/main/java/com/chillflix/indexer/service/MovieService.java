package com.chillflix.indexer.service;

import com.chillflix.indexer.dto.MovieDTO;
import com.chillflix.indexer.exception.MovieNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.mapper.MovieMapper;
import com.chillflix.indexer.models.Movie;
import com.chillflix.indexer.repository.MovieRepository;
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
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final Validator validator;

    @CircuitBreaker(name = "searchMovies", fallbackMethod = "searchMoviesFallback")
    @RateLimiter(name = "searchMovies")
    public Flux<MovieDTO> searchMovies(String searchTerm, Pageable pageable) {
        log.debug("Searching movies with term: {}", searchTerm);
        return movieRepository.searchMovies(searchTerm, pageable.getPageSize(), pageable.getOffset())
                .map(movieMapper::toDto);
    }

    public Flux<MovieDTO> searchMoviesFallback(String searchTerm, Pageable pageable, Throwable t) {
        log.error("Fallback: Error searching movies", t);
        return Flux.empty();
    }

    @Cacheable(cacheNames = "movieCache", key = "#id")
    public Mono<MovieDTO> getMovieById(UUID id) {
        log.debug("Fetching movie with id: {}", id);
        return movieRepository.findById(id)
                .map(movieMapper::toDto)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with id: " + id)));
    }

    public Flux<MovieDTO> getMoviesByTmdbId(Integer tmdbId) {
        log.debug("Fetching movies with TMDB id: {}", tmdbId);
        return movieRepository.findByTmdbId(tmdbId)
                .map(movieMapper::toDto);
    }

    public Flux<MovieDTO> getMoviesByImdbId(String imdbId) {
        log.debug("Fetching movies with IMDB id: {}", imdbId);
        return movieRepository.findByImdbId(imdbId)
                .map(movieMapper::toDto);
    }

    public Mono<MovieDTO> saveMovie(Mono<MovieDTO> movieDTO) {
        log.debug("Saving new movie");
        return movieDTO
                .flatMap(this::validateMovie)
                .map(movieMapper::toEntity)
                .flatMap(movie -> {
                    movie.setCreatedAt(LocalDateTime.now());
                    movie.setUpdatedAt(LocalDateTime.now());
                    return movieRepository.saveOrUpdate(movie);
                })
                .map(movieMapper::toDto)
                .doOnSuccess(savedMovie -> log.info("Successfully saved movie with id: {}", savedMovie.id()))
                .doOnError(error -> log.error("Error saving movie", error));
    }

    public Mono<MovieDTO> updateMovie(UUID id, Mono<MovieDTO> movieDTO) {
        log.debug("Updating movie with id: {}", id);
        return movieRepository.findById(id)
                .flatMap(existingMovie ->
                        movieDTO.flatMap(this::validateMovie)
                                .map(dto -> {
                                    movieMapper.updateEntityFromDto(dto, existingMovie);
                                    existingMovie.setUpdatedAt(LocalDateTime.now());
                                    return existingMovie;
                                })
                )
                .flatMap(movieRepository::saveOrUpdate)
                .map(movieMapper::toDto)
                .switchIfEmpty(Mono.error(new MovieNotFoundException("Movie not found with id: " + id)))
                .doOnSuccess(updatedMovie -> log.info("Successfully updated movie with id: {}", updatedMovie.id()))
                .doOnError(error -> log.error("Error updating movie with id: {}", id, error));
    }

    public Mono<Void> deleteMovie(UUID id) {
        log.debug("Deleting movie with id: {}", id);
        return movieRepository.deleteById(id)
                .doOnSuccess(__ -> log.info("Successfully deleted movie with id: {}", id))
                .doOnError(error -> log.error("Error deleting movie with id: {}", id, error));
    }

    public Mono<MovieDTO> createOrUpdateMovie(Mono<MovieDTO> movieDTO) {
        return movieDTO.flatMap(dto -> {
            if (dto.id() != null) {
                return updateMovie(dto.id(), Mono.just(dto));
            } else {
                return saveMovie(Mono.just(dto));
            }
        });
    }

    @Cacheable(cacheNames = "allMoviesCache", key = "#pageable")
    public Flux<MovieDTO> getAllMovies(Pageable pageable) {
        log.debug("Fetching all movies with pagination");
        return movieRepository.findAllMoviesPaginated(pageable.getPageSize(), pageable.getOffset())
                .map(movieMapper::toDto);
    }

    public Mono<Long> countMovies() {
        return movieRepository.count();
    }

    public Flux<MovieDTO> getMoviesByYear(int year, Pageable pageable) {
        log.debug("Fetching movies for year: {}", year);
        return movieRepository.findByYear(year, pageable.getPageSize(), pageable.getOffset())
                .map(movieMapper::toDto);
    }

    public Flux<MovieDTO> getMoviesByLanguage(String language, Pageable pageable) {
        log.debug("Fetching movies in language: {}", language);
        return movieRepository.findByLanguage(language, pageable.getPageSize(), pageable.getOffset())
                .map(movieMapper::toDto);
    }

    public Mono<Void> deleteMoviesByIds(List<UUID> ids) {
        log.debug("Deleting movies with ids: {}", ids);
        return movieRepository.deleteAllByIdIn(ids)
                .doOnSuccess(__ -> log.info("Successfully deleted movies with ids: {}", ids))
                .doOnError(error -> log.error("Error deleting movies with ids: {}", ids, error));
    }

    public Mono<Long> countMoviesByYear(int year) {
        return movieRepository.countByYear(year)
                .doOnSuccess(count -> log.debug("Count of movies for year {}: {}", year, count))
                .doOnError(error -> log.error("Error counting movies for year: {}", year, error));
    }

    public Flux<MovieRepository.LanguageCount> getTopLanguages(int limit) {
        return movieRepository.getTopLanguages(limit)
                .doOnComplete(() -> log.debug("Fetched top {} languages", limit))
                .doOnError(error -> log.error("Error fetching top languages", error));
    }

    public Flux<MovieRepository.YearCount> getMovieCountByYear(int limit) {
        return movieRepository.getMovieCountByYear(limit)
                .doOnComplete(() -> log.debug("Fetched movie count by year, limit: {}", limit))
                .doOnError(error -> log.error("Error fetching movie count by year", error));
    }

    @Transactional
    public Mono<MovieDTO> saveOrUpdateMovie(MovieDTO movieDTO) {
        log.debug("Saving or updating movie: {}", movieDTO);
        return Mono.just(movieDTO)
                .flatMap(this::validateMovie)
                .map(movieMapper::toEntity)
                .flatMap(movie -> {
                    movie.setUpdatedAt(LocalDateTime.now());
                    if (movie.getCreatedAt() == null) {
                        movie.setCreatedAt(LocalDateTime.now());
                    }
                    return movieRepository.saveOrUpdate(movie);
                })
                .map(movieMapper::toDto)
                .doOnSuccess(savedMovie -> log.info("Successfully saved/updated movie with id: {}", savedMovie.id()))
                .doOnError(error -> log.error("Error saving/updating movie", error));
    }

    private Mono<MovieDTO> validateMovie(MovieDTO movieDTO) {
        return Mono.fromSupplier(() -> {
            Errors errors = new BeanPropertyBindingResult(movieDTO, "movieDTO");
            validator.validate(movieDTO, errors);
            if (errors.hasErrors()) {
                throw new ValidationException(errors.getAllErrors());
            }
            return movieDTO;
        });
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkDeleteMoviesFallback")
    @RateLimiter(name = "bulkOperation")
    public Mono<Void> bulkDeleteMovies(List<UUID> ids) {
        log.debug("Performing bulk delete operation for {} movies", ids.size());
        return Flux.fromIterable(ids)
                .flatMap(this::deleteMovie)
                .then();
    }

    public Mono<Void> bulkDeleteMoviesFallback(List<UUID> ids, Throwable t) {
        log.error("Fallback: Error performing bulk delete operation", t);
        return Mono.empty();
    }

    @CircuitBreaker(name = "bulkOperation", fallbackMethod = "bulkUpdateMoviesFallback")
    @RateLimiter(name = "bulkOperation")
    public Flux<MovieDTO> bulkUpdateMovies(List<MovieDTO> movieDTOs) {
        log.debug("Performing bulk update operation for {} movies", movieDTOs.size());
        return Flux.fromIterable(movieDTOs)
                .flatMap(dto -> saveOrUpdateMovie(dto));
    }

    public Flux<MovieDTO> bulkUpdateMoviesFallback(List<MovieDTO> movieDTOs, Throwable t) {
        log.error("Fallback: Error performing bulk update operation", t);
        return Flux.empty();
    }

    public Flux<MovieDTO> advancedSearch(String title, Integer year, String language, String quality, String fileType, Pageable pageable) {
        return movieRepository.advancedSearch(title, year, language, quality, fileType, pageable.getPageSize(), pageable.getOffset())
                .map(movieMapper::toDto);
    }
}