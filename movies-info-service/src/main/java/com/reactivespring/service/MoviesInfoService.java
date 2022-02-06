package com.reactivespring.service;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.repository.MovieInfoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class MoviesInfoService {

    private MovieInfoRepository movieInfoRepository;

    public MoviesInfoService(MovieInfoRepository movieInfoRepository) {
        this.movieInfoRepository = movieInfoRepository;
    }

    public Mono<MovieInfo> addMovieInfo(MovieInfo movieInfo) {
        return movieInfoRepository.save(movieInfo);
    }

    public Flux<MovieInfo> getAllMovieInfos() {
        return movieInfoRepository.findAll();
    }

    public Mono<MovieInfo> getMovieInfoById(String id) {
        return movieInfoRepository.findById(id);
    }

    public Mono<MovieInfo> updateMovieInfo(MovieInfo updateMovieInfo, String id) {
        return movieInfoRepository.findById(id)
                .flatMap(movieInfo -> { // whenever we call some operation that will give reactive type : Flux and Mono we use flatmap
                    movieInfo.setCast(updateMovieInfo.getCast());
                    movieInfo.setName(updateMovieInfo.getName());
                    movieInfo.setYear(updateMovieInfo.getYear());
                    movieInfo.setRelease_date(updateMovieInfo.getRelease_date());
                    return movieInfoRepository.save(movieInfo);
                });

    }

    public Mono<Void> deleteById(String id) {
        return movieInfoRepository.deleteById(id);
    }
}
