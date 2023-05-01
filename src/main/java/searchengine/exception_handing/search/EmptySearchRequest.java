package searchengine.exception_handing.search;

import lombok.Data;

@Data
public class EmptySearchRequest {
    private boolean result = false;
    private String error = "Задан пустой поисковый запрос";
}
