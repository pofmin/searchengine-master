package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private SitesList sites;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;

    @Autowired
    public StatisticsServiceImpl(SitesList sites, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        this.sites = sites;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public StatisticsResponse getStatistics() {
        List<SiteEntity> sitesList = siteRepository.findAll();
        TotalStatistics total = new TotalStatistics();
        total.setSites(sitesList.size());
        total.setIndexing(IndexingServiceImpl.isRunning.containsValue(true));
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        if (sitesList.size() == 0) {
            return new StatisticsResponse(true, new StatisticsData(total, detailed));
        }

        for(int i = 0; i < sitesList.size(); i++) {
            SiteEntity siteEntity = sitesList.get(i);
            int pages = pageRepository.findAllBySiteEntity(siteEntity).size();
            int lemmas = lemmaRepository.findAllBySiteEntity(siteEntity).size();

            DetailedStatisticsItem item = getItem(siteEntity);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteEntity.getStatus().toString());
            item.setStatusTime(siteEntity.getStatusTime().getTime());

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        StatisticsData data = new StatisticsData(total, detailed);
        return new StatisticsResponse(true, data);
    }

    public DetailedStatisticsItem getItem(SiteEntity siteEntity) {
        if (siteEntity.getLastError() == null) {
            DetailedStatisticsItemSuccess item = new DetailedStatisticsItemSuccess(siteEntity.getUrl(), siteEntity.getName());
            return item;
        } else {
            DetailedStatisticsItemError item = new DetailedStatisticsItemError(siteEntity.getUrl(), siteEntity.getName());
            item.setError(siteEntity.getLastError());
            return item;
        }
    }
}
