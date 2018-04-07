package lib.File;

import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import lib.unit.*;

public class MergeSortFile {
    public MergeSortFile(String[] InFile, String OutFile, int[] Row, String Model, String Regex) throws IOException {
        System.out.print(new Date() + "\tMerge ");
        for (String s : InFile) {
            System.out.print(s + " ");
        }
        System.out.print("to " + OutFile + "\n");
        //=========================================================================================
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        if (InFile.length == 0) {
            return;
        }
        BufferedReader[] infile = new BufferedReader[InFile.length];
        for (int i = 0; i < InFile.length; i++) {
            infile[i] = new BufferedReader(new FileReader(InFile[i]));
        }
        String regex = Regex;
        if (Regex.isEmpty()) {
            regex = "\\s+";
        }
        String[] line = new String[InFile.length];
        String[] str;
        //==============================开始合并=================================
        if (Model.matches(".*n.*")) {
            //数字排序
            LinkedList<IntegerArrays> list = new LinkedList<>();
            for (int i = 0; i < infile.length; i++) {
                line[i] = infile[i].readLine();
                if (line[i] == null) {
                    continue;
                }
                str = line[i].split(regex);
                IntegerArrays tempint = new IntegerArrays(Row.length + 1);
                tempint.set(tempint.getLength() - 1, i);
                for (int j = 0; j < Row.length; j++) {
                    tempint.set(j, Integer.parseInt(str[Row[j] - 1]));
                }
                //================插入链表===sort====================
                list.add(tempint);
                Collections.sort(list);
            }
            //=================================================
            while (list.size() > 1) {
                IntegerArrays tempint = list.removeFirst();
                int index = tempint.getItem()[tempint.getLength() - 1];
                outfile.write(line[index] + "\n");
                if ((line[index] = infile[index].readLine()) != null) {
                    str = line[index].split(regex);
                    for (int j = 0; j < Row.length; j++) {
                        tempint.set(j, Integer.parseInt(str[Row[j] - 1]));
                    }
                    list.add(tempint);
                    Collections.sort(list);
                } else {
                    infile[index].close();
                }
            }
            IntegerArrays tempint = list.removeFirst();
            int index = tempint.getItem()[tempint.getLength() - 1];
            outfile.write(line[index] + "\n");
            while ((line[index] = infile[index].readLine()) != null) {
                outfile.write(line[index] + "\n");
            }
        } else {
            //字符串排序
            LinkedList<StringArrays> list = new LinkedList<>();
            for (int i = 0; i < infile.length; i++) {
                line[i] = infile[i].readLine();
                if (line[i] == null) {
                    continue;
                }
                str = line[i].split(regex);
                StringArrays tempstr = new StringArrays(Row.length + 1);
                tempstr.set(tempstr.getLength() - 1, String.valueOf(i));
                for (int j = 0; j < Row.length; j++) {
                    tempstr.set(j, str[Row[j] - 1]);
                }
                //================插入链表=========================
                list.add(tempstr);
                Collections.sort(list);
            }
            //======================================================================================================
            while (list.size() > 1) {
                StringArrays tempstr = list.removeFirst();
                int index = Integer.parseInt(tempstr.getItem()[tempstr.getLength() - 1]);
                outfile.write(line[index] + "\n");
                if ((line[index] = infile[index].readLine()) != null) {
                    str = line[index].split(regex);
                    for (int j = 0; j < Row.length; j++) {
                        tempstr.set(j, str[Row[j] - 1]);
                    }
                    list.add(tempstr);
                    Collections.sort(list);
                } else {
                    infile[index].close();
                }
            }
            StringArrays tempstr = list.removeFirst();
            int index = Integer.parseInt(tempstr.getItem()[tempstr.getLength() - 1]);
            outfile.write(line[index] + "\n");
            while ((line[index] = infile[index].readLine()) != null) {
                outfile.write(line[index] + "\n");
            }
        }
        outfile.close();
        //============================================================================================
        System.out.print(new Date() + "\tEnd merge ");
        for (String s : InFile) {
            System.out.print(s + " ");
        }
        System.out.print("to " + OutFile + "\n");
    }
}
