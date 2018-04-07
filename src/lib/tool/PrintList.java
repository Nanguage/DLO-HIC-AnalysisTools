package lib.tool;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class PrintList {
    public PrintList(ArrayList<String> List, String OutFile) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        for (String s : List) {
            outfile.write(s + "\n");
        }
        outfile.close();
    }
}
