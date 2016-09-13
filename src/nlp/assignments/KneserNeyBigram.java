package nlp.assignments;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.lang.*;

/**
 * Created by atakata on 9/08/16.
 */
class KneserNeyBigram implements LanguageModel {

    static final String START = "<S>";
    static final String STOP = "</S>";
    static final String UNKNOWN = "*UNKNOWN*";

    double discount;

    Counter<String> wordCounter = new Counter<String>();
    CounterMap<String, String> bigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> reverseBigramCounter = new CounterMap<String, String>();
    int totalBigramCount;

    public KneserNeyBigram(Collection<List<String>> sentenceCollection, double newDiscount) {
        discount = newDiscount;
        for (List<String> sentence : sentenceCollection) {
            List<String> stoppedSentence = new ArrayList<String>(sentence);
            stoppedSentence.add(0, START);
            stoppedSentence.add(STOP);

            //
            wordCounter.incrementCount(START, 1);

            String previousWord = stoppedSentence.get(0);
            for (int i = 1; i < stoppedSentence.size(); i++) {
                String word = stoppedSentence.get(i);
                wordCounter.incrementCount(word, 1.0);
                bigramCounter.incrementCount(previousWord, word, 1.0);
                reverseBigramCounter.incrementCount(word, previousWord, 1.0);
                previousWord = word;
            }
        }
        wordCounter.incrementCount(UNKNOWN, 1.0);
        for (String word : bigramCounter.keySet()) {
            totalBigramCount += bigramCounter.getCounter(word).size();
        }
    }

    public double getBigramProbability(String previousWord, String word) {
        double bigramCount = bigramCounter.getCount(previousWord, word);
        //System.out.println(bigramCounter.getCounter(previousWord));
        double unigramCount = wordCounter.getCount(previousWord);
        //double unigramCount = wordCounter.getCount(word);

        //System.out.println(wordCounter.getCount(word));
        double reverseBigramCount = reverseBigramCounter.getCounter(word).size();
        //System.out.println(reverseBigramCounter.getCounter(word));


        double part1 =  (Math.max((bigramCount - discount), 0) / unigramCount);
        double part2 = ((discount / unigramCount) * bigramCounter.getCounter(previousWord).size());
        double part3 = (reverseBigramCount / totalBigramCount);

        return part1 + part2 * part3;
    }

    @SuppressWarnings("Duplicates")
    public double getSentenceProbability(List<String> sentence) {
        List<String> stoppedSentence = new ArrayList<String>(sentence);
        stoppedSentence.add(0, START);
        stoppedSentence.add(STOP);
        double probability = 1.0;
        String previousWord = stoppedSentence.get(0);
        for (int i = 1; i < stoppedSentence.size(); i++) {
            String word = stoppedSentence.get(i);
            probability *= getBigramProbability(previousWord, word);
            previousWord = word;
        }
        return probability;
    }

    String generateWord(String previousWord) {
        double sample = Math.random();
        double sum = 0.0;
        normalizeDistributions();


        System.out.println(bigramCounter.getCounter(UNKNOWN));

        for (String word : bigramCounter.getCounter(previousWord).keySet()) {
            sum += bigramCounter.getCount(previousWord, word);
            if (sum > sample) {
                return word;
            }
        }
        return UNKNOWN;
    }

    public List<String> generateSentence() {
        List<String> sentence = new ArrayList<String>();

        String word = generateWord(START);
        while (!word.equals(STOP)) {
            sentence.add(word);
            word = generateWord(word);
        }
        return sentence;
    }

    private void normalizeDistributions() {
        for (String previousWord : bigramCounter.keySet()) {
            bigramCounter.getCounter(previousWord).normalize();
        }

    }

}