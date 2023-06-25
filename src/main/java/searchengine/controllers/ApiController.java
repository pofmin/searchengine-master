package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.SearchRequestParams;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchService;
import searchengine.services.statistics.StatisticsService;
import searchengine.utility.UtilityClass;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody String inputUrl) throws UnsupportedEncodingException {
        String url = java.net.URLDecoder.decode(inputUrl, StandardCharsets.UTF_8.name()).
                replace("www.", "").replace("url=", "");
        return ResponseEntity.ok(indexingService.indexPage(UtilityClass.getURLWithSlash(url)));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchMethod(@RequestParam("query") Optional<String> queryOptional,
                                                       @RequestParam("site") Optional<String> siteOptional,
                                                       @RequestParam("offset") Optional<Integer> offsetOptional,
                                                       @RequestParam("limit") Optional<Integer> limitOptional) throws IOException, InterruptedException {
        String query = queryOptional.orElse("");
        String site = siteOptional.orElse("");
        int offset = offsetOptional.orElse(0);
        int limit = limitOptional.orElse(20);
        SearchRequestParams searchRequestParams = new SearchRequestParams(query, site, offset, limit);
        return ResponseEntity.ok(searchService.getSearchResults(searchRequestParams));
    }
}
