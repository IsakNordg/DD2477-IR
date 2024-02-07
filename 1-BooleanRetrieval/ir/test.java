package ir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class test {

    public static void main(String[] args) throws IOException {
        
        RandomAccessFile term1 = new RandomAccessFile("C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\index\\terms00014", "r");
        RandomAccessFile term2 = new RandomAccessFile("C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\index\\terms8", "r");
        RandomAccessFile term3 = new RandomAccessFile("C:\\GitHub\\DD2477-IR\\1-BooleanRetrieval\\index\\outputTest", "rw");

        String t1 = term1.readLine();
        String t2 = term2.readLine();
        String curTerm;

        while(true){
            // System.out.println("t1: " + t1 + " t2: " + t2 + " Thread: " + Thread.currentThread().getName());
            if(t1 == null && t2 == null){
                break;
            }

            PostingsList pl1 = null, pl2 = null;
            if(t1 == null){
                curTerm = t2;
                t2 = term2.readLine();
            }else if(t2 == null){
                curTerm = t1;
                t1 = term1.readLine();
            }else{
                if(t1.compareTo(t2) < 0){
                    curTerm = t1;
                    t1 = term1.readLine();
                }else if(t1.compareTo(t2) > 0){
                    curTerm = t2;
                    t2 = term2.readLine();
                }else{
                    curTerm = t1;
                    t1 = term1.readLine();
                    t2 = term2.readLine();
                }
            }
            if(curTerm.equals("i")){
                System.out.println("found the i");
            }
            
            String dataString = curTerm + "\n";
            byte[] data = dataString.getBytes();
            term3.write( data );

        }

        /* 
        PostingsList pl1 = new PostingsList();
        PostingsList pl2 = new PostingsList();

        PostingsEntry pe1 = new PostingsEntry(1);
        PostingsEntry pe2 = new PostingsEntry(2);
        PostingsEntry pe3 = new PostingsEntry(3);

        pe1.offset.add(1);
        pe1.offset.add(2);
        pe1.offset.add(3);

        pe2.offset.add(2);
        pe2.offset.add(3);
        pe2.offset.add(4);

        pe3.offset.add(3);
        pe3.offset.add(4);
        pe3.offset.add(5);

        pl1.add(pe1);

        pl2.add(pe2);
        pl2.add(pe3);

        System.out.println(pl1.toString());
        System.out.println(pl2.toString());

        pl1.merge(pl2);

        System.out.println(pl1.list.size());
        System.out.println(pl1.toString());
        */


    }
    
}
