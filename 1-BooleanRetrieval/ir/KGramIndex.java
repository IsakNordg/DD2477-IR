/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        
        if(p1 == null) return p2;
        if(p2 == null) return p1;

        p1 = new ArrayList<KGramPostingsEntry>(p1);
        p2 = new ArrayList<KGramPostingsEntry>(p2);

        int i = 0, j = 0;
        List<KGramPostingsEntry> result = new ArrayList<KGramPostingsEntry>();
        while (i < p1.size() && j < p2.size()){
            int id1 = p1.get(i).tokenID;
            int id2 = p2.get(j).tokenID;
            if (id1 == id2) {
                result.add(p1.get(i));
                i++; j++;
            } else if (id1 < id2) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }

    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        
        String paddedToken = "^" + token + "$";
        for (int i = 0; i < paddedToken.length() - K + 1; i++) {
            String kgram = paddedToken.substring(i, i + K);

            Integer ID = getIDByTerm(token);
            if (ID == null) {
                ID = generateTermID();
                id2term.put(ID, token);
                term2id.put(token, ID);
            }

            if (!index.containsKey(kgram)) {
                index.put(kgram, new ArrayList<KGramPostingsEntry>());
            }
            
            List<KGramPostingsEntry> postings = (List<KGramPostingsEntry>) index.get(kgram);
            KGramPostingsEntry newPosting = new KGramPostingsEntry(ID);

            if(!contains(postings, newPosting)){

                postings.add(newPosting);
                index.put(kgram, postings);
            }
        }
    }

    private boolean contains(List<KGramPostingsEntry> postings, KGramPostingsEntry newPosting) {
        if (postings == null) {
            return false;
        }

        // binary search
        int left = 0;
        int right = postings.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (postings.get(mid).tokenID == newPosting.tokenID) {
                return true;
            } else if (postings.get(mid).tokenID < newPosting.tokenID) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return false;
    }

    private List<KGramPostingsEntry> insertSorted(List<KGramPostingsEntry> postings, KGramPostingsEntry newPosting) {
        if (postings == null) {
            postings = new ArrayList<KGramPostingsEntry>();
            postings.add(newPosting);
            return postings;
        }

        List<KGramPostingsEntry> newPostings = new ArrayList<KGramPostingsEntry>();
        int i = 0;
        while (i < postings.size() && postings.get(i).tokenID < newPosting.tokenID) {
            newPostings.add(postings.get(i));
            i++;
        }
        newPostings.add(newPosting);
        while (i < postings.size()) {
            newPostings.add(postings.get(i));
            i++;
        }
        return newPostings;
    }
    


    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        if (index.containsKey(kgram)) {
            return index.get(kgram);
        }
        return null;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
