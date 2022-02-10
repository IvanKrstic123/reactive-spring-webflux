package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MoviesInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1")
@Slf4j
public class MoviesInfoController {

    private MoviesInfoService movieInfoService;

    Sinks.Many<MovieInfo> moviesInfoSink = Sinks.many().replay().latest();

    public MoviesInfoController(MoviesInfoService movieInfoService) {
        this.movieInfoService = movieInfoService;
    }

    @GetMapping("/movieInfos") // if response status not defined it is 200
    public Flux<MovieInfo> getAllMovieInfos(@RequestParam(value = "year", required = false) Integer year) {
        log.info("godina je: " + year);
        if (year != null) {
            return movieInfoService.getMovieInfoByYear(year);
        }
        return movieInfoService.getAllMovieInfos().log();
    }

    @GetMapping("movieInfos/{id}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfoById(@PathVariable String id) {
        return movieInfoService.getMovieInfoById(id)
                .map(movieInfo -> {
                    return ResponseEntity.ok().body(movieInfo);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build())) // if movieinfo not found return 404
                .log();
    }

    @GetMapping(value = "movieInfos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MovieInfo> getMovieInfoById() {
        // ataching subscriber to a sink
        return moviesInfoSink.asFlux();
    }

    @PostMapping("/movieInfos")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MovieInfo> addMovieInfo(@RequestBody @Valid MovieInfo movieInfo) {

        // when movie is added publish it to the sink
        return movieInfoService.addMovieInfo(movieInfo)
                .doOnNext(savedInfo -> {
                    moviesInfoSink.tryEmitNext(savedInfo);
                });
    }

    @PutMapping("movieInfos/{id}")
    public Mono<ResponseEntity<MovieInfo>> updateMovieInfo(@RequestBody MovieInfo updatedMovieInfo, @PathVariable String id) {
        return movieInfoService.updateMovieInfo(updatedMovieInfo, id)
                .map(movieInfo -> {
                    return ResponseEntity.ok().body(movieInfo);
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build())) // if above call returns no data
                .log();
    }

    @DeleteMapping("/movieInfos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMovieInfo(@PathVariable String id) {
        return movieInfoService.deleteById(id);
    }
}
