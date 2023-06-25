package searchengine.services.search.additional_classes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexRepository;
import searchengine.services.search.SearchServiceImpl;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SetRelevanceService {

    private final IndexRepository indexRepository;

    public void setRelevance(List<LemmaEntity> allLemmaEntities) {
        setAbsoluteRelevance(allLemmaEntities);
        for (PageEntity pageEntity : SearchServiceImpl.allPages) {
            pageEntity.setRelevance(pageEntity.getRelevanceAbsolute() / getBiggestAbsoluteRelevance());
        }
    }

    public void setAbsoluteRelevance(List<LemmaEntity> allLemmaEntities) {
        for (PageEntity pageEntity : SearchServiceImpl.allPages) {
            List<IndexEntity> indexEntities = indexRepository.findByPageEntityAndLemmaEntityIn(pageEntity, allLemmaEntities);
            float absoluteRelevance = 0;
            for (IndexEntity indexEntity : indexEntities) {
                absoluteRelevance += indexEntity.getRanking();
            }
            pageEntity.setRelevanceAbsolute(absoluteRelevance);
        }
    }

    public float getBiggestAbsoluteRelevance() {
        float biggestRelevanceAbsolute = 0F;
        for (PageEntity pageEntity : SearchServiceImpl.allPages) {
            if (pageEntity.getRelevanceAbsolute() > biggestRelevanceAbsolute) {
                biggestRelevanceAbsolute = pageEntity.getRelevanceAbsolute();
            }
        }
        return biggestRelevanceAbsolute;
    }
}
