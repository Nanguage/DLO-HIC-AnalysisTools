package lib.File;

import java.io.*;
import java.util.Date;

public class Merge {
    public Merge(String[] File, String MergeFile) throws IOException {
        BufferedWriter write_file = new BufferedWriter(new FileWriter(MergeFile));
        for (String x : File) {
            System.out.println(new Date() + "\tMerge " + x + " to " + MergeFile);
            String line;
            BufferedReader read_file = new BufferedReader(new FileReader(x));
            while ((line = read_file.readLine()) != null) {
                write_file.write(line + "\n");
            }
            read_file.close();
        }
        write_file.close();
        System.out.println(new Date() + "\tDone merge");
    }
}
