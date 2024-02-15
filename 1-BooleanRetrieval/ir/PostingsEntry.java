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

    public void computeScore(Query query, RankingType rankingType, NormalizationType normType, Index index){
        if(query.queryterm.size() == 1){
            // # occurences of term in document
            int tf_dt = this.offset.size();
            
            // # documents in the corpus
            int N = index.docNames.size();

            // # documents in the corpus which contain the term
            int df_t = index.getPostings(query.queryterm.get(0).term).size();

            // # of words in d
            int len_d = index.docLengths.get(docID);

            this.score = tf_dt*(Math.log(N/df_t)/len_d);
            System.out.println("Score: " + this.score);
        }else{
            System.out.println("Not implemented");
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

