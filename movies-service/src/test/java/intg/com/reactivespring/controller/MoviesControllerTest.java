package com.reactivespring.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.reactivespring.domain.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // spring context, and do not use 8080
@ActiveProfiles("test") // different from specified profiles
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 8084) // spin up a HTTP server in port 8084
@TestPropertySource(    // overriding properties when running tests
        properties = {
                "restClient.moviesInfoUrl=http://localhost:8084/v1/movieInfos",
                "restClient.reviewsUrl=http://localhost:8084/v1/reviews"
        }
)
class MoviesControllerTest {

    @Autowired
    WebTestClient webTestClient;

    /*
        in order to create a response from a http call - STUB - stubFor()
    */

    @Test
    void retrieveMovieById() {
        var movieId = "abc";
        stubFor(get(urlEqualTo("/v1/movieInfos" + "/" + movieId))
                .willReturn(
                        aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("movieinfo.json"))); // searching in resource folder

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("reviews.json"))); // searching in resource folder

        webTestClient.get()
                .uri("/v1/movies/{id}", movieId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Movie.class)
                .consumeWith(movieEntityExchangeResult -> {
                    var movie = movieEntityExchangeResult.getResponseBody();
                    assertNotNull(movie);
                    assertEquals(movie.getReviewList().size(), 3);
                    assertEquals(movie.getMovieInfo().getName(), "Vruc vetar");
                });
    }

    @Test
    void retrieveMovieInfoById_404() {
        var movieId = "abc";
        stubFor(get(urlEqualTo("/v1/movieInfos" + "/" + movieId))
                .willReturn(
                        aResponse()
                                .withStatus(404)));

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("reviews.json"))); // searching in resource folder

        webTestClient.get()
                .uri("/v1/movies/{id}", movieId)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(String.class)
                .isEqualTo("There is no Movie Info for the passed in ID:  abc");
    }

    @Test
    void retrieveMovieInfoById_reviews_404() {
        var movieId = "abc";
        stubFor(get(urlEqualTo("/v1/movieInfos" + "/" + movieId))
                .willReturn(
                        aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBodyFile("movieinfo.json"))); // searching in resource folder

        stubFor(get(urlPathEqualTo("/v1/reviews"))
                .willReturn(
                        aResponse()
                                .withStatus(404))); // searching in resource folder

        // still successful because reviews are optional and movie can have no reviews
        webTestClient.get()
                .uri("/v1/movies/{id}", movieId)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Movie.class)
                .consumeWith(movieEntityExchangeResult -> {
                    var movie = movieEntityExchangeResult.getResponseBody();
                    assertNotNull(movie);
                    assertEquals(movie.getReviewList().size(), 0);
                    assertEquals(movie.getMovieInfo().getName(), "Vruc vetar");
                });
    }

    @Test
    void retrieveMovieInfoById_5xx() {
        var movieId = "abc";
        stubFor(get(urlEqualTo("/v1/movieInfos" + "/" + movieId))
                .willReturn(
                        aResponse()
                                .withStatus(500)
                                .withBody("MovieInfo Service Unavailable")));

        // we dont need stub for review because it will fail before calling reviews controller
//        stubFor(get(urlPathEqualTo("/v1/reviews"))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", "application.json")
//                        .withBody("reviews.json"))); // searching in resource folder

        webTestClient.get()
                .uri("/v1/movies/{id}", movieId)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody(String.class)
                .isEqualTo("Server Exception in MoviesInfoService:  MovieInfo Service Unavailable");

        // testing retry when failure occur
        WireMock.verify(4, getRequestedFor(urlEqualTo("/v1/movieInfos" + "/" + movieId)));
    }
}