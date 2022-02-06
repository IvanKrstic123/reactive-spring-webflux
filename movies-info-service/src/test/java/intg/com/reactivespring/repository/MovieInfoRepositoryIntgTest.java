package com.reactivespring.repository;

import com.reactivespring.domain.MovieInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DataMongoTest // scan application and look for repo classes and making it available in test case and set embedded mongo version in config
@ActiveProfiles("test")
class MovieInfoRepositoryIntgTest {

    @Autowired
    MovieInfoRepository movieInfoRepository;

    @BeforeEach
    void setUp() {
        var movieinfos = List.of(
                new MovieInfo(null, "Betmen Begins", 2005, List.of("Cristian Bale", "Michael Cane"), LocalDate.parse("2005-06-15")),
                new MovieInfo(null, "The Dark Knight", 2008, List.of("Cristian Bale", "Hearhledger"), LocalDate.parse("2008-07-18")),
                new MovieInfo("abc", "Dark Knight Rises", 2012, List.of("Cristian Bale", "Tom Hardt"), LocalDate.parse("2012-07-20"))
                );

        movieInfoRepository.saveAll(movieinfos)
                .blockLast(); // make sure this gets completed before invoking findAll(). blocking is alowed only in test cases
    }

    @AfterEach
    void tearDown() {
        movieInfoRepository.deleteAll().block();
    }

    @Test
    void findAll() {
        // when
       var moviesInfoFlux = movieInfoRepository.findAll().log();

       // then
        StepVerifier.create(moviesInfoFlux)
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void findById() {
        // when
        Mono<MovieInfo> moviesInfoMono = movieInfoRepository.findById("abc").log();

        // then
        StepVerifier.create(moviesInfoMono)
                .assertNext(movieInfo -> {
                    assertEquals("Dark Knight Rises", movieInfo.getName());
                })
                .verifyComplete();
    }

    @Test
    void saveMovieInfo() {
        // given
        var movieInfo = new MovieInfo(null, "Betmen Begins 1", 2005, List.of("Cristian Bale", "Michael Cane"), LocalDate.parse("2005-06-15"));

        // when
        var result = movieInfoRepository.save(movieInfo).log();

        // then
        StepVerifier.create(result)
                .assertNext(movieInfo1 -> {
                    assertNotNull(movieInfo.getMovieInfoId());
                    assertEquals("Betmen Begins 1", movieInfo1.getName());
                })
                .verifyComplete();
    }

    @Test
    void updatingMovieInfo() {
        // given
        var movieInfo = movieInfoRepository.findById("abc").block();
        movieInfo.setYear(2021);

        // when
        var result = movieInfoRepository.save(movieInfo).log();

        // then
        StepVerifier.create(result)
                .assertNext(movieInfo1 -> {
                    assertEquals(2021, movieInfo.getYear());
                })
                .verifyComplete();
    }

    @Test
    void deleteMovieInfo() {
        //given

        // when
        movieInfoRepository.deleteById("abc").block();
        var moviesInfo = movieInfoRepository.findAll();

        // then
        StepVerifier.create(moviesInfo)
                .expectNextCount(2)
                .verifyComplete();
    }



}