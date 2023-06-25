package searchengine.services.indexing.additional_classes;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.config.Site;
import searchengine.exception_handing.indexing.PageCantBeIndexedException;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.indexing.IndexingServiceImpl;
import searchengine.utility.UtilityClass;
import java.util.Date;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class OnePageIndexer extends Thread {

    private final Site currentSite;
    private final String url;

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private GetIndexService getIndexService;

    @Override
    public void run() {
        IndexingServiceImpl.onePageIndexes.add(url);
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
            response = getIndexService.getIndex(url, siteEntity);
        } catch (Exception e) {
            throw e;
        }
        if (response == null) {
            throw new PageCantBeIndexedException();
        }
    }
}
