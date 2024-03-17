/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public double pagerank = 0;
    
    public ArrayList offset = new ArrayList<>();

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }


    //
    // YOUR CODE HERE
    //

    public void computeScore(String term, RankingType rankingType, NormalizationType normType, Index index, double prWeight){
        if(rankingType == RankingType.PAGERANK){
            this.score = pagerank;
            return;
        }
        
        int N = index.docNames.size();            // Number of documents in collection
        int tf = offset.size();                    // Number of occurrences of term in document
        int df = index.getPostings(term).size();    // Number of documents containing term
        double idf = Math.log(N/df); 
        double tfidf = tf * idf;

        // Should I normalize before or after combining with pagerank?
        if(rankingType == RankingType.TF_IDF){
            score = tfidf;
        } else if(rankingType == RankingType.COMBINATION){
            score = prWeight * pagerank + (1 - prWeight) * tfidf;
        }

        // Normalize score
        if(normType == NormalizationType.NUMBER_OF_WORDS){
            score = score / index.docLengths.get(docID);
        }else if(normType == NormalizationType.EUCLIDEAN){
            score = score / index.euclidianLengths.get(docID);
            System.out.println("Score: " + score + " Euclidian length: " + index.euclidianLengths.get(docID));
        }
    }
    
    public PostingsEntry(int docID){
        this.docID = docID;
    }
    
    public String toString(){
        String s = Integer.toString(docID) + ":";
        s += offset.get(0);
        for(int i = 1; i < offset.size(); i++){
            s += "," + offset.get(i);
        }
        s += ";";
        return s;
    }

    public void merge(PostingsEntry pe){
        for(int i = 0; i < pe.offset.size(); i++){
            offset.add(pe.offset.get(i));
        }
    }
}

