package searchengine.services;

import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.exception_handing.indexing.*;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utility.LemmaGetter;
import searchengine.utility.UtilityClass;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Service
public class IndexingServiceImpl implements IndexingService {
    private static final String EXCLUDE_REGEX = ".+(\\.pdf|\\.PDF|\\.png|\\.json|\\.css|\\.doc|\\.docx|\\.jpg|\\.JPG|\\.jpeg|\\.xlsx|\\.xls|\\.ppt|\\.xml|\\.php|\\.jfif|\\.eps|\\?|=).*";
    private static final String URL_REGEX = "[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
    public static Map<String, Boolean> isRunning = new HashMap<>();
    private List<String> onePageIndexes = new ArrayList<>();
    private SitesList sitesList;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;

    @Value("${connection-humanizer.user-agent}")
    private String userAgent;
    @Value("${connection-humanizer.referrer}")
    private String referrer;
    @Value("${connection-humanizer.sleep-length.minimum}")
    private int sleepMinimumValue;
    @Value("${connection-humanizer.sleep-length.maximum}")
    private int sleepMaximumValue;

    @Autowired
    public IndexingServiceImpl(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public IndexingResponse startIndexing() {
        if (isRunning.containsValue(true)) {
            throw new IndexingStatusException("Индексация уже запущена");
        }
        onePageIndexes.clear();
        List<Thread> threads = new ArrayList<>();
        for (Site site : sitesList.getSites()) {
            isRunning.put(site.getName(), true);
            SiteScanner siteScanner = new SiteScanner(site);
            Thread thread = new Thread(siteScanner);
            threads.add(thread);
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
        if (!url.matches(URL_REGEX)) {
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
            OnePageIndexer onePageIndexer = new OnePageIndexer(currentSite, url);
            Thread thread = new Thread(onePageIndexer);
            thread.start();
        }
        return new IndexingResponse();
    }

    @SneakyThrows
    public Connection.Response getIndex(String url, SiteEntity siteEntity) {
        Connection.Response response;
        try {
            Thread.sleep((int) (sleepMinimumValue + Math.random() * (sleepMaximumValue - sleepMinimumValue)));
            response = Jsoup.connect(url).userAgent(userAgent).referrer(referrer).ignoreHttpErrors(true).execute();
        } catch (IOException e) {
            throw e;
        }
        PageEntity pageEntity = new PageEntity(siteEntity, UtilityClass.getPath(siteEntity, url), response.statusCode(), response.parse().html());
        if (!savePage(pageEntity)) return null;

        siteEntity.setStatusTime(new Date());
        siteRepository.save(siteEntity);
        if (response.statusCode() >= 400) return null;

        String text = Jsoup.parse(response.parse().html()).text();
        Map<String, Integer> lemmas = LemmaGetter.getLemmas(text);
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            LemmaEntity lemmaEntity = saveLemma(siteEntity, entry.getKey());
            IndexEntity indexEntity = new IndexEntity(pageEntity, lemmaEntity, entry.getValue());
            indexRepository.save(indexEntity);
        }
        return response;
    }

    public boolean savePage(PageEntity pageEntity) {
        if (pageRepository.findByPathAndSiteEntity(pageEntity.getPath(), pageEntity.getSiteEntity()) == null) {
            synchronized (LinkChecker.class) {
                if (pageRepository.findByPathAndSiteEntity(pageEntity.getPath(), pageEntity.getSiteEntity()) == null) {
                    pageRepository.save(pageEntity);
                    return true;
                }
            }
        }
        return false;
    }

    public LemmaEntity saveLemma(SiteEntity siteEntity, String lemma) {
        synchronized (LinkChecker.class) {
            LemmaEntity lemmaEntity = lemmaRepository.findBySiteEntityAndLemma(siteEntity, lemma);
            if (lemmaEntity == null) {
                lemmaEntity = new LemmaEntity(siteEntity, lemma, 1);
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            lemmaRepository.save(lemmaEntity);
            return lemmaEntity;
        }
    }

    private class OnePageIndexer extends Thread {
        private Site currentSite;
        private String url;

        public OnePageIndexer(Site currentSite, String url) {
            this.currentSite = currentSite;
            this.url = url;
        }

        @Override
        public void run() {
            onePageIndexes.add(url);
            String cutURL = UtilityClass.getCutURL(currentSite.getUrl());
            SiteEntity siteEntity = siteRepository.findByUrl(cutURL);
            if (siteEntity == null) {
                siteEntity = new SiteEntity(Status.INDEXED, new Date(), cutURL, currentSite.getName());
                siteRepository.save(siteEntity);
            }
            PageEntity oldPageEntity = pageRepository.findByPathAndSiteEntity(UtilityClass.getPath(siteEntity, url), siteEntity);
            if (oldPageEntity != null) {
                pageRepository.delete(oldPageEntity);
            }
            Connection.Response response;
            try {
                response = getIndex(url, siteEntity);
            } catch (Exception e) {
                throw e;
            }
            if (response == null) {
                throw new PageCantBeIndexedException();
            }
        }
    }


    private class SiteScanner extends Thread {
        private final Site site;

        public SiteScanner(Site site) {
            this.site = site;
        }

        @Override
        public void run() {
            String cutURL = UtilityClass.getCutURL(site.getUrl());
            SiteEntity oldSiteEntity = siteRepository.findByUrl(cutURL);
            if (oldSiteEntity != null) {
                siteRepository.delete(oldSiteEntity);
            }
            SiteEntity newSiteEntity = new SiteEntity(Status.INDEXING, new Date(), cutURL, site.getName());
            siteRepository.save(newSiteEntity);

            LinkChecker linkChecker = new LinkChecker(newSiteEntity.getUrl() + "/", newSiteEntity);
            try {
                new ForkJoinPool().invoke(linkChecker);
                if (isRunning.get(newSiteEntity.getName())) {
                    newSiteEntity.setStatus(Status.INDEXED);
                } else {
                    newSiteEntity.setStatus(Status.FAILED);
                    newSiteEntity.setLastError("Индексация остановлена пользователем");
                }
            } catch (Exception e) {
                newSiteEntity.setStatus(Status.FAILED);
                newSiteEntity.setLastError(e.toString());
            } finally {
                siteRepository.save(newSiteEntity);
                isRunning.put(newSiteEntity.getName(), false);
            }
        }
    }


    private class LinkChecker extends RecursiveAction {
        private String url;
        private SiteEntity siteEntity;

        public LinkChecker(String url, SiteEntity siteEntity) {
            this.url = url;
            this.siteEntity = siteEntity;
        }

        @SneakyThrows
        @Override
        protected void compute() {
            if (!isRunning.get(siteEntity.getName())) return;
            Connection.Response response;
            try {
                response = getIndex(url, siteEntity);
            } catch (Exception e) {
                throw e;
            }
            if (response == null) return;

            List<LinkChecker> taskList = new ArrayList<>();
            Elements links = response.parse().select("a[href]");
            for (int i = 0; i < links.size(); i++) {
                if (!isRunning.get(siteEntity.getName())) break;
                String currentLink = links.get(i).attr("href");
                if (!currentLink.startsWith("/") || currentLink.matches(EXCLUDE_REGEX) ||
                        pageRepository.findByPathAndSiteEntity(currentLink, siteEntity) != null) {
                    continue;
                }
                LinkChecker task = new LinkChecker(siteEntity.getUrl() + currentLink, siteEntity);
                task.fork();
                taskList.add(task);
            }
            taskList.forEach(task -> task.join());
        }
    }
}
