package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import searchengine.model.StatusType;
import searchengine.model.Website;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = getTotal();
        List<DetailedStatisticsItem> list = getDetailedStatistics();
        return new StatisticsResponse(true, new StatisticsData(total, list));
    }

    private TotalStatistics getTotal(){
        long sitesCount = siteRepository.count();
        long pagesCount = pageRepository.count();
        long lemmasCount = lemmaRepository.count();
        return new TotalStatistics(sitesCount, pagesCount, lemmasCount, true);
    }

    private List<DetailedStatisticsItem> getDetailedStatistics(){
        List<Website> websiteList = siteRepository.findAll();
        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
        for (Website website : websiteList){
            DetailedStatisticsItem item = getDetailed(website);
            detailedStatisticsItems.add(item);
        }
        return detailedStatisticsItems;
    }

    private DetailedStatisticsItem getDetailed(Website website){
        StatusType status = website.getStatus();
        Date statusTime = website.getStatusTime();
        String error = website.getLastError();
        String url = website.getUrl();
        String name = website.getName();
        long pagesCount = pageRepository.countBySiteId(website);
        long lemmasCount = lemmaRepository.countBySiteId(website);
        return  new DetailedStatisticsItem(url, name, status, statusTime, error, pagesCount, lemmasCount);
    }
}
