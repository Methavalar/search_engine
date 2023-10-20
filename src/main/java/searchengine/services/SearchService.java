package searchengine.services;

import searchengine.dto.statistics.SearchData;

import java.util.List;

public interface SearchService {
    List<SearchData> searchAcrossAllSites(String text, int offset, int limit);
    List<SearchData> searchAcrossSite(String text, String url, int offset, int limit);
    int getCount();
}
