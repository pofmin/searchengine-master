package searchengine.services.indexing.additional_classes;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.utility.UtilityClass;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SiteScanner extends Thread {

    private final Site site;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private SiteRepository siteRepository;

    @Override
    public void run() {
        String cutURL = UtilityClass.getCutURL(site.getUrl());
        SiteEntity oldSiteEntity = siteRepository.findByUrl(cutURL);
        if (oldSiteEntity != null) {
            siteRepository.delete(oldSiteEntity);
        }
        SiteEntity newSiteEntity = new SiteEntity(Status.INDEXING, new Date(), cutURL, site.getName());
        siteRepository.save(newSiteEntity);

        LinkChecker linkChecker = applicationContext.getBean(LinkChecker.class, newSiteEntity.getUrl() + "/", newSiteEntity);
        try {
            new ForkJoinPool().invoke(linkChecker);
            if (IndexingServiceImpl.isRunning.get(newSiteEntity.getName())) {
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
            IndexingServiceImpl.isRunning.put(newSiteEntity.getName(), false);
        }
    }
}

