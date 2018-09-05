package lib.tool;


import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

import lib.unit.CustomFile;
import lib.unit.Opts;
import org.apache.commons.math3.distribution.*;

public class Statistic {

    public static Long[] CalculateLinkerCount(File InFile, String[] LinkerList, int MinScore, int Threads) throws IOException, InterruptedException {
        Long[] Count = new Long[LinkerList.length];
        for (int i = 0; i < Count.length; i++) {
            Count[i] = 0L;
        }
        BufferedReader reader = new BufferedReader(new FileReader(InFile));
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Process.length; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String Line;
                        String[] Str;
                        while ((Line = reader.readLine()) != null) {
                            Str = Line.split("\\t+");
                            if (Integer.parseInt(Str[5]) >= MinScore) {
                                for (int j = 0; j < LinkerList.length; j++) {
                                    if (Integer.parseInt(Str[4]) == j) {
                                        synchronized (LinkerList[j]) {
                                            Count[j]++;
                                        }
                                        break;
                                    }
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
        for (int i = 0; i < Process.length; i++) {
            Process[i].join();
        }
        return Count;
    }

    public static long RangeCount(CustomFile BedpeFile, double Min, double Max, int Threads) throws IOException, InterruptedException {
        if (!BedpeFile.isFile()) {
            return 0;
        }
        BufferedReader bedpe = new BufferedReader(new FileReader(BedpeFile));
        final long[] Count = {0};
        int[] Index = new int[4];
        switch (BedpeFile.BedpeDetect()) {
            case BedpePointFormat:
                Index = new int[]{3, 3, 1, 1};
                break;
            case BedpeRegionFormat:
                Index = new int[]{5, 4, 2, 1};
                break;
        }
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            int[] finalIndex = Index;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
                        while ((line = bedpe.readLine()) != null) {
                            str = line.split("\\s+");
                            int dis = Math.abs(Integer.parseInt(str[finalIndex[0]]) + Integer.parseInt(str[finalIndex[1]]) - Integer.parseInt(str[finalIndex[2]]) - Integer.parseInt(str[finalIndex[3]])) / 2;
                            if (dis <= Max && dis >= Min) {
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

    public static ArrayList<int[]> PowerLaw(CustomFile BedpeFile, int StepLength) throws IOException {
        ArrayList<int[]> List = new ArrayList<>();
        List.add(new int[]{0, StepLength, 0});
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        String line;
        String[] str;
        int distant;
        byte[] index = null;
        switch (BedpeFile.BedpeDetect()) {
            case BedpePointFormat:
                index = new byte[]{1, 1, 3, 3};
                break;
            case BedpeRegionFormat:
                index = new byte[]{5, 4, 2, 1};
                break;
            default:
                System.err.println("Error format!");
                return List;
        }
        while ((line = infile.readLine()) != null) {
            str = line.split("\\s+");
            distant = Math.abs(Integer.parseInt(str[index[0]]) + Integer.parseInt(str[index[1]]) - Integer.parseInt(str[index[2]]) - Integer.parseInt(str[index[3]])) / 2;
            int i = 0;
            while (i < List.size()) {
                if (distant > List.get(i)[1]) {
                    i++;
                } else {
                    List.get(i)[2]++;
                    break;
                }
            }
            if (i == List.size()) {
                List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                while (List.get(i)[1] < distant) {
                    i++;
                    List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                }
                List.get(i)[2]++;
            }
        }
        return List;
    }

    /**
     * 阶乘
     *
     * @param k
     * @return
     */
    public static long Factorial(int k) {
        return k == 0 ? 1 : k * Factorial(k - 1);
    }

    public static double Average(Object[] arrays) {
        double sum = 0;
        for (int i = 0; i < arrays.length; i++) {
            sum += Double.parseDouble(arrays[i].toString());
        }
//        for (double a : (Double[]) arrays) {
//            sum += a;
//        }
        return sum / arrays.length;
    }

    public static void main(String[] args) {
        double s = 0;
        int num = 7;
        PoissonDistribution p = new PoissonDistribution(4.56);
        s = p.cumulativeProbability(num);
        System.out.println(s);
    }

}
