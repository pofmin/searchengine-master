package searchengine.exception_handing.indexing;

import lombok.Data;

@Data
public class PageOutsideConfig {
    private boolean result = false;
    private String error = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";
}
