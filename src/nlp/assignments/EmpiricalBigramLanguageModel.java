package nlp.assignments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nlp.langmodel.LanguageModel;
import nlp.util.Counter;
import nlp.util.CounterMap;

/**
 * A dummy language model -- uses empirical unigram counts, plus a single
 * ficticious count for unknown words.
 */
class EmpiricalBigramLanguageModel implements LanguageModel {

	static final String START = "<S>";
	static final String STOP = "</S>";
	static final String UNKNOWN = "*UNKNOWN*";
	double lambda;

	Counter<String> wordCounter = new Counter<String>();
	CounterMap<String, String> bigramCounter = new CounterMap<String, String>();


	public double getBigramProbability(String previousWord, String word) {
		//bigramCount is the probability of word given previousWord
        //System.out.println(bigramCounter.getCounter(previousWord));
        double bigramCount = bigramCounter.getCount(previousWord, word);
        //unigramCount is probability of word
        //System.out.println(wordCounter.getCount(word));
        double unigramCount = wordCounter.getCount(word);

		if (unigramCount == 0) {
			//System.out.println("UNKNOWN Word: " + word);
			unigramCount = wordCounter.getCount(UNKNOWN);
		}
		return lambda * bigramCount + (1.0 - lambda) * unigramCount;
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

        for (String word : bigramCounter.getCounter(previousWord).keySet()) {
            sum += bigramCounter.getCount(previousWord, word);
            if (sum > sample) {
                return word;
            }
        }

        /*
        for (String word : wordCounter.keySet()) {
			sum += wordCounter.getCount(word);
			if (sum > sample) {
				return word;
			}
		}*/
		return UNKNOWN;
	}

	public List<String> generateSentence() {
		List<String> sentence = new ArrayList<String>();

        String word = generateWord(START);
        while (!word.equals(STOP)) {
            sentence.add(word);
            word = generateWord(word);
        }

        /*
        String word = generateWord();
		while (!word.equals(STOP)) {
			sentence.add(word);
			word = generateWord();
		}
		*/
		return sentence;
	}

	public EmpiricalBigramLanguageModel(Collection<List<String>> sentenceCollection,
                                        double newLambda) {
        lambda = newLambda;

		for (List<String> sentence : sentenceCollection) {
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(0, START);
			stoppedSentence.add(STOP);
			String previousWord = stoppedSentence.get(0);
			for (int i = 1; i < stoppedSentence.size(); i++) {
				String word = stoppedSentence.get(i);
				wordCounter.incrementCount(word, 1.0);
				bigramCounter.incrementCount(previousWord, word, 1.0);
				previousWord = word;
			}
		}
		wordCounter.incrementCount(UNKNOWN, 1.0);
		normalizeDistributions();
	}

	private void normalizeDistributions() {
		for (String previousWord : bigramCounter.keySet()) {
			bigramCounter.getCounter(previousWord).normalize();
		}
		wordCounter.normalize();

	}
}
