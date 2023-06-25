package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception_handing.indexing.*;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.additional_classes.OnePageIndexer;
import searchengine.services.indexing.additional_classes.SiteScanner;
import searchengine.utility.UtilityClass;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    public static final String EXCLUDE_REGEX = ".+(\\.pdf|\\.PDF|\\.png|\\.json|\\.css|\\.doc|\\.docx|\\.jpg|\\.JPG|\\.jpeg|\\.xlsx|\\.xls|\\.ppt|\\.xml|\\.php|\\.jfif|\\.eps|\\?|=).*";
    public static final String URL_REGEX_HTTP = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
    public static final String URL_REGEX = "[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
    public static Map<String, Boolean> isRunning = new HashMap<>();
    public static List<String> onePageIndexes = new ArrayList<>();

    private final ApplicationContext applicationContext;
    private final SitesList sitesList;
    private final SiteRepository siteRepository;

    @Value("${connection-humanizer.user-agent}")
    private String userAgent;
    @Value("${connection-humanizer.referrer}")
    private String referrer;
    @Value("${connection-humanizer.sleep-length.minimum}")
    private int sleepMinimumValue;
    @Value("${connection-humanizer.sleep-length.maximum}")
    private int sleepMaximumValue;

    @Override
    public IndexingResponse startIndexing() {
        if (isRunning.containsValue(true)) {
            throw new IndexingStatusException("Индексация уже запущена");
        }
        onePageIndexes.clear();
        for (Site site : sitesList.getSites()) {
            isRunning.put(site.getName(), true);
            SiteScanner siteScanner = applicationContext.getBean(SiteScanner.class, site);
            Thread thread = new Thread(siteScanner);
            thread.start();
        }
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (!isRunning.containsValue(true)) {
            throw new IndexingStatusException("Индексация не запущена");
        }
        for (Site site : sitesList.getSites()) {
            isRunning.put(site.getName(), false);
        }
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse indexPage(String url) {
        if (!url.matches(URL_REGEX) && !url.matches(URL_REGEX_HTTP)) {
            throw new InvalidUrlException();
        }
        Site currentSite = UtilityClass.getCurrentSite(url, sitesList);
        if (currentSite == null) {
            throw new PageOutsideConfigException();
        }
        String cutURL = UtilityClass.getCutURL(currentSite.getUrl());
        if (siteRepository.findByUrl(cutURL) != null &&
                siteRepository.findByUrl(cutURL).getStatus().toString().equals("INDEXING")) {
            throw new IndexingInProgressException();
        }
        if (!onePageIndexes.contains(url)) {
            OnePageIndexer onePageIndexer = applicationContext.getBean(OnePageIndexer.class, currentSite, url);
            Thread thread = new Thread(onePageIndexer);
            thread.start();
        }
        return new IndexingResponse();
    }
}
