package netgen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;

public class Corpus implements Comparable {

    //CLASS MEMBERS
    private String rawText;
    private ArrayList<ArrayList<Token>> processedText;
    private String date;
    private String summary;
    private String title;
    private String link;
//    public HashMap<String, String> attributes;
    private HashMap<Token, Integer> tokenFrequency;
    private HashSet<Token> stopwords;
    private HashSet<Token> namedEntities;
    private Stemmer stemmer;

    //CONSTRUCTORS & ASSOCIATED METHODS
    public Corpus(String text) {
        rawText = text.trim();
//        attributes = new HashMap<String, String>();
        title = "";
        link = "";
        date = "";
        summary = "";
    }

    
    
    //////////TEXT PROCESSING METHODS///////////
    //Splits corpus into strings consisting of complete sentences
    //Splits on both periods and semicolons
    public static ArrayList<String> splitSentences(ArrayList<String> corpus) {
        ArrayList<String> sentences = new ArrayList<>();

        String preCompletion = "( ([A-Za-z0-9,]|\\#|-|'){2,}?)";
        String completion = "(\\.|;|\\?|!)+"; //Valid completion characters for a sentence; 1 or more required, any composition permitted
        String postCompletion = "((\\\"|\\\'|[0-9]| |\\z|$|\\n){0,3})"; //Post-sentence characters permitted; extremely tolerant

        for (String line : corpus) {
            Pattern pattern = Pattern.compile(preCompletion + completion + postCompletion);
            Matcher m = pattern.matcher(line);
            int index = 0;
            while (m.find()) {
                String s = line.substring(index, m.end());
                if (!s.matches(".* (Mr|Mrs|Ms|Dr|Rev|Esq|Mass|Conn).( |,)") //Avoid matching on abbreviated titles 
                        //TODO: Make this list of titles more comprehensive
                        //TODO: 
                        && !s.matches(".*[0-9]+(\\.|,)[0-9]+")) //Avoid matching decimal numbers
                {
                    if (s.length() > 1) { //avoid adding empty sentences
                        sentences.add(s.trim());
                        index = m.end();
                    }
                }
            }
        }

        return sentences;
    }

    //Single string version of sentence splitter
    public static ArrayList<String> splitSentences(String line) {
        ArrayList<String> list = new ArrayList<String>(1);
        list.add(line);
        return Corpus.splitSentences(list);
    }

    //Modified version which takes an ArrayList of strings
    public static ArrayList<ArrayList<Token>> tokenize(ArrayList<String> input) {
        ArrayList<ArrayList<Token>> output = new ArrayList<>();

        for (String line : input) {
            output.add(Corpus.tokenize(line));
        }
        return output;
    }

    //Takes a filtered sentence and returns its contents as a list of tokens
    //Possible alternative: return a token set rather than a token list?
    public static ArrayList<Token> tokenize(String input) {
        ArrayList<Token> sentence = new ArrayList<>();
        String[] split = input.split("\\s+");
        for (String word : split) {
            word = word.trim();
            if (word.length() > 0) {
                sentence.add(new Token(word));
            }
        }
        return sentence;
    }

    //Eliminates duplicates from a list of tokens
    //NOTE: The returned list may no longer be in the same order
    public static void removeDuplicates(ArrayList<Token> tokenlist) {
        HashSet<Token> tokenset = new HashSet<>();
        tokenset.addAll(tokenlist);
        tokenlist = new ArrayList<>();
        tokenlist.addAll(tokenset);
    }

    //Filters out everything but spaces, letters
    //Trims and converts letters to lower-case
    public static String filterNonAlpha(String input) {
        return input.replaceAll("[^a-zA-Z ]", " ").toLowerCase().trim();
    }

    //Filters out everything but spaces, letters
    //Trims and converts to lower-case
    public static ArrayList<String> makeFilteredStrings(ArrayList<String> input) {
        ArrayList<String> output = new ArrayList<>();
        for (String line : input) {
            output.add(Corpus.filterNonAlpha(line));
        }
        return output;
    }

    //Removes stopwords from this processed text
    public void removeStopwords() {
        for (ArrayList<Token> sentence : this.processedText) {
            sentence.removeAll(this.stopwords);
        }
    }

    //Tags stopwords as such
    public void tagStopwords() {
        for (ArrayList<Token> sentence : this.processedText) {
            for (Token token : sentence) {
                if (stopwords.contains(token)) {
                    token.tag(Token.Tag.STOPWORD);
                }
            }
        }
    }

