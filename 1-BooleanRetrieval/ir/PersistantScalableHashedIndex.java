package ir;

import static ir.PersistentHashedIndex.getChecksum;
import static ir.PersistentHashedIndex.getDictPtr;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.StringTokenizer;

public class PersistantScalableHashedIndex extends PersistentHashedIndex{
    
    private int dc = 0; // total count of documents


    /** The dictionary hash table on disk can fit this many entries. */
    // public static final long TABLESIZE = 611953L;
    public static final long TABLESIZE = 100003L;

    /** The max amout of tokens that are indexed in one file */
    public static final long MAXTOKENS = 100000L;

    private RandomAccessFile termFile;    

    ArrayList<String> mergeQueue = new ArrayList<String>();

    public PersistantScalableHashedIndex(){
        super();
        try {
            termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {

        // switch to next index file
        if(index.keySet().size() % MAXTOKENS == 0 && index.keySet().size() != 0){
            System.out.println("Writing index to disk...");
            writeIndex();
            writeTermFile();
            index.clear();
            try {
                dictionaryFile.close();
                dataFile.close();
                termFile.close();
                
                dc++;
                
                dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + dc, "rw" );
                dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + dc, "rw" );
                termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + dc, "rw" );    
            } catch ( IOException e ) {
                e.printStackTrace();
            }
            if(dc > 1){
                // Merge indexes
                tryMerge();
            }
        }

        // new token
        if(index.get(token) == null){
            PostingsList pl = new PostingsList();
            PostingsEntry pe = new PostingsEntry(docID);
            pe.offset.add(offset);
            pl.add(pe);
            index.put(token, pl);
        }
        // existing token
        else{
            PostingsList pl = index.get(token);
            PostingsEntry pe = new PostingsEntry(docID);
            if(pl.get(pl.size()-1).docID != docID){
                pe.offset.add(offset);
                pl.add(pe);
                index.put(token, pl);
            }else{
                pl.get(pl.size()-1).offset.add(offset);
            }
        }
    }

    /**
     *  Write the terms in the index to disk in a sorted order
     * 
     */
    protected void writeTermFile(){
        ArrayList<String> terms = new ArrayList<String>(index.keySet());
        Collections.sort(terms);
        for(String term : terms){
            try {
                termFile.writeBytes(term + "\n");
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Merge all the indexes into one
     */
    public void cleanup() {
        
    }

    public void add(String index){
        mergeQueue.add(index);
    }

    public void tryMerge(){
        while(mergeQueue.size() > 1){
            String i1 = mergeQueue.get(0);
            mergeQueue.remove(0);
            String i2 = mergeQueue.get(0);
            mergeQueue.remove(0);
            Merger m = new Merger(i1, i2);
        }
    }


    private class Merger extends Thread{
        String i1, i2;


        Merger(String i1, String i2){
            this.i1 = i1;
            this.i2 = i2;
            start();
        }

        public void run(){
            try{
                RandomAccessFile dict1 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + i1, "r" );
                RandomAccessFile data1 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + i1, "r" );
                RandomAccessFile term1 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + i1, "r" );

                RandomAccessFile dict2 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + i2, "r" );
                RandomAccessFile data2 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + i2, "r" );
                RandomAccessFile term2 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + i2, "r" );

                RandomAccessFile dict3 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + (i1 + i2), "rw" );
                RandomAccessFile data3 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + (i1 + i2), "rw" );
                RandomAccessFile term3 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + (i1 + i2), "rw" );
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    
    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token , RandomAccessFile dictionaryFile, RandomAccessFile dataFile) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        try{
            Entry entry = readEntry(token, dictionaryFile, dataFile);

            if(entry != null){
                PostingsList pl = new PostingsList();
                StringTokenizer st = new StringTokenizer(entry.repr, ":;");
                while(st.hasMoreTokens()){
                    int docID = Integer.parseInt(st.nextToken());
                    PostingsEntry pe = new PostingsEntry(docID);
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), ",");
                    while(st2.hasMoreTokens()){
                        pe.offset.add(Integer.parseInt(st2.nextToken()));
                    }
                    pl.add(pe);
                }
                return pl;
            }else{
                return null;
            }
        }catch(IOException e){
            e.printStackTrace();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }

    Entry readEntry( String token, RandomAccessFile dictionaryFile, RandomAccessFile dataFile ) throws IOException, NoSuchAlgorithmException {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
        long ptr = getDictPtr(token);
        String hash = token;

        int count = 0;
        while(true){
            dictionaryFile.seek( ptr );
            byte[] read = new byte[ENTRYSIZE];
            dictionaryFile.readFully( read );
            String dictEntry = new String(read);

            long dataPtr = Long.parseLong(dictEntry.substring(0, MAXDATAPTRLENGTH));
            long size = Long.parseLong(dictEntry.substring(MAXDATAPTRLENGTH, MAXDATAPTRLENGTH + MAXLENGTH));
            String checksum = dictEntry.substring(MAXDATAPTRLENGTH + MAXLENGTH, MAXDATAPTRLENGTH + MAXLENGTH + HASHLENGTH);

            if(checksum.equals(getChecksum(token))){
                return new Entry(token, dataPtr, readData(dataPtr, (int)size, dataFile), (int)size);
            }else if(count == 50){
                return null;
            }else{
                ptr = getDictPtr(getChecksum(hash));
                hash = getChecksum(hash);
                count++;
            }
        }
        
    }
    
    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size , RandomAccessFile dataFile) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }
}
