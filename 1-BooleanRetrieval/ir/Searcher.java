/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;
    
    /** Constructor */
    public Searcher( Index index, KGramIndex kgIndex ) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType, NormalizationType normType ) { 
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //

        if(query.size() == 1 && queryType == QueryType.INTERSECTION_QUERY){
            return index.getPostings(query.queryterm.get(0).term);
        }else{
            PostingsList result = new PostingsList();
            PostingsList pl = index.getPostings(query.queryterm.get(0).term);
            
            // Get intersection of all words
            for(int i = 1; i < query.size(); i++){
                pl = intersect(pl, index.getPostings(query.queryterm.get(i).term));
            }
        

            if(queryType == QueryType.INTERSECTION_QUERY){
                return pl;
            }
            else if(queryType == QueryType.PHRASE_QUERY){
                PostingsList firstList = index.getPostings(query.queryterm.get(0).term);
                if(query.queryterm.size() == 1){
                    return firstList;
                }else{
                    boolean match = false;
                    
                    ArrayList<PostingsList> plList = new ArrayList<PostingsList>();
                    for(int k = 1; k < query.queryterm.size(); k++){
                        plList.add(index.getPostings(query.queryterm.get(k).term));
                    }

                    // Each interesting PostingsEntry
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

}

    