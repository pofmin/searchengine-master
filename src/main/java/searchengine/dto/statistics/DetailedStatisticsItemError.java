package searchengine.dto.statistics;

import lombok.Data;

@Data
public class DetailedStatisticsItemError extends DetailedStatisticsItem {
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private String error;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItemError(String url, String name) {
        super(url, name);
        this.url = url;
        this.name = name;
    }
}
