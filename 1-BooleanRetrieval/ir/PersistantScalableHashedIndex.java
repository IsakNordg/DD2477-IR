package ir;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PersistantScalableHashedIndex extends PersistentHashedIndex{
    
    private int dc = 0; // total count of documents


    /** The dictionary hash table on disk can fit this many entries. */
    // public static final long TABLESIZE = 611953L;
    public static final long TABLESIZE = 100003L;


    public static final long MAXTOKENS = 100000L;
    private RandomAccessFile termFile;

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

        if(index.keySet().size() % MAXTOKENS == 0){
            System.out.println("Writing index to disk...");
            writeIndex();
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
        }

        // new token
        if(index.get(token) == null){
            PostingsList pl = new PostingsList();
            PostingsEntry pe = new PostingsEntry(docID);
            pe.offset.add(offset);
            pl.add(pe);
            index.put(token, pl);

            // add to 
            try {
                termFile.writeBytes(token + "\n");
            } catch ( IOException e ) {
                e.printStackTrace();
            }
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
     *  Merge all the indexes into one
     */
    public void cleanup() {
        index.clear();
        try {
            dictionaryFile.close();
            dataFile.close();
            termFile.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            RandomAccessFile dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            RandomAccessFile dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
            RandomAccessFile termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME, "rw" );

            for(int i=0; i<dc; i++){
                RandomAccessFile dictionaryFileTemp = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + i, "rw" );
                RandomAccessFile dataFileTemp = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + i, "rw" );
                RandomAccessFile termFileTemp = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + i, "rw" );

                String term;
                while((term = termFileTemp.readLine()) != null){
                    // TODO: merge the indexes
                }
                dictionaryFileTemp.close();
                dataFileTemp.close();
                termFileTemp.close();
            }
            dictionaryFile.close();
            dataFile.close();
            termFile.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }
}
