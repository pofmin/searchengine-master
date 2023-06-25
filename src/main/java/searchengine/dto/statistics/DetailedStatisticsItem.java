package searchengine.dto.statistics;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DetailedStatisticsItem {
    private final String url;
    private final String name;
    private final String error;

    private String status;
    private long statusTime;
    private int pages;
    private int lemmas;
}