    //TODO: 
    //Need to figure out whether adjacent capitalized words are in fact distinct tokens/named entities
//    private void tagNamedEntitiesREGEX() {
//        //First pass: identify additional named entities in this context        
//        for(ArrayList<Token> sentence : processedText) {
//            for(int i = 0; i < sentence.size(); i++) {
//                if()
//            }
//        }
//        
//        
//    }
    public void generateFrequencyMap() {
        HashMap<Token, Integer> map = new HashMap<>();

        for (ArrayList<Token> tokens : this.processedText) {
            for (int i = 0; i < tokens.size(); i++) {
                if (map.containsKey(tokens.get(i))) {
                    int value = map.get(tokens.get(i)) + 1;
                    map.put(tokens.get(i), value);
                } else {
                    map.put(tokens.get(i), 1);
                }
            }
        }
        this.tokenFrequency = map;
    }

    //Conducts all processing activities on the corpus
    public void process(Stemmer stemmer, HashSet<Token> stopwords) {
        //Set stemmer and stopwords
        this.stemmer = stemmer;
        this.stopwords = stopwords;
        
        //Split on sentences, filter characters, and tokenize
        ArrayList<ArrayList<Token>> processed = Corpus.tokenize(makeFilteredStrings(Corpus.splitSentences(rawText)));

        for (ArrayList<Token> line : processed) {
            for (Token token : line) {
                token.setSignature(stemmer.stem(token.getSignature()));
            }
        }
        
        this.processedText = processed;
        
        //Filter stopwords, generate metadata
        //TODO: switch to tagging stopwords?
        this.removeStopwords();
        this.generateFrequencyMap();

    }

    //ACCESSORS AND MUTATORS
    public String getDate() {
        return date;
    }
    
    //Returns a set of all unique tokens in the corpus
    public HashSet<Token> getTokenSet() {
        HashSet<Token> tokenSet = new HashSet<>();
        tokenSet.addAll(tokenFrequency.keySet());
        return tokenSet;
    }

    //Returns the number of tokens in the processed text
    public int getTokenizedSize() {
        int count = 0;
        for (ArrayList<Token> sentence : this.processedText) {
            count += sentence.size();
        }
        return count;
    }

    //Accepts only YYYY-MM-DD format
    //Warns & sets to "UNKOWN DATE" if provided nonmatching string
    public void setDate(String date) {
        date = date.trim();
        if (date.matches("[0-9]{4}(-[0-9]{2}){2}")) {
            this.date = date;
        } else {
            System.out.println("Date? " + date);
            this.date = "UNKNOWN DATE";
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary.trim();
    }

    public String getTitle() {
        return title;
    }

    //Accepts only alphanumeric characters and spaces
    //Filters out all other characters
    public void setTitle(String title) {
        this.title = title.replaceAll("[^A-Za-z0-9 ]", "").trim();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link.trim();
        if (!this.link.matches("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")) {
            System.out.println("Link? " + this.link);
        }

    }

    @Override
    public int compareTo(Object other) throws NullPointerException, ClassCastException {
        if (other == null) {
            throw new NullPointerException();
        }
        if (!other.getClass().equals(this.getClass())) {
            throw new ClassCastException();
        }
        Corpus otherCorpus = (Corpus) other;

        int myYear = Integer.parseInt(date.substring(0, 3));
        int otherYear = Integer.parseInt(otherCorpus.getDate().substring(0, 3));
        int myMonth = Integer.parseInt(date.substring(5, 6));
        int otherMonth = Integer.parseInt(otherCorpus.getDate().substring(5, 6));
        int myDay = Integer.parseInt(date.substring(8, 9));
        int otherDay = Integer.parseInt(otherCorpus.getDate().substring(8, 9));

        if (myYear > otherYear) {
            return 1;
        } else if (myYear < otherYear) {
            return -1;
        } else if (myMonth > otherMonth) {
            return 1;
        } else if (myMonth < otherMonth) {
            return -1;
        } else if (myDay > otherDay) {
            return 1;
        } else if (myDay < otherDay) {
            return -1;
        } else {
            return 0;
        }

    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public ArrayList<ArrayList<Token>> getProcessedText() {
        return processedText;
    }

    public void setProcessedText(ArrayList<ArrayList<Token>> processedText) {
        this.processedText = processedText;
    }

    public void setStopwords(HashSet<Token> stopwords) {
        this.stopwords = stopwords;
    }

    //
    

}
