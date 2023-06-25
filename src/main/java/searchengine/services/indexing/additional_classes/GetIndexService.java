package searchengine.services.indexing.additional_classes;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utility.LemmaGetter;
import searchengine.utility.UtilityClass;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetIndexService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Value("${connection-humanizer.user-agent}")
    private String userAgent;
    @Value("${connection-humanizer.referrer}")
    private String referrer;
    @Value("${connection-humanizer.sleep-length.minimum}")
    private int sleepMinimumValue;
    @Value("${connection-humanizer.sleep-length.maximum}")
    private int sleepMaximumValue;

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
}
