/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {

        // Results contain all documents
        // docIsRelevant contains the relevant documents
        //
        // From the relevant documents, we can extract the terms and add them to the query

        for(QueryTerm term : queryterm){
            term.weight = term.weight * alpha;
        }

        HashMap<String,Double> wordCount = new HashMap<String,Double>();
        int numberOfRelevantDocuments = 0;
        for(int i = 0; i < docIsRelevant.length; i++){   // each pe is a document which was returned by the search engine
            if(docIsRelevant[i]){
                System.out.println(i + " is relevant");
                PostingsEntry pe = results.get(i);
                // Add the terms of the relevant documents to the query
                wordCount = getTermsFromDoc(engine.index.docNames.get(pe.docID), wordCount);
                numberOfRelevantDocuments++;
            }
        }

        // normalize the wordCount
        for(String term : wordCount.keySet()){
            wordCount.put(term, wordCount.get(term) / numberOfRelevantDocuments);
        }

        // Add the terms to the query
        for(String term : wordCount.keySet()){
            queryterm.add(new QueryTerm(term, wordCount.get(term) * beta));
        }
    }

    private HashMap<String,Double> getTermsFromDoc(String docName, HashMap<String,Double> wordCount){
        File f = new File(docName);
        String patterns_file = "C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\patterns.txt";

        try {
            Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
            Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );

            while ( tok.hasMoreTokens() ) {
                String token = tok.nextToken();

                if(wordCount.containsKey(token)){
                    wordCount.put(token, wordCount.get(token) + 1);
                }else{
                    wordCount.put(token, 1.0);
                }
            }
            reader.close();
            return wordCount;
        } catch (IOException e){
            System.out.println("Error reading file");
            return null;
        }
    } 

    public String toString(){
        String s = "";
        for(int i = 0; i < queryterm.size(); i++){
            s += queryterm.get(i).term + "(" + queryterm.get(i).weight + ") ";
        }
        return s;
    }
}


