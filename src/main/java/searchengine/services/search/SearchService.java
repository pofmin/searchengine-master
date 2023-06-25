package searchengine.services.search;

import searchengine.dto.search.SearchRequestParams;
import searchengine.dto.search.SearchResponse;
import java.io.IOException;

public interface SearchService {
    SearchResponse getSearchResults(SearchRequestParams searchRequestParams) throws IOException, InterruptedException;
}
