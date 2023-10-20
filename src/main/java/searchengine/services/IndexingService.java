package searchengine.services;

public interface IndexingService {
    boolean indexingAll();
    boolean stopIndexing();
    boolean urlIndexing(String url);
}