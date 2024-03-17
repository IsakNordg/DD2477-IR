import java.util.*;
import java.io.*;
import java.sql.Time;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

	final static Double c = 0.85;

	final static boolean monteCarlo = false;

	final static boolean iterate = true;

	final static int monteCarloType = 5;

	// Number of docs: 17478
	static int N = 10000000;

	static int t = 1000;

	static int m = 1;


	/**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.01;

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

	/** The real pagerank score saved as a hashtable. */
	private HashMap<Integer,Double> realPagerank = new HashMap<Integer,Double>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

       
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
    @SuppressWarnings("unused")
	void iterate( int numberOfDocs, int maxIterations ) {

		Time time = new Time(System.currentTimeMillis());

		if(!monteCarlo){
			System.out.println("Not Monte Carlo");
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

			print( a );
			// printToFile( a );
			// printError( a );
		}else if(!iterate){	// Monte Carlo approximation

			double[] a = new double[numberOfDocs];
			if(monteCarloType == 1){

				// N = numberOfDocs * m;	// m is a scaling factor. Same as in method 2 for easier comparison

				// chose random node and follow links t times
				for(int i = 0; i < N; i++){

					Random rand = new Random();
					int r = rand.nextInt(numberOfDocs);	// random node

					for(int j = 0; j < t; j++){
						// terminate if random is smaller than (1 - c)
						if(Math.random() < (1 - c)) break;

						HashMap<Integer,Boolean> outlinks = link.get(r);
						int noOfOutlinks = out[r];
						if(outlinks == null) {
							// System.out.println("No outlinks for node 2 " + r);
							// if no outlinks, choose random node
							r = rand.nextInt(numberOfDocs);
							continue;
						}
						if ( noOfOutlinks == 0 ) {
							// System.out.println("No outlinks for node 1 " + r);
							// if no outlinks, choose random node
							r = rand.nextInt(numberOfDocs);
						} else {
							int[] outlinkArray = new int[noOfOutlinks];
							int k = 0;
							for ( int l : link.get(r).keySet() ) {
								outlinkArray[k] = l;
								k++;
							}
							r = outlinkArray[rand.nextInt(noOfOutlinks)];
						}
					}
					// add 1 to the rank of the node
					a[r]++;
					// System.out.println("Node: " + r + " Rank: " + a[r]);
				}
				// normalize the rank
				for(int i = 0; i < numberOfDocs; i++){
					// System.out.println(a[i]);
					a[i] = a[i] / N;
				}

			}else if(monteCarloType == 2){

				// start at each page exactly m times
				for(int j = 0; j < N; j++){
					int r;
					for(int k = 0; k < m; k++){	// follow links m times
						r = j;
						for(int l = 0; l < t; l++){

							// terminate if random is smaller than (1 - c)
							if(Math.random() < (1 - c)) break;

							HashMap<Integer,Boolean> outlinks = link.get(r);
							int noOfOutlinks = out[r];
							if(outlinks == null) {
								// if no outlinks, choose random node
								r = (int)(Math.random() * numberOfDocs);
								continue;
							}
							if ( noOfOutlinks == 0 ) {
								// if no outlinks, choose random node
								r = (int)(Math.random() * numberOfDocs);
							} else {
								int[] outlinkArray = new int[noOfOutlinks];
								int m = 0;
								for ( int n : link.get(r).keySet() ) {
									outlinkArray[m] = n;
									m++;
								}
								r = outlinkArray[(int)(Math.random() * noOfOutlinks)];
							}
						}
						// add 1 to the rank of the node
						a[r]++;
					}
				}
				// normalize the rank
				for(int i = 0; i < numberOfDocs; i++){
					a[i] = a[i] / (N * m);
				}
			}else if(monteCarloType == 4){
				// Simulate N = mn runs of the random walk {Dt}t≥0 
				// initiated at each page exactly m times and stopping
				// when it reaches a dangling node

				// N = numberOfDocs * m;

				int visits = 0;

				// start at each page exactly m times
				for(int j = 0; j < N; j++){
					
					int r;
					for(int k = 0; k < m; k++){	// follow links m times
						r = j;
						for(int l = 0; l < t; l++){
							// add 1 to the rank of the node
							a[r]++;
							visits++;

							// terminate if random is smaller than (1 - c)
							if(Math.random() < (1 - c)) break;

							HashMap<Integer,Boolean> outlinks = link.get(r);
							int noOfOutlinks = out[r];
							if(outlinks == null) {
								// if no outlinks we have reached a dangling node
								break;
							}
							if ( noOfOutlinks == 0 ) {
								// if no outlinks we have reached a dangling node
								break;
							} else {
								int[] outlinkArray = new int[noOfOutlinks];
								int m = 0;
								for ( int n : link.get(r).keySet() ) {
									outlinkArray[m] = n;
									m++;
								}
								r = outlinkArray[(int)(Math.random() * noOfOutlinks)];
							}
						}
					}
				}
				// normalize the rank
				for(int i = 0; i < numberOfDocs; i++){
					a[i] = a[i] / visits;
				}
			}else if(monteCarloType == 5){
				// Simulate N runs of the random walk {Dt}t≥0 initiated
				// at a randomly chosen page and stopping when it
				// reaches a dangling node

				// N = numberOfDocs * m;
				int visits = 0;

				// chose random node and follow links t times
				for(int i = 0; i < N; i++){
					// terminate if random is smaller than (1 - c)

					Random rand = new Random();
					int r = rand.nextInt(numberOfDocs);	// random node
					for(int j = 0; j < t; j++){
						// add 1 to the rank of the node
						a[r]++;
						visits++;

						// terminate if random is smaller than (1 - c)
						if(Math.random() < (1 - c)) break;
						HashMap<Integer,Boolean> outlinks = link.get(r);
						int noOfOutlinks = out[r];
						if(outlinks == null) {
							// if no outlinks we have reached a dangling node
							break;
						}
						if ( noOfOutlinks == 0 ) {
							// if no outlinks we have reached a dangling node
							break;
						} else {
							int[] outlinkArray = new int[noOfOutlinks];
							int k = 0;
							for ( int l : link.get(r).keySet() ) {
								outlinkArray[k] = l;
								k++;
							}
							r = outlinkArray[rand.nextInt(noOfOutlinks)];
						}
					}
				}

				// normalize the rank
				for(int i = 0; i < numberOfDocs; i++){
					a[i] = a[i] / visits;
				}
			}


			print( a );
			// printToFile( a );
			// printError( a );

			System.out.println("Time: " + (new Time(System.currentTimeMillis()).getTime() - time.getTime()) + "ms");
		}else{
			// Here we want to iterate until the top 30 documents are stable
			// We will use method 1, because it is my favorite

			Time timeTilStable = new Time(System.currentTimeMillis());

			double[] a = new double[numberOfDocs];
			double[] a_prev = new double[numberOfDocs];

			boolean stable = false;
			boolean first = true;
			int iters = 0;

			// chose random node and follow links t times
			while(!stable){	

				for(int i = 0; i < N; i++){

					Random rand = new Random();
					int r = rand.nextInt(numberOfDocs);	// random node

					for(int j = 0; j < t; j++){
						// terminate if random is smaller than (1 - c)
						if(Math.random() < (1 - c)) break;

						HashMap<Integer,Boolean> outlinks = link.get(r);
						int noOfOutlinks = out[r];
						if(outlinks == null) {
							// System.out.println("No outlinks for node 2 " + r);
							// if no outlinks, choose random node
							r = rand.nextInt(numberOfDocs);
							continue;
						}
						if ( noOfOutlinks == 0 ) {
							// System.out.println("No outlinks for node 1 " + r);
							// if no outlinks, choose random node
							r = rand.nextInt(numberOfDocs);
						} else {
							int[] outlinkArray = new int[noOfOutlinks];
							int k = 0;
							for ( int l : link.get(r).keySet() ) {
								outlinkArray[k] = l;
								k++;
							}
							r = outlinkArray[rand.nextInt(noOfOutlinks)];
						}
					}
					// add 1 to the rank of the node
					a[r]++;
					iters++;
				}

				if(first){
					first = false;
				}else{
					stable = checkIfStable(a, a_prev);
					// copy a to a_prev
					a_prev = a.clone();
				}
				// System.out.println("Iterations: " + iters);
				System.out.print(stable + " ");
				printInline( a );
			}

			// normalize the rank
			for(int i = 0; i < numberOfDocs; i++){
				// System.out.println(a[i]);
				a[i] = a[i] / iters;
			}

			print( a );

			System.out.println("Time: " + (new Time(System.currentTimeMillis()).getTime() - timeTilStable.getTime()) + "ms");

			System.out.println("Iterations: " + iters);
		}
    }

	boolean checkIfStable(double[] a, double[] prev_a){
		// return true if the top 30 documents are the same in a and prev_a

		// Create a list of all documents
		ArrayList<Integer> list = new ArrayList<Integer>();
		for ( int i=0; i<a.length; i++ ) {
			list.add(i);
		}
		ArrayList<Integer> prev_list = new ArrayList<Integer>();
		for ( int i=0; i<prev_a.length; i++ ) {
			prev_list.add(i);
		}

		// Sort the list based on the values in a
		Collections.sort(list, new Comparator<Integer>() {
			public int compare( Integer i, Integer j ) {
				return Double.compare(a[j], a[i]);
			}
		});

		Collections.sort(prev_list, new Comparator<Integer>() {
			public int compare( Integer i, Integer j ) {
				return Double.compare(prev_a[j], prev_a[i]);
			}
		});

		for ( int i=0; i<30; i++ ) {
			
			int doc = list.get(i);
			int prev_doc = prev_list.get(i);

			if(doc != prev_doc){
				return false;
			}
		}
		return true;
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
			System.out.printf(" %.7f\n", a[list.get(i)]);
		}
	}

	void printInline( double[] a ) {
		
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
			System.out.print( list.get(i) + " ");
		}
		System.out.println();
	}

	void printError( double[] a ) {
		double error = 0;

		/* 
		System.out.println(a[142]);
		System.out.println(a[151]);
		System.out.println(a[24 ]);
		System.out.println(a[296]);
		System.out.println(a[869]);
		System.out.println(a[346]);
		System.out.println(a[244]);
		System.out.println(a[674]);
		System.out.println(a[455]);
		System.out.println(a[241]);
		System.out.println(a[253]);
		System.out.println(a[217]);
		System.out.println(a[263]);
		System.out.println(a[430]);
		System.out.println(a[6  ]);
		System.out.println(a[226]);
		System.out.println(a[239]);
		System.out.println(a[419]);
		System.out.println(a[135]);
		System.out.println(a[210]);
		System.out.println(a[173]);
		System.out.println(a[397]);
		System.out.println(a[448]);
		System.out.println(a[122]);
		System.out.println(a[90 ]);
		System.out.println(a[77 ]);
		System.out.println(a[49 ]);
		System.out.println(a[636]);
		System.out.println(a[683]);
		System.out.println(a[597]);
		*/

		/*
		error += Math.pow(a[142] - 0.01593, 2);
		error += Math.pow(a[151] - 0.01158, 2);
		error += Math.pow(a[24 ] - 0.01064, 2);
		error += Math.pow(a[296] - 0.00346, 2);
		error += Math.pow(a[869] - 0.00285, 2);
		error += Math.pow(a[346] - 0.00266, 2);
		error += Math.pow(a[244] - 0.00260, 2);
		error += Math.pow(a[674] - 0.00247, 2);
		error += Math.pow(a[455] - 0.00230, 2);
		error += Math.pow(a[241] - 0.00206, 2);
		error += Math.pow(a[253] - 0.00204, 2);
		error += Math.pow(a[217] - 0.00204, 2);
		error += Math.pow(a[263] - 0.00201, 2);
		error += Math.pow(a[430] - 0.00195, 2);
		error += Math.pow(a[6  ] - 0.00182, 2);
		error += Math.pow(a[226] - 0.00178, 2);
		error += Math.pow(a[239] - 0.00177, 2);
		error += Math.pow(a[419] - 0.00172, 2);
		error += Math.pow(a[135] - 0.00165, 2);
		error += Math.pow(a[210] - 0.00159, 2);
		error += Math.pow(a[173] - 0.00156, 2);
		error += Math.pow(a[397] - 0.00142, 2);
		error += Math.pow(a[448] - 0.00140, 2);
		error += Math.pow(a[122] - 0.00138, 2);
		error += Math.pow(a[90 ] - 0.00135, 2);
		error += Math.pow(a[77 ] - 0.00130, 2);
		error += Math.pow(a[49 ] - 0.00117, 2);
		error += Math.pow(a[636] - 0.00115, 2);
		error += Math.pow(a[683] - 0.00113, 2);
		error += Math.pow(a[597] - 0.00110, 2);
		*/

		readPageRank("C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\pagerank.txt");

		for(int i = 0; i < a.length; i++){
			try{
				error += Math.pow(a[i] - realPagerank.get(i), 2);
			}catch(NullPointerException e){
				// System.out.println("Error: " + e);
			}
		}
		System.out.print("Error: ");
		System.out.printf(" %.8f\n", error);
	}
    /* --------------------------------------------- */

	void readPageRank(String filename){
        File pr = new File(filename);
        Scanner in;
        try {
            in = new Scanner(pr);
        
            int i = 0;
            while(in.hasNextLine()){
                String[] line = in.nextLine().split(" ");
                realPagerank.put(i, Double.parseDouble(line[1]));
				// System.out.println(i + " " + realPagerank.get(i));
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}