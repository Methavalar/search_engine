package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GetSnippet {
    private final Lemmatisator lemmatisator;

    public String getSnippet(String text, List<String> lemmasFromText){
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder snippet = new StringBuilder();
        for (String lemma : lemmasFromText){
            lemmaIndex.addAll(lemmatisator.findIndexOfLemmaInText(text, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> words = getWordsFromText(text, lemmaIndex);
        int j = 0;
        for (String word : words) {
            if (snippet.toString().equals(word)) {
                continue;
            }
            snippet.append(word).append("... ");
            j++;
            if (j > 2) {
                break;
            }
        }
        return snippet.toString();
    }
    private List<String> getWordsFromText(String text, List<Integer> lemmaIndex){
        List<String> result = new ArrayList<>();
        List<String> stringList = new ArrayList<>();
        List<String> words = new ArrayList<>();
        for (int begin : lemmaIndex) {
            int end = text.indexOf(" ", begin);
            words.add(text.substring(begin, end));
            if (text.indexOf(" ", end + 30) == -1) {
                end = text.length() - 1;
            } else end = text.indexOf(" ", end + 30);
            stringList.add(text.substring(begin, end));
        }
        for (String string : stringList){
            for (String word : words){
                result.add(string.replaceAll(word, "<b>" + word + "</b>"));
            }
        }
        return result;
    }
}
