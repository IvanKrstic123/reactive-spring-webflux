package com.reactivespring.controller;

import com.reactivespring.client.MoviesInfoRestClient;
import com.reactivespring.client.ReviewsRestClient;
import com.reactivespring.domain.Movie;
import com.reactivespring.domain.MovieInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/movies")
public class MoviesController {

    /*
        this controller is used to communicate with other two services
        movie-info-service
        movie-review-service
        movie is represented by movie info and multiple reviews
     */

    private MoviesInfoRestClient moviesInfoRestClient;
    private ReviewsRestClient reviewsRestClient;

    public MoviesController(MoviesInfoRestClient moviesInfoRestClient, ReviewsRestClient reviewsRestClient) {
        this.moviesInfoRestClient = moviesInfoRestClient;
        this.reviewsRestClient = reviewsRestClient;
    }

    @GetMapping("/{id}")
    public Mono<Movie> retrieveMovieById(@PathVariable("id") String movieId) {

        // any type we are dealing with transformation that returning reactive type we use flatmap
        // e.g. calling function that returns Mono<Object> - Flux<Object>
        return moviesInfoRestClient.retrieveMovieInfo(movieId)
                .flatMap(movieInfo -> {
                    var reviewsListMono = reviewsRestClient.retrieveReviews(movieId)
                            .collectList();

                    return reviewsListMono.map(reviews -> {
                        return new Movie(movieInfo, reviews);
                    });
                });
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MovieInfo> retrieveMovieInfos() {
        /*
            start movie info and movie service
            open http://localhost:8082/v1/movies/stream
            POST movieInfo using http://localhost:8080/v1/movieInfos/
         */
       return moviesInfoRestClient.retrieveMovieInfoStream();
    }

}
