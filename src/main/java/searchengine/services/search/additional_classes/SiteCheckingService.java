package searchengine.services.search.additional_classes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.utility.UtilityClass;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteCheckingService {
    private final SitesList sitesList;
    private final SiteRepository siteRepository;

    public List<SiteEntity> getSitesToCheck(String siteUrl) {
        List<SiteEntity> sites = new ArrayList<>();
        if (siteUrl.equals("")) {
            for (Site site : sitesList.getSites()) {
                SiteEntity siteEntity = siteRepository.findByUrl(UtilityClass.getCutURL(site.getUrl()));
                sites.add(siteEntity);
            }
            return sites;
        }
        boolean siteIsPresent = false;
        for (Site site : sitesList.getSites()) {
            if (site.getUrl().contains(siteUrl)) {
                siteIsPresent = true;
                SiteEntity siteEntity = siteRepository.findByUrl(UtilityClass.getCutURL(siteUrl));
                sites.add(siteEntity);
            }
        }
        if (!siteIsPresent) {
            return null;
        }
        return sites;
    }

    public boolean checkSitesStatuses(List<SiteEntity> sitesToCheck) {
        boolean isValid = false;
        for (SiteEntity siteEntity : sitesToCheck) {
            if (siteEntity.getStatus().toString().equals("INDEXED")) {
                isValid = true;
            }
        }
        return isValid;
    }
}
