package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexData;
import searchengine.dto.statistics.LemmaData;
import searchengine.dto.statistics.PageData;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.IndexingServiceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Slf4j
public class SiteIndexing implements Runnable{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final int coreCount = Runtime.getRuntime().availableProcessors();
    private final String url;
    private final GetLemmas getLemmas;
    private final LemmasIndexing lemmasIndexing;

    @Override
    public void run() {
        if (siteRepository.findByUrl(url) != null) {
            log.info("Удаление данный сайта - " + url);
            deletingDataSite();
        }
        saveDataSite();
        try {
            List<PageData> pageData = getPages();
            savePagesToBase(pageData);
            getLemmasFromPages();
            indexingLemmas();
            log.info("Индексация завершена");
        } catch (InterruptedException ex) {
            log.error("Индексайия сайта - " + getName() + " остановлена");
            getError();
        }

    }
    private List<PageData> getPages() throws InterruptedException{
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted){
            List<String> urlList = new ArrayList<>();
            List<PageData> pageDataList = new ArrayList<>();
            ForkJoinPool forkJoinPool = new ForkJoinPool(coreCount);
            String urlV2 = getUrlV2(url);
            List<PageData> pages = forkJoinPool.invoke(new LinksSearch(url, urlV2, url, urlList,
                    pageDataList, siteRepository));
            return new CopyOnWriteArrayList<>(pages);
        }else {
            throw new InterruptedException();
        }
    }
    private void getLemmasFromPages() throws InterruptedException {
        if(!Thread.interrupted() && !IndexingServiceImpl.isInterapted){
            Website website = siteRepository.findByUrl(url);
            getLemmas.start(website);
            List<LemmaData> lemmaDataList = getLemmas.getLemmaDataList();
            List<Lemma> lemmaList = new CopyOnWriteArrayList<>();
            for (LemmaData lemmaData : lemmaDataList){
                lemmaList.add(new Lemma(website, lemmaData.getLemma(), lemmaData.getFrequency()));
            }
            lemmaRepository.flush();
            lemmaRepository.saveAll(lemmaList);
            website.setStatusTime(new Date());
            siteRepository.flush();
            siteRepository.save(website);
        } else {
            throw new InterruptedException();
        }
    }
    private void indexingLemmas() throws InterruptedException {
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted){
            Website website = siteRepository.findByUrl(url);
            lemmasIndexing.start(website);
            List<IndexData> indexDataList = lemmasIndexing.getIndexData();
            List<Index> indexList = new CopyOnWriteArrayList<>();
            website.setStatusTime(new Date());
            siteRepository.flush();
            siteRepository.save(website);
            for (IndexData indexData : indexDataList){
                Page pageId = pageRepository.getById(indexData.getPageId());
                Lemma lemmaId = lemmaRepository.getById(indexData.getLemmaId());
                indexList.add(new Index(pageId, lemmaId, indexData.getRank()));
            }
            indexRepository.flush();
            indexRepository.saveAll(indexList);
            website.setStatusTime(new Date());
            website.setStatus(StatusType.INDEXED);
            siteRepository.flush();
            siteRepository.save(website);
        } else {
            throw new InterruptedException();
        }
    }
    private void savePagesToBase(List<PageData> pages) throws InterruptedException{
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted){
            List<Page> pageList = new CopyOnWriteArrayList<>();
            Website website = siteRepository.findByUrl(url);
            for (PageData page : pages){
                int first = page.getUrl().indexOf(url) + url.length();
                String format = page.getUrl().substring(first);
                pageList.add(new Page(website, "/" + format, page.getCode(), page.getContent()));
            }
            pageRepository.flush();
            pageRepository.saveAll(pageList);
            website.setStatusTime(new Date());
            siteRepository.flush();
            siteRepository.save(website);
        } else {
            throw new InterruptedException();
        }
    }
    private void deletingDataSite(){
        Website website = siteRepository.findByUrl(url);
        website.setStatus(StatusType.INDEXING);
        website.setStatusTime(new Date());
        website.setName(getName());
        siteRepository.save(website);
        siteRepository.flush();
        siteRepository.delete(website);
    }
    private void saveDataSite(){
        Website website = new Website();
        website.setStatus(StatusType.INDEXING);
        website.setStatusTime(new Date());
        website.setUrl(url);
        website.setName(getName());
        siteRepository.flush();
        siteRepository.save(website);
    }
    private String getName(){
        List<Site> sites = sitesList.getSites();
        for (Site site : sites){
            if (site.getUrl().equals(url)){
                return site.getName();
            }
        }
        return "";
    }
    private void getError(){
        Website website = siteRepository.findByUrl(url);
        website.setStatus(StatusType.FAILED);
        website.setStatusTime(new Date());
        website.setLastError("Индексация остановлена пользователем");
        siteRepository.flush();
        siteRepository.save(website);
    }

    private String getUrlV2(String url){
        String newLink;
        if (url.contains("www")){
            newLink = url.replace("www.", "");
        } else {
            StringBuilder link = new StringBuilder(url);
            if (url.contains("https://")){
                newLink = link.insert(8, "www.").toString();
            } else {
                newLink = link.insert(7, "www.").toString();
            }
        }
        return newLink;
    }
}
