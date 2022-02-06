package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MoviesInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MoviesInfoController.class)
@AutoConfigureWebTestClient
public class MoviesInfoControllerUnitTest {

    public static final String MOVIE_INFOS_URL = "/v1/movieInfos";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MoviesInfoService moviesInfoService;

    @Test
     void getAllMoviesInfo() {
        // given
        var movieinfos = List.of(
                new MovieInfo(null, "Betmen Begins", 2005, List.of("Cristian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Cristian Bale", "Hearhledger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises", 2012, List.of("Cristian Bale", "Tom Hardt"), LocalDate.parse("2012-07-20"))
        );

        // when
        when(moviesInfoService.getAllMovieInfos()).thenReturn(Flux.fromIterable(movieinfos));

        //then
        webTestClient.get()
                .uri(MOVIE_INFOS_URL)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(MovieInfo.class)
                .hasSize(3);
     }

    @Test
    void addMovieInfo() {
        // given
        var movieInfo =  new MovieInfo("abcde", "Betmen Begins", 2005, List.of("Cristian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));

        // when
        when(moviesInfoService.addMovieInfo(isA(MovieInfo.class))).thenReturn(Mono.just(movieInfo));
        webTestClient.post()
                .uri(MOVIE_INFOS_URL)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var savedMovieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(savedMovieInfo);
                    assertNotNull(savedMovieInfo.getMovieInfoId());
                    assertEquals("abcde", savedMovieInfo.getMovieInfoId());
                });
    }

    @Test
    void addMovieInfoValidation() {
        // given
        var movieInfo =  new MovieInfo("abcde", "", null, List.of(""), LocalDate.parse("2005-06-15"));
        var expectedErrorMessage = "movieInfo.cast must be present,movieInfo.name must be present,must not be null";

        webTestClient.post()
                .uri(MOVIE_INFOS_URL)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .consumeWith(stringEntityExchangeResult -> {
                    var responseBody = stringEntityExchangeResult.getResponseBody();
                    System.out.println(responseBody);
                    assertNotNull(responseBody);
                    assertEquals(expectedErrorMessage, responseBody);
                });
    }

    @Test
    void updateMovieInfo() {
        // given
        var movieInfoId = "abc";
        var movieInfo =  new MovieInfo("abcde", "Vratice se rode", 2007, List.of("Srdjan Todorovic", "Ljubomir Bandovic"), LocalDate.parse("2007-06-15"));
        // when
        when(moviesInfoService.updateMovieInfo(isA(MovieInfo.class), anyString())).thenReturn(Mono.just(movieInfo));
        webTestClient.put()
                .uri(MOVIE_INFOS_URL + "/{id}", movieInfoId)
                .bodyValue(movieInfo)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(MovieInfo.class)
                .consumeWith(movieInfoEntityExchangeResult -> {
                    var updatedMovieInfo = movieInfoEntityExchangeResult.getResponseBody();
                    assertNotNull(updatedMovieInfo);
                    assertNotNull(updatedMovieInfo.getMovieInfoId());
                    assertEquals("Vratice se rode", updatedMovieInfo.getName());
                });
    }

}
