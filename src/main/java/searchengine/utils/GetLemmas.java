package searchengine.utils;

import lombok.RequiredArgsConstructor;
import searchengine.model.Page;
import searchengine.model.Website;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.LemmaData;
import searchengine.repository.PageRepository;
import searchengine.services.impl.IndexingServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class GetLemmas {
    private final PageRepository pageRepository;
    private final Lemmatisator lemmatisator;
    private List<LemmaData> lemmaDataList;

    public List<LemmaData> getLemmaDataList(){
        return lemmaDataList;
    }

    public void start(Website website) throws InterruptedException {
        if (!Thread.interrupted() && !IndexingServiceImpl.isInterapted) {
            int frequency;
            lemmaDataList = new CopyOnWriteArrayList<>();
            Iterable<Page> pages = pageRepository.findBySiteId(website);
            HashMap<String, Integer> lemmaList = new HashMap<>();
            for (Page page : pages) {
                var firstLetter = String.valueOf(page.getCode()).charAt(0);
                if (firstLetter == '4' || firstLetter == '5') {
                    continue;
                }
                String content = page.getContent();
                String text = lemmatisator.cleanFromHtmlTags(content);
                HashMap<String, Integer> wordsList = lemmatisator.getLemmaList(text);
                for (String word : wordsList.keySet()) {
                    frequency = lemmaList.getOrDefault(word, 0) + 1;
                    lemmaList.put(word, frequency);
                }
            }
            for (String lemma : lemmaList.keySet()) {
                lemmaDataList.add(new LemmaData(lemma, lemmaList.get(lemma)));
            }
        } else {
            throw new InterruptedException();
        }
    }
}
