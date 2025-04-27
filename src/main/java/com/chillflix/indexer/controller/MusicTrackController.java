package com.chillflix.indexer.controller;

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
@RequestMapping("/v1/music-tracks")
@RequiredArgsConstructor
@Tag(name = "Music Track", description = "Music Track management APIs")
@Slf4j
public class MusicTrackController {

    private final MusicTrackService musicTrackService;

    @GetMapping
    @Operation(summary = "Get all music tracks", description = "Retrieve all music tracks with pagination")
    public Flux<MusicTrackDTO> getAllMusicTracks(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicTrackService.getAllMusicTracks(pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching all music tracks", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching all music tracks"));
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a music track by ID", description = "Retrieve a music track by its UUID")
    @ApiResponse(responseCode = "200", description = "Successful operation", content = @Content(schema = @Schema(implementation = MusicTrackDTO.class)))
    @ApiResponse(responseCode = "404", description = "Music track not found")
    public Mono<ResponseEntity<MusicTrackDTO>> getMusicTrackById(@Parameter(description = "Music Track UUID") @PathVariable UUID id) {
        return musicTrackService.getMusicTrackById(id)
                .map(ResponseEntity::ok)
                .onErrorResume(MusicTrackNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error fetching music track by ID", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @GetMapping("/artist/{artistId}")
    @Operation(summary = "Get music tracks by artist ID", description = "Retrieve all music tracks from a specific artist")
    public Flux<MusicTrackDTO> getMusicTracksByArtistId(
            @Parameter(description = "Artist UUID") @PathVariable UUID artistId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        return musicTrackService.getMusicTracksByArtistId(artistId, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music tracks by artist ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music tracks by artist ID"));
                });
    }

    @GetMapping("/album/{albumId}")
    @Operation(summary = "Get music tracks by album ID", description = "Retrieve all music tracks from a specific album")
    public Flux<MusicTrackDTO> getMusicTracksByAlbumId(
            @Parameter(description = "Album UUID") @PathVariable UUID albumId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        return musicTrackService.getMusicTracksByAlbumId(albumId, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music tracks by album ID", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music tracks by album ID"));
                });
    }

    @GetMapping("/search")
    @Operation(summary = "Search music tracks", description = "Search music tracks based on a search term")
    public Flux<MusicTrackDTO> searchMusicTracks(
            @Parameter(description = "Search term") @RequestParam String term,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicTrackService.searchMusicTracks(term, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error searching music tracks", e);
                    return Flux.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error searching music tracks"));
                });
    }

    @GetMapping("/advanced-search")
    @Operation(summary = "Advanced search for music tracks", description = "Search music tracks with multiple optional parameters")
    public Flux<MusicTrackDTO> advancedSearch(
            @Parameter(description = "Track title") @RequestParam(required = false) String title,
            @Parameter(description = "Artist name") @RequestParam(required = false) String artist,
            @Parameter(description = "Album name") @RequestParam(required = false) String album,
            @Parameter(description = "Genre") @RequestParam(required = false) String genre,
            @Parameter(description = "Release year") @RequestParam(required = false) Integer year,
            @Parameter(description = "Language") @RequestParam(required = false) String language,
            @Parameter(description = "Quality") @RequestParam(required = false) String quality,
            @Parameter(description = "File type") @RequestParam(required = false) String fileType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (e.g. title,asc)") @RequestParam(defaultValue = "title,asc") String sort) {

        PageRequest pageRequest = createPageRequest(page, size, sort);

        return musicTrackService.advancedSearch(title, artist, album, genre, year, language, quality, fileType, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error performing advanced search on music tracks", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing advanced search on music tracks"));
                });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new music track", description = "Create a new music track entry")
    @ApiResponse(responseCode = "201", description = "Music track created successfully", content = @Content(schema = @Schema(implementation = MusicTrackDTO.class)))
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MusicTrackDTO>> createMusicTrack(@Valid @RequestBody Mono<MusicTrackDTO> musicTrackDTO) {
        return musicTrackService.saveMusicTrack(musicTrackDTO)
                .map(savedTrack -> ResponseEntity.status(HttpStatus.CREATED).body(savedTrack))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicTrackDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating music track", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a music track", description = "Update an existing music track entry")
    @ApiResponse(responseCode = "200", description = "Music track updated successfully", content = @Content(schema = @Schema(implementation = MusicTrackDTO.class)))
    @ApiResponse(responseCode = "404", description = "Music track not found")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public Mono<ResponseEntity<MusicTrackDTO>> updateMusicTrack(
            @Parameter(description = "Music Track UUID") @PathVariable UUID id,
            @Valid @RequestBody Mono<MusicTrackDTO> musicTrackDTO) {
        return musicTrackService.updateMusicTrack(id, musicTrackDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(MusicTrackNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicTrackDTO(id, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error updating music track", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a music track", description = "Delete a music track entry by its UUID")
    @ApiResponse(responseCode = "204", description = "Music track deleted successfully")
    @ApiResponse(responseCode = "404", description = "Music track not found")
    public Mono<ResponseEntity<Void>> deleteMusicTrack(@Parameter(description = "Music Track UUID") @PathVariable UUID id) {
        return musicTrackService.deleteMusicTrack(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(MusicTrackNotFoundException.class, e -> Mono.just(ResponseEntity.notFound().build()))
                .onErrorResume(e -> {
                    log.error("Error deleting music track", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @DeleteMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Bulk delete music tracks", description = "Delete multiple music tracks by their UUIDs")
    public Mono<ResponseEntity<Void>> bulkDeleteMusicTracks(@RequestBody List<UUID> ids) {
        return musicTrackService.bulkDeleteMusicTracks(ids)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(e -> {
                    log.error("Error performing bulk delete of music tracks", e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @PutMapping("/bulk")
    @Operation(summary = "Bulk update music tracks", description = "Update multiple music tracks")
    public Flux<MusicTrackDTO> bulkUpdateMusicTracks(@RequestBody List<MusicTrackDTO> musicTrackDTOs) {
        return musicTrackService.bulkUpdateMusicTracks(musicTrackDTOs)
                .onErrorResume(e -> {
                    log.error("Error performing bulk update of music tracks", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error performing bulk update of music tracks"));
                });
    }

    @GetMapping("/count")
    @Operation(summary = "Get total music track count", description = "Retrieve the total number of music tracks")
    public Mono<Long> getMusicTrackCount() {
        return musicTrackService.countMusicTracks()
                .onErrorResume(e -> {
                    log.error("Error counting music tracks", e);
                    return Mono.error(
                            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error counting music tracks"));
                });
    }

    @GetMapping("/language/{language}")
    @Operation(summary = "Get music tracks by language", description = "Retrieve music tracks in a specific language")
    public Flux<MusicTrackDTO> getMusicTracksByLanguage(
            @Parameter(description = "Language") @PathVariable String language,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return musicTrackService.getMusicTracksByLanguage(language, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music tracks by language", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music tracks by language"));
                });
    }

    @GetMapping("/genre/{genre}")
    @Operation(summary = "Get music tracks by genre", description = "Retrieve music tracks of a specific genre")
    public Flux<MusicTrackDTO> getMusicTracksByGenre(
            @Parameter(description = "Genre") @PathVariable String genre,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return musicTrackService.getMusicTracksByGenre(genre, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music tracks by genre", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music tracks by genre"));
                });
    }

    @GetMapping("/year/{year}")
    @Operation(summary = "Get music tracks by year", description = "Retrieve music tracks released in a specific year")
    public Flux<MusicTrackDTO> getMusicTracksByYear(
            @Parameter(description = "Year") @PathVariable int year,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return musicTrackService.getMusicTracksByYear(year, pageRequest)
                .onErrorResume(e -> {
                    log.error("Error fetching music tracks by year", e);
                    return Flux.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error fetching music tracks by year"));
                });
    }

    @PostMapping("/create-or-update")
    @Operation(summary = "Create or update a music track", description = "Create a new music track or update an existing one")
    public Mono<ResponseEntity<MusicTrackDTO>> createOrUpdateMusicTrack(@Valid @RequestBody Mono<MusicTrackDTO> musicTrackDTO) {
        return musicTrackService.createOrUpdateMusicTrack(musicTrackDTO)
                .map(ResponseEntity::ok)
                .onErrorResume(ValidationException.class,
                        e -> Mono.just(ResponseEntity.badRequest()
                                .body(new MusicTrackDTO(null, e.getMessage(), null, null, null, null, null, null, null, null,
                                        null, null, null, null, null, null, null, null))))
                .onErrorResume(e -> {
                    log.error("Error creating or updating music track", e);
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