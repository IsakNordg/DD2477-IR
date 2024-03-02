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

    public void computeScore(Query query, RankingType rankingType, NormalizationType normType, Index index, double prWeight){
        
        if(rankingType == RankingType.PAGERANK){
            this.score = pagerank;
            return;
        }

        int N = index.docNames.size();

        // compute tf-idf
        for(int i = 0; i < query.queryterm.size(); i++){
            int df = index.getPostings(query.queryterm.get(i).term).size();
            System.out.println("df: " + df + " N: " + N + " offset: " + offset.size() + " term: " + query.queryterm.get(i).term);
            Double idf = Math.log(N/df);
            this.score += idf * offset.size();
        }
        
        // normalize score 
        if(normType == NormalizationType.NUMBER_OF_WORDS){

        }else if(normType == NormalizationType.EUCLIDEAN){
            this.score = this.score / index.euclidianLengths.get(docID);
        }

        // combine with pagerank
        if(rankingType == RankingType.COMBINATION){
            this.score = this.score * prWeight + pagerank * (1-prWeight);
        }

        return;
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

