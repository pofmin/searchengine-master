package searchengine.dto.statistics;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class DetailedStatisticsItem {
    String url;
    String name;
    private String status;
    private long statusTime;
    private int pages;
    private int lemmas;

    public DetailedStatisticsItem(String url, String name) {
        this.url = url;
        this.name = name;
    }
}
