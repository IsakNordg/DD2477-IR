/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;

/**
 *  This is the main class for the search engine.
 */
public class Engine {

    /** The inverted index. */
    //Index index = new HashedIndex();
    // Assignment 1.7: Comment the line above and uncomment the next line
    Index index = new PersistentHashedIndex();

    /** The indexer creating the search index. */
    Indexer indexer;

    /** The searcher used to search the index. */
    Searcher searcher;

    /** K-gram index */
    // KGramIndex kgIndex = null;
    // Assignment 3: Comment the line above and uncomment the next line
    KGramIndex kgIndex = new KGramIndex(2);

    /** Spell checker */
    SpellChecker speller;
    // Assignment 3: Comment the line above and uncomment the next line
    // SpellChecker = new SpellChecker( index, kgIndex );
    
    /** The engine GUI. */
    SearchGUI gui;

    /** Directories that should be indexed. */
    ArrayList<String> dirNames = new ArrayList<String>();

    /** Lock to prevent simultaneous access to the index. */
    Object indexLock = new Object();

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file = null;

    /** The file containing the logo. */
    String pic_file = "";

    /** The file containing the pageranks. */
    String rank_file = "";

    //public static String euclidean_File = "euclideanIndex.txt";
    String euclidean_File = "./index/euclideanIndex.txt";

    /** For persistent indexes, we might not need to do any indexing. */
    boolean is_indexing = true;


    boolean euclidianExists;


    /* ----------------------------------------------- */


    /**  
     *   Constructor. 
     *   Indexes all chosen directories and files
     */
    public Engine( String[] args ) {
        decodeArgs( args );
        
        index.readPageRank(rank_file);

        indexer = new Indexer( index, kgIndex, patterns_file );
        searcher = new Searcher( index, kgIndex );
        gui = new SearchGUI( this );
        gui.init();
        /* 
         *   Calls the indexer to index the chosen directory structure.
         *   Access to the index is synchronized since we don't want to 
         *   search at the same time we're indexing new files (this might 
         *   corrupt the index).
         */
        if (is_indexing) {
            synchronized ( indexLock ) {
                gui.displayInfoText( "Indexing, please wait..." );
                long startTime = System.currentTimeMillis();

                for ( int i=0; i<dirNames.size(); i++ ) {
                    File dokDir = new File( dirNames.get( i ));
                    indexer.processFiles( dokDir, is_indexing, euclidianExists );
                }

                // check if euclidean index exists
                File euclideanIndex = new File(euclidean_File);
                if (euclideanIndex.exists()) {
                    index.readEuclideanIndex(euclidean_File);
                } else {
                    index.createAndWriteEuclideanIndex(euclidean_File);
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                gui.displayInfoText( String.format( "Indexing done in %.1f seconds.", elapsedTime/1000.0 ));
                index.cleanup();

                if(kgIndex != null){
                    System.out.println("Number of words containing \"ve\": " + kgIndex.getPostings("ve").size() );
                    System.out.println("Number of words containing \"th\" and \"he\": " + 
                                        kgIndex.intersect(kgIndex.getPostings("th"), kgIndex.getPostings("he")).size() );
                }
            }
        } else {
            gui.displayInfoText( "Index is loaded from disk" );
            // check if euclidean index exists
            File euclideanIndex = new File(euclidean_File);
            if (euclideanIndex.exists()) {
                index.readEuclideanIndex(euclidean_File);
            } else {
                System.out.println("Euclidean index does not exist. Please re-index.");
            }
        }
    }


    /* ----------------------------------------------- */

    /**
     *   Decodes the command line arguments.
     */
    private void decodeArgs( String[] args ) {
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-d".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    dirNames.add( args[i++] );
                }
            } else if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    patterns_file = args[i++];
                }
            } else if ( "-l".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    pic_file = args[i++];
                }
            } else if ( "-r".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    rank_file = args[i++];
                }
            } else if ( "-ni".equals( args[i] )) {
                i++;
                is_indexing = false;
            } else if ( "-e".equals( args[i] )) {
                i++;
                if(i < args.length){
                    euclidean_File = args[i++];
                }
            }else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }                   
    }


    /* ----------------------------------------------- */


    public static void main( String[] args ) {
        Engine e = new Engine( args );
    }

}

