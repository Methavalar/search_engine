package searchengine.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Website;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.IndexData;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.services.impl.IndexingServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class LemmasIndexing {
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final Lemmatisator lemmatisator;
    private List<IndexData> indexDataList;

    public List<IndexData> getIndexData(){
        return indexDataList;
    }

    public void start(Website website) throws InterruptedException {
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted) {
            float rank;
            Iterable<Page> pages = pageRepository.findBySiteId(website);
            List<Lemma> lemmaList = lemmaRepository.findBySiteId(website);
            indexDataList = new CopyOnWriteArrayList<>();
            for (Page page : pages) {
                var firstLetter = String.valueOf(page.getCode()).charAt(0);
                if (firstLetter == '4' || firstLetter == '5') {
                    continue;
                }
                int pageId = page.getId();
                String content = page.getContent();
                String text = lemmatisator.cleanFromHtmlTags(content);
                HashMap<String, Integer> wordsList = lemmatisator.getLemmaList(text);
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
        } else {
            throw new InterruptedException();
        }
    }
}
