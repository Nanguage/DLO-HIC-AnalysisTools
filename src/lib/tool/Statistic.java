package lib.tool;

import java.io.*;
import java.util.Hashtable;

public class Statistic {
    public static long CalculatorLineNumber(String InFile) throws IOException {
        if (!new File(InFile).isFile()) {
            return 0;
        }
        BufferedReader file = new BufferedReader(new FileReader(InFile));
        long LineNumber = 0;
        while (file.readLine() != null) {
            LineNumber++;
        }
        file.close();
        return LineNumber;
    }
    public static long RangeCount(String Bedpe, double Min, double Max, int Threads) throws IOException, InterruptedException {
        if (!new File(Bedpe).isFile()) {
            return 0;
        }
        BufferedReader bedpe = new BufferedReader(new FileReader(Bedpe));
        final long[] Count = {0};
        String line;
        String[] str;
        line = bedpe.readLine();
        str = line.split("\\s+");
        try {
            if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) >= Min) {
                Count[0]++;
            }
            Thread[] Process = new Thread[Threads];
            for (int i = 0; i < Threads; i++) {
                Process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
                            while ((line = bedpe.readLine()) != null) {
                                str = line.split("\\s+");
                                if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) >= Min) {
                                    synchronized (Thread.class) {
                                        Count[0]++;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
                Process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                Process[i].join();
            }
        } catch (NumberFormatException e) {
            if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) >= Min) {
                Count[0]++;
            }
            Thread[] Process = new Thread[Threads];
            for (int i = 0; i < Threads; i++) {
                Process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
                            while ((line = bedpe.readLine()) != null) {
                                str = line.split("\\s+");
                                if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) >= Min) {
                                    synchronized (Thread.class) {
                                        Count[0]++;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                Process[i].join();
            }
        }
        bedpe.close();
        return Count[0];
    }
    public static int[] CalculatorBinSize(int[] ChrSize, int Resolution) {
        int[] ChrBinSize = new int[ChrSize.length];
        for (int i = 0; i < ChrSize.length; i++) {
            ChrBinSize[i] = ChrSize[i] / Resolution + 1;
        }
        return ChrBinSize;
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
    public static Hashtable<String, Integer> FindRestrictionSite(String FastFile, String Restriction, String Prefix) throws IOException {
        BufferedReader fastfile = new BufferedReader(new FileReader(FastFile));
        BufferedWriter chrwrite;
        Hashtable<String, Integer> ChrSize = new Hashtable<>();
        StringBuilder Seq = new StringBuilder();
        String line;
        String Chr = "";
        int Site = Restriction.indexOf("^");
        Restriction = Restriction.replace("^", "");
        int ResLength = Restriction.length();
        //找到第一个以 ">" 开头的行
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                Chr = line.split("\\s+")[0].replace(">", "");
                break;
            }
        }
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                int Count = 0;
                int len = Seq.length();
                chrwrite = new BufferedWriter(new FileWriter(Prefix + "." + Chr + ".txt"));
                chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
                ChrSize.put(Chr, len);
                for (int i = 0; i <= len - ResLength; i++) {
                    if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                        Count++;
                        chrwrite.write(Count + "\t+\t" + Chr + "\t" + String.valueOf(i + Site) + "\n");
                    }
                }
                chrwrite.write(++Count + "\t+\t" + Chr + "\t" + len + "\n");
                chrwrite.close();
                Seq.setLength(0);
                Chr = line.split("\\s+")[0].replace(">", "");
            } else {
                Seq.append(line);
            }
        }
        //========================================打印最后一条染色体=========================================
        int Count = 0;
        int len = Seq.length();
        chrwrite = new BufferedWriter(new FileWriter(Prefix + "." + Chr + ".txt"));
        chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
        ChrSize.put(Chr, len);
        for (int i = 0; i <= len - ResLength; i++) {
            if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                Count++;
                chrwrite.write(Count + "\t+\t" + Chr + "\t" + String.valueOf(i + Site) + "\n");
            }
        }
        chrwrite.write(++Count + "\t+\t" + Chr + "\t" + len + "\n");
        chrwrite.close();
        Seq.setLength(0);
        return ChrSize;
    }
}
