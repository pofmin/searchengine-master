package searchengine.exception_handing.indexing;

import lombok.Data;

@Data
public class PageCantBeIndexed {
    private boolean result = false;
    private String error = "Данная страница не существует или недоступна";
}
