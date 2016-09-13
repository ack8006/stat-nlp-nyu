package nlp.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

import java.lang.*;


/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words.
 */
class EmpiricalTrigramLanguageModel implements LanguageModel {

	static final String START = "<S>";
	static final String STOP = "</S>";
	static final String UNKNOWN = "*UNKNOWN*";
	double lambda1;// = 0.5;
	double lambda2;// = 0.3;

	Counter<String> wordCounter = new Counter<String>();
	CounterMap<String, String> bigramCounter = new CounterMap<String, String>();
	CounterMap<String, String> trigramCounter = new CounterMap<String, String>();


    public double getTrigramProbability(String prePreviousWord,
			String previousWord, String word) {
		double trigramCount = trigramCounter.getCount(prePreviousWord
				+ previousWord, word);
		double bigramCount = bigramCounter.getCount(previousWord, word);
		double unigramCount = wordCounter.getCount(word);
		if (unigramCount == 0) {
			//System.out.println("UNKNOWN Word: " + word);
			unigramCount = wordCounter.getCount(UNKNOWN);
		}

		/*
        //lambda1 = Math.max((trigramCount) / (trigramCount+bigramCount+unigramCount), 0.4);
        //lambda2 = Math.max((1-lambda1) * (bigramCount/(bigramCount+unigramCount)), (1-lambda1) * (3/7));

        if (bigramCount != 0.0) {
            lambda1 = (trigramCount / (trigramCount + bigramCount)) * .895;
            lambda2 = .895 - lambda1;
        } else {
            lambda1 = .44;
            lambda2 = .44;
        }
        */

		return lambda1 * trigramCount + lambda2 * bigramCount
				+ (1.0 - lambda1 - lambda2) * unigramCount;
	}

	public double getSentenceProbability(List<String> sentence) {
		List<String> stoppedSentence = new ArrayList<String>(sentence);
		stoppedSentence.add(0, START);
		stoppedSentence.add(0, START);
		stoppedSentence.add(STOP);
		double probability = 1.0;
		String prePreviousWord = stoppedSentence.get(0);
		String previousWord = stoppedSentence.get(1);
		for (int i = 2; i < stoppedSentence.size(); i++) {
			String word = stoppedSentence.get(i);
			probability *= getTrigramProbability(prePreviousWord, previousWord,
					word);
			prePreviousWord = previousWord;
			previousWord = word;
		}
		return probability;
	}

	String generateWord(String previousWords) {
		double sample = Math.random();
		double sum = 0.0;

		for (String word : trigramCounter.getCounter(previousWords).keySet()) {
			sum += trigramCounter.getCount(previousWords, word);
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
		String word = generateWord(prePreviousWord + previousWord);
		while (!word.equals(STOP)) {
			sentence.add(word);
			prePreviousWord = previousWord;
			previousWord = word;
			word = generateWord(prePreviousWord + previousWord);
		}
		return sentence;
	}

	public EmpiricalTrigramLanguageModel(Collection<List<String>> sentenceCollection,
										 double newLambda1, double newLambda2) {
		lambda1 = newLambda1;
		lambda2 = newLambda2;

		for (List<String> sentence : sentenceCollection) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(0, START);
			stoppedSentence.add(0, START);
			stoppedSentence.add(STOP);
			String prePreviousWord = stoppedSentence.get(0);
			String previousWord = stoppedSentence.get(1);
			for (int i = 2; i < stoppedSentence.size(); i++) {
				String word = stoppedSentence.get(i);
				wordCounter.incrementCount(word, 1.0);
				bigramCounter.incrementCount(previousWord, word, 1.0);
				trigramCounter.incrementCount(prePreviousWord + previousWord,
						word, 1.0);
				prePreviousWord = previousWord;
				previousWord = word;
			}
		}
		wordCounter.incrementCount(UNKNOWN, 1.0);
		normalizeDistributions();
	}

	private void normalizeDistributions() {
		for (String previousBigram : trigramCounter.keySet()) {
			trigramCounter.getCounter(previousBigram).normalize();
		}
		for (String previousWord : bigramCounter.keySet()) {
			bigramCounter.getCounter(previousWord).normalize();
		}
		wordCounter.normalize();
	}
}
