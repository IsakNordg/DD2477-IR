/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


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
    // public static final long TABLESIZE = 611953L;
    public static final long TABLESIZE = 7000029L;

    /** The maximum length of the size (number of characters) of a data entry. */
    public static final int MAXLENGTH = 10;  // 6 is an assumption

    /** The length of a hash saved to check for collisions */
    public static final int HASHLENGTH = 19;

    /** The length of the data entry */
    public static final int MAXDATAPTRLENGTH = 16;  // 16 is an assumption

    /** The size of the dictionary entry */
    public static final int ENTRYSIZE = MAXLENGTH + MAXDATAPTRLENGTH + HASHLENGTH;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    int collisions = 0;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    private HashMap<Integer,Double> pagerank = new HashMap<Integer,Double>();

    // public HashMap<Integer,Double> euclidianLengths = new HashMap<Integer,Double>();

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        protected String token;
        protected long ptr;
        protected String repr;
        protected int size;

        //
        //  YOUR CODE HERE
        //

        public Entry(String token, long ptr, String repr, int size){
            this.token = token; // the token
            this.ptr = ptr; // pointer to the data file
            this.repr = repr; // string representation of the postings list
            this.size = size;   // size of the string representation
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
    void writeEntry( Entry entry ) throws NoSuchAlgorithmException{
        
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
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     * @throws NoSuchAlgorithmException 
     */
    Entry readEntry( String token ) throws IOException, NoSuchAlgorithmException {   
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
                return new Entry(token, dataPtr, readData(dataPtr, (int)size), (int)size);
            }else if(count == 50){
                return null;
            }else{
                ptr = getDictPtr(getChecksum(hash));
                hash = getChecksum(hash);
                count++;
            }
        }
        
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    public void writeDocInfo() throws IOException {
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
    protected void readDocInfo() throws IOException {
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
     * @throws NoSuchAlgorithmException 
     */
    public void writeIndex() {
        try {
            dataFile.setLength(0);
            dictionaryFile.setLength(0);

            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for(String token : index.keySet()){
                PostingsList pl = index.get(token);
                String stringRepresentation = pl.toString();
                int size = writeData(stringRepresentation, free);

                Entry entry = new Entry(token, free, stringRepresentation, size);

                free += size;

                writeEntry(entry);  
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
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
        try{
            Entry entry = readEntry(token);

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
                    pe.pagerank = pagerank.get(docID);
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
            pe.pagerank = pagerank.get(docID);
            pl.add(pe);
            index.put(token, pl);
        }
        
        // existing token
        else{
            PostingsList pl = index.get(token);
            PostingsEntry pe = new PostingsEntry(docID);
            if(pl.get(pl.size()-1).docID != docID){
                pe.offset.add(offset);
                pe.pagerank = pagerank.get(docID);
                pl.add(pe);
                index.put(token, pl);
            }else{
                pl.get(pl.size()-1).offset.add(offset);
                pe.pagerank = pagerank.get(docID);
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

    protected static long getDictPtr(String token) throws NoSuchAlgorithmException{
        return Math.abs((getSHA(token) % TABLESIZE) * ENTRYSIZE);
    }

    public static long getSHA(String input) throws NoSuchAlgorithmException
    {
        // Static getInstance method is called with hashing SHA
        MessageDigest md = MessageDigest.getInstance("SHA-256");
 
        // digest() method called
        // to calculate message digest of an input
        // and return array of byte
        

        long hash = 0;
        ByteBuffer wrapped = ByteBuffer.wrap(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        hash = wrapped.getLong();
        return hash;
    }

    protected static String getChecksum(String token) throws NoSuchAlgorithmException{

        // reverse token
        String reversed = new StringBuilder(token).reverse().toString();

        String hash = Long.toString(Math.abs(getSHA(reversed)));
        
        // pad hash
        while(hash.length() < HASHLENGTH){
            hash = "0" + hash;
        }

        return hash;
    }

    public void readPageRank(String filename){
        System.out.println("Reading pagerank...");
        File pr = new File(filename);
        Scanner in;
        try {
            in = new Scanner(pr);

            int i = 0;
            while(in.hasNextLine()){
                String[] line = in.nextLine().split(" ");
                pagerank.put(i, Double.parseDouble(line[1]));
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void readEuclideanIndex(String filename){
        System.out.println("Reading euclidean index...");
        File el = new File(filename);
        Scanner in;
        try {
            in = new Scanner(el);

            while(in.hasNextLine()){
                String[] line = in.nextLine().split(" ");
                euclidianLengths.put(Integer.parseInt(line[0]), Double.parseDouble(line[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void createAndWriteEuclideanIndex(String filename){
        System.out.println("Creating euclidean index...");
        // for every document in the index, calculate the euclidean length

        int N = docNames.size();

        HashMap<String,Integer> tokenToDf = new HashMap<String,Integer>();

        for (Map.Entry<String,PostingsList> entry : index.entrySet()) {
            String token = entry.getKey();
            tokenToDf.put(token, getPostings(token).size());
        }

        for(Integer docID : docWords.keySet()){
            double euclidianLength = 0;
            for(String token : docWords.get(docID).keySet()){
                double tf = docWords.get(docID).get(token);
                double idf = Math.log(N / tokenToDf.get(token));

                euclidianLength += Math.pow(tf * idf, 2);
            }
            euclidianLengths.put(docID, Math.sqrt(euclidianLength));
        }

        writeEuclideanIndex(filename);
    }

    public void writeEuclideanIndex(String filename){
        System.out.println("Writing euclidean index...");
        try {
            FileOutputStream fout = new FileOutputStream( filename );
            for ( Map.Entry<Integer,Double> entry : euclidianLengths.entrySet() ) {
                Integer key = entry.getKey();
                String euclidianIndexEntry = key + " " + entry.getValue() + "\n";
                fout.write( euclidianIndexEntry.getBytes() );
            }
            fout.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
