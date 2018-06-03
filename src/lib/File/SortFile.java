package lib.File;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import lib.unit.*;
import lib.tool.*;

public class SortFile {
    public SortFile(String InFile, int[] Col, String Model, String Regex, String OutFile, int Threads) throws IOException {
        System.out.print(new Date() + "\tBegin to sort file " + InFile + " sort col is");
        for (int col : Col) {
            System.out.print(" " + col);
        }
        System.out.println();
        if (Regex.isEmpty()) {
            Regex = "\\s+";
        }
//        long LineNumber = Statistic.CalculatorLineNumber(InFile);
//        System.out.println(new Date() + "\t" + InFile + " Line Number:\t" + LineNumber);
//        long LineBin = LineNumber / (Threads * 10) + 1;//计算每个文件的行数
        final long LineBin = 2000000;//200万行一个bin
        String line = null;
        long linecount = 0;
        int bin = 0;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        if (Model.matches(".*n.*")) {
            //数字排序
            while (true) {
                ArrayList<String> List = new ArrayList<>();
                ArrayList<IntegerArrays> SortList = new ArrayList<>();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line);
                    str = line.split(Regex);
                    IntegerArrays tempint = new IntegerArrays(Col.length + 1);
                    for (int i = 0; i < Col.length; i++) {
                        tempint.set(i, Integer.parseInt(str[Col[i] - 1]));
                    }
                    tempint.set(tempint.getLength() - 1, SortList.size());
                    SortList.add(tempint);
                    linecount++;
                }
                if (SortList.size() != 0) {
                    bin++;
                    Collections.sort(SortList);
                    //====================================排序前后分割线==============================================
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile + ".temp" + bin));
                    for (int i = 0; i < SortList.size(); i++) {
                        outfile.write(List.get(SortList.get(i).getItem()[Col.length]));
                        outfile.write("\n");
                    }
                    List.clear();
                    SortList.clear();
                    outfile.close();
                    linecount = 0;
                } else {
                    break;
                }
            }
            infile.close();
            String[] tempfile = new String[bin];
            for (int i = 0; i < tempfile.length; i++) {
                tempfile[i] = OutFile + ".temp" + String.valueOf(i + 1);
            }
            new MergeSortFile(tempfile, OutFile, Col, Model, Regex);
            for (String aTempfile : tempfile) {
                new File(aTempfile).delete();
            }
        } else {
            //字符串排序
            while (true) {
                ArrayList<String> List = new ArrayList<>();
                ArrayList<StringArrays> SortList = new ArrayList<>();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line);
                    str = line.split(Regex);
                    StringArrays tempstr = new StringArrays(Col.length + 1);
                    for (int i = 0; i < Col.length; i++) {
                        tempstr.set(i, str[Col[i] - 1]);
                    }
                    tempstr.set(tempstr.getLength() - 1, String.valueOf(SortList.size()));
                    SortList.add(tempstr);
                    linecount++;
                }
                if (SortList.size() != 0) {
                    bin++;
                    Collections.sort(SortList);
                    //====================================排序前后分割线==============================================
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile + ".temp" + bin));
                    for (int i = 0, len = SortList.size(); i < len; i++) {
                        outfile.write(List.get(Integer.parseInt(SortList.get(i).getItem()[Col.length])));
                        outfile.write("\n");
                    }
                    List.clear();
                    SortList.clear();
                    outfile.close();
                    linecount = 0;
                } else {
                    break;
                }
            }
            infile.close();
            String[] tempfile = new String[bin];
            for (int i = 0; i < tempfile.length; i++) {
                tempfile[i] = OutFile + ".temp" + String.valueOf(i + 1);
            }
            new MergeSortFile(tempfile, OutFile, Col, Model, Regex);
            for (int i = 0; i < tempfile.length; i++) {
                new File(tempfile[i]).delete();
            }
        }
        System.out.print(new Date() + "\tEnd sort file " + InFile + " by number sort row is");
        for (int row : Col) {
            System.out.print(" " + row);
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
//        new SortFile("DLO-HiC.AA.chrY.valid", new int[]{2,3,5,6}, "n", "\\s+", "test.sort.bedpe", 2);
    }
}
