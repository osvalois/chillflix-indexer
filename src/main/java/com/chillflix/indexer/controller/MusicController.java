package com.chillflix.indexer.controller;

import com.chillflix.indexer.dto.MusicDTO;
import com.chillflix.indexer.service.MusicService;
import com.chillflix.indexer.exception.MusicNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.models.TmdbSearchRequest;
import com.chillflix.indexer.repository.MusicRepository;
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
@RequestMapping("/v1/music")
@RequiredArgsConstructor
@Tag(name = "Music", description = "Music management APIs")
@Slf4j
public class MusicController {

    private final MusicService musicService;

    @GetMapping("/search")
    @Operation(summary = "Search music", description = "Search music based on a search term")
    public Flux<MusicDTO> searchMusic(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicService.searchMusic(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching music", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching music"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for music", description = "Search music with multiple optional parameters")
    public Flux<MusicDTO> advancedSearch(
            @Parameter(description = "Music title") @RequestParam(required = false) String title,
            @Parameter(description = "Release year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Language") @RequestParam(required = false) String language,
            @Parameter(description = "Quality") @RequestParam(required = false) String quality,
            @Parameter(description = "File type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicService.advancedSearch(title, year, language, quality, fileType, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error performing advanced search", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing advanced search"));
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get music by ID", description = "Retrieve music by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = MusicDTO.class)))
    @ApiResponse(responseCode = "404", description = "Music not found")
    public Mono<ResponseEntity<MusicDTO>> getMusicById(@Parameter(description = "Music UUID") @PathVariable UUID id) {
        return musicService.getMusicById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(MusicNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching music by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new music", description = "Create a new music entry")
    @ApiResponse(responseCode = "201", description = "Music created successfully", content = @Content(schema = @Schema(implementation = MusicDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MusicDTO>> createMusic(@Valid @RequestBody Mono<MusicDTO> musicDTO) {
        return musicService.saveMusic(musicDTO)
                .map(savedMusic -> ResponseEntity.status(HttpStatus.CREATED).body(savedMusic))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating music", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update music", description = "Update an existing music entry")
    @ApiResponse(responseCode = "200", description = "Music updated successfully", content = @Content(schema = @Schema(implementation = MusicDTO.class)))
    @ApiResponse(responseCode = "404", description = "Music not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MusicDTO>> updateMusic(
            @Parameter(description = "Music UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<MusicDTO> musicDTO) {
        return musicService.updateMusic(id, musicDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(MusicNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicDTO(id, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error updating music", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete music", description = "Delete a music entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Music deleted successfully")
    @ApiResponse(responseCode = "404", description = "Music not found")
    public Mono<ResponseEntity<Void>> deleteMusic(@Parameter(description = "Music UUID") @PathVariable UUID id) {
        return musicService.deleteMusic(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(MusicNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting music", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping
    @Operation(summary = "Get all music", description = "Retrieve all music with pagination")
    public Flux<MusicDTO> getAllMusic(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicService.getAllMusic(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all music", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all music"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total music count", description = "Retrieve the total number of music items")
    public Mono<Long> getMusicCount() {
        return musicService.countMusic()
                .onErrorResume(e -> {
                    log.error("Error counting music", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting music"));
                });
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get music by year", description = "Retrieve music released in a specific year")
    public Flux<MusicDTO> getMusicByYear(
            @Parameter(description = "Year") @PathVariable int year,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return musicService.getMusicByYear(year, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music by year"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get music by language", description = "Retrieve music in a specific language")
    public Flux<MusicDTO> getMusicByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return musicService.getMusicByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music by language"));
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete music", description = "Delete multiple music items by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteMusic(@RequestBody List<UUID> ids) {
        return musicService.bulkDeleteMusic(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update music", description = "Update multiple music items")
    public Flux<MusicDTO> bulkUpdateMusic(@RequestBody List<MusicDTO> musicDTOs) {
        return musicService.bulkUpdateMusic(musicDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update"));
                });
    }

    @GetMapping("/top-languages")
    @Operation(summary = "Get top languages", description = "Retrieve the top languages used in music")
    public Flux<MusicRepository.LanguageCount> getTopLanguages(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return musicService.getTopLanguages(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching top languages", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching top languages"));
                });
    }

    @GetMapping("/year-count")
    @Operation(summary = "Get music count by year", description = "Retrieve the count of music for each year")
    public Flux<MusicRepository.YearCount> getMusicCountByYear(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        return musicService.getMusicCountByYear(limit)
                .onErrorResume(e -> {
                    log.error("Error fetching music count by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music count by year"));
                });
    }
    
    @GetMapping("/count-by-year/{year}")
    @Operation(summary = "Get music count for a specific year", description = "Retrieve the count of music for a specific year")
    public Mono<Long> getMusicCountBySpecificYear(
            @Parameter(description = "Year") @PathVariable int year) {
        return musicService.countMusicByYear(year)
                .onErrorResume(e -> {
                    log.error("Error counting music for year: {}", year, e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error counting music for year: " + year));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update music", description = "Create new music or update an existing one")
    public Mono<ResponseEntity<MusicDTO>> createOrUpdateMusic(@Valid @RequestBody Mono<MusicDTO> musicDTO) {
        return musicService.createOrUpdateMusic(musicDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating music", e);
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