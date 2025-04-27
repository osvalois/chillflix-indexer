package com.chillflix.indexer.controller;

import com.chillflix.indexer.dto.MovieDTO;
import com.chillflix.indexer.service.MovieService;
import com.chillflix.indexer.exception.MovieNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.models.TmdbSearchRequest;
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
@RequestMapping("/v1/videos")
@RequiredArgsConstructor
@Tag(name = "Video", description = "Video management APIs")
@Slf4j
public class VideoController {

    private final MovieService movieService;

    @GetMapping("/search")
    @Operation(summary = "Search videos", description = "Search videos based on a search term")
    public Flux<MovieDTO> searchVideos(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return movieService.searchMovies(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching videos", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching videos"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for videos", description = "Search videos with multiple optional parameters")
    public Flux<MovieDTO> advancedSearch(
            @Parameter(description = "Video title") @RequestParam(required = false) String title,
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
    @Operation(summary = "Get a video by ID", description = "Retrieve a video by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "404", description = "Video not found")
    public Mono<ResponseEntity<MovieDTO>> getVideoById(@Parameter(description = "Video UUID") @PathVariable UUID id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching video by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get videos by TMDB ID", description = "Retrieve videos by their TMDB ID")
    public Flux<MovieDTO> getVideosByTmdbId(@Parameter(description = "TMDB ID") @PathVariable Integer tmdbId) {
        return movieService.getMoviesByTmdbId(tmdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching videos by TMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching videos by TMDB ID"));
                });
    }

    @GetMapping("/imdb/{imdbId}")
    @Operation(summary = "Get videos by IMDB ID", description = "Retrieve videos by their IMDB ID")
    public Flux<MovieDTO> getVideosByImdbId(@Parameter(description = "IMDB ID") @PathVariable String imdbId) {
        return movieService.getMoviesByImdbId(imdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching videos by IMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching videos by IMDB ID"));
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new video", description = "Create a new video entry")
    @ApiResponse(responseCode = "201", description = "Video created successfully", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MovieDTO>> createVideo(@Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.saveMovie(movieDTO)
                .map(savedMovie -> ResponseEntity.status(HttpStatus.CREATED).body(savedMovie))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating video", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a video", description = "Update an existing video entry")
    @ApiResponse(responseCode = "200", description = "Video updated successfully", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "404", description = "Video not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MovieDTO>> updateVideo(
            @Parameter(description = "Video UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.updateMovie(id, movieDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(id, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error updating video", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a video", description = "Delete a video entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Video deleted successfully")
    @ApiResponse(responseCode = "404", description = "Video not found")
    public Mono<ResponseEntity<Void>> deleteVideo(@Parameter(description = "Video UUID") @PathVariable UUID id) {
        return movieService.deleteMovie(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(MovieNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting video", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/tmdb")
    @Operation(summary = "Find video by TMDB ID and language", description = "Retrieve video information based on TMDB ID and language")
    @ApiResponse(responseCode = "200", description = "Video found", content = @Content(schema = @Schema(implementation = MovieDTO.class)))
    @ApiResponse(responseCode = "404", description = "Video not found")
    public Mono<ResponseEntity<MovieDTO>> findVideoByTmdbIdAndLanguage(
            @RequestParam("tmdbId") Integer tmdbId,
            @RequestParam("language") String language) {
        return movieService.getMoviesByTmdbId(tmdbId)
                .filter(movie -> movie.language().equalsIgnoreCase(language))
                .next()
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error finding video by TMDB ID and language", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    @Operation(summary = "Get all videos", description = "Retrieve all videos with pagination")
    public Flux<MovieDTO> getAllVideos(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return movieService.getAllMovies(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all videos", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all videos"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total video count", description = "Retrieve the total number of videos")
    public Mono<Long> getVideoCount() {
        return movieService.countMovies()
                .onErrorResume(e -> {
                    log.error("Error counting videos", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting videos"));
                });
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get videos by year", description = "Retrieve videos released in a specific year")
    public Flux<MovieDTO> getVideosByYear(
            @Parameter(description = "Year") @PathVariable int year,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return movieService.getMoviesByYear(year, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching videos by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching videos by year"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get videos by language", description = "Retrieve videos in a specific language")
    public Flux<MovieDTO> getVideosByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return movieService.getMoviesByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching videos by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching videos by language"));
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete videos", description = "Delete multiple videos by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteVideos(@RequestBody List<UUID> ids) {
        return movieService.bulkDeleteMovies(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update videos", description = "Update multiple videos")
    public Flux<MovieDTO> bulkUpdateVideos(@RequestBody List<MovieDTO> movieDTOs) {
        return movieService.bulkUpdateMovies(movieDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update"));
                });
    }

    @GetMapping("/top-languages")
    @Operation(summary = "Get top languages", description = "Retrieve the top languages used in videos")
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
    @Operation(summary = "Get video count by year", description = "Retrieve the count of videos for each year")
    public Flux<MovieRepository.YearCount> getVideoCountByYear(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return movieService.getMovieCountByYear(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching video count by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching video count by year"));
                });
    }
    
    @GetMapping("/count-by-year/{year}")
    @Operation(summary = "Get video count for a specific year", description = "Retrieve the count of videos for a specific year")
    public Mono<Long> getVideoCountBySpecificYear(
            @Parameter(description = "Year") @PathVariable int year) {
        return movieService.countMoviesByYear(year)
                .onErrorResume(e -> {
                    log.error("Error counting videos for year: {}", year, e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error counting videos for year: " + year));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update a video", description = "Create a new video or update an existing one")
    public Mono<ResponseEntity<MovieDTO>> createOrUpdateVideo(@Valid @RequestBody Mono<MovieDTO> movieDTO) {
        return movieService.createOrUpdateMovie(movieDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MovieDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating video", e);
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