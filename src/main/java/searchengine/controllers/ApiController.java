package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.ErrorResponse;
import searchengine.dto.response.Response;
import searchengine.dto.response.SearchResult;
import searchengine.dto.statistics.SearchData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing(){
        if (indexingService.indexingAll()){
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ErrorResponse(false, "Индексация уже запущена"),
                    HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing(){
        if (indexingService.stopIndexing()){
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ErrorResponse(false, "Индексация не запущена"),
                    HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url){
        if (url.isEmpty()){
            return new ResponseEntity<>(new ErrorResponse(false, "Страница не указана"),
                    HttpStatus.BAD_REQUEST);
        }
        if (indexingService.urlIndexing(url)){
            return new ResponseEntity<>(new Response(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new ErrorResponse(
                    false,
                    "Данная страница находится за пределами сайтов, указанных в конфигурацинном файле"),
            HttpStatus.BAD_REQUEST);
        }
    }
    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "")
                                                     String request,
                                         @RequestParam(name = "site", required = false, defaultValue = "")
                                                     String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0")
                                                     int offset){
        if (request.isEmpty()){
            return new ResponseEntity<>(new ErrorResponse(false, "Пустой запрос"), HttpStatus.BAD_REQUEST);
        } else {
            List<SearchData> searchData;
            int count;
            if (!site.isEmpty()){
                if (siteRepository.findByUrl(site) != null){
                    searchData = searchService.searchAcrossSite(request, site, offset, 20);
                    count = searchService.getCount();
                } else {
                    return new ResponseEntity<>(new ErrorResponse(false, "Требуемая страница не найдена"),
                            HttpStatus.BAD_REQUEST);
                }
            } else {
                searchData = searchService.searchAcrossAllSites(request, offset, 20);
                count = searchService.getCount();
            }
            return new ResponseEntity<>(new SearchResult(true, count, searchData), HttpStatus.OK);
        }

    }
}
