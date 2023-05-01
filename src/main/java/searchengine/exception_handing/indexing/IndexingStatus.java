package searchengine.exception_handing.indexing;

import lombok.Data;

@Data
public class IndexingStatus {
    private boolean result = false;
    private String error;

    public IndexingStatus(String error) {
        this.error = error;
    }
}
