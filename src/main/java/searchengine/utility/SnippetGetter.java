package searchengine.utility;

import org.jsoup.Jsoup;
import searchengine.model.PageEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class SnippetGetter {
    private PageEntity pageEntity;
    private List<String> queryLemmas;
    private int snippetLengthMax;
    private int snippetLengthForEachWord;
    private String[] contentArray;
    List<String> contentLemmas;
    List<int []> allIndexes = new ArrayList<>();

    public SnippetGetter(PageEntity pageEntity, List<String> queryLemmas, int snippetLengthMax) {
        this.pageEntity = pageEntity;
        this.queryLemmas = queryLemmas;
        this.snippetLengthMax = snippetLengthMax;
    }

    public String getSnippet() throws IOException {
        String content = Jsoup.parse(pageEntity.getContent()).text();
        contentArray = content.split("\\s+");
        contentLemmas = LemmaGetter.getAllOrderedLemmas(content);
        snippetLengthForEachWord = snippetLengthMax / queryLemmas.size();

        for (String queryWord : queryLemmas) {
            int queryWordIndex = contentLemmas.indexOf(queryWord);
            int[] indexes = getSnippetIndexes(queryWordIndex);
            allIndexes.add(indexes);
        }
        Collections.sort(allIndexes, (o1, o2) -> o1[0] - o2[0]);

        if (allIndexes.size() > 1) {
            allIndexes = joinCloseArrays();
            int totalLength = getTotalLength();
            int lengthRemainingHalf = (snippetLengthMax - totalLength) / 2;

            if (lengthRemainingHalf > 1) {
                int startIndex = allIndexes.get(0)[0];
                int endIndex = allIndexes.get(allIndexes.size() - 1)[1];
                int[] newIndexes = addRemainingWords(startIndex, endIndex, lengthRemainingHalf);
                allIndexes.get(0)[0] = newIndexes[0];
                allIndexes.get(allIndexes.size() - 1)[1] = newIndexes[1];
            }
        }
        List<String> allSnippetComponents = getSnippetComponents();
        return getFinalSnippet(allSnippetComponents);
    }

    public int[] getSnippetIndexes(int index) {
        int snippetLengthForEachWordHalf = snippetLengthForEachWord / 2;
        int startIndex = Math.max(index - snippetLengthForEachWordHalf, 0);
        int addToEnd = 0;
        if ((index - startIndex) < snippetLengthForEachWordHalf) {
            addToEnd = snippetLengthForEachWordHalf - (index - startIndex);
        }
        int endIndex = Math.min(index + snippetLengthForEachWordHalf + addToEnd, contentArray.length - 1);
        int addToBeginning;
        if (endIndex - index < snippetLengthForEachWordHalf) {
            addToBeginning = snippetLengthForEachWordHalf - (endIndex - index);
            startIndex = Math.max(startIndex - addToBeginning, 0);
        }
        return new int[] {startIndex, endIndex};
    }

    public List<int[]> joinCloseArrays() {
        int i = 0;
        boolean isValid = true;
        while (isValid) {
            if (allIndexes.get(i + 1)[0] <= allIndexes.get(i)[1]) {
                allIndexes.get(i)[1] = allIndexes.get(i + 1)[1];
                allIndexes.remove(i + 1);
            } else {
                i++;
            }
            if ((i + 1) == allIndexes.size()) {
                isValid = false;
            }
        }
        return allIndexes;
    }

    public int getTotalLength() {
        int totalLength = 0;
        for (int[] array : allIndexes) {
            int length = array[1] - array[0];
            totalLength += length;
        }
        return totalLength;
    }

    public int[] addRemainingWords(int startIndex, int endIndex, int lengthRemainingHalf) {
        int startIndexNew = Math.max(startIndex - lengthRemainingHalf, 0);
        int addToEnd = 0;
        if ((startIndex - startIndexNew) < lengthRemainingHalf) {
            addToEnd = lengthRemainingHalf - startIndex;
        }
        int endIndexNew = Math.min(endIndex + lengthRemainingHalf + addToEnd, contentArray.length - 1);
        int addToBeginning;
        if (endIndexNew - endIndex < lengthRemainingHalf) {
            addToBeginning = lengthRemainingHalf - ((contentArray.length - 1) - endIndex);
            startIndexNew = Math.max(startIndexNew - addToBeginning, 0);
        }
        return new int[] {startIndexNew, endIndexNew};
    }

    public List<String> getSnippetComponents() {
        List<String> allSnippetComponents = new ArrayList<>();
        for (int[] array : allIndexes) {
            StringJoiner joiner = new StringJoiner(" ");
            for (int i = array[0]; i <= array[1]; i++) {
                String element = queryLemmas.contains(contentLemmas.get(i)) ? "<b>" + contentArray[i] + "</b>" : contentArray[i];
                joiner.add(element);
            }
            allSnippetComponents.add(joiner.toString());
        }
        return allSnippetComponents;
    }

    public String getFinalSnippet(List<String> allSnippetComponents) {
        StringJoiner joiner = new StringJoiner(" <...> ");
        allSnippetComponents.forEach(word -> joiner.add(word));
        return joiner.toString();
    }
}
