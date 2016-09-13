package nlp.assignments;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import nlp.langmodel.LanguageModel;
import nlp.util.CommandLineUtils;

/**
 * This is the main harness for assignment 1. To run this harness, use
 * <p/>
 * java nlp.assignments.LanguageModelTester -path ASSIGNMENT_DATA_PATH -model
 * MODEL_DESCRIPTOR_STRING
 * <p/>
 * First verify that the data can be read on your system. Second, find the point
 * in the main method (near the bottom) where an EmpiricalUnigramLanguageModel
 * is constructed. You will be writing new implementations of the LanguageModel
 * interface and constructing them there.
 */
public class LanguageModelTester {

	// HELPER CLASS FOR THE HARNESS, CAN IGNORE
	static class EditDistance {
		static double INSERT_COST = 1.0;
		static double DELETE_COST = 1.0;
		static double SUBSTITUTE_COST = 1.0;

		private double[][] initialize(double[][] d) {
			for (int i = 0; i < d.length; i++) {
				for (int j = 0; j < d[i].length; j++) {
					d[i][j] = Double.NaN;
				}
			}
			return d;
		}

		public double getDistance(List<? extends Object> firstList,
				List<? extends Object> secondList) {
			double[][] bestDistances = initialize(new double[firstList.size() + 1][secondList
					.size() + 1]);
			return getDistance(firstList, secondList, 0, 0, bestDistances);
		}

		private double getDistance(List<? extends Object> firstList,
				List<? extends Object> secondList, int firstPosition,
				int secondPosition, double[][] bestDistances) {
			if (firstPosition > firstList.size()
					|| secondPosition > secondList.size())
				return Double.POSITIVE_INFINITY;
			if (firstPosition == firstList.size()
					&& secondPosition == secondList.size())
				return 0.0;
			if (Double.isNaN(bestDistances[firstPosition][secondPosition])) {
				double distance = Double.POSITIVE_INFINITY;
				distance = Math.min(
						distance,
						INSERT_COST
								+ getDistance(firstList, secondList,
										firstPosition + 1, secondPosition,
										bestDistances));
				distance = Math.min(
						distance,
						DELETE_COST
								+ getDistance(firstList, secondList,
										firstPosition, secondPosition + 1,
										bestDistances));
				distance = Math.min(
						distance,
						SUBSTITUTE_COST
								+ getDistance(firstList, secondList,
										firstPosition + 1, secondPosition + 1,
										bestDistances));
				if (firstPosition < firstList.size()
						&& secondPosition < secondList.size()) {
					if (firstList.get(firstPosition).equals(
							secondList.get(secondPosition))) {
						distance = Math.min(
								distance,
								getDistance(firstList, secondList,
										firstPosition + 1, secondPosition + 1,
										bestDistances));
					}
				}
				bestDistances[firstPosition][secondPosition] = distance;
			}
			return bestDistances[firstPosition][secondPosition];
		}
	}

	// HELPER CLASS FOR THE HARNESS, CAN IGNORE
	static class SentenceCollection extends AbstractCollection<List<String>> {
		static class SentenceIterator implements Iterator<List<String>> {

			BufferedReader reader;

			public boolean hasNext() {
				try {
					return reader.ready();
				} catch (IOException e) {
					return false;
				}
			}

