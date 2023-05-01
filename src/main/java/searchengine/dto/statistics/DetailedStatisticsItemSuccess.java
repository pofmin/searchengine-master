package searchengine.dto.statistics;

import lombok.Data;

@Data
public class DetailedStatisticsItemSuccess extends DetailedStatisticsItem{
    private String url;
    private String name;
    private String status;
    private long statusTime;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItemSuccess(String url, String name) {
        super(url, name);
        this.url = url;
        this.name = name;
    }
}
