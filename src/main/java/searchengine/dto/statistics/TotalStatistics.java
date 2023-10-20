package searchengine.dto.statistics;

import lombok.Value;

@Value
public class TotalStatistics {
    long sites;
    long pages;
    long lemmas;
    boolean indexing;
}
