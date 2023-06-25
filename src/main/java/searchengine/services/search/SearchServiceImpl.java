package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchRequestParams;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchData;
import searchengine.exception_handing.search.*;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.services.search.additional_classes.GetPagesService;
import searchengine.services.search.additional_classes.SetRelevanceService;
import searchengine.services.search.additional_classes.SearchDataGetter;
import searchengine.services.search.additional_classes.SiteCheckingService;
import searchengine.utility.LemmaGetter;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    public static List<SearchData> searchDataList = new ArrayList<>();
    public static List<PageEntity> allPages = new ArrayList<>();
    public static List<String> queryLemmas = new ArrayList<>();

    private final SitesList sitesList;
    private final LemmaRepository lemmaRepository;
    private final SiteCheckingService siteCheckingService;
    private final SetRelevanceService setRelevanceService;
    private final GetPagesService getPagesService;
    private final ApplicationContext applicationContext;

    private int previousOffset;

    @Value("${connection-humanizer.user-agent}")
    private String userAgent;
    @Value("${connection-humanizer.referrer}")
    private String referrer;

    @Override
    public SearchResponse getSearchResults(SearchRequestParams searchRequestParams) throws IOException, InterruptedException {
        if (searchRequestParams.getOffset() == 0) {
            previousOffset = 0;
            if (sitesList.getSites().size() == 0) {
                throw new NoSitesInConfigException();
            }
            if (searchRequestParams.getQuery().equals("")) {
                throw new EmptySearchRequestException();
            }
            List<SiteEntity> sitesToCheck = siteCheckingService.getSitesToCheck(searchRequestParams.getSite());
            if (sitesToCheck == null) {
                throw new SiteOutsideConfigException();
            }
            if (!siteCheckingService.checkSitesStatuses(sitesToCheck) && sitesToCheck.size() == 1) {
                throw new SiteNotIndexedException("Сайт для поиска не проиндексирован");
            } else if (!siteCheckingService.checkSitesStatuses(sitesToCheck) && sitesToCheck.size() > 1) {
                throw new SiteNotIndexedException("Нет проиндексированных сайтов для поиска");
            }
            getQueryLemmas(searchRequestParams.getQuery());
            getPagesService.getAllPages(sitesToCheck);
            List<LemmaEntity> allLemmaEntities = lemmaRepository.findBySiteEntityInAndLemmaIn(sitesToCheck, queryLemmas);
            setRelevanceService.setRelevance(allLemmaEntities);
        }
        getSearchDataList(searchRequestParams.getOffset(), searchRequestParams.getLimit());
        return new SearchResponse(allPages.size(), searchDataList);
    }

    public void getQueryLemmas(String query) throws IOException {
        queryLemmas.clear();
        HashMap<String, Integer> lemmasMap = LemmaGetter.getLemmas(query);
        lemmasMap.forEach((key, value) -> queryLemmas.add(key));
    }

    public void getSearchDataList(int offset, int limit) throws InterruptedException {
        searchDataList.clear();
        List<Thread> threadList = new ArrayList<>();
        int start = offset + previousOffset;
        int end = Math.min(allPages.size(), start + limit);
        for (int i = start; i < end; i++) {
            SearchDataGetter searchDataGetter = applicationContext.getBean(SearchDataGetter.class, allPages.get(i), queryLemmas);
            Thread thread = new Thread(searchDataGetter);
            thread.start();
            threadList.add(thread);
        }
        for (Thread thread : threadList) {
            thread.join();
        }
        Collections.sort(searchDataList, Comparator.comparing(SearchData::getRelevance));
        Collections.reverse(searchDataList);
    }
}
