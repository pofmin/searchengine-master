package searchengine.services.search.additional_classes;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import searchengine.dto.search.SearchData;
import searchengine.model.PageEntity;
import searchengine.services.search.SearchServiceImpl;
import searchengine.utility.SnippetGetter;
import searchengine.utility.UtilityClass;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class SearchDataGetter extends Thread {

    private final PageEntity pageEntity;
    private final List<String> queryLemmas;

    @Value("${indexing-settings.snippet-length-max}")
    private int snippetLengthMax;

    @SneakyThrows
    @Override
    public void run() {
        SearchData searchData = new SearchData();
        SnippetGetter snippetGetter = new SnippetGetter(pageEntity, queryLemmas, snippetLengthMax);
        searchData.setSnippet(snippetGetter.getSnippet());
        searchData.setSite(pageEntity.getSiteEntity().getUrl());
        searchData.setSiteName(pageEntity.getSiteEntity().getName());
        searchData.setUri(pageEntity.getPath());
        searchData.setTitle(UtilityClass.getTitle(pageEntity));
        searchData.setRelevance(pageEntity.getRelevance());
        SearchServiceImpl.searchDataList.add(searchData);
    }
}
