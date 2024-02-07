/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
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

    public String toString(){
        String s = "";
        for(int i = 0; i < list.size(); i++){
            s += list.get(i).toString();
        }
        return s;
    }

    public void merge(PostingsList pl){
        int firstOfFirst = list.get(0).docID;
        int lastOfFirst = list.get(list.size()-1).docID;
        int firstOfSecond = pl.list.get(0).docID;
        int lastOfSecond = pl.list.get(pl.list.size()-1).docID;

        if(firstOfFirst < firstOfSecond){
            if(list.get(list.size()-1) == pl.list.get(0)){
                PostingsEntry pe = list.get(list.size()-1);
                pe.merge(pl.list.get(0));
                list.set(list.size()-1, pe);
                list.addAll(pl.list.subList(1, pl.list.size()));
            } else {
                list.addAll(pl.list);
            }
        } else {
            if(pl.list.get(pl.list.size()-1) == list.get(0)){
                PostingsEntry pe = pl.list.get(pl.list.size()-1);
                pe.merge(list.get(0));
                pl.list.set(pl.list.size()-1, pe);
                pl.list.addAll(list.subList(1, list.size()));
                list = pl.list;
            } else {
                list.addAll(pl.list);
            }
        }
        
    }
}

