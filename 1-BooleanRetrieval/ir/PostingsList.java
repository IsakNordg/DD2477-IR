/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;

public class PostingsList {
    
    /** The postings list */
    public ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    public void add(PostingsEntry pe){
        list.add(pe);
    }

    public boolean hasEntry(PostingsEntry pe){
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).docID == pe.docID) return true;
        }
        return false;
    }

    public PostingsEntry getFromDocID(int docID){
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).docID == docID) return list.get(i);
        }
        return null;
    }
}