			public List<String> next() {
				try {
					String line = reader.readLine();
					String[] words = line.split("\\s+");
					List<String> sentence = new ArrayList<String>();
					for (int i = 0; i < words.length; i++) {
						String word = words[i];
						sentence.add(word.toLowerCase());
					}
					return sentence;
				} catch (IOException e) {
					throw new NoSuchElementException();
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			public SentenceIterator(BufferedReader reader) {
				this.reader = reader;
			}
		}

		String fileName;

		public Iterator<List<String>> iterator() {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						fileName));
				return new SentenceIterator(reader);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Problem with SentenceIterator for "
						+ fileName);
			}
		}

		public int size() {
			int size = 0;
			Iterator<List<String>> i = iterator();
			while (i.hasNext()) {
				size++;
				i.next();
			}
			return size;
		}

		public SentenceCollection(String fileName) {
			this.fileName = fileName;
		}

		public static class Reader {
			static Collection<List<String>> readSentenceCollection(
					String fileName) {
				return new SentenceCollection(fileName);
			}
		}

	}

	static double calculatePerplexity(LanguageModel languageModel,
			Collection<List<String>> sentenceCollection) {
		double logProbability = 0.0;
		double numSymbols = 0.0;
		for (List<String> sentence : sentenceCollection) {
			logProbability += Math.log(languageModel
					.getSentenceProbability(sentence)) / Math.log(2.0);
			numSymbols += sentence.size();
		}
		double avgLogProbability = logProbability / numSymbols;
		double perplexity = Math.pow(0.5, avgLogProbability);
		return perplexity;
	}

	static double calculateWordErrorRate(LanguageModel languageModel,
			List<SpeechNBestList> speechNBestLists, boolean verbose) {
		double totalDistance = 0.0;
		double totalWords = 0.0;
		EditDistance editDistance = new EditDistance();
		for (SpeechNBestList speechNBestList : speechNBestLists) {
            List<String> correctSentence = speechNBestList.getCorrectSentence();
			List<String> bestGuess = null;

            if ((correctSentence.get(0) == "in") && (correctSentence.get(1) == "tokyo")) {
                System.out.println("");
            }

			double bestScore = Double.NEGATIVE_INFINITY;
			double numWithBestScores = 0.0;
			double distanceForBestScores = 0.0;
			for (List<String> guess : speechNBestList.getNBestSentences()) {
                double score = Math.log(languageModel
						.getSentenceProbability(guess))
						+ (speechNBestList.getAcousticScore(guess) / 16.0);

				double distance = editDistance.getDistance(correctSentence,
						guess);
				if (score == bestScore) {
					numWithBestScores += 1.0;
					distanceForBestScores += distance;
				}
				if (score > bestScore || bestGuess == null) {
					bestScore = score;
					bestGuess = guess;
					distanceForBestScores = distance;
					numWithBestScores = 1.0;
				}
			}
			// double distance = editDistance.getDistance(correctSentence,
			// bestGuess);
			totalDistance += distanceForBestScores / numWithBestScores;
			totalWords += correctSentence.size();
			if (verbose) {
				System.out.println();
                displayHypothesis("GUESS:", bestGuess, speechNBestList,
                        languageModel);
                displayHypothesis("GOLD:", correctSentence, speechNBestList,
                        languageModel);

			}
		}
		return totalDistance / totalWords;
	}

	private static NumberFormat nf = new DecimalFormat("0.00E00");

	private static void displayHypothesis(String prefix, List<String> guess,
			SpeechNBestList speechNBestList, LanguageModel languageModel) {
		double acoustic = speechNBestList.getAcousticScore(guess) / 16.0;
		double language = Math.log(languageModel.getSentenceProbability(guess));
		System.out.println(prefix + "\tAM: " + nf.format(acoustic) + "\tLM: "
				+ nf.format(language) + "\tTotal: "
				+ nf.format(acoustic + language) + "\t" + guess);
	}

	static double calculateWordErrorRateLowerBound(
			List<SpeechNBestList> speechNBestLists) {
		double totalDistance = 0.0;
		double totalWords = 0.0;
		EditDistance editDistance = new EditDistance();
		for (SpeechNBestList speechNBestList : speechNBestLists) {
			List<String> correctSentence = speechNBestList.getCorrectSentence();
			double bestDistance = Double.POSITIVE_INFINITY;
			for (List<String> guess : speechNBestList.getNBestSentences()) {
				double distance = editDistance.getDistance(correctSentence,
						guess);
				if (distance < bestDistance)
					bestDistance = distance;
			}
			totalDistance += bestDistance;
			totalWords += correctSentence.size();
		}
		return totalDistance / totalWords;
	}

	static double calculateWordErrorRateUpperBound(
			List<SpeechNBestList> speechNBestLists) {
		double totalDistance = 0.0;
		double totalWords = 0.0;
		EditDistance editDistance = new EditDistance();
		for (SpeechNBestList speechNBestList : speechNBestLists) {
			List<String> correctSentence = speechNBestList.getCorrectSentence();
			double worstDistance = Double.NEGATIVE_INFINITY;
			for (List<String> guess : speechNBestList.getNBestSentences()) {
				double distance = editDistance.getDistance(correctSentence,
						guess);
				if (distance > worstDistance)
					worstDistance = distance;
			}
			totalDistance += worstDistance;
			totalWords += correctSentence.size();
		}
		return totalDistance / totalWords;
	}

	static double calculateWordErrorRateRandomChoice(
			List<SpeechNBestList> speechNBestLists) {
		double totalDistance = 0.0;
		double totalWords = 0.0;
		EditDistance editDistance = new EditDistance();
		for (SpeechNBestList speechNBestList : speechNBestLists) {
			List<String> correctSentence = speechNBestList.getCorrectSentence();
			double sumDistance = 0.0;
			double numGuesses = 0.0;
			for (List<String> guess : speechNBestList.getNBestSentences()) {
				double distance = editDistance.getDistance(correctSentence,
						guess);
				sumDistance += distance;
				numGuesses += 1.0;
			}
			totalDistance += sumDistance / numGuesses;
			totalWords += correctSentence.size();
		}
		return totalDistance / totalWords;
	}

	static Collection<List<String>> extractCorrectSentenceList(
			List<SpeechNBestList> speechNBestLists) {
		Collection<List<String>> correctSentences = new ArrayList<List<String>>();
		for (SpeechNBestList speechNBestList : speechNBestLists) {
			correctSentences.add(speechNBestList.getCorrectSentence());
		}
		return correctSentences;
	}

	static Set<String> extractVocabulary(
			Collection<List<String>> sentenceCollection) {
		Set<String> vocabulary = new HashSet<String>();
		for (List<String> sentence : sentenceCollection) {
			for (String word : sentence) {
				vocabulary.add(word);
			}
		}
		return vocabulary;
	}

	public static void main(String[] args) throws IOException {
        // Parse command line flags and arguments
        Map<String, String> argMap = CommandLineUtils
                .simpleCommandLineParser(args);

        // Set up default parameters and settings
        String basePath = "/Users/atakata/GitHub/stat-nlp-nyu/data1";
        String model = "trigram";
        model = "kneserney-trigram";
        //model = "bigram";
        boolean verbose = true;

        // Update defaults using command line specifications

        // The path to the assignment data
        if (argMap.containsKey("-path")) {
            basePath = argMap.get("-path");
        }
        System.out.println("Using base path: " + basePath);

        // A string descriptor of the model to use
        if (argMap.containsKey("-model")) {
            model = argMap.get("-model");
        }
        System.out.println("Using model: " + model);

        // Whether or not to print the individual speech errors.
        if (argMap.containsKey("-verbose")) {
            verbose = true;
        }
        if (argMap.containsKey("-quiet")) {
            verbose = false;
        }

        // Read in all the assignment data
        String trainingSentencesFile = "/treebank-sentences-spoken-train.txt";
        Collection<List<String>> trainingSentenceCollection = SentenceCollection.Reader
                .readSentenceCollection(basePath + trainingSentencesFile);
        Set<String> trainingVocabulary = extractVocabulary(trainingSentenceCollection);

        String speechNBestListsPath = "/wsj_n_bst";
        List<SpeechNBestList> speechNBestLists = SpeechNBestList.Reader
                .readSpeechNBestLists(basePath + speechNBestListsPath,
                        trainingVocabulary);

        // String validationSentencesFile =
        // "/treebank-sentences-spoken-validate.txt";
        // Collection<List<String>> validationSentenceCollection =
        // SentenceCollection.Reader.readSentenceCollection(basePath +
        // validationSentencesFile);

        String testSentencesFile = "/treebank-sentences-spoken-test.txt";
        Collection<List<String>> testSentenceCollection =
                SentenceCollection.Reader.readSentenceCollection(basePath +
                        testSentencesFile);

        // Build the language model


        LanguageModel languageModel = null;
        if (model.equalsIgnoreCase("baseline")) {
            languageModel = new EmpiricalUnigramLanguageModel(
                    trainingSentenceCollection);
        } else if (model.equalsIgnoreCase("sri")) {
            languageModel = new SriLanguageModel(argMap.get("-sri"));
        } else if (model.equalsIgnoreCase("bigram")) {
            languageModel = new EmpiricalBigramLanguageModel(
                    trainingSentenceCollection, 0.9);
        } else if (model.equalsIgnoreCase("trigram")) {
            languageModel = new EmpiricalTrigramLanguageModel(
                    trainingSentenceCollection, 0.5, 0.3);
        } else if (model.equalsIgnoreCase("katz-bigram")) {
            languageModel = new KatzBigramLanguageModel(
                    trainingSentenceCollection);
        } else if (model.equalsIgnoreCase("katz-trigram")) {
            languageModel = new KatzTrigramLanguageModel(
                    trainingSentenceCollection);
        } else if (model.equalsIgnoreCase("kneserney-bigram")) {
            languageModel = new KneserNeyBigram(
                    trainingSentenceCollection, 0.4);
        } else if (model.equalsIgnoreCase("kneserney-trigram")) {
            languageModel = new KneserNeyTrigram(
                    trainingSentenceCollection, 0.34, 0.38);
        } else if (model.equalsIgnoreCase("quadgram")) {
            languageModel = new EmpiricalQuadgramLanguageModel(
                    trainingSentenceCollection, 0.15, 0.45, 0.3);
        } else {
            throw new RuntimeException("Unknown model descriptor: " + model);
        }


        /*for (double j = 0.26 ; j < 0.35; j += 0.01) {
            System.out.println("J: " + j);
            for (double k = 0.26; k < 0.4; k += 0.02) {
            languageModel = new KneserNeyTrigram(trainingSentenceCollection, j, k);
            //languageModel = new KneserNeyBigram(trainingSentenceCollection, j);
*/
                //System.out.println(j);
                //System.out.println(k);

                //System.out.println(j);
                //languageModel = new KneserNeyTrigram(trainingSentenceCollection, j);

                //List<String> messages = Arrays.asList("reluctant", "advertisers", "try", "soft", "sell", "spots");
            /*
            List<String> messages = Arrays.asList("how", "much", "as", "sale", "might");
            System.out.println(messages);
            System.out.println(languageModel.getSentenceProbability(messages));

            messages = Arrays.asList("how", "much", "a", "sale", "might");
            System.out.println(messages);
            System.out.println(languageModel.getSentenceProbability(messages));
*/
                //System.out.println(j);
                // Evaluate the language model
                //

                //double wsjPerplexity = calculatePerplexity(languageModel,
                //        testSentenceCollection);

                //double hubPerplexity = calculatePerplexity(languageModel,
                //        extractCorrectSentenceList(speechNBestLists));


                //System.out.println("WSJ Perplexity:  " + wsjPerplexity);

                //System.out.println("HUB Perplexity:  " + hubPerplexity);
                //System.out.println(hubPerplexity);

/*
            System.out.println("WER Baselines:");
            System.out.println("  Best Path:  "
                    + calculateWordErrorRateLowerBound(speechNBestLists));
            System.out.println("  Worst Path: "
                    + calculateWordErrorRateUpperBound(speechNBestLists));
            System.out.println("  Avg Path:   "
                    + calculateWordErrorRateRandomChoice(speechNBestLists));
*/
                double wordErrorRate = calculateWordErrorRate(languageModel,
                        speechNBestLists, verbose);
                if (wordErrorRate < 0.0615) {
                    //System.out.println(j+ " " + k);

                    System.out.println("HUB Word Error Rate: " + wordErrorRate);

                    double hubPerplexity = calculatePerplexity(languageModel,
                            extractCorrectSentenceList(speechNBestLists));

                    System.out.println("HUB Perplexity:  " + hubPerplexity);
                }

                //System.out.println(wordErrorRate);

                //System.out.println("Generated Sentences:");
                //for (int i = 0; i < 5; i++)
                //   System.out.println("  " + languageModel.generateSentence());

	}
}
