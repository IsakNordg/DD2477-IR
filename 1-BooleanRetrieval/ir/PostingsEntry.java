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
    
    // offset is a list of integers, where each integer is the position of the term in the document
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

    public void computeScore(double idf, RankingType rankingType, NormalizationType normType, Index index, double prWeight){
        
        if(rankingType == RankingType.PAGERANK){
            this.score = prWeight * pagerank;
            return;
        }
        
        int tf = offset.size();                    // Number of occurrences of term in document
        double tfidf = tf * idf;

        if(rankingType == RankingType.TF_IDF){
            score = tfidf;
        } else if(rankingType == RankingType.COMBINATION){
            score = prWeight * pagerank + tfidf;
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

