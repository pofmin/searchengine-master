package searchengine.exception_handing.search;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SiteNotIndexedException extends RuntimeException{
    public SiteNotIndexedException(String message) {
        super(message);
    }
}
