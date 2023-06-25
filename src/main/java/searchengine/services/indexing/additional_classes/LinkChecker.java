package searchengine.services.indexing.additional_classes;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Connection;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.services.indexing.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class LinkChecker extends RecursiveAction {

    private final String url;
    private final SiteEntity siteEntity;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private GetIndexService getIndexService;

    @SneakyThrows
    @Override
    protected void compute() {
        if (!IndexingServiceImpl.isRunning.get(siteEntity.getName())) return;
        Connection.Response response;
        try {
            response = getIndexService.getIndex(url, siteEntity);
        } catch (Exception e) {
            throw e;
        }
        if (response == null) return;

        List<LinkChecker> taskList = new ArrayList<>();
        Elements links = response.parse().select("a[href]");
        for (int i = 0; i < links.size(); i++) {
            if (!IndexingServiceImpl.isRunning.get(siteEntity.getName())) break;
            String currentLink = links.get(i).attr("href");
            if (!currentLink.startsWith("/") || currentLink.matches(IndexingServiceImpl.EXCLUDE_REGEX) ||
                    pageRepository.findByPathAndSiteEntity(currentLink, siteEntity) != null) {
                continue;
            }
            LinkChecker task = applicationContext.getBean(LinkChecker.class, siteEntity.getUrl() + currentLink, siteEntity);
            task.fork();
            taskList.add(task);
        }
        taskList.forEach(task -> task.join());
    }
}
