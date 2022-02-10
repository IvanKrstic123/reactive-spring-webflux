package com.reactivespring.handler;

import com.reactivespring.domain.Review;
import com.reactivespring.exception.ReviewDataException;
import com.reactivespring.exception.ReviewNotFoundException;
import com.reactivespring.repository.ReviewReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReviewHandler {

    Sinks.Many<Review> reviewsSink = Sinks.many().replay().all();

    @Autowired
    private Validator validator;

    private ReviewReactiveRepository reviewReactiveRepository;

    public ReviewHandler(ReviewReactiveRepository reviewReactiveRepository) {
        this.reviewReactiveRepository = reviewReactiveRepository;
    }

    // persisting data in database and building ServerResponse
    public Mono<ServerResponse> addReview(ServerRequest request) {
        return request.bodyToMono(Review.class)
                .doOnNext(this::validate) // bean validation using Validator
                .flatMap(review -> {
                    return reviewReactiveRepository.save(review);
                })
                .doOnNext(review -> {
                    reviewsSink.tryEmitNext(review);
                })
                .flatMap(savedReview -> {
                    return ServerResponse.status(HttpStatus.CREATED)
                            .bodyValue(savedReview);
                });
    }

    private void validate(Review review) {
        var constraintViolations = validator.validate(review);
        log.info("constraintViolations: {}", constraintViolations);
        if (constraintViolations.size() > 0 ) {
            var errorMessage = constraintViolations
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(","));
            // break flow - do not persist data
            throw new ReviewDataException(errorMessage);
        }


    }

    public Mono<ServerResponse> getReviews(ServerRequest request) {

        var movieInfoId = request.queryParam("movieInfoId");
        System.out.println(movieInfoId);

        if (movieInfoId.isPresent()) {
            var reviewsFlux = reviewReactiveRepository.findReviewsByMovieInfoId(Long.valueOf(movieInfoId.get()));
            return ServerResponse.ok().body(reviewsFlux, Review.class); // can be extracted to a method
        } else {
            var reviewsFlux = reviewReactiveRepository.findAll();
            return ServerResponse.ok().body(reviewsFlux, Review.class);
        }
    }

    public Mono<ServerResponse> updateReview(ServerRequest request) {

        String reviewId = request.pathVariable("id");

        // if movieInfoId not correct throw ReviewNotFound
        Mono<Review> existingReview = reviewReactiveRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new ReviewNotFoundException("Review not found for the given Review id " + reviewId)));

        return existingReview
                 .flatMap(review -> request.bodyToMono(Review.class)
                         .map(reqReview -> {
                             review.setComment(reqReview.getComment());
                             review.setRating(reqReview.getRating());

                            return review;
                         }))
                 .flatMap(reviewReactiveRepository::save)
                 .flatMap(savedReview -> ServerResponse.ok().bodyValue(savedReview));
    }

    public Mono<ServerResponse> deleteReview(ServerRequest request) {
        String reviewId = request.pathVariable("id");

        Mono<Review> existingReview = reviewReactiveRepository.findById(reviewId);

       return existingReview.flatMap(review -> reviewReactiveRepository.deleteById(reviewId)
               .then(ServerResponse.noContent().build()));
    }

    public Mono<ServerResponse> getReviewsStream(ServerRequest request) {
        System.out.println("Sve ti jebem!");

        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(reviewsSink.asFlux(), Review.class)
                .log();
    }
}
