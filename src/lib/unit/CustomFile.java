package lib.unit;

import lib.tool.Tools;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class CustomFile extends File {
    public CustomFile(@NotNull String pathname) {
        super(pathname);
    }

    public CustomFile(File file) {
        this(file.getPath());
    }


    public long CalculatorLineNumber() throws IOException {
        if (!isFile()) {
            return 0;
        }
        long LineNumber = 0;
        BufferedReader file = new BufferedReader(new FileReader(getPath()));
        while (file.readLine() != null) {
            LineNumber++;
        }
        file.close();
        return LineNumber;
    }

    public void Append(File file) throws IOException {
        System.out.println(new Date() + "\tAppend " + file.getName() + " to " + getName());
        String line;
        BufferedReader read = new BufferedReader(new FileReader(file));
        BufferedWriter write = new BufferedWriter(new FileWriter(getPath(), true));
        while ((line = read.readLine()) != null) {
            write.write(line + "\n");
        }
        read.close();
        write.close();
        System.out.println(new Date() + "\tEnd append " + file.getName() + " to " + getName());
    }

    public void Append(String s) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(getPath(), true));
        writer.write(s);
        writer.close();
    }

    public int FastqPhred() throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(getPath()));
        int[] FormatEdge = new int[]{(int) '9', (int) 'K'};
        int[] Count = new int[2];
        String line;
        int LineNum = 0;
        while ((line = infile.readLine()) != null && ++LineNum <= 400) {
            if (LineNum % 4 != 0) {
                continue;
            }
            for (int i = 0; i < line.length(); i++) {
                if ((int) line.charAt(i) <= FormatEdge[0]) {
                    Count[0]++;
                } else if ((int) line.charAt(i) >= FormatEdge[1]) {
                    Count[1]++;
                }
            }
        }
        return Count[0] >= Count[1] ? Opts.Phred33 : Opts.Phred64;
    }

    public void SortFile(int[] Col, String Model, String Regex, File OutFile) throws IOException {
        System.out.print(new Date() + "\tBegin to sort file " + getName() + " sort col is");
        for (int col : Col) {
            System.out.print(" " + col);
        }
        System.out.println();
        Regex = Regex.isEmpty() ? "\\s+" : Regex;
        final long LineBin = 5000000;//500万行一个bin
        String line = null;
        long linecount = 0;
        int bin = 0;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(getPath()));
        if (Model.matches(".*n.*")) {
            //数字排序
            while (true) {
                ArrayList<char[]> List = new ArrayList<>();
                ArrayList<IntegerArrays> SortList = new ArrayList<>();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line.toCharArray());
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
                    for (IntegerArrays aSortList : SortList) {
                        outfile.write(List.get(aSortList.getItem()[Col.length]));
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
            File[] tempfile = new File[bin];
            for (int i = 0; i < tempfile.length; i++) {
                tempfile[i] = new File(OutFile + ".temp" + (i + 1));
            }
            new CustomFile(OutFile.getPath()).MergeSortFile(tempfile, Col, Model, Regex);
            for (File aTempfile : tempfile) {
                aTempfile.delete();
            }
        } else {
            //字符串排序
            while (true) {
                ArrayList<char[]> List = new ArrayList<>();
                ArrayList<StringArrays> SortList = new ArrayList<>();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line.toCharArray());
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
                    for (StringArrays aSortList : SortList) {
                        outfile.write(List.get(Integer.parseInt(aSortList.getItem()[Col.length])));
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
            File[] tempfile = new File[bin];
            for (int i = 0; i < tempfile.length; i++) {
                tempfile[i] = new File(OutFile + ".temp" + String.valueOf(i + 1));
            }
            new CustomFile(OutFile.getPath()).MergeSortFile(tempfile, Col, Model, Regex);
            for (int i = 0; i < tempfile.length; i++) {
                tempfile[i].delete();
            }
        }
        System.out.print(new Date() + "\tEnd sort file " + getName() + " by number sort col is");
        for (int col : Col) {
            System.out.print(" " + col);
        }
        System.out.println();
    }

    public void MergeSortFile(File[] InFile, int[] Row, String Model, String Regex) throws IOException {
        System.out.print(new Date() + "\tMerge ");
        for (File s : InFile) {
            System.out.print(s.getName() + " ");
        }
        System.out.print("to " + getName() + "\n");
        //=========================================================================================
        BufferedWriter outfile = new BufferedWriter(new FileWriter(getPath()));
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
        for (File s : InFile) {
            System.out.print(s.getName() + " ");
        }
        System.out.print("to " + getName() + "\n");
    }

    public void Merge(File[] File) throws IOException {
        BufferedWriter write_file = new BufferedWriter(new FileWriter(getPath()));
        String line;
        for (File x : File) {
            System.out.println(new Date() + "\tMerge " + x.getName() + " to " + getName());
            BufferedReader read_file = new BufferedReader(new FileReader(x));
            while ((line = read_file.readLine()) != null) {
                write_file.write(line + "\n");
            }
            read_file.close();
        }
        write_file.close();
        System.out.println(new Date() + "\tDone merge");
    }

    public ArrayList<File> SplitFile(String Prefix, int LineNum) throws IOException {
        int filecount = 0;
        int count = 0;
        String line;
        ArrayList<File> Outfile = new ArrayList<>();
        BufferedReader infile = new BufferedReader(new FileReader(getPath()));
        Outfile.add(new File(Prefix + ".Split" + filecount));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(Outfile.get(Outfile.size() - 1)));
        while ((line = infile.readLine()) != null) {
            count++;
            if (count > LineNum) {
                outfile.close();
                filecount++;
                Outfile.add(new File(Prefix + ".Split" + filecount));
                outfile = new BufferedWriter(new FileWriter(Outfile.get(Outfile.size() - 1)));
                count = 1;
            }
            outfile.write(line + "\n");
        }
        outfile.close();
        infile.close();
        return Outfile;
    }

    public int BedpeDetect() throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(getPath()));
        String line;
        String[] str;
        line = infile.readLine();
        if (line == null) {
            infile.close();
            return Opts.EmptyFile;
        }
        str = line.split("\\s+");
        try {
            Integer.parseInt(str[1]);
            Integer.parseInt(str[2]);
            Integer.parseInt(str[4]);
            Integer.parseInt(str[5]);
        } catch (IndexOutOfBoundsException | NumberFormatException i) {
            try {
                Integer.parseInt(str[1]);
                Integer.parseInt(str[3]);
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                e.printStackTrace();
                infile.close();
                return Opts.ErrorFormat;
            }
            infile.close();
            return Opts.BedpePointFormat;
        }
        infile.close();
        return Opts.BedpeRegionFormat;
    }

    public String AdapterDetect(File Prefix, int SubIndex) throws IOException, InterruptedException {
        StringBuilder Adapter = new StringBuilder();
        ArrayList<char[]> MsaStat = new ArrayList<>();
        int SeqNum = 31;
        int[] CountArrays = new int[255];
        File HeadFile = new File(Prefix + ".head" + SeqNum);
        File MsaFile = new File(Prefix + ".msa");
        BufferedReader reader = new BufferedReader(new FileReader(getPath()));
        BufferedWriter writer = new BufferedWriter(new FileWriter(HeadFile));
        String line;
        int linenumber = 0;
        while ((line = reader.readLine()) != null && ++linenumber <= SeqNum * 4) {
            if (linenumber % 4 == 1) {
                writer.write(line.replace("@", ">") + "\n");
            } else if (linenumber % 4 == 2) {
                writer.write(line.substring(SubIndex, line.length()) + "\n");
            }
        }
        reader.close();
        writer.close();
        String ComLine = "mafft " + HeadFile.getPath();
        Opts.CommandOutFile.Append(ComLine + "\n");
        Tools.ExecuteCommandStr(ComLine, MsaFile.getPath());
        HeadFile.delete();
        reader = new BufferedReader(new FileReader(MsaFile));
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            if (line.matches("^>.*")) {
                MsaStat.add(Adapter.toString().toCharArray());
                Adapter.setLength(0);
                continue;
            }
            Adapter.append(line);
        }
        MsaStat.add(Adapter.toString().toCharArray());
        Adapter.setLength(0);
        reader.close();
        for (int i = 0; i < MsaStat.get(0).length; i++) {
            CountArrays['A'] = 0;
            CountArrays['T'] = 0;
            CountArrays['C'] = 0;
            CountArrays['G'] = 0;
            CountArrays['-'] = 0;
            for (int j = 0; j < MsaStat.size(); j++) {
                CountArrays[Character.toUpperCase(MsaStat.get(j)[i])]++;
            }
            int MaxValue = 0;
            char MaxBase = '-';
            for (char base : new char[]{'A', 'T', 'C', 'G', '-'}) {
                if (CountArrays[base] > MaxValue) {
                    MaxValue = CountArrays[base];
                    MaxBase = base;
                }
            }
            if (MaxValue > SeqNum / 2) {
                Adapter.append(MaxBase);
            } else {
                Adapter.append('N');
            }
        }
        String[] SplitAdapter = Adapter.toString().replace("-", "").split("N+");
        int MaxValue = 0;
        for (int i = 0; i < SplitAdapter.length; i++) {
            if (SplitAdapter[i].length() > MaxValue) {
                MaxValue = SplitAdapter[i].length();
                Adapter = new StringBuilder(SplitAdapter[i]);
            }
        }
        return Adapter.toString();
    }


}
