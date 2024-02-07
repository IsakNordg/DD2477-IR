package ir;

import static ir.PersistentHashedIndex.getChecksum;
import static ir.PersistentHashedIndex.getDictPtr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.StringTokenizer;

public class PersistantScalableHashedIndex extends PersistentHashedIndex{
    
    private int dc = 0; // total count of documents, used for naming new index files

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 7000003L;
    // public static final long TABLESIZE = 100003L;

    /** The max amout of tokens that are indexed in one file */
    public static final long MAXTOKENS = 100000L;

    private RandomAccessFile termFile;   

    ArrayList<String> mergeQueue = new ArrayList<String>();

    ArrayList<Merger> mergers = new ArrayList<Merger>();

    public PersistantScalableHashedIndex(){
        
        super();
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + "0", "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + "0", "rw" );
            termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + "0", "rw" );
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
                free = 0;

                mergeQueue.add(Integer.toString(dc));

                dc++;

                dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + dc, "rw" );
                dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + dc, "rw" );
                termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + dc, "rw" );
            } catch ( IOException e ) {
                e.printStackTrace();
            }

            collisions = 0;
            if(dc > 1){
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

        outerloop:
        while(true){
            tryMerge();
            for(Merger m : mergers){
                if(m.isAlive()){
                    System.out.println(mergeQueue.toString());
                    System.out.println("Waiting for " + m.i1 + " and " + m.i2 + " to finish...");
                    try{
                        m.join();
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
            System.out.println(mergeQueue.toString());
            
            if(mergeQueue.size() < 2){
                for(Merger m : mergers){
                    if(m.isAlive()){
                        continue outerloop;
                    }
                }
                break;
            }
        }

        // rename final file
        try{
            dictionaryFile.close();
            dataFile.close();
            termFile.close();

            String finalIndex = mergeQueue.get(0);

            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + finalIndex, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + finalIndex, "rw" );
            termFile = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + finalIndex, "rw" );

            mergeQueue.remove(0);
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Cleanup done (all done)");
    }

    public void tryMerge(){;
            while(mergeQueue.size() > 1){
                Collections.sort(mergeQueue);
                String i1 = mergeQueue.get(0);
                mergeQueue.remove(0);
                String i2 = mergeQueue.get(0);
                mergeQueue.remove(0);

                System.out.println("Trying to merge " + i1 + " and " + i2 + "...");

                Merger m = new Merger(i1, i2);
                mergers.add(m);
                m.start();
            }
    }


    private class Merger extends Thread{
        String i1, i2;


        Merger(String i1, String i2){
            this.i1 = i1;
            this.i2 = i2;
        }

        public void run(){
            // System.out.println("Merging " + i1 + " and " + i2 + "... (Thread " + Thread.currentThread().getName() + ")");
            try{
                RandomAccessFile dict1 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + i1, "r" );
                RandomAccessFile data1 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + i1, "r" );
                RandomAccessFile term1 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + i1, "r" );

                RandomAccessFile dict2 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + i2, "r" );
                RandomAccessFile data2 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + i2, "r" );
                RandomAccessFile term2 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + i2, "r" );

                RandomAccessFile dict3 = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME + "0" + (i1 + i2), "rw" );
                RandomAccessFile data3 = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + "0" + (i1 + i2), "rw" );
                RandomAccessFile term3 = new RandomAccessFile( INDEXDIR + "/" + TERMS_FNAME + "0" + (i1 + i2), "rw" );
            
                String t1 = term1.readLine();
                String t2 = term2.readLine();
                String curTerm;
                int sizeMerger;
                long freeMerger = 0;
                while(true){
                    // System.out.println("t1: " + t1 + " t2: " + t2 + " Thread: " + Thread.currentThread().getName());
                    if(t1 == null && t2 == null){
                        System.out.println("Merging of " + i1 + " and " + i2 + "done. Breaking");
                        break;
                    }

                    PostingsList pl1 = null, pl2 = null;
                    if(t1 == null){
                        curTerm = t2;
                        t2 = term2.readLine();
                        pl2 = getPostings(curTerm, dict2, data2);
                        term3.writeBytes(curTerm + "\n");
                    }else if(t2 == null){
                        curTerm = t1;
                        t1 = term1.readLine();
                        pl1 = getPostings(curTerm, dict1, data1);
                        term3.writeBytes(curTerm + "\n");
                    }else{
                        if(t1.compareTo(t2) < 0){
                            curTerm = t1;
                            t1 = term1.readLine();
                            pl1 = getPostings(curTerm, dict1, data1);
                        }else if(t1.compareTo(t2) > 0){
                            curTerm = t2;
                            t2 = term2.readLine();
                            pl2 = getPostings(curTerm, dict2, data2);
                        }else{
                            curTerm = t1;
                            t1 = term1.readLine();
                            t2 = term2.readLine();
                            pl1 = getPostings(curTerm, dict1, data1);
                            pl2 = getPostings(curTerm, dict2, data2);
                        }
                        term3.writeBytes(curTerm + "\n");
                    }
                    
                    if(pl1 == null && pl2 == null){
                        continue;
                    }

                    Entry entry;
                    if(pl1 == null){
                        sizeMerger = writeData(pl2.toString(), freeMerger, data3);
                        entry = new Entry(curTerm, freeMerger, pl2.toString(), sizeMerger);
                    }else if(pl2 == null){
                        sizeMerger = writeData(pl1.toString(), freeMerger, data3);
                        entry = new Entry(curTerm, freeMerger, pl1.toString(), sizeMerger);
                    }else{
                        pl1.merge(pl2);
                        sizeMerger = writeData(pl1.toString(), freeMerger, data3);
                        entry = new Entry(curTerm, freeMerger, pl1.toString(), sizeMerger);
                    }
                    
                    freeMerger += sizeMerger;
                    writeEntry(entry, dict3);
                }
                dict1.close();
                data1.close();
                term1.close();

                dict2.close();
                data2.close();
                term2.close();
                
                dict3.close();
                data3.close();
                term3.close();

                mergeQueue.add("0" + i1 + i2);
            }
            catch(IOException e){
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, RandomAccessFile dictionaryFile ) throws NoSuchAlgorithmException{
        
        // pad data
        String data = Long.toString(entry.ptr);
        while(data.length() < MAXDATAPTRLENGTH){
            data = "0" + data;
        }
        if(data.length() > MAXDATAPTRLENGTH){
            System.out.println("Data length too long: " + data.length() + " " + data);
        }

        // pad size
        String size = Integer.toString(entry.size);
        while(size.length() < MAXLENGTH){   
            size = "0" + size;
        }

        String checksum = getChecksum(entry.token);

        long ptr = getDictPtr(entry.token);
        String hash = entry.token;

        try {
            // check for collision
            while(true){
                if(dictionaryFile.length() > ptr + ENTRYSIZE){
                    dictionaryFile.seek( ptr );
                    byte[] read = new byte[ENTRYSIZE];
                    dictionaryFile.readFully( read );
                    String dictEntry = new String(read);
                    if(dictEntry.matches(".*\\d.*")){
                        ptr = getDictPtr(getChecksum(hash));
                        hash = getChecksum(hash);
                        collisions++;
                        // System.out.println("Collision! " + hash + " " + ptr + " " + collisions);
                    }else{
                        break;
                    }
                }else{
                    break; 
                }
            }

            // write
            dictionaryFile.seek( ptr ); 
            byte[] dataToWrite = data.getBytes();
            dictionaryFile.write( dataToWrite );
            byte[] sizeToWrite = size.getBytes();
            dictionaryFile.write( sizeToWrite );
            byte[] checksumToWrite = checksum.getBytes();
            dictionaryFile.write( checksumToWrite );

        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr, RandomAccessFile dataFile) {
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

    
    public PostingsList getPostings( String token){
        return getPostings( token, dictionaryFile, dataFile );
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
                    StringTokenizer st2 = new StringTokenizer(st.nextToken(), ","); // crash here
                    while(st2.hasMoreTokens()){
                        pe.offset.add(Integer.parseInt(st2.nextToken()));
                    }
                    pl.add(pe);
                }
                // System.out.println("Returning " + pl.toString() + " for " + token);
                return pl;
            }else{
                return null;
            }
        }catch(IOException e){
            System.out.println(e.getMessage());
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
            // System.out.println("Reading entry for " + token + " at " + ptr + " (Thread " + Thread.currentThread().getName() + ")");
            // System.out.println(dc);
            dictionaryFile.seek( ptr );
            byte[] read = new byte[ENTRYSIZE];
            dictionaryFile.readFully( read );
            String dictEntry = new String(read);
            if(!dictEntry.matches(".*[0-9].*")){
                // System.out.println("Entry " + token + " not found");
                return null;
            }

            // System.out.println("token: " + token + " DictEntry: " + dictEntry + " length: " + dictEntry.length());
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
