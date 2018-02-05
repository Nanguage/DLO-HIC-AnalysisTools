/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *
 * Input:
 * 1) sequence file in text format
 * 2) linker sequences
 * 3) threshold
 *
 * Assumptions:
 * 1) there are ?? possible linker sequences
 * 2) all the linkers have the same length
 *
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author snowf
 */
public class SequenceFiltering {

    private String FastqFile;
    private String LinkerFile;
    private String AdapterFile;
    private String OutputPrefix;
    private String OutFile;
    private String DistributionFile;
    private String[] linkers;
    private String[] Adapters;
    private String[] AdapterAlignment;
    private int AdapterLength;
    private int LinkersNum = 0;
    private int[] scoreHist;
    private int[] secondBestScoreDiffHist;
    private int maxTagLength = 300;
    private int[] tagLengthDistribution = new int[maxTagLength];
    private int linkerLength;
    private int Threads;
    private int flip_tail;
    private int MatchScore;
    private int MisMatchScore;
    private int IndelScore;

    SequenceFiltering(String fastqFile, String linkerFile, String outputPrefix, int flip_tail, int Threads) throws IOException {
        FastqFile = fastqFile;
        LinkerFile = linkerFile;
        OutputPrefix = outputPrefix;
        this.flip_tail = flip_tail;
        this.Threads = Threads;
        MatchScore = 1;
        MisMatchScore = -1;
        IndelScore = -1;
        Init();
    }

    SequenceFiltering(String fastqFile, String linkerFile, String adapterFile, String outputPrefix, int flip_tail, int Threads) throws IOException {
        FastqFile = fastqFile;
        LinkerFile = linkerFile;
        AdapterFile = adapterFile;
        OutputPrefix = outputPrefix;
        this.flip_tail = flip_tail;
        this.Threads = Threads;
        MatchScore = 1;
        MisMatchScore = -1;
        IndelScore = -1;
        Init();
    }

    SequenceFiltering(String fastqFile, String linkerFile, String outputPrefix, int matchscore, int mismatchscore, int indelscore, int flip_tail, int Threads) throws IOException {
        FastqFile = fastqFile;
        LinkerFile = linkerFile;
        OutputPrefix = outputPrefix;
        this.flip_tail = flip_tail;
        this.Threads = Threads;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Init();
    }

    SequenceFiltering(String fastqFile, String linkerFile, String adapterFile, String outputPrefix, int matchscore, int mismatchscore, int indelscore, int flip_tail, int Threads) throws IOException {
        FastqFile = fastqFile;
        LinkerFile = linkerFile;
        AdapterFile = adapterFile;
        OutputPrefix = outputPrefix;
        this.flip_tail = flip_tail;
        this.Threads = Threads;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Init();
    }

    private void Init() throws IOException {
        ReadLinkers();
        if (LinkersNum <= 0) {
            System.out.println("No linker sequence information. Stop!!!");
            System.exit(0);
        }
        if (LinkersNum > 100) {
            System.out.println("Too many linkers. Please check!!!");
            System.exit(0);
        }
        if (AdapterFile != null && !AdapterFile.equals("")) {
            ReadAdapter();
        }
        if (Adapters != null) {
            AdapterAlignment = new String[Adapters.length];
            for (int i = 0; i < Adapters.length; i++) {
                try {
                    AdapterAlignment[i] = Adapters[i].substring(0, 30);
                } catch (IndexOutOfBoundsException e) {
                    AdapterAlignment[i] = Adapters[i];
                }
            }
        }
        OutFile = OutputPrefix + ".output.txt";
        DistributionFile = OutputPrefix + ".ScoreDistribution.txt";
    }

