package com.reactivespring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.Objects;

@WebFluxTest(controllers = FluxAndMonoController.class) //giving access to all endpoints of class
    @AutoConfigureWebTestClient
class FluxAndMonoControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void flux() {

        webTestClient
                .get()
                .uri("/flux")
                .exchange() //invoking endpoint
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Integer.class)
                .hasSize(3);
    }

    @Test
    void fluxApproach2() {

        var flux = webTestClient
                .get()
                .uri("/flux")
                .exchange() //invoking endpoint
                .expectStatus()
                .is2xxSuccessful()
                .returnResult(Integer.class) // type of response body
                .getResponseBody();

        StepVerifier.create(flux)
                .expectNext(1, 2, 3)
                .verifyComplete();
    }

    @Test
    void fluxApproach3() {

        var flux = webTestClient
                .get()
                .uri("/flux")
                .exchange() //invoking endpoint
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Integer.class)
                .consumeWith(listEntityExchangeResult -> {
                   var responseBody = listEntityExchangeResult.getResponseBody();
                   assert  (Objects.requireNonNull(responseBody).size() == 3);
                });

    }
}