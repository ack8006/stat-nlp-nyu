package nlp.assignments;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by atakata on 9/12/16.
 */
class EmpiricalQuadgramLanguageModel implements LanguageModel {

    static final String START = "<S>";
    static final String STOP = "</S>";
    static final String UNKNOWN = "*UNKNOWN*";
    double lambda1;// = 0.5;
    double lambda2;// = 0.3;
    double lambda3;

    Counter<String> wordCounter = new Counter<String>();
    CounterMap<String, String> bigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> trigramCounter = new CounterMap<String, String>();
    CounterMap<String, String> quadgramCounter = new CounterMap<String, String>();


    public double getQuadgramProbability(String PPPreviousWord, String prePreviousWord,
                                        String previousWord, String word) {
        double quadgramCount = quadgramCounter.getCount(PPPreviousWord + prePreviousWord + previousWord,
                word);
        double trigramCount = trigramCounter.getCount(prePreviousWord
                + previousWord, word);
        double bigramCount = bigramCounter.getCount(previousWord, word);
        double unigramCount = wordCounter.getCount(word);
        if (unigramCount == 0) {
            //System.out.println("UNKNOWN Word: " + word);
            unigramCount = wordCounter.getCount(UNKNOWN);
        }

        return lambda1 * quadgramCount + lambda2 * trigramCount + lambda3 * bigramCount
                + (1.0 - lambda1 - lambda2- lambda3) * unigramCount;
    }

    public double getSentenceProbability(List<String> sentence) {
        List<String> stoppedSentence = new ArrayList<String>(sentence);
        stoppedSentence.add(0, START);
        stoppedSentence.add(0, START);
        stoppedSentence.add(0, START);
        stoppedSentence.add(STOP);
        double probability = 1.0;
        String PPPreviousWord = stoppedSentence.get(0);
        String prePreviousWord = stoppedSentence.get(1);
        String previousWord = stoppedSentence.get(2);
        for (int i = 3; i < stoppedSentence.size(); i++) {
            String word = stoppedSentence.get(i);
            probability *= getQuadgramProbability(PPPreviousWord, prePreviousWord, previousWord,
                    word);
            PPPreviousWord = prePreviousWord;
            prePreviousWord = previousWord;
            previousWord = word;
        }
        return probability;
    }

    String generateWord(String previousWords) {
        double sample = Math.random();
        double sum = 0.0;

        for (String word : quadgramCounter.getCounter(previousWords).keySet()) {
            sum += quadgramCounter.getCount(previousWords, word);
            if (sum > sample) {
                return word;
            }
        }
        return UNKNOWN;
    }

    public List<String> generateSentence() {
        List<String> sentence = new ArrayList<String>();
        String prePreviousWord = START;
        String previousWord = START;
        String PPPreviousWord = START;
        String word = generateWord(PPPreviousWord + prePreviousWord + previousWord);
        while (!word.equals(STOP)) {
            sentence.add(word);
            PPPreviousWord = prePreviousWord;
            prePreviousWord = previousWord;
            previousWord = word;
            word = generateWord(PPPreviousWord + prePreviousWord + previousWord);
        }
        return sentence;
    }

    public EmpiricalQuadgramLanguageModel(Collection<List<String>> sentenceCollection,
                                         double newLambda1, double newLambda2, double newLambda3) {
        lambda1 = newLambda1;
        lambda2 = newLambda2;
        lambda3 = newLambda3;

        for (List<String> sentence : sentenceCollection) {
            List<String> stoppedSentence = new ArrayList<String>(sentence);
            stoppedSentence.add(0, START);
            stoppedSentence.add(0, START);
            stoppedSentence.add(0, START);
            stoppedSentence.add(STOP);
            String PPPreviousWord = stoppedSentence.get(0);
            String prePreviousWord = stoppedSentence.get(1);
            String previousWord = stoppedSentence.get(2);
            for (int i = 3; i < stoppedSentence.size(); i++) {
                String word = stoppedSentence.get(i);
                wordCounter.incrementCount(word, 1.0);
                bigramCounter.incrementCount(previousWord, word, 1.0);
                trigramCounter.incrementCount(prePreviousWord + previousWord,
                        word, 1.0);
                quadgramCounter.incrementCount(PPPreviousWord + prePreviousWord + previousWord,
                        word, 1.0);
                PPPreviousWord = prePreviousWord;
                prePreviousWord = previousWord;
                previousWord = word;
            }
        }
        wordCounter.incrementCount(UNKNOWN, 1.0);
        normalizeDistributions();
    }

    private void normalizeDistributions() {
        for (String previousTrigram : quadgramCounter.keySet()) {
            quadgramCounter.getCounter(previousTrigram).normalize();
        }
        for (String previousBigram : trigramCounter.keySet()) {
            trigramCounter.getCounter(previousBigram).normalize();
        }
        for (String previousWord : bigramCounter.keySet()) {
            bigramCounter.getCounter(previousWord).normalize();
        }
        wordCounter.normalize();
    }
}


