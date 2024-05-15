package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.UserAgent;
import searchengine.dto.statistics.IndexData;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@Slf4j
public class PageIndexing implements Runnable{
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final String url;
    private final Site site;
    private final Lemmatisator lemmatisator;

    @Override
    public void run() {
        String path = getPath();
        if (pageRepository.findByPath(path) != null) {
            log.info("Удаление данных страницы - " + url);
            deletingDataPage(path);
        }
        getPageInfo(path);
        getLemmasFromPage(path);
        indexingLemmas(path);
        log.info("Индексация завершена");

    }
    private void getPageInfo(String path){
        try {
            Thread.sleep(100);
            Document document;
            String html;
            try {
                document = getConnectWithUserAgent(url);
                html = document.outerHtml();
            } catch (Exception e) {
                document = getConnect(url);
                html = document.outerHtml();
            }
            Connection.Response response = document.connection().response();
            int statusCode = response.statusCode();
            Website website = siteRepository.findByUrl(site.getUrl());
            Page page = new Page(website, path, statusCode, html);
            pageRepository.flush();
            pageRepository.save(page);
        } catch (Exception ex){
            log.debug("Ошибка парсинга: " + url);
            Website website = siteRepository.findByUrl(site.getUrl());
            Page page = new Page(website, path, 500, "");
            pageRepository.flush();
            pageRepository.save(page);
        }
    }
    private void getLemmasFromPage(String path){
        int frequency;
        List<Lemma> lemmaDataList = new CopyOnWriteArrayList<>();
        Page page = pageRepository.findByPath(path);
        Website website = siteRepository.findByUrl(site.getUrl());
        HashMap<String, Integer> lemmaList = new HashMap<>();
        var firstLetter = String.valueOf(page.getCode()).charAt(0);
        if (firstLetter != '4' && firstLetter != '5') {
            String content = page.getContent();
            String text = lemmatisator.cleanFromHtmlTags(content);
            HashMap<String, Integer> wordsList = lemmatisator.getLemmaList(text);
            for (String word : wordsList.keySet()) {
                frequency = lemmaList.getOrDefault(word, 0) + 1;
                lemmaList.put(word, frequency);
            }
        }
        for (String lemma : lemmaList.keySet()) {
            lemmaDataList.add(new Lemma(website, lemma, lemmaList.get(lemma)));
        }
        lemmaRepository.flush();
        lemmaRepository.saveAll(lemmaDataList);
    }
    private void indexingLemmas(String path){
        Page page = pageRepository.findByPath(path);
        Website website = siteRepository.findByUrl(site.getUrl());
        List<Lemma> lemmaList = lemmaRepository.findBySiteId(website);
        List<IndexData> indexDataList = new CopyOnWriteArrayList<>();
        List<Index> indexList = new CopyOnWriteArrayList<>();
        var firstLetter = String.valueOf(page.getCode()).charAt(0);
        if (firstLetter != '4' && firstLetter != '5') {
            int pageId = page.getId();
            String content = page.getContent();
            String text = lemmatisator.cleanFromHtmlTags(content);
            HashMap<String, Integer> wordsList = lemmatisator.getLemmaList(text);
            setIndex(indexDataList, lemmaList, wordsList, pageId);
        }
        for (IndexData indexData : indexDataList){
            Lemma lemmaId = lemmaRepository.getById(indexData.getLemmaId());
            indexList.add(new Index(page, lemmaId, indexData.getRank()));
        }
        indexRepository.flush();
        indexRepository.saveAll(indexList);
    }
    private void setIndex(List<IndexData> indexDataList, List<Lemma> lemmaList, HashMap<String, Integer> wordsList,
                          int pageId){
        float rank;
        for (Lemma lemma : lemmaList) {
            int lemmaId = lemma.getId();
            String lemmaInData = lemma.getLemma();
            if (wordsList.containsKey(lemmaInData)) {
                rank = Float.valueOf(wordsList.get(lemmaInData));
                indexDataList.add(new IndexData(pageId, lemmaId, rank));
            } else {
                log.debug("Лемма не найдена");
            }
        }
    }
    private void deletingDataPage(String path){
        Page page = pageRepository.findByPath(path);
        pageRepository.delete(page);
        pageRepository.flush();
    }
    private String getPath(){
        String path = url.replace(site.getUrl(), "");
        return "/" + path;
    }
    private Document getConnectWithUserAgent(String url){
        Document document = null;
        try{
            Thread.sleep(100);
            document = Jsoup.connect(url).userAgent(UserAgent.getUserAgent()).referrer("http://www.google.com").get();
        }catch (Exception ex){
            log.debug("Не удается подключится к сайту " + url);
        }
        return document;
    }
    private Document getConnect(String url){
        Document document = null;
        try{
            document = Jsoup.connect(url).get();
        } catch (Exception e) {
            log.debug("Не удается подключится к сайту " + url);
        }
        return document;
    }
}
