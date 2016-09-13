package nlp.assignments;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.lang.*;

/**
 * Created by atakata on 9/11/16.
 */
class KneserNeyTrigram implements LanguageModel {

    static final String START = "<S>";
    static final String STOP = "</S>";
    static final String UNKNOWN = "*UNKNOWN*";

    double triDiscount;
    double biDiscount;

    Counter<String> wordCounter = new Counter<String>();
    CounterMap<String, String> bigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> trigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> reverseTrigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> reverseBigramCounter = new CounterMap<String, String>();
    int totalBigramCount;
    int totalTrigramCount;

    public KneserNeyTrigram(Collection<List<String>> sentenceCollection, double d1, double d2) {
        triDiscount = d1;
        biDiscount = d2;


        for (List<String> sentence : sentenceCollection) {
            List<String> stoppedSentence = new ArrayList<String>(sentence);


            stoppedSentence.add(0, START);
            stoppedSentence.add(0, START);
            stoppedSentence.add(STOP);
            String prePreviousWord = stoppedSentence.get(0);
            String previousWord = stoppedSentence.get(1);

            //
            wordCounter.incrementCount(START, 1);

            for (int i = 2; i < stoppedSentence.size(); i++) {
                String word = stoppedSentence.get(i);
                wordCounter.incrementCount(word, 1.0);
                bigramCounter.incrementCount(previousWord, word, 1.0);
                trigramCounter.incrementCount(prePreviousWord + previousWord,
                        word, 1.0);
                reverseBigramCounter.incrementCount(word, previousWord, 1.0);
                reverseTrigramCounter.incrementCount(word, prePreviousWord + previousWord, 1.0);

                prePreviousWord = previousWord;
                previousWord = word;
            }
        }
        wordCounter.incrementCount(UNKNOWN, 1.0);
        for (String word : bigramCounter.keySet()) {
            totalBigramCount += bigramCounter.getCounter(word).size();
        }
        for (String word : trigramCounter.keySet()) {
            totalTrigramCount += trigramCounter.getCounter(word).size();
        }
    }

    @SuppressWarnings("Duplicates")
    public double getTrigramProbability(String prePreviousWord, String previousWord, String word) {

        double trigramCount = trigramCounter.getCount(prePreviousWord + previousWord, word);
        double preAndPrePreCount = trigramCounter.getCounter(prePreviousWord + previousWord).totalCount();
        //double unigramCount = wordCounter.getCount(prePreviousWord) + wordCounter.getCount(previousWord);
        //double reverseTrigramCount = reverseTrigramCounter.getCounter(word).size();
        double trigramCountSize = trigramCounter.getCounter(prePreviousWord + previousWord).size();

        double part1 =  (Math.max((trigramCount - triDiscount), 0) / preAndPrePreCount);
        //works but no perplex double part2 = (discount / totalTrigramCount); //* trigramCounter.getCounter(prePreviousWord + previousWord).size());
        double part2 = (triDiscount / preAndPrePreCount);
        double part3 = trigramCountSize;

        double bgp = getBigramProbability(previousWord, word);

        if (Double.isInfinite(part2)) {
            return bgp;
        }
        return part1 + part2 * part3 * bgp;
    }

    @SuppressWarnings("Duplicates")
    public double getBigramProbability(String previousWord, String word) {
        double bigramCount = bigramCounter.getCount(previousWord, word);
        //System.out.println(bigramCounter.getCounter(previousWord));
        double unigramCount = wordCounter.getCount(previousWord);
        //double unigramCount = wordCounter.getCount(word);

        //System.out.println(wordCounter.getCount(word));
        double reverseBigramCount = reverseBigramCounter.getCounter(word).size();
        //System.out.println(reverseBigramCounter.getCounter(word));


        double part1 =  (Math.max((bigramCount - biDiscount), 0) / unigramCount);
        double part2 = ((biDiscount / unigramCount) * bigramCounter.getCounter(previousWord).size());
        double part3 = ((reverseBigramCount) / (totalBigramCount));


        return part1 + part2 * part3;
    }

    @SuppressWarnings("Duplicates")
    public double getSentenceProbability(List<String> sentence) {
        List<String> stoppedSentence = new ArrayList<String>(sentence);
        String test = stoppedSentence.get(0);
        stoppedSentence.add(0, START);
        stoppedSentence.add(0, START);
        stoppedSentence.add(STOP);
        double probability = 1.0;
        String previousWord = stoppedSentence.get(1);
        String prePreviousWord = stoppedSentence.get(0);
        for (int i = 2; i < stoppedSentence.size(); i++) {
            String word = stoppedSentence.get(i);
            probability *= getTrigramProbability(prePreviousWord, previousWord, word);
            prePreviousWord = previousWord;
            previousWord = word;
        }
        return probability;
    }

    String generateWord(String previousWord) {
        double sample = Math.random();
        double sum = 0.0;

        return UNKNOWN;
    }

    public List<String> generateSentence() {
        List<String> sentence = new ArrayList<String>();

        return sentence;
    }

}
