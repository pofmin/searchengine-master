package searchengine.exception_handing.search;

import lombok.Data;

@Data
public class NoSitesInConfig {
    private boolean result = false;
    private String error = "В конфигурационном файле не указан ни один сайт";
}
