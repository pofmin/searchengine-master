package searchengine.exception_handing.indexing;

import lombok.Data;

@Data
public class IndexingInProgress {
    private boolean result = false;
    private String error = "Индексация в процессе, повторите попытку, когда она будет завершена";
}
