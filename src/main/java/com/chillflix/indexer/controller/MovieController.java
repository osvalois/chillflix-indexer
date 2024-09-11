package com.chillflix.indexer.controller;

import com.chillflix.indexer.dto.MovieDTO;
import com.chillflix.indexer.service.MovieService;
import com.chillflix.indexer.exception.MovieNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.repository.MovieRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movie", description = "Movie management APIs")
@Slf4j
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    @Operation(summary = "Search movies", description = "Search movies based on a search term")
    public Flux<MovieDTO> searchMovies(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return movieService.searchMovies(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching movies", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching movies"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for movies", description = "Search movies with multiple optional parameters")
    public Flux<MovieDTO> advancedSearch(
            @Parameter(description = "Movie title") @RequestParam(required = false) String title,
            @Parameter(description = "Release year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Language") @RequestParam(required = false) String language,
            @Parameter(description = "Quality") @RequestParam(required = false) String quality,
            @Parameter(description = "File type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return movieService.advancedSearch(title, year, language, quality, fileType, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error performing advanced search", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing advanced search"));
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a movie by ID", description = "Retrieve a movie by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "404", description = "Movie not found")
    public Mono<ResponseEntity<MovieDTO>> getMovieById(@Parameter(description = "Movie UUID") @PathVariable UUID id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching movie by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get movies by TMDB ID", description = "Retrieve movies by their TMDB ID")
    public Flux<MovieDTO> getMoviesByTmdbId(@Parameter(description = "TMDB ID") @PathVariable Integer tmdbId) {
        return movieService.getMoviesByTmdbId(tmdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching movies by TMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching movies by TMDB ID"));
                });
    }

    @GetMapping("/imdb/{imdbId}")
    @Operation(summary = "Get movies by IMDB ID", description = "Retrieve movies by their IMDB ID")
    public Flux<MovieDTO> getMoviesByImdbId(@Parameter(description = "IMDB ID") @PathVariable String imdbId) {
        return movieService.getMoviesByImdbId(imdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching movies by IMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching movies by IMDB ID"));
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new movie", description = "Create a new movie entry")
    @ApiResponse(responseCode = "201", description = "Movie created successfully", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MovieDTO>> createMovie(@Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.saveMovie(movieDTO)
                .map(savedMovie -> ResponseEntity.status(HttpStatus.CREATED).body(savedMovie))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null))))
                .onErrorResume(e -> {
                    log.error("Error creating movie", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie", description = "Update an existing movie entry")
    @ApiResponse(responseCode = "200", description = "Movie updated successfully", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "404", description = "Movie not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MovieDTO>> updateMovie(
            @Parameter(description = "Movie UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.updateMovie(id, movieDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(id, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null))))
                .onErrorResume(e -> {
                    log.error("Error updating movie", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a movie", description = "Delete a movie entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Movie deleted successfully")
    @ApiResponse(responseCode = "404", description = "Movie not found")
    public Mono<ResponseEntity<Void>> deleteMovie(@Parameter(description = "Movie UUID") @PathVariable UUID id) {
        return movieService.deleteMovie(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting movie", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    @Operation(summary = "Get all movies", description = "Retrieve all movies with pagination")
    public Flux<MovieDTO> getAllMovies(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return movieService.getAllMovies(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all movies", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all movies"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total movie count", description = "Retrieve the total number of movies")
    public Mono<Long> getMovieCount() {
        return movieService.countMovies()
                .onErrorResume(e -> {
                    log.error("Error counting movies", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting movies"));
                });
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get movies by year", description = "Retrieve movies released in a specific year")
    public Flux<MovieDTO> getMoviesByYear(
            @Parameter(description = "Year") @PathVariable int year,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return movieService.getMoviesByYear(year, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching movies by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching movies by year"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get movies by language", description = "Retrieve movies in a specific language")
    public Flux<MovieDTO> getMoviesByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return movieService.getMoviesByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching movies by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching movies by language"));
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete movies", description = "Delete multiple movies by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteMovies(@RequestBody List<UUID> ids) {
        return movieService.bulkDeleteMovies(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update movies", description = "Update multiple movies")
    public Flux<MovieDTO> bulkUpdateMovies(@RequestBody List<MovieDTO> movieDTOs) {
        return movieService.bulkUpdateMovies(movieDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update"));
                });
    }

    @GetMapping("/top-languages")
    @Operation(summary = "Get top languages", description = "Retrieve the top languages used in movies")
    public Flux<MovieRepository.LanguageCount> getTopLanguages(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return movieService.getTopLanguages(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching top languages", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching top languages"));
                });
    }

    @GetMapping("/year-count")
    @Operation(summary = "Get movie count by year", description = "Retrieve the count of movies for each year")
    public Flux<MovieRepository.YearCount> getMovieCountByYear(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return movieService.getMovieCountByYear(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching movie count by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching movie count by year"));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update a movie", description = "Create a new movie or update an existing one")
    public Mono<ResponseEntity<MovieDTO>> createOrUpdateMovie(@Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.createOrUpdateMovie(movieDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating movie", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    private PageRequest createPageRequest(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1])
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
    }
}