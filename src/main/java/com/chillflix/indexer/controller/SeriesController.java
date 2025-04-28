package com.chillflix.indexer.controller;

import com.chillflix.indexer.dto.SeriesDTO;
import com.chillflix.indexer.service.SeriesService;
import com.chillflix.indexer.exception.SeriesNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.models.TmdbSearchRequest;
import com.chillflix.indexer.repository.SeriesRepository;
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
@RequestMapping("/v1/series")
@RequiredArgsConstructor
@Tag(name = "Series", description = "Series management APIs")
@Slf4j
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping("/search")
    @Operation(summary = "Search series", description = "Search series based on a search term")
    public Flux<SeriesDTO> searchSeries(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesService.searchSeries(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching series", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching series"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for series", description = "Search series with multiple optional parameters")
    public Flux<SeriesDTO> advancedSearch(
            @Parameter(description = "Series title") @RequestParam(required = false) String title,
            @Parameter(description = "Release year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Language") @RequestParam(required = false) String language,
            @Parameter(description = "Quality") @RequestParam(required = false) String quality,
            @Parameter(description = "File type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesService.advancedSearch(title, year, language, quality, null, fileType, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error performing advanced search", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing advanced search"));
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a series by ID", description = "Retrieve a series by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = SeriesDTO.class)))
    @ApiResponse(responseCode = "404", description = "Series not found")
    public Mono<ResponseEntity<SeriesDTO>> getSeriesById(@Parameter(description = "Series UUID") @PathVariable UUID id) {
        return seriesService.getSeriesById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(SeriesNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching series by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get series by TMDB ID", description = "Retrieve series by their TMDB ID")
    public Flux<SeriesDTO> getSeriesByTmdbId(@Parameter(description = "TMDB ID") @PathVariable Integer tmdbId) {
        return seriesService.getSeriesByTmdbId(tmdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching series by TMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching series by TMDB ID"));
                });
    }

    @GetMapping("/imdb/{imdbId}")
    @Operation(summary = "Get series by IMDB ID", description = "Retrieve series by their IMDB ID")
    public Flux<SeriesDTO> getSeriesByImdbId(@Parameter(description = "IMDB ID") @PathVariable String imdbId) {
        return seriesService.getSeriesByImdbId(imdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching series by IMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching series by IMDB ID"));
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new series", description = "Create a new series entry")
    @ApiResponse(responseCode = "201", description = "Series created successfully", content = @Content(schema = @Schema(implementation = SeriesDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<SeriesDTO>> createSeries(@Valid @RequestBody Mono<SeriesDTO> seriesDTO) {
        return seriesService.saveSeries(seriesDTO)
                .map(savedSeries -> ResponseEntity.status(HttpStatus.CREATED).body(savedSeries))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesDTO(null, e.getMessage(), 0))))
                .onErrorResume(e -> {
                    log.error("Error creating series", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a series", description = "Update an existing series entry")
    @ApiResponse(responseCode = "200", description = "Series updated successfully", content = @Content(schema = @Schema(implementation = SeriesDTO.class)))
    @ApiResponse(responseCode = "404", description = "Series not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<SeriesDTO>> updateSeries(
            @Parameter(description = "Series UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<SeriesDTO> seriesDTO) {
        return seriesService.updateSeries(id, seriesDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(SeriesNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesDTO(id, e.getMessage(), 0))))
                .onErrorResume(e -> {
                    log.error("Error updating series", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a series", description = "Delete a series entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Series deleted successfully")
    @ApiResponse(responseCode = "404", description = "Series not found")
    public Mono<ResponseEntity<Void>> deleteSeries(@Parameter(description = "Series UUID") @PathVariable UUID id) {
        return seriesService.deleteSeries(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(SeriesNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting series", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
    
    @GetMapping("/tmdb")
    @Operation(summary = "Find series by TMDB ID and language", description = "Retrieve series information based on TMDB ID and language")
    @ApiResponse(responseCode = "200", description = "Series found", content = @Content(schema = @Schema(implementation = SeriesDTO.class)))
    @ApiResponse(responseCode = "404", description = "Series not found")
    public Mono<ResponseEntity<SeriesDTO>> findSeriesByTmdbIdAndLanguage(
            @RequestParam("tmdbId") Integer tmdbId,
            @RequestParam("language") String language) {
        return seriesService.getSeriesByTmdbId(tmdbId)
                .filter(series -> series.language().equalsIgnoreCase(language))
                .next()
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error finding series by TMDB ID and language", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    @Operation(summary = "Get all series", description = "Retrieve all series with pagination")
    public Flux<SeriesDTO> getAllSeries(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesService.getAllSeries(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all series", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all series"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total series count", description = "Retrieve the total number of series")
    public Mono<Long> getSeriesCount() {
        return seriesService.countSeries()
                .onErrorResume(e -> {
                    log.error("Error counting series", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting series"));
                });
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get series by year", description = "Retrieve series released in a specific year")
    public Flux<SeriesDTO> getSeriesByYear(
            @Parameter(description = "Year") @PathVariable int year,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return seriesService.getSeriesByYear(year, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching series by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching series by year"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get series by language", description = "Retrieve series in a specific language")
    public Flux<SeriesDTO> getSeriesByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return seriesService.getSeriesByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching series by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching series by language"));
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete series", description = "Delete multiple series by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteSeries(@RequestBody List<UUID> ids) {
        return seriesService.bulkDeleteSeries(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update series", description = "Update multiple series")
    public Flux<SeriesDTO> bulkUpdateSeries(@RequestBody List<SeriesDTO> seriesDTOs) {
        return seriesService.bulkUpdateSeries(seriesDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update"));
                });
    }

    @GetMapping("/top-languages")
    @Operation(summary = "Get top languages", description = "Retrieve the top languages used in series")
    public Flux<SeriesRepository.LanguageCount> getTopLanguages(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return seriesService.getTopLanguages(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching top languages", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching top languages"));
                });
    }

    @GetMapping("/year-count")
    @Operation(summary = "Get series count by year", description = "Retrieve the count of series for each year")
    public Flux<SeriesRepository.YearCount> getSeriesCountByYear(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return seriesService.getSeriesCountByYear(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching series count by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching series count by year"));
                });
    }
    
    @GetMapping("/count-by-year/{year}")
    @Operation(summary = "Get series count for a specific year", description = "Retrieve the count of series for a specific year")
    public Mono<Long> getSeriesCountBySpecificYear(
            @Parameter(description = "Year") @PathVariable int year) {
        return seriesService.countSeriesByYear(year)
                .onErrorResume(e -> {
                    log.error("Error counting series for year: {}", year, e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error counting series for year: " + year));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update a series", description = "Create a new series or update an existing one")
    public Mono<ResponseEntity<SeriesDTO>> createOrUpdateSeries(@Valid @RequestBody Mono<SeriesDTO> seriesDTO) {
        return seriesService.createOrUpdateSeries(seriesDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesDTO(null, e.getMessage(), 0))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating series", e);
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