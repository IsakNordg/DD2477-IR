package ir;

import java.io.IOException;
import java.io.RandomAccessFile;

public class test {

    public static void main(String[] args) throws IOException {
        
        RandomAccessFile raf = new RandomAccessFile("test/test1.f", "r");

        String line = raf.readLine();
        System.out.println(line);

        System.out.println(raf.readLine());

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
