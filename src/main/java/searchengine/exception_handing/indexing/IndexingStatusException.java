package searchengine.exception_handing.indexing;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IndexingStatusException extends RuntimeException {
    public IndexingStatusException(String message) {
        super(message);
    }
}
