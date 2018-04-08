package lib.File;

import java.io.*;
import java.util.ArrayList;

public class SplitFile {
    private String InFile;
    private String Prefix;
    private int LineNum;
    private ArrayList<String> SplitFile = new ArrayList<>();

    SplitFile(String file, String prefix, int line_num) {
        this.InFile = file;
        this.Prefix = prefix;
        this.LineNum = line_num;
    }

    public static void main(String[] args) throws IOException {
//        new SplitFile("test.bed","split-test",100).Run();
    }

    public ArrayList<String> Run() throws IOException {
        int filecount = 0;
        int count = 0;
        String line;
        String outfilename;
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        outfilename = Prefix + filecount;
        SplitFile.add(outfilename);
        BufferedWriter outfile = new BufferedWriter(new FileWriter(outfilename));
        while ((line = infile.readLine()) != null) {
            count++;
            if (count <= LineNum) {
                outfile.write(line + "\n");
            } else {
                outfile.close();
                filecount++;
                outfilename = Prefix + filecount;
                SplitFile.add(outfilename);
                outfile = new BufferedWriter(new FileWriter(outfilename));
                outfile.write(line + "\n");
                count = 1;
            }
        }
        outfile.close();
        infile.close();
        return SplitFile;
    }

    public ArrayList<String> getSplitFile() throws IOException {
        if (SplitFile.size() == 0) {
            int filecount = 0;
            int count = 0;
            BufferedReader infile = new BufferedReader(new FileReader(InFile));
            SplitFile.add(Prefix + filecount);
            while (infile.readLine() != null) {
                count++;
                if (count > LineNum) {
                    filecount++;
                    SplitFile.add(Prefix + filecount);
                    count = 1;
                }
            }
        }
        return SplitFile;
    }
}
