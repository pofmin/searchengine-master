package searchengine.exception_handing.search;

import lombok.Data;

@Data
public class SiteOutsideConfig {
    private boolean result = false;
    private String error = "Данный сайт находится за пределами сайтов, указанных в конфигурационном файле";
}
