/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The data file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        private long ptr;
        private String repr;

        //
        //  YOUR CODE HERE
        //

        public Entry(long ptr, String repr){
            this.ptr = ptr;
            this.repr = repr;
        }

    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
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


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ){
        //
        //  YOUR CODE HERE
        //
        String data = Long.toString(entry.ptr);
        if(data.length() < 6){
            for(int i = 0; i < 6-data.length(); i++){
                data = "0" + data;
            }
        }

        try {
            dataFile.seek( ptr ); 
            dictionaryFile.writeChars(data);
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) throws IOException {   
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //
        dictionaryFile.seek(ptr*2); // *2 because of char size
        System.out.println("ptr: " + (ptr*2));
        String dataPtr = "";
        for(int i = 0; i < 6; i++){
            try {
                dataPtr += Character.toString(dictionaryFile.readChar());
                dictionaryFile.seek(ptr*2 + i*2);
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        dataFile.seek(Integer.parseInt(dataPtr) * 2);

        String data = "";
        String next = "";
        while(true){
            try {
                next = Character.toString(dataFile.readChar());
                if(next == "\n") break;
                data += next;
                System.out.println("data: " + data + "next: " + next + "ptr: " + dataPtr + "*2");
            } catch ( IOException e ) {
                System.out.println("Crash in readEntry while loop");
                e.printStackTrace();
                break;
            }
        }
        System.out.println("data: " + data);
        return new Entry(ptr, data);
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for ( Map.Entry<Integer,String> entry : docNames.entrySet() ) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write( docInfoEntry.getBytes() );
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try ( BufferedReader br = new BufferedReader(freader) ) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put( new Integer(data[0]), data[1] );
                docLengths.put( new Integer(data[0]), new Integer(data[2]) );
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for(String token : index.keySet()){
                PostingsList pl = index.get(token);
                String stringRepresentation = pl.toString();
                free += writeData(stringRepresentation, free);

                Entry entry = new Entry(free, stringRepresentation);

                writeEntry(entry, (Math.abs(token.hashCode() % TABLESIZE/6) * 6));
            }
            
            
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
        
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        String repr = null;
        try{
            Entry entry = readEntry((Math.abs(token.hashCode() % TABLESIZE/6) * 6));    // crash here
            repr = entry.repr;
        }catch(IOException e){
            e.printStackTrace();
        }
        if(repr == null) return null;
        
        PostingsList pl = new PostingsList();
        String[] postings = repr.split(";");

        for(int i = 0; i < postings.length; i++){
            String[] data = postings[i].split(":");
            PostingsEntry pe = new PostingsEntry(Integer.parseInt(data[0]));
            String[] offsets = data[1].split(",");
            for(int j = 0; j < offsets.length; j++){
                pe.offset.add(Integer.parseInt(offsets[j]));
            }
            pl.add(pe);
        }

        return pl;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //

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
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
