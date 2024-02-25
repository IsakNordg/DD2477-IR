/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    /** The pagerank score saved as a hashtable. */
    private HashMap<String,Double> pagerank = new HashMap<String,Double>();


    public void readPageRank(String filename) throws FileNotFoundException{
        File pr = new File(filename);
        Scanner in = new Scanner(pr);
        
        while(in.hasNextLine()){
            String[] line = in.nextLine().split(" ");
            pagerank.put(line[0], Double.parseDouble(line[1]));
        }
    }

    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        // YOUR CODE HERE
        //
        
        System.out.println("Inserting token: " + token + " with docID: " + docID + " and offset: " + offset);

        // new token
        if(index.get(token) == null){
            PostingsList pl = new PostingsList();
            PostingsEntry pe = new PostingsEntry(docID);
            pe.offset.add(offset);
            pe.pagerank = pagerank.get(...);
            pl.add(pe);
            index.put(token, pl);
        }
        // existing token
        else{
            PostingsList pl = index.get(token);
            PostingsEntry pe = new PostingsEntry(docID);
            if(pl.get(pl.size()-1).docID != docID){
                pe.offset.add(offset);
                pe.pagerank = pagerank.get(...);
                pl.add(pe);
                index.put(token, pl);
            }else{
                pl.get(pl.size()-1).offset.add(offset);
            }
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
