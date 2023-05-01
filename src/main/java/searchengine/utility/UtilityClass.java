package searchengine.utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

public class UtilityClass {

    public static String getTitle(PageEntity pageEntity) {
        Document document = Jsoup.parse(pageEntity.getContent());
        return document.title();
    }

    public static String getCutURL(String url) {
        return url.endsWith("/") ? url.substring(0, (url.length() - 1)) : url;
    }

    public static String getURLWithSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    public static String getPath(SiteEntity siteEntity, String url) {
        return url.substring(siteEntity.getUrl().length());
    }

    public static Site getCurrentSite(String url, SitesList sitesList) {
        Site currentSite = null;
        for (Site site : sitesList.getSites()) {
            if (url.contains(site.getUrl().replace("www.", ""))) {
                currentSite = site;
            }
        }
        return currentSite;
    }
}
