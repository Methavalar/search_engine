package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Website;
import searchengine.model.StatusType;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.utils.GetLemmas;
import searchengine.utils.LemmasIndexing;
import searchengine.utils.SiteIndexing;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final int coreCount = Runtime.getRuntime().availableProcessors();
    private ExecutorService executorService;
    private final GetLemmas getLemmas;
    private final LemmasIndexing lemmasIndexing;
    public static boolean isInterapted = false;

    @Override
    public boolean indexingAll(){
        if (isIndexingProcessing()){
            log.debug("Индексация уже запущена");
        } else{
            executorService = Executors.newFixedThreadPool(coreCount);
            List<Site> siteList = sitesList.getSites();
            for (Site site : siteList){
                String url = site.getUrl();
                Website website = new Website();
                website.setName(site.getName());
                log.info("Индексация сайта: " + site.getName());
                executorService.submit(new SiteIndexing(siteRepository, pageRepository, lemmaRepository, indexRepository, sitesList, url, getLemmas, lemmasIndexing));
            }
            executorService.shutdown();
        }
        return true;
    }
    @Override
    public boolean stopIndexing(){
        if (isIndexingProcessing()){
            log.info("Индексация остановлена");
            executorService.shutdownNow();
            isInterapted = true;
            return true;
        } else{
            log.info("Индексация не запущена");
            return false;
        }
    }
    @Override
    public boolean urlIndexing(String url){
        if (isUrlInData(url)){
            log.info("Индексация сайта: " + url);
            executorService = Executors.newFixedThreadPool(coreCount);
            executorService.submit(new SiteIndexing(siteRepository, pageRepository, lemmaRepository, indexRepository, sitesList, url, getLemmas, lemmasIndexing));
            executorService.shutdown();
            return true;
        } else {
            return false;
        }
    }

    private boolean isUrlInData(String url){
        List<Site> siteList = sitesList.getSites();
        for (Site site : siteList){
            if (site.getUrl().equals(url)){
                return true;
            }
        }
        return false;
    }

    private boolean isIndexingProcessing(){
        siteRepository.flush();
        Iterable<Website> siteList = siteRepository.findAll();
        for (Website site : siteList){
            if (site.getStatus() == StatusType.INDEXING){
                return true;
            }
        }
        return false;
    }
}