    public void Run() throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(FastqFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        final int[] Count = new int[]{0};
        LocalAlignment[] local = new LocalAlignment[Threads];
        for (int i = 0; i < Threads; i++) {
            local[i] = new LocalAlignment(MatchScore, MisMatchScore, IndelScore);
        }
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            int finalI = i;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line1 = "", line2 = "", line3 = "", line4 = "";
                    synchronized (Thread.class) {
                        try {
                            line1 = infile.readLine();
                            line2 = infile.readLine();
                            line3 = infile.readLine();
                            line4 = infile.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    while (line4 != null) {
                        float MaxScore = 0;
                        int Index = 0;
                        int MaxIndex = 0;
                        int MinIndex = 0;
                        if (AdapterAlignment != null) {
                            for (int j = 0; j < AdapterAlignment.length; j++) {
                                local[finalI].CreatMatrix(line2, AdapterAlignment[j]);
                                local[finalI].FindMaxIndex();
                                local[finalI].FindMinIndex();
                                float score = (float) local[finalI].getMaxScore() / AdapterAlignment[j].length();
                                int minindex = local[finalI].getMinIndex()[0];
                                if (score > MaxScore) {
                                    MaxScore = score;
                                    Index = j;
                                    MinIndex = minindex;
                                }
                            }
                            if (MaxScore > 0.7) {
                                line2 = line2.substring(0, MinIndex);
                            }
                            MaxScore = 0;
                        }
                        for (int j = 0; j < linkers.length; j++) {
                            local[finalI].CreatMatrix(line2, linkers[j]);
                            local[finalI].FindMaxIndex();
                            int score = local[finalI].getMaxScore();
                            if (score > MaxScore) {
                                local[finalI].FindMinIndex();
                                MaxScore = score;
                                Index = j;
                                MaxIndex = local[finalI].getMaxIndex()[0];
                                MinIndex = local[finalI].getMinIndex()[0];
                            }
                        }
                        //=====================================================输出结果==========================================
                        int TagStart = 0;
                        synchronized (Thread.class) {
                            try {
                                if (MaxIndex < line2.length()) {
                                    if (flip_tail == 1) {
                                        outfile.write(line2.substring(TagStart, MinIndex) + "\t" + String.valueOf(MinIndex) + "\t" + String.valueOf(MaxIndex - 1) + "\t" + revComplement(line2.substring(MaxIndex)) + "\t" + Index + "\t" + (int) MaxScore);
                                    } else {
                                        outfile.write(line2.substring(TagStart, MinIndex) + "\t" + String.valueOf(MinIndex) + "\t" + String.valueOf(MaxIndex - 1) + "\t" + line2.substring(MaxIndex) + "\t" + Index + "\t" + (int) MaxScore);
                                    }
                                } else {
                                    outfile.write(line2.substring(TagStart, MinIndex) + "\t" + String.valueOf(MinIndex) + "\tNA\tNA\t" + Index + "\t" + (int) MaxScore);
                                }
                                outfile.write("\t" + line1 + "\t" + line2 + "\t" + line3 + "\t" + line4 + "\n");
                                Count[0]++;
                                if (Count[0] % 1000000 == 0) {
                                    System.out.println(new Date() + "\t" + (Count[0] / 1000000) + " Million reads processed");
                                }
                                line1 = infile.readLine();
                                line2 = infile.readLine();
                                line3 = infile.readLine();
                                line4 = infile.readLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //=============================================================================================
                    }
                }
            });
            Process[i].start();
        }
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(new Date() + "\t" + Count[0] + " reads processed in total.");
        infile.close();
        outfile.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 5) {
            new SequenceFiltering(args[0], args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4])).Run();
        } else if (args.length == 6) {
            new SequenceFiltering(args[0], args[1], args[2], args[3], Integer.parseInt(args[4]), Integer.parseInt(args[5])).Run();
        } else {
            System.out.println("Usage: java SequenceFiltering <sequence file> <linker file> <output prefix> <flip_tail> <threads>");
            System.out.println("flip_tail: 1: output the reverseComplement of the tail;");
            System.out.println("           0: output the original tail sequences");
            System.exit(0);
        }
    }

    private void printDistribution() throws IOException {
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(DistributionFile));

        fileOut.println("schoreHist");
        for (int i = 0; i < scoreHist.length; i++) {
            fileOut.println(i + "\t" + scoreHist[i]);
        }

        fileOut.println("\nsecondBestScoreDiffHist");
        for (int i = 0; i < secondBestScoreDiffHist.length; i++) {
            fileOut.println(i + "\t" + secondBestScoreDiffHist[i]);
        }

        fileOut.println("\ntagLengthDistribution");
        for (int i = 0; i < tagLengthDistribution.length; i++) {
            fileOut.println(i + "\t" + tagLengthDistribution[i]);
        }
        fileOut.close();
    }

    private void ReadLinkers() throws IOException {
        BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(this.LinkerFile)));
        ArrayList<String> tempLinkers = new ArrayList<>();
        String line;
        while ((line = fileIn.readLine()) != null && !line.equals("")) {
            tempLinkers.add(line);
        }
        fileIn.close();
        this.linkers = new String[tempLinkers.size()];
        this.linkerLength = 0;
        for (int i = 0; i < linkers.length; i++) {
            this.linkers[i] = tempLinkers.get(i);
            if (this.linkerLength < this.linkers[i].length()) {
                this.linkerLength = this.linkers[i].length();
            }
        }
        LinkersNum = linkers.length;
    }

    private void ReadAdapter() throws IOException {
        BufferedReader adapterfile = new BufferedReader(new FileReader(AdapterFile));
        String line;
        ArrayList<String> templist = new ArrayList<>();
        while ((line = adapterfile.readLine()) != null && !line.equals("")) {
            templist.add(line);
        }
        adapterfile.close();
        Adapters = new String[templist.size()];
        for (int i = 0; i < Adapters.length; i++) {
            Adapters[i] = templist.get(i);
            if (AdapterLength < Adapters[i].length()) {
                AdapterLength = Adapters[i].length();
            }
        }
    }

    private static char[] complTable = new char[255];

    static {
        complTable['A'] = 'T';
        complTable['C'] = 'G';
        complTable['G'] = 'C';
        complTable['T'] = 'A';
        complTable['N'] = 'N';

        complTable['a'] = 't';
        complTable['c'] = 'g';
        complTable['g'] = 'c';
        complTable['t'] = 'a';
        complTable['n'] = 'n';
    }

    private static String revComplement(String seq) {
        StringBuilder result = new StringBuilder(seq);
        result.reverse();
        for (int i = seq.length() - 1; i >= 0; i--) {
            switch (result.charAt(i)) {
                case 'A':
                case 'C':
                case 'G':
                case 'T':
                case 'N':
                case 'a':
                case 'c':
                case 'g':
                case 't':
                case 'n':
                    result.setCharAt(i, complTable[result.charAt(i)]);
                    break;
                default:
                    break;
            }
        }
        return result.toString();
    }

    public String getDistributionFile() {
        return DistributionFile;
    }

    public String getOutFile() {
        return OutFile;
    }
}
