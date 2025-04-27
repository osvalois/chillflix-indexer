package com.chillflix.controller;
import com.chillflix.indexer.controller.MovieController;
import com.chillflix.indexer.dto.MovieDTO;
import com.chillflix.indexer.exception.MovieNotFoundException;
import com.chillflix.indexer.exception.ValidationException;
import com.chillflix.indexer.repository.MovieRepository;
import com.chillflix.indexer.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieController movieController;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(movieController).build();
    }

    @Test
    void searchMovies_Success() {
        MovieDTO movie = new MovieDTO(UUID.randomUUID(), "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.searchMovies(anyString(), any(PageRequest.class))).thenReturn(Flux.just(movie));

        webTestClient.get().uri("/v1/movies/search?term=Test&page=0&size=10&sort=title,asc")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(1)
                .contains(movie);
    }

    @Test
    void getMovieById_Success() {
        UUID id = UUID.randomUUID();
        MovieDTO movie = new MovieDTO(id, "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.getMovieById(id)).thenReturn(Mono.just(movie));

        webTestClient.get().uri("/v1/movies/" + id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MovieDTO.class)
                .isEqualTo(movie);
    }

    @Test
    void getMovieById_NotFound() {
        UUID id = UUID.randomUUID();
        when(movieService.getMovieById(id)).thenReturn(Mono.error(new MovieNotFoundException("Movie not found")));

        webTestClient.get().uri("/v1/movies/" + id)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createMovie_Success() {
        MovieDTO movieDTO = new MovieDTO(null, "New Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        MovieDTO savedMovie = new MovieDTO(UUID.randomUUID(), "New Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.saveMovie(any())).thenReturn(Mono.just(savedMovie));

        webTestClient.post().uri("/v1/movies")
                .bodyValue(movieDTO)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MovieDTO.class)
                .isEqualTo(savedMovie);
    }

    @Test
    void updateMovie_Success() {
        UUID id = UUID.randomUUID();
        MovieDTO movieDTO = new MovieDTO(id, "Updated Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.updateMovie(eq(id), any())).thenReturn(Mono.just(movieDTO));

        webTestClient.put().uri("/v1/movies/" + id)
                .bodyValue(movieDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MovieDTO.class)
                .isEqualTo(movieDTO);
    }

    @Test
    void deleteMovie_Success() {
        UUID id = UUID.randomUUID();
        when(movieService.deleteMovie(id)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/v1/movies/" + id)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void getAllMovies_Success() {
        MovieDTO movie1 = new MovieDTO(UUID.randomUUID(), "Movie 1", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        MovieDTO movie2 = new MovieDTO(UUID.randomUUID(), "Movie 2", 2022, "magnet:?xt=urn:btih:456", 2, "tt7654321", "Spanish", "Spanish", "4K", "MKV", "def456", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.getAllMovies(any(PageRequest.class))).thenReturn(Flux.just(movie1, movie2));

        webTestClient.get().uri("/v1/movies?page=0&size=10&sort=title,asc")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(2)
                .contains(movie1, movie2);
    }

    @Test
    void getMovieCount_Success() {
        when(movieService.countMovies()).thenReturn(Mono.just(10L));

        webTestClient.get().uri("/v1/movies/count")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(10L);
    }

    @Test
    void getMoviesByYear_Success() {
        MovieDTO movie = new MovieDTO(UUID.randomUUID(), "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.getMoviesByYear(eq(2021), any(PageRequest.class))).thenReturn(Flux.just(movie));

        webTestClient.get().uri("/v1/movies/year/2021?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(1)
                .contains(movie);
    }

    @Test
    void getMoviesByLanguage_Success() {
        MovieDTO movie = new MovieDTO(UUID.randomUUID(), "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.getMoviesByLanguage(eq("English"), any(PageRequest.class))).thenReturn(Flux.just(movie));

        webTestClient.get().uri("/v1/movies/language/English?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(1)
                .contains(movie);
    }

    @Test
    void bulkDeleteMovies_Success() {
        List<UUID> ids = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        when(movieService.bulkDeleteMovies(ids)).thenReturn(Mono.empty());

        ((RequestBodySpec) webTestClient.delete().uri("/v1/movies/bulk"))
                .bodyValue(ids)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void bulkUpdateMovies_Success() {
        MovieDTO movie1 = new MovieDTO(UUID.randomUUID(), "Movie 1", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        MovieDTO movie2 = new MovieDTO(UUID.randomUUID(), "Movie 2", 2022, "magnet:?xt=urn:btih:456", 2, "tt7654321", "Spanish", "Spanish", "4K", "MKV", "def456", null, null, null, null, null, null, null, null, null, null, null);
        List<MovieDTO> movies = Arrays.asList(movie1, movie2);
        when(movieService.bulkUpdateMovies(movies)).thenReturn(Flux.fromIterable(movies));

        webTestClient.put().uri("/v1/movies/bulk")
                .bodyValue(movies)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(2)
                .contains(movie1, movie2);
    }

    @Test
    void advancedSearch_Success() {
        MovieDTO movie = new MovieDTO(UUID.randomUUID(), "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.advancedSearch(anyString(), anyInt(), anyString(), anyString(), anyString(), any(PageRequest.class)))
                .thenReturn(Flux.just(movie));

        webTestClient.get().uri("/v1/movies/advanced-search?title=Test&year=2021&language=English&quality=HD&fileType=MP4&page=0&size=10&sort=title,asc")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MovieDTO.class)
                .hasSize(1)
                .contains(movie);
    }

    @Test
    void getTopLanguages_Success() {
        MovieRepository.LanguageCount languageCount = new MovieRepository.LanguageCount() {
            @Override
            public String getLanguage() {
                return "English";
            }

            @Override
            public Long getCount() {
                return 10L;
            }
        };
        when(movieService.getTopLanguages(anyInt())).thenReturn(Flux.just(languageCount));

        webTestClient.get().uri("/v1/movies/top-languages?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].language").isEqualTo("English")
                .jsonPath("$[0].count").isEqualTo(10);
    }

    @Test
    void getMovieCountByYear_Success() {
        MovieRepository.YearCount yearCount = new MovieRepository.YearCount() {
            @Override
            public Integer getYear() {
                return 2021;
            }

            @Override
            public Long getCount() {
                return 10L;
            }
        };
        when(movieService.getMovieCountByYear(anyInt())).thenReturn(Flux.just(yearCount));

        webTestClient.get().uri("/v1/movies/year-count?limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].year").isEqualTo(2021)
                .jsonPath("$[0].count").isEqualTo(10);
    }

    @Test
    void getMovieCountByYear_SpecificYear_Success() {
        when(movieService.countMoviesByYear(2021)).thenReturn(Mono.just(10L));

        webTestClient.get().uri("/v1/movies/count-by-year/2021")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(10L);
    }

    @Test
    void createOrUpdateMovie_Success() {
        MovieDTO movieDTO = new MovieDTO(UUID.randomUUID(), "Test Movie", 2021, "magnet:?xt=urn:btih:123", 1, "tt1234567", "English", "English", "HD", "MP4", "abc123", null, null, null, null, null, null, null, null, null, null, null);
        when(movieService.createOrUpdateMovie(any())).thenReturn(Mono.just(movieDTO));

        webTestClient.post().uri("/v1/movies/create-or-update")
                .bodyValue(movieDTO)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MovieDTO.class)
                .isEqualTo(movieDTO);
    }
}