package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import java.util.Collection;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    List<IndexEntity> findByLemmaEntity(LemmaEntity lemmaEntity);

    List<IndexEntity> findByPageEntityAndLemmaEntityIn(PageEntity pageEntity, Collection<LemmaEntity> lemmaEntities);
}
