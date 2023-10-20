package searchengine.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class Lemmatisator {
    private static final RussianLuceneMorphology morphologyRus;
    private static final EnglishLuceneMorphology morphologyEng;

    static {
        try{
            morphologyRus = new RussianLuceneMorphology();
            morphologyEng = new EnglishLuceneMorphology();
        } catch (Exception ex){
            throw new RuntimeException();
        }
    }

    public List<Integer> findIndexOfLemmaInText(String text, String lemma){
        List<Integer> lemmaIndexList = new ArrayList<>();
        String[] words = text.toLowerCase(Locale.ROOT).split("\\p{Punct}|\\s+");
        int index = 0;
        for (String word : words){
            List<String> lemmas = getLemma(word);
            for (String lem : lemmas){
                if (lem.equals(lemma)){
                    lemmaIndexList.add(index);
                }
            }
            index += word.length() + 1;
        }
        return lemmaIndexList;
    }

    public HashMap<String, Integer> getLemmaList(String text){
        int count;
        String[] words = text.toLowerCase(Locale.ROOT).replaceAll("[^а-яa-z\\s]", " ").trim().split("\\s+");
        HashMap<String, Integer> result = new HashMap<>();
        for (String word : words){
            if (word.isEmpty()){
                continue;
            }
            List<String> lemmas = getLemma(word);
            for (String lemma : lemmas){
                count = result.getOrDefault(lemma, 0) + 1;
                result.put(lemma, count);
            }
        }
        return result;
    }

    public List<String> getLemma(String word){
        List<String> lemmas = new ArrayList<>();
        try{
            List<String> normalForm;
            if (isRussianLetter(word)){
                normalForm = morphologyRus.getNormalForms(word);
            } else {
                normalForm = morphologyEng.getNormalForms(word);
            }
            if(!isServiceWord(word)){
                lemmas.addAll(normalForm);
            }
        } catch (Exception ex){
            log.debug("Слово <" + word + "> не содержится в словаре");
        }
        return lemmas;
    }

    public String cleanFromHtmlTags(String content){
        String result;
        var document = Jsoup.parse(content);
        result = document.title() + " ";
        result += document.body().text();
        return result;
    }

    private boolean isServiceWord(String word){
        List<String> info;
        if (isRussianLetter(word)) {
            info = morphologyRus.getMorphInfo(word);
        } else {
            info = morphologyEng.getMorphInfo(word);
        }
        for (String elem : info){
            if (elem.contains("ПРЕДЛ") || elem.contains("МЕЖД") || elem.contains("СОЮЗ") || elem.contains("ЧАСТ")){
                return true;
            }
        }
        return false;
    }
    private boolean isRussianLetter(String word){
        Pattern patternRusLetter = Pattern.compile("[а-я]*");
        Matcher matcher = patternRusLetter.matcher(word);
        return matcher.matches();
    }
}
