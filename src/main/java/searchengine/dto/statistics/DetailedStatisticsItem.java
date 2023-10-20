package searchengine.dto.statistics;

import lombok.Value;
import searchengine.model.StatusType;

import java.util.Date;

@Value
public class DetailedStatisticsItem {
    String url;
    String name;
    StatusType status;
    Date statusTime;
    String error;
    long pages;
    long lemmas;
}
