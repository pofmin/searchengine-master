package searchengine.exception_handing.search;

import lombok.Data;

@Data
public class SiteNotIndexed {
    private boolean result = false;
    private String error;

    public SiteNotIndexed(String error) {
        this.error = error;
    }
}
