package com.reactivespring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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

    @Test
    void retrieveMovieById() {
    }
}