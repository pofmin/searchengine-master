package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

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
            return new DetailedStatisticsItem(
                    siteEntity.getUrl(),
                    siteEntity.getName(),
                    ""
            );
        } else {
            return new DetailedStatisticsItem(
                    siteEntity.getUrl(),
                    siteEntity.getName(),
                    siteEntity.getLastError()
            );
        }
    }
}
