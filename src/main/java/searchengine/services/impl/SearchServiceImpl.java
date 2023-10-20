package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.utils.GetSnippet;
import searchengine.dto.statistics.SearchData;
import searchengine.utils.Lemmatisator;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Lemmatisator lemmatisator;
    private final GetSnippet snippet;
    private int count = 0;

    @Override
    public List<SearchData> searchAcrossAllSites(String text, int offset, int limit) {
        log.info("Поиск по всем сайтам");
        List<Website> siteList = siteRepository.findAll();
        List<SearchData> searchDataList = new ArrayList<>();
        List<Lemma> lemmaList = new ArrayList<>();
        List<String> lemmasFromText = getLemmaFromSearch(text);
        for (Website site : siteList){
            lemmaList.addAll(getLemmasFromSite(lemmasFromText, site));
        }
        for (Lemma lem : lemmaList){
            if (lem.getLemma().equals(text)){
                searchDataList = new ArrayList<>(getSearchDataToList(lemmaList, lemmasFromText, offset, limit));
                searchDataList.sort((a1, a2) -> Float.compare(a2.getRelevance(), a1.getRelevance()));
            }
        }
        log.info("Поиск выполнен");
        return searchDataList;
    }

    @Override
    public List<SearchData> searchAcrossSite(String text, String url, int offset, int limit){
        Website site = siteRepository.findByUrl(url);
        log.info("Поиск по сайту - " + site.getName());
        List<String> lemmasFromText = getLemmaFromSearch(text);
        List<Lemma> lemmaList = getLemmasFromSite(lemmasFromText, site);
        log.info("Поиск выполнен");
        return getSearchDataToList(lemmaList, lemmasFromText, offset, limit);
    }

    private List<String> getLemmaFromSearch(String text){
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^а-яa-z\\s]", " ").trim().split("\\s+");
        List<String> lemmaList = new ArrayList<>();
        for (String word : words){
            List<String> lemmas = lemmatisator.getLemma(word);
            lemmaList.addAll(lemmas);
        }
        return lemmaList;
    }

    private List<Lemma> getLemmasFromSite(List<String> lemmas, Website site){
        lemmaRepository.flush();
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySite(lemmas, site);
        lemmaList.sort(Comparator.comparingInt(Lemma::getFrequency));
        return lemmaList;
    }

    private List<SearchData> getSearchDataToList(List<Lemma> lemmaList, List<String> lemmasFromText, int offset, int limit){
        List<SearchData> searchDataList = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= lemmasFromText.size()){
            List<Page> pageList = pageRepository.findByLemmas(lemmaList);
            indexRepository.flush();
            List<Index> indexList = indexRepository.findByPagesAndLemmas(lemmaList, pageList);
            HashMap<Page, Float> sortedPagesByAbsRelevance = getPagesByAbsRelevance(pageList, indexList);
            List<SearchData> dataList = getSearchData(sortedPagesByAbsRelevance, lemmasFromText);
            if (offset > dataList.size()){
                return new ArrayList<>();
            }
            count = dataList.size();
            if (dataList.size() > limit){
                for (int i = 0; i < limit; i++){
                    searchDataList.add(dataList.get(i));
                }
                return searchDataList;
            } else return dataList;
        }
        return searchDataList;
    }

    private HashMap<Page, Float> getPagesByAbsRelevance(List<Page> pageList, List<Index> indexList){
        HashMap<Page, Float> relevancePages = new HashMap<>();
        for (Page page : pageList){
            float rankSum = 0.0F;
            for (Index index : indexList){
                if (index.getPageId() == page){
                    rankSum += index.getRank();
                }
            }
            relevancePages.put(page, rankSum);
        }
        HashMap<Page, Float> absRelevancePages = new HashMap<>();
        for (Page page : relevancePages.keySet()){
            float absRelevance = relevancePages.get(page) / Collections.max(relevancePages.values());
            absRelevancePages.put(page, absRelevance);
        }
        return absRelevancePages;
    }

    private List<SearchData> getSearchData(HashMap<Page, Float> pageList, List<String> lemmasFromText){
        List<SearchData> searchDataList = new ArrayList<>();
        for (Page page : pageList.keySet()){
            String content = page.getContent();
            Website website = page.getSiteId();
            String site = website.getUrl();
            String siteName = website.getName();
            String uri = page.getPath();
            var document = Jsoup.parse(content);
            String title = document.title();
            float absRelevance = pageList.get(page);
            String text = lemmatisator.cleanFromHtmlTags(content);
            String snippet = this.snippet.getSnippet(text, lemmasFromText);
            searchDataList.add(new SearchData(site, siteName, uri, title, snippet, absRelevance));
        }
        return searchDataList;
    }

    @Override
    public int getCount() {
        return count;
    }
}
