package searchengine.services;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchRequestParams;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SearchData;
import searchengine.exception_handing.search.*;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utility.LemmaGetter;
import searchengine.utility.SnippetGetter;
import searchengine.utility.UtilityClass;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    private SitesList sitesList;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private List<LemmaEntity> lemmaEntities = new ArrayList<>();
    private List<SearchData> searchDataList = new ArrayList<>();
    private List<PageEntity> allPages = new ArrayList<>();
    private List<String> queryLemmas = new ArrayList<>();
    private int previousOffset;

    @Value("${indexing-settings.snippet-length-max}")
    private int snippetLengthMax;
    @Value("${indexing-settings.frequency-threshold}")
    private float frequencyThreshold;
    @Value("${connection-humanizer.user-agent}")
    private String userAgent;
    @Value("${connection-humanizer.referrer}")
    private String referrer;

    @Autowired
    public SearchServiceImpl(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

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
            List<SiteEntity> sitesToCheck = getSitesToCheck(searchRequestParams.getSite());
            if (sitesToCheck == null) {
                throw new SiteOutsideConfigException();
            }
            if (!checkSitesStatuses(sitesToCheck) && sitesToCheck.size() == 1) {
                throw new SiteNotIndexedException("Сайт для поиска не проиндексирован");
            } else if (!checkSitesStatuses(sitesToCheck) && sitesToCheck.size() > 1) {
                throw new SiteNotIndexedException("Нет проиндексированных сайтов для поиска");
            }
            getQueryLemmas(searchRequestParams.getQuery());
            getAllPages(sitesToCheck);
            List<LemmaEntity> allLemmaEntities = lemmaRepository.findBySiteEntityInAndLemmaIn(sitesToCheck, queryLemmas);
            setAbsoluteRelevance(allLemmaEntities);
            setRelevance(getBiggestAbsoluteRelevance());
        }
        getSearchDataList(searchRequestParams.getOffset(), searchRequestParams.getLimit());
        return new SearchResponse(allPages.size(), searchDataList);
    }

    public List<SiteEntity> getSitesToCheck(String siteUrl) {
        List<SiteEntity> sites = new ArrayList<>();
        if (siteUrl.equals("")) {
            for (Site site : sitesList.getSites()) {
                SiteEntity siteEntity = siteRepository.findByUrl(UtilityClass.getCutURL(site.getUrl()));
                sites.add(siteEntity);
            }
            return sites;
        }
        boolean siteIsPresent = false;
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().contains(siteUrl)) {
                siteIsPresent = true;
                SiteEntity siteEntity = siteRepository.findByUrl(UtilityClass.getCutURL(siteUrl));
                sites.add(siteEntity);
            }
        }
        if (!siteIsPresent) {
            return null;
        }
        return sites;
    }

    public boolean checkSitesStatuses(List<SiteEntity> sitesToCheck) {
        boolean isValid = false;
        for (SiteEntity siteEntity : sitesToCheck) {
            if (siteEntity.getStatus().toString().equals("INDEXED")) {
                isValid = true;
            }
        }
        return isValid;
    }

    public void getQueryLemmas(String query) throws IOException {
        queryLemmas.clear();
        HashMap<String, Integer> lemmasMap = LemmaGetter.getLemmas(query);
        lemmasMap.forEach((key, value) -> queryLemmas.add(key));
    }

    public void getAllPages(List<SiteEntity> sitesToCheck) {
        allPages.clear();
        for (SiteEntity siteEntity : sitesToCheck) {
            int allPagesCount = pageRepository.findAllBySiteEntity(siteEntity).size();
            lemmaEntities = lemmaRepository.findBySiteEntityAndLemmaIn(siteEntity, queryLemmas);
            if (lemmaEntities.size() < queryLemmas.size()) continue;

            lemmaEntities.removeIf(lemmaEntity -> ((float) lemmaEntity.getFrequency() / (float) allPagesCount) > frequencyThreshold);
            Collections.sort(lemmaEntities, Comparator.comparing(LemmaEntity::getFrequency));
            if (lemmaEntities.size() == 0) continue;

            List<PageEntity> pages = getPagesByLemma(0);
            List<PageEntity> pagesFinal = lemmaEntities.size() == 1 ? pages : compareListsOfPages(pages, 1);
            if (pagesFinal != null) {
                allPages.addAll(pagesFinal);
            }
        }
    }

    public List<PageEntity> compareListsOfPages(List<PageEntity> pages, int count) {
        List<PageEntity> pagesNext = getPagesByLemma(count);
        List<PageEntity> result = pages.stream().filter(pagesNext::contains).collect(Collectors.toList());
        if (result.size() == 0) {
            return null;
        }
        count++;
        if (count < lemmaEntities.size()) {
            compareListsOfPages(result, count);
        }
        return result;
    }

    public List<PageEntity> getPagesByLemma(int count) {
        List<IndexEntity> indexes = indexRepository.findByLemmaEntity(lemmaEntities.get(count));
        List<PageEntity> pages = new ArrayList<>();
        indexes.forEach(index -> pages.add(index.getPageEntity()));
        return pages;
    }

    public void setAbsoluteRelevance(List<LemmaEntity> allLemmaEntities) {
        for (PageEntity pageEntity : allPages) {
            List<IndexEntity> indexEntities = indexRepository.findByPageEntityAndLemmaEntityIn(pageEntity, allLemmaEntities);
            float absoluteRelevance = 0;
            for (IndexEntity indexEntity : indexEntities) {
                absoluteRelevance += indexEntity.getRanking();
            }
            pageEntity.setRelevanceAbsolute(absoluteRelevance);
        }
    }

    public void setRelevance(float biggestRelevanceAbsolute) {
        for (PageEntity pageEntity : allPages) {
            pageEntity.setRelevance(pageEntity.getRelevanceAbsolute() / biggestRelevanceAbsolute);
        }
    }

    public float getBiggestAbsoluteRelevance() {
        float biggestRelevanceAbsolute = 0F;
        for (PageEntity pageEntity : allPages) {
            if (pageEntity.getRelevanceAbsolute() > biggestRelevanceAbsolute) {
                biggestRelevanceAbsolute = pageEntity.getRelevanceAbsolute();
            }
        }
        return biggestRelevanceAbsolute;
    }

    public void getSearchDataList(int offset, int limit) throws InterruptedException {
        searchDataList.clear();
        List<Thread> threadList = new ArrayList<>();
        int start = offset + previousOffset;
        int end = Math.min(allPages.size(), start + limit);
        for (int i = start; i < end; i++) {
            SearchDataGetter searchDataGetter = new SearchDataGetter(allPages.get(i));
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

    private class SearchDataGetter extends Thread {
        PageEntity pageEntity;

        public SearchDataGetter(PageEntity pageEntity) {
            this.pageEntity = pageEntity;
        }

        @SneakyThrows
        @Override
        public void run() {
            SearchData searchData = new SearchData();
            SnippetGetter snippetGetter = new SnippetGetter(pageEntity, queryLemmas, snippetLengthMax);
            searchData.setSnippet(snippetGetter.getSnippet());
            searchData.setSite(pageEntity.getSiteEntity().getUrl());
            searchData.setSiteName(pageEntity.getSiteEntity().getName());
            searchData.setUri(pageEntity.getPath());
            searchData.setTitle(UtilityClass.getTitle(pageEntity));
            searchData.setRelevance(pageEntity.getRelevance());
            searchDataList.add(searchData);
        }
    }
}
