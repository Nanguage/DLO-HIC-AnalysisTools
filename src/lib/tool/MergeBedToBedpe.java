package lib.tool;

import java.io.*;
import java.util.Date;

public class MergeBedToBedpe {
    public MergeBedToBedpe(String InFile1, String InFile2, String OutFile, int Row, String Regex) throws IOException {
        System.out.println(new Date() + "\tMerge " + InFile1 + " and " + InFile2 + " to " + OutFile + " start");
        BufferedReader infile1 = new BufferedReader(new FileReader(InFile1));
        BufferedReader infile2 = new BufferedReader(new FileReader(InFile2));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String regex = Regex;
        if (Regex.isEmpty()) {
            regex = "\\s+";
        }
        String line1, line2;
        String[] str1, str2;
        line1 = infile1.readLine();
        line2 = infile2.readLine();
        str1 = line1.split(regex);
        str2 = line2.split(regex);
        boolean Flage = true;
        while (line1 != null && line2 != null) {
            if (Flage) {
                str1 = line1.split(regex);
            } else {
                str2 = line2.split(regex);
            }
            if (str1[Row - 1].compareTo(str2[Row - 1]) < 0) {
                line1 = infile1.readLine();
                Flage = true;
            } else if (str1[Row - 1].compareTo(str2[Row - 1]) > 0) {
                line2 = infile2.readLine();
                Flage = false;
            } else {
                outfile.write(str1[0] + "\t" + str1[1] + "\t" + str1[2] + "\t" + str2[0] + "\t" + str2[1] + "\t" + str2[2] + "\t" + str1[Row - 1] + "\t" + str1[str1.length - 2] + "\t" + str2[str2.length - 2] + "\t" + str1[str1.length - 1] + "\t" + str2[str2.length - 1] + "\n");
                line1 = infile1.readLine();
                line2 = infile2.readLine();
                try {
                    str1 = line1.split(regex);
                    str2 = line2.split(regex);
                } catch (NullPointerException ignored) {

                }
            }
        }
        infile1.close();
        infile2.close();
        outfile.close();
        System.out.println(new Date() + "\tMerge " + InFile1 + " and " + InFile2 + " to " + OutFile + " end");
    }
}
