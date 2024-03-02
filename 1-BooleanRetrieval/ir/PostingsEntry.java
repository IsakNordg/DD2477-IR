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

    public void computeScore(String term, RankingType rankingType, NormalizationType normType, Index index, double prWeight, int numberOfWords){
        // # occurences of term in document
        int tf_dt = this.offset.size();
        // # documents in the corpus
        int N = index.docNames.size();
        // # documents in the corpus which contain the term
        int df_t = index.getPostings(term).size();
        
        Double len_d;
        if(normType == NormalizationType.NUMBER_OF_WORDS){
            len_d = Double.valueOf( index.docLengths.get(docID) );
        }else if(normType == NormalizationType.EUCLIDEAN){
            len_d = index.euclidianLengths.get(docID);
        }else{
            System.out.println("Normalization type not recognized. Using default (NUMBER_OF_WORDS)");
            len_d = Double.valueOf( index.docLengths.get(docID) );
        }

        if(rankingType == RankingType.TF_IDF){
            Double tf_idf = tf_dt*(Math.log(N/df_t)/len_d);
            // score = ((number of occurences of term in document) / (total number of words in document)) * tf_idf
            this.score = tf_idf/len_d;
        }else if(rankingType == RankingType.PAGERANK){
            this.score = pagerank;
        }else if(rankingType == RankingType.COMBINATION){
            this.score = tf_dt*(Math.log(N/df_t)/len_d)*prWeight + pagerank * (1-prWeight);
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

