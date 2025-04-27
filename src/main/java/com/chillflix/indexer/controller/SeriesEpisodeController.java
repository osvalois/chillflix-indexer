package com.chillflix.indexer.controller;

import com.chillflix.indexer.dto.SeriesEpisodeDTO;
import com.chillflix.indexer.exception.EpisodeNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.service.SeriesEpisodeService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/episodes")
@RequiredArgsConstructor
@Tag(name = "Series Episode", description = "Series Episode management APIs")
@Slf4j
public class SeriesEpisodeController {

    private final SeriesEpisodeService seriesEpisodeService;

    @GetMapping
    @Operation(summary = "Get all episodes", description = "Retrieve all series episodes with pagination")
    public Flux<SeriesEpisodeDTO> getAllEpisodes(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesEpisodeService.getAllEpisodes(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all episodes", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all episodes"));
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an episode by ID", description = "Retrieve an episode by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = SeriesEpisodeDTO.class)))
    @ApiResponse(responseCode = "404", description = "Episode not found")
    public Mono<ResponseEntity<SeriesEpisodeDTO>> getEpisodeById(@Parameter(description = "Episode UUID") @PathVariable UUID id) {
        return seriesEpisodeService.getEpisodeById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(EpisodeNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching episode by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "Get episodes by series ID", description = "Retrieve all episodes belonging to a specific series")
    public Flux<SeriesEpisodeDTO> getEpisodesBySeriesId(
            @Parameter(description = "Series UUID") @PathVariable UUID seriesId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        return seriesEpisodeService.getEpisodesBySeriesId(seriesId, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching episodes by series ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching episodes by series ID"));
                });
    }

    @GetMapping("/series/{seriesId}/season/{seasonNumber}")
    @Operation(summary = "Get episodes by series ID and season", description = "Retrieve all episodes for a specific series and season")
    public Flux<SeriesEpisodeDTO> getEpisodesBySeriesIdAndSeason(
            @Parameter(description = "Series UUID") @PathVariable UUID seriesId,
            @Parameter(description = "Season number") @PathVariable int seasonNumber,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        return seriesEpisodeService.getEpisodesBySeriesIdAndSeason(seriesId, seasonNumber, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching episodes by series ID and season", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching episodes by series ID and season"));
                });
    }

    @GetMapping("/search")
    @Operation(summary = "Search episodes", description = "Search episodes based on a search term")
    public Flux<SeriesEpisodeDTO> searchEpisodes(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesEpisodeService.searchEpisodes(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching episodes", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching episodes"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for episodes", description = "Search episodes with multiple optional parameters")
    public Flux<SeriesEpisodeDTO> advancedSearch(
            @Parameter(description = "Episode title") @RequestParam(required = false) String title,
            @Parameter(description = "Series ID") @RequestParam(required = false) UUID seriesId,
            @Parameter(description = "Season number") @RequestParam(required = false) Integer seasonNumber,
            @Parameter(description = "Episode number") @RequestParam(required = false) Integer episodeNumber,
            @Parameter(description = "Language") @RequestParam(required = false) String language,
            @Parameter(description = "Quality") @RequestParam(required = false) String quality,
            @Parameter(description = "File type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return seriesEpisodeService.advancedSearch(title, seriesId, seasonNumber, episodeNumber, language, quality, fileType, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error performing advanced search on episodes", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing advanced search on episodes"));
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new episode", description = "Create a new episode entry")
    @ApiResponse(responseCode = "201", description = "Episode created successfully", content = @Content(schema = @Schema(implementation = SeriesEpisodeDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<SeriesEpisodeDTO>> createEpisode(@Valid @RequestBody Mono<SeriesEpisodeDTO> episodeDTO) {
        return seriesEpisodeService.saveEpisode(episodeDTO)
                .map(savedEpisode -> ResponseEntity.status(HttpStatus.CREATED).body(savedEpisode))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesEpisodeDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating episode", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an episode", description = "Update an existing episode entry")
    @ApiResponse(responseCode = "200", description = "Episode updated successfully", content = @Content(schema = @Schema(implementation = SeriesEpisodeDTO.class)))
    @ApiResponse(responseCode = "404", description = "Episode not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<SeriesEpisodeDTO>> updateEpisode(
            @Parameter(description = "Episode UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<SeriesEpisodeDTO> episodeDTO) {
        return seriesEpisodeService.updateEpisode(id, episodeDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(EpisodeNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesEpisodeDTO(id, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error updating episode", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an episode", description = "Delete an episode entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Episode deleted successfully")
    @ApiResponse(responseCode = "404", description = "Episode not found")
    public Mono<ResponseEntity<Void>> deleteEpisode(@Parameter(description = "Episode UUID") @PathVariable UUID id) {
        return seriesEpisodeService.deleteEpisode(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(EpisodeNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting episode", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete episodes", description = "Delete multiple episodes by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteEpisodes(@RequestBody List<UUID> ids) {
        return seriesEpisodeService.bulkDeleteEpisodes(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete of episodes", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update episodes", description = "Update multiple episodes")
    public Flux<SeriesEpisodeDTO> bulkUpdateEpisodes(@RequestBody List<SeriesEpisodeDTO> episodeDTOs) {
        return seriesEpisodeService.bulkUpdateEpisodes(episodeDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update of episodes", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update of episodes"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total episode count", description = "Retrieve the total number of episodes")
    public Mono<Long> getEpisodeCount() {
        return seriesEpisodeService.countEpisodes()
                .onErrorResume(e -> {
                    log.error("Error counting episodes", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting episodes"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get episodes by language", description = "Retrieve episodes in a specific language")
    public Flux<SeriesEpisodeDTO> getEpisodesByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return seriesEpisodeService.getEpisodesByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching episodes by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching episodes by language"));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update an episode", description = "Create a new episode or update an existing one")
    public Mono<ResponseEntity<SeriesEpisodeDTO>> createOrUpdateEpisode(@Valid @RequestBody Mono<SeriesEpisodeDTO> episodeDTO) {
        return seriesEpisodeService.createOrUpdateEpisode(episodeDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new SeriesEpisodeDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating episode", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get episodes by TMDB ID", description = "Retrieve episodes by their TMDB ID")
    public Flux<SeriesEpisodeDTO> getEpisodesByTmdbId(@Parameter(description = "TMDB ID") @PathVariable Integer tmdbId) {
        return seriesEpisodeService.getEpisodesByTmdbId(tmdbId)
                .onErrorResume(e -> {
                    log.error("Error fetching episodes by TMDB ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching episodes by TMDB ID"));
                });
    }

    private PageRequest createPageRequest(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 ? Sort.Direction.fromString(sortParams[1])
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
    }
}