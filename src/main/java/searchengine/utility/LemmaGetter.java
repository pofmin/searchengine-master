package searchengine.utility;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LemmaGetter {
    private static final String[] EXCLUDED_PARTS_OF_SPEECH = new String[]{"СОЮЗ", "МЕЖД", "ПРЕДЛ", "МС"};

    public static HashMap<String, Integer> getLemmas(String text) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = text.toLowerCase().replaceAll("([^а-я\\s])", "").split("\\s+");

        OUTER:
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals("")) continue;
            List<String> partOfSpeechCheck = luceneMorphology.getMorphInfo(words[i]);
            for (String item : EXCLUDED_PARTS_OF_SPEECH) {
                if (partOfSpeechCheck.get(0).toUpperCase().contains(item)) continue OUTER;
            }
            List<String> wordBaseForms = luceneMorphology.getNormalForms(words[i]);
            int count = 1;
            if (lemmas.get(wordBaseForms.get(0)) != null) {
                count = lemmas.get(wordBaseForms.get(0)) + 1;
            }
            lemmas.put(wordBaseForms.get(0), count);
        }
        return lemmas;
    }

    public static List<String> getAllOrderedLemmas(String content) throws IOException {
        LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
        List<String> allOrderedLemmas = new ArrayList<>();
        String[] words = content.toLowerCase().split("\\s+");

        for (String word : words) {
            if (word.matches(".*[а-я]+.*")) {
                word = word.replaceAll("([^а-я\\s])", "");
                List<String> wordBaseForms = luceneMorphology.getNormalForms(word);
                allOrderedLemmas.add(wordBaseForms.get(0));
            } else {
                allOrderedLemmas.add(word);
            }
        }
        return allOrderedLemmas;
    }
}
