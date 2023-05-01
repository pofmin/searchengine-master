package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class SearchResponse {
    private boolean result = true;
    private int count;
    private List<SearchData> data;

    public SearchResponse(int count, List<SearchData> data) {
        this.count = count;
        this.data = data;
    }
}
