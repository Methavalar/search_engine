package searchengine.dto.statistics;

import lombok.Value;

@Value
public class IndexData {
    int pageId;
    int lemmaId;
    float rank;
}
