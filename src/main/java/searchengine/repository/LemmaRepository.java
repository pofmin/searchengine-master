package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import java.util.Collection;
import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    List<LemmaEntity> findAllBySiteEntity(SiteEntity siteEntity);

    LemmaEntity findBySiteEntityAndLemma(SiteEntity siteEntity, String lemma);

    List<LemmaEntity> findBySiteEntityInAndLemmaIn(Collection<SiteEntity> siteEntities, Collection<String> lemmas);

    List<LemmaEntity> findBySiteEntityAndLemmaIn(SiteEntity siteEntity, Collection<String> lemmas);
}
