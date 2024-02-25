import java.util.*;
import java.io.*;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.000000001;

       
    /* --------------------------------------------- */


    public PageRank( String filename ) {
		int noOfDocs = readDocs( filename );
		iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
				// This is a previousy unseen doc, so add it to the table.
				otherDoc = fileIndex++;
				docNumber.put( otherTitle, otherDoc );
				docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
				link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
				link.get(fromdoc).put( otherDoc, true );
				out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

		double[] a = new double[numberOfDocs];
		a[0] = 1;

		while( maxIterations-- > 0 ) {

			double[] aNext = new double[numberOfDocs];
			for ( int i=0; i<numberOfDocs; i++ ) {
				aNext[i] = BORED / numberOfDocs;
			}

			// Compute aP
			for ( int i = 0; i < numberOfDocs; i++) {
				HashMap<Integer,Boolean> outlinks = link.get(i);
				int noOfOutlinks = out[i];
				
				// if no outlinks, distribute the rank evenly
				if(outlinks == null) {
					for ( int j=0; j<numberOfDocs; j++ ) {
						aNext[j] += a[i] / numberOfDocs;
					}
					continue;
				}
				

				if ( noOfOutlinks == 0 ) {
					for ( int j=0; j<numberOfDocs; j++ ) {
						aNext[j] += a[i] / numberOfDocs;
					}
				} else {
					for ( int j : link.get(i).keySet() ) {
						aNext[j] += a[i] / noOfOutlinks * (1 - BORED);
					}
				}
			}

			// Regularize
			double sum = 0;
			for ( int i=0; i<numberOfDocs; i++ ) {
				sum += aNext[i];
			}
			for ( int i=0; i<numberOfDocs; i++ ) {
				aNext[i] = aNext[i]/sum;
			}

			// Check for convergence
			boolean done = true;
			for ( int i=0; i<numberOfDocs; i++ ) {
				// If any of the values differ more than EPSILON, we're not done
				if ( Math.abs(a[i]-aNext[i]) > EPSILON ) {
					done = false;
					break;
				}
			}

			if ( done ) {
				break;
			}

			a = aNext;

		}

		//print( a );
		printToFile( a );

    }

	// Prints all documents and their corresponding rank to a file
	void printToFile( double[] a ) {
		try {
			PrintWriter writer = new PrintWriter("../../1-BooleanRetrieval/pagerank.txt", "UTF-8");
			for ( int i=0; i<a.length; i++ ) {
				writer.print( docName[i]);
				writer.print(" ");
				writer.println(a[i]);
			}
			writer.close();
		} catch (IOException e) {
			System.err.println("Error writing to file");
		}
	}

	// Prints the 30 highest ranked documents
	void print( double[] a ) {
		
		// Create a list of all documents
		ArrayList<Integer> list = new ArrayList<Integer>();
		for ( int i=0; i<a.length; i++ ) {
			list.add(i);
		}

		// Sort the list based on the values in a
		Collections.sort(list, new Comparator<Integer>() {
			public int compare( Integer i, Integer j ) {
				return Double.compare(a[j], a[i]);
			}
		});

		// Print the 30 highest ranked documents
		for ( int i=0; i<30; i++ ) {
			System.out.print( list.get(i) + ":\t");
			System.out.printf(" %.5f\n", a[list.get(i)]);
		}
	}


    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}