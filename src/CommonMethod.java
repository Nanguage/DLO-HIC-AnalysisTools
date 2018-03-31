

import java.io.*;
import java.util.*;

/**
 * @author snowf
 */
public class CommonMethod {

    public static void Merge(String[] File, String MergeFile) throws IOException {
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

    /**
     * @param File1
     * @param File2
     * @throws IOException
     */
    public static void Append(String File1, String File2) throws IOException {
        System.out.println(new Date() + "\tAppend " + File1 + " to " + File2);
        String line;
        BufferedReader read = new BufferedReader(new FileReader(File1));
        BufferedWriter write = new BufferedWriter(new FileWriter(File2, true));
        while ((line = read.readLine()) != null) {
            write.write(line + "\n");
        }
        read.close();
        write.close();
        System.out.println(new Date() + "\tEnd append " + File1 + " to " + File2);
    }

    public static void Paste(String[] file, String out_file) throws IOException {
        String null_str = new String();
        StringBuilder line = new StringBuilder();
        String TmpStr;
        BufferedReader[] file_read = new BufferedReader[file.length];
        System.out.print(new Date() + "\tBegin past ");
        for (int i = 0; i < file.length; i++) {
            null_str = null_str.concat("\t");
            System.out.print(file[i] + " ");
            file_read[i] = new BufferedReader(new FileReader(file[i]));
            line.append(file_read[i].readLine()).append("\t");
        }
        System.out.println("to " + out_file);
        null_str = null_str.substring(0, null_str.length() - 1);
        line.deleteCharAt(line.length() - 1);

        BufferedWriter file_write = new BufferedWriter(new FileWriter(out_file));
        while (!line.toString().equals(null_str)) {
            file_write.write(line.toString() + "\n");//当line含有制表符之外的内容（至少有一个文件还有内容），输出
            line.setLength(0);//清空line
            for (int i = 0; i < file.length; i++) {
                TmpStr = file_read[i].readLine();//读取每个文件的下一行
                if (TmpStr == null) {
                    TmpStr = "";//如果为空就赋初值“”
                }
                line.append(TmpStr).append("\t");//连接到line后面
            }
            line.deleteCharAt(line.length() - 1);//去掉line最后一个制表符
        }
        file_write.close();
        for (int i = 0; i < file.length; i++) {
            file_read[i].close();
        }
        System.out.println(new Date() + "\tDone paste");
    }

    public static int CommandStrExe(String CommandStr, String LogFile) throws IOException {
        Process P;
        int ExitValue;
        BufferedReader bufferedReader;
        BufferedWriter bufferedwriter = new BufferedWriter(new FileWriter(LogFile));
        String line;
        try {
            System.out.println(new Date() + "\t" + CommandStr);
            P = Runtime.getRuntime().exec(CommandStr);
            bufferedReader = new BufferedReader(new InputStreamReader(P.getErrorStream()));
            while ((line = bufferedReader.readLine()) != null) {
                bufferedwriter.write(line + "\n");
            }
            bufferedReader.close();
            bufferedwriter.close();
            ExitValue = P.waitFor();
            return ExitValue;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int CommandStrExe(String CommandStr, String OutFile, String LogFile) throws IOException {
        Process P;
        int ExitValue;
        try {
            System.out.println(new Date() + "\t" + CommandStr);
            P = Runtime.getRuntime().exec(CommandStr);
            Thread ErrThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getErrorStream()));
                        BufferedWriter bufferedwriter_err = new BufferedWriter(new FileWriter(LogFile));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            bufferedwriter_err.write(line + "\n");
                        }
                        bufferedReaderIn.close();
                        bufferedwriter_err.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread OutThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getInputStream()));
                        BufferedWriter bufferedwriter_out = new BufferedWriter(new FileWriter(OutFile));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            bufferedwriter_out.write(line + "\n");
                        }
                        bufferedReaderIn.close();
                        bufferedwriter_out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                OutThread.start();
                ErrThread.start();
                OutThread.join();
                ErrThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ExitValue = P.waitFor();
            return ExitValue;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int CommandStrExe(String CommandStr) throws IOException {
        Process P;
        int ExitValue;
        try {
            System.out.println(new Date() + "\t" + CommandStr);
            P = Runtime.getRuntime().exec(CommandStr);
            Thread ErrThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getErrorStream()));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                        }
                        bufferedReaderIn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread OutThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getInputStream()));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                        }
                        bufferedReaderIn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                OutThread.start();
                ErrThread.start();
                OutThread.join();
                ErrThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ExitValue = P.waitFor();
            return ExitValue;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void SortFile(String InFile, int[] Row, String Model, String Regex, String OutFile, int Threads) throws IOException {
        System.out.print(new Date() + "\tBegin to sort file " + InFile + " by number\tsort row is");
        for (int row : Row) {
            System.out.print(" " + row);
        }
        System.out.println();
        if (Regex.isEmpty()) {
            Regex = "\\s+";
        }
        long LineNumber = Statistics.CalculatorLineNumber(InFile);
        System.out.println(new Date() + "\t" + InFile + " Line Number:\t" + LineNumber);
        long LineBin = LineNumber / (Threads * 10) + 1;
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        if (Model.matches(".*n.*")) {
            //数字排序
            String line = null;
            long linecount = 0;
            int bin = 0;
            String[] str;
            int[] tempint = new int[Row.length];
            while (true) {
                ArrayList<char[]> List = new ArrayList<>();
                SortListInt SortList = new SortListInt();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line.toCharArray());
                    str = line.split(Regex);
                    for (int i = 0; i < Row.length; i++) {
                        tempint[i] = Integer.parseInt(str[Row[i] - 1]);
                    }
                    SortList.add(tempint);
                    linecount++;
                }
                if (SortList.size() != 0) {
                    bin++;
                    SortList.QuickSort(0, SortList.size() - 1, Model, (int) (Math.log(Threads) / Math.log(2)));
                    //====================================排序前后分割线==============================================
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile + ".temp" + bin));
//                    System.out.println(new Date() + "\tWrite " + OutFile + ".temp" + bin);
                    for (int i = 0, len = SortList.size(); i < len; i++) {
                        outfile.write(List.get(SortList.get(i)[0]));
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
            CommonMethod.MergeSortedFile(tempfile, OutFile, Row, Model, Regex);
            for (int i = 0; i < tempfile.length; i++) {
                new File(tempfile[i]).delete();
            }
        } else {
            //字符串排序
            String line = null;
            long linecount = 0;
            int bin = 0;
            String[] str;
            String[] tempstr = new String[Row.length];
            while (true) {
                ArrayList<char[]> List = new ArrayList<>();
                SortListStr SortList = new SortListStr();
                while (linecount < LineBin && (line = infile.readLine()) != null) {
                    List.add(line.toCharArray());
                    str = line.split(Regex);
                    for (int i = 0; i < Row.length; i++) {
                        tempstr[i] = str[Row[i] - 1];
                    }
                    SortList.add(tempstr);
                    linecount++;
                }
                if (SortList.size() != 0) {
                    bin++;
                    SortList.QuickSort(0, SortList.size() - 1, Model, (int) (Math.log(Threads) / Math.log(2)));
                    //====================================排序前后分割线==============================================
                    BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile + ".temp" + bin));
//                    System.out.println(new Date() + "\tWrite " + OutFile + ".temp" + bin);
                    for (int i = 0, len = SortList.size(); i < len; i++) {
                        outfile.write(List.get(Integer.parseInt(String.valueOf(SortList.get(i)[0]))));
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
            CommonMethod.MergeSortedFile(tempfile, OutFile, Row, Model, Regex);
            for (int i = 0; i < tempfile.length; i++) {
                new File(tempfile[i]).delete();
            }
        }
        System.out.print(new Date() + "\tEnd sort file " + InFile + " by number\tsort row is");
        for (int row : Row) {
            System.out.print(" " + row);
        }
        System.out.println();
    }

    public static void MergeSortedFile(String[] InFile, String OutFile, int[] Row, String Model, String Regex) throws IOException {
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
            LinkedList<int[]> list = new LinkedList<>();
            for (int i = 0; i < infile.length; i++) {
                line[i] = infile[i].readLine();
                if (line[i] == null) {
                    continue;
                }
                str = line[i].split(regex);
                int[] tempint = new int[Row.length + 1];
                tempint[0] = i;
                for (int j = 0; j < Row.length; j++) {
                    tempint[j + 1] = Integer.parseInt(str[Row[j] - 1]);
                }
                //================插入链表=========================
                int index = 0;
                for (int[] s : list) {
                    if (CommonMethod.CompareTo(Arrays.copyOfRange(s, 1, s.length), Arrays.copyOfRange(tempint, 1, tempint.length)) <= 0) {
                        index++;
                    } else {
                        list.add(index, tempint);
                        break;
                    }
                }
                if (index == list.size()) {
                    list.add(tempint);
                }
            }
            //=================================================
            while (list.size() > 1) {
                int[] ints = list.removeFirst();
                outfile.write(line[ints[0]] + "\n");
                if ((line[ints[0]] = infile[ints[0]].readLine()) != null) {
                    int[] tempint = new int[Row.length + 1];
                    tempint[0] = ints[0];
                    str = line[ints[0]].split(regex);
                    for (int j = 0; j < Row.length; j++) {
                        tempint[j + 1] = Integer.parseInt(str[Row[j] - 1]);
                    }
                    int index = 0;
                    for (int[] s : list) {
                        if (CommonMethod.CompareTo(Arrays.copyOfRange(s, 1, s.length), Arrays.copyOfRange(tempint, 1, tempint.length)) <= 0) {
                            index++;
                        } else {
                            list.add(index, tempint);
                            break;
                        }
                    }
                    if (index == list.size()) {
                        list.add(tempint);
                    }
                } else {
                    infile[ints[0]].close();
                }
            }
            int[] tempint = list.removeFirst();
            outfile.write(line[tempint[0]] + "\n");
            int index = tempint[0];
            while ((line[index] = infile[index].readLine()) != null) {
                outfile.write(line[index] + "\n");
            }
        } else {
            //字符串排序
            LinkedList<String[]> list = new LinkedList<>();
            for (int i = 0; i < infile.length; i++) {
                line[i] = infile[i].readLine();
                if (line[i] == null) {
                    continue;
                }
                str = line[i].split(regex);
                String[] tempstr = new String[Row.length + 1];
                tempstr[0] = String.valueOf(i);
                for (int j = 0; j < Row.length; j++) {
                    tempstr[j + 1] = str[Row[j] - 1];
                }
                //================插入链表=========================
                int index = 0;
                for (String[] s : list) {
                    if (CommonMethod.CompareTo(Arrays.copyOfRange(s, 1, s.length), Arrays.copyOfRange(tempstr, 1, tempstr.length)) <= 0) {
                        index++;
                    } else {
                        list.add(index, tempstr);
                        break;
                    }
                }
                if (index == list.size()) {
                    list.add(tempstr);
                }
            }
            //======================================================================================================
            while (list.size() > 1) {
                str = list.removeFirst();
                outfile.write(line[Integer.parseInt(str[0])] + "\n");
                if ((line[Integer.parseInt(str[0])] = infile[Integer.parseInt(str[0])].readLine()) != null) {
                    String[] tempstr = new String[Row.length + 1];
                    tempstr[0] = str[0];
                    str = line[Integer.parseInt(str[0])].split(regex);
                    for (int j = 0; j < Row.length; j++) {
                        tempstr[j + 1] = str[Row[j] - 1];
                    }
                    int index = 0;
                    for (String[] s : list) {
                        if (CommonMethod.CompareTo(Arrays.copyOfRange(s, 1, s.length), Arrays.copyOfRange(tempstr, 1, tempstr.length)) <= 0) {
                            index++;
                        } else {
                            list.add(index, tempstr);
                            break;
                        }
                    }
                    if (index == list.size()) {
                        list.add(tempstr);
                    }
                } else {
                    infile[Integer.parseInt(str[0])].close();
                }
            }
            str = list.removeFirst();
            outfile.write(line[Integer.parseInt(str[0])] + "\n");
            int index = Integer.parseInt(str[0]);
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

    public static Hashtable<String, Integer> GetChromosomeSize(String InFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        Hashtable<String, Integer> ChrSizeList = new Hashtable<>();
        String line;
        String[] str;
        if (InFile.matches(".+\\.ann")) {
            infile.readLine();
            while ((line = infile.readLine()) != null) {
                String Chr = line.split("\\s+")[1];
                line = infile.readLine();
                int Size = Integer.parseInt(line.split("\\s+")[1]);
                ChrSizeList.put(Chr, Size);
            }
        } else if (InFile.matches(".+\\.(fna|fa|fasta)")) {
            String Chr = "";
            int Size = 0;
            while ((line = infile.readLine()) != null) {
                if (line.matches("^>.+")) {
                    Chr = line.split("\\s+")[0].replace(">", "");
                    break;
                }
            }
            while ((line = infile.readLine()) != null) {
                if (line.matches("^>.+")) {
                    ChrSizeList.put(Chr, Size);
                    Chr = line.split("\\s+")[0].replace(">", "");
                    Size = 0;
                } else {
                    Size += line.length();
                }
            }
        } else if (InFile.matches(".+\\.sam")) {
            while ((line = infile.readLine()).matches("^@.+")) {
                if (line.matches("^@SQ.+")) {
                    str = line.split("\\s+");
                    ChrSizeList.put(str[1].replace("SN:", ""), Integer.parseInt(str[2].replace("LN:", "")));
                }
            }
        }
        infile.close();
        return ChrSizeList;
    }

    public static void PrintMatrix(int[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
        BufferedWriter twodfile = new BufferedWriter(new FileWriter(TwoDMatrixFile));
        BufferedWriter sparefile = new BufferedWriter(new FileWriter(SpareMatrix));
        //打印二维矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                twodfile.write(Matrix[i][j] + "\t");
            }
            twodfile.write("\n");
        }
        //打印稀疏矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j] != 0) {
                    sparefile.write(String.valueOf(i + 1) + "\t" + String.valueOf(j + 1) + "\t" + Matrix[i][j] + "\n");
                }
            }
        }
        sparefile.close();
        twodfile.close();
    }

    public static void PrintMatrix(double[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
        BufferedWriter twodfile = new BufferedWriter(new FileWriter(TwoDMatrixFile));
        BufferedWriter sparefile = new BufferedWriter(new FileWriter(SpareMatrix));
        //打印二维矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                twodfile.write(Matrix[i][j] + "\t");
            }
            twodfile.write("\n");
        }
        //打印稀疏矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j] != 0) {
                    sparefile.write(String.valueOf(i + 1) + "\t" + String.valueOf(j + 1) + "\t" + Matrix[i][j] + "\n");
                }
            }
        }
        sparefile.close();
        twodfile.close();
    }

    public static void PrintList(ArrayList<String> List, String OutFile) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        for (String s : List) {
            outfile.write(s + "\n");
        }
        outfile.close();
    }

    public static int[] CalculatorBinSize(int[] ChrSize, int Resolution) {
        int[] ChrBinSize = new int[ChrSize.length];
        for (int i = 0; i < ChrSize.length; i++) {
            ChrBinSize[i] = ChrSize[i] / Resolution + 1;
        }
        return ChrBinSize;
    }

    public static int CompareTo(String[] a, String[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i].compareTo(b[i]) != 0) {
                return a[i].compareTo(b[i]);
            }
        }
        return a.length - b.length;
    }

    public static int CompareTo(char[] a, char[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i] - b[i] != 0) {
                return a[i] - b[i];
            }
        }
        return a.length - b.length;
    }

    public static int CompareTo(int[] a, int[] b) {
        for (int i = 0; i < a.length && i < b.length; i++) {
            if (a[i] - b[i] != 0) {
                return a[i] - b[i];
            }
        }
        return a.length - b.length;
    }

}