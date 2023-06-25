package searchengine.services.search.additional_classes;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.search.SearchServiceImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetPagesService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private List<LemmaEntity> lemmaEntities = new ArrayList<>();

    @Value("${indexing-settings.frequency-threshold}")
    private float frequencyThreshold;

    public void getAllPages(List<SiteEntity> sitesToCheck) {
        SearchServiceImpl.allPages.clear();
        for (SiteEntity siteEntity : sitesToCheck) {
            int allPagesCount = pageRepository.findAllBySiteEntity(siteEntity).size();
            lemmaEntities = lemmaRepository.findBySiteEntityAndLemmaIn(siteEntity, SearchServiceImpl.queryLemmas);
            if (lemmaEntities.size() < SearchServiceImpl.queryLemmas.size()) continue;

            lemmaEntities.removeIf(lemmaEntity -> ((float) lemmaEntity.getFrequency() / (float) allPagesCount) > frequencyThreshold);
            Collections.sort(lemmaEntities, Comparator.comparing(LemmaEntity::getFrequency));
            if (lemmaEntities.size() == 0) continue;

            List<PageEntity> pages = getPagesByLemma(0);
            List<PageEntity> pagesFinal = lemmaEntities.size() == 1 ? pages : compareListsOfPages(pages, 1);
            if (pagesFinal != null) {
                SearchServiceImpl.allPages.addAll(pagesFinal);
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
}
