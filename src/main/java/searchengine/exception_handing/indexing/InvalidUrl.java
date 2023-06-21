package searchengine.exception_handing.indexing;

import lombok.Data;

@Data
public class InvalidUrl {
    private boolean result = false;
    private String error = "Неверный формат введенного URL";
}
