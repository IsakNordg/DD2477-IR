/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    private boolean printDCG = true;

    Double prWeight = 0.98;

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;

        readAverageRelevanceRatings();
    }

    private HashMap<String, Integer> averageRatings = new HashMap<String, Integer>();

    private void readAverageRelevanceRatings(){
        try {
            File f = new File("C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\average_relevance_filtered.txt");
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                averageRatings.put(parts[0], Integer.parseInt(parts[1]));
            }
            br.close();
        } catch (IOException e){
            System.out.println("Error reading file");
        }
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //

        // System.out.println("Query Size: " + query.size());
        // System.out.println("Query: " + query.toString());

        if(query.size() == 1 && queryType == QueryType.INTERSECTION_QUERY){
            return index.getPostings(query.queryterm.get(0).term);
        }else{
            PostingsList result = new PostingsList();
            PostingsList pl = index.getPostings(query.queryterm.get(0).term);
            
            // Get intersection of all words
            for(int i = 1; i < query.size(); i++){
                pl = intersect(pl, index.getPostings(query.queryterm.get(i).term));
            }

            if(queryType == QueryType.RANKED_QUERY){
                return RankedSearch(query, queryType, rankingType, normType);
            } else if(queryType == QueryType.INTERSECTION_QUERY){
                return pl;
            } else if(queryType == QueryType.PHRASE_QUERY){
                PostingsList firstList = index.getPostings(query.queryterm.get(0).term);
                if(query.queryterm.size() == 1){
                    // System.out.println(firstList.toString());
                    return firstList;
                }else{
                    boolean match = false;
                    
                    ArrayList<PostingsList> plList = new ArrayList<PostingsList>();
                    for(int k = 1; k < query.queryterm.size(); k++){
                        plList.add(index.getPostings(query.queryterm.get(k).term));
                    }

                    // Each interesting PostingsEntry (meaning each document)
                    for(int i = 0; i < pl.size(); i++){
                        int curDoc = pl.get(i).docID;
                        // each offset in the current PostingEntry
                        for(int j = 0; j < pl.get(i).offset.size(); j++){
                            int curOffset = (int) pl.get(i).offset.get(j);
                            match = true;
                            for(int k = 0; k < plList.size(); k++){
                                PostingsList plNext = plList.get(k);
                                PostingsEntry peNext = plNext.getFromDocID(curDoc);
                                if(!peNext.offset.contains(curOffset+1)){
                                    match = false;
                                    break;
                                }
                                curOffset++;
                            }
                            if(match){
                                result.add(pl.get(i));
                                break;
                            }
                        }
                    }
                    return result;
                }
            }
        }
        return null;
    }


    private PostingsList RankedSearch( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        PostingsList result = new PostingsList();
    
        for(int i = 0; i < query.queryterm.size(); i++){
            PostingsList pl = index.getPostings(query.queryterm.get(i).term);
            PostingsList tmppl = new PostingsList();
            
            int N = index.docNames.size();            // Number of documents in collection
            int df = index.getPostings(query.queryterm.get(i).term).size();    // Number of documents containing term
            double idf = Math.log(N/df);          // Inverse document frequency
            
            for(int j = 0; j < pl.size(); j++){
                PostingsEntry pe = pl.get(j);
                pe.computeScore(idf, rankingType, normType, index, prWeight);
                pe.score = pe.score * query.queryterm.get(i).weight;
                tmppl.add(pe);
            }
            if(i == 0){
                result = tmppl;
            }else{
                result = union(result, tmppl);
            }
        }

        // Normalize score
        for(int i = 0; i < result.size(); i++){
            double score = result.get(i).score;
            if(normType == NormalizationType.NUMBER_OF_WORDS){
                result.get(i).score = score / index.docLengths.get( result.get(i).docID );
            }else if(normType == NormalizationType.EUCLIDEAN){
                result.get(i).score = score / index.euclidianLengths.get( result.get(i).docID );
            }
        }

        Collections.sort(result.list);

        if(printDCG){
            double DCG = 0;
            int i = 0;
            int max = Math.min(50, result.size());
            for(PostingsEntry pe : result.list){
                String docName = index.docNames.get(pe.docID);
                String[] parts = docName.split("\\\\");
                docName = parts[parts.length - 1];

                Integer rating = averageRatings.get(docName);

                if(query.docsSelected.contains(pe.docID)){
                    continue;
                }

                if(rating == null){
                    rating = 0;
                }
                
                if(i >= max){
                    break;
                }

                int rank = i + 1;
                double log = Math.log(rank + 1) / Math.log(2);
                DCG += rating / log;

                i++;
            }
            System.out.println("DCG: " + DCG);
        }

        return result;
    }

    private PostingsList intersect(PostingsList pl1, PostingsList pl2){
        PostingsList pl = new PostingsList();
        int i = 0, j = 0;
        
        while(i < pl1.size() && j < pl2.size()){
            if(pl1.get(i).docID == pl2.get(j).docID){
                pl.add(pl1.get(i));
                i++; j++;
            }else if(pl1.get(i).docID < pl2.get(j).docID){
                i++;
            }else{
                j++;
            }
        }
        return pl;
    }

    private PostingsList union(PostingsList pl1, PostingsList pl2){
        PostingsList pl = new PostingsList();
        int i = 0, j = 0;
        
        while(i < pl1.size() && j < pl2.size()){
            if(pl1.get(i).docID == pl2.get(j).docID){
                PostingsEntry pe = pl1.get(i);
                // pe.merge(pl2.get(j)); // This is not needed

                pe.score = pl1.get(i).score + pl2.get(j).score;
                pl.add(pe);

                i++; j++;
            }else if(pl1.get(i).docID < pl2.get(j).docID){
                pl.add(pl1.get(i));
                i++;
            }else{
                pl.add(pl2.get(j));
                j++;
            }
        }
        while(i < pl1.size()){
            pl.add(pl1.get(i));
            i++;
        }
        while(j < pl2.size()){
            pl.add(pl2.get(j));
            j++;
        }
        return pl;
        
    }

}

    