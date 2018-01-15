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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * @author ligl
 */
public class LinkerFiltering {

    private String sequenceFile;
    private String linkerFile;
    private String outputPrefix;
    private String[] linkers;
    private int nLinkers = 0;
    private int[] scoreHist;
    private int[] secondBestScoreDiffHist;
    private int maxTagLength = 300;
    private int[] tagLengthDistribution = new int[maxTagLength];
    private int linkerLength;
    private int Threads;
    private int flip_tail;

    public LinkerFiltering(String sequenceFile, String linkerFile, String outputPrefix, int flip_tail, int Threads) throws IOException {
        this.sequenceFile = sequenceFile;
        this.linkerFile = linkerFile;
        this.outputPrefix = outputPrefix;
        this.flip_tail = flip_tail;
        this.Threads = Threads;
        readLinkers();
        if (nLinkers <= 0) {
            System.out.println("No linker sequence information. Stop!!!");
            System.exit(0);
        }
        if (nLinkers > 100) {
            System.out.println("Too many linkers. Please check!!!");
            System.exit(0);
        }
        filterSequenceByLinker();
        printDistribution();
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 5) {
            new LinkerFiltering(args[0], args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } else {
            System.out.println("Usage: java LinkerFiltering <sequence file> <linker file> <output prefix> <flip_tail> <threads>");
            System.out.println("flip_tail: 1: output the reverseComplement of the tail;");
            System.out.println("           0: output the original tail sequences");
            System.exit(0);
        }
    }

    private void printDistribution() throws IOException {
        PrintWriter fileOut = new PrintWriter(new FileOutputStream(this.outputPrefix + ".ScoreDistribution.txt"));

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

    public void filterSequenceByLinker() throws IOException {
        BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(this.sequenceFile)));
        PrintWriter fileOutAA = new PrintWriter(new FileOutputStream(this.outputPrefix + ".output.txt"));
        Thread[] Process = new Thread[Threads];
        final long[] nLines = {0};
        for (int i = 0; i < Threads; i++) {

            Process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                        String line;
                        while ((line = fileIn.readLine()) != null) {
                            if (line.length() <= 0) {
                                continue;  // skip the short lines
                            }
                            line = line.trim();
                            String result = processOneLine(line);
                            synchronized (Process) {
                                fileOutAA.write(result + "\n");
                                nLines[0]++;
                                if (nLines[0] % 1000000 == 0) {
                                    fileOutAA.flush();
                                    System.out.println(new Date() + "\t" + (nLines[0] / 1000000) + " Million reads processed");
                                }
                            }
                        }
                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");

                    } catch (IOException e) {
                        e.printStackTrace();
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
        System.out.println(new Date() + "\t" + nLines[0] + " reads processed in total.");
//        while ((line = fileIn.readLine()) != null) {
//            if (line.length() <= 0) {
//                continue;  // skip the short lines
//            }
//            line = line.trim();
//            processOneLine(line, fileOutAA);
//            nLines++;
//            if (nLines % 1000000 == 0) {
//                fileOutAA.flush();
//                System.out.println((nLines / 1000000) + " Million reads processed");
//            }
//        }
        fileIn.close();
        fileOutAA.close();
    }

    public String processOneLine(String seq) throws IOException {
        // Align different linkers
        int bestScore = -1;
        int secondBestScore = -1;
        int bestIndex = -1;
        int minI = -1;
        int minJ = -1;
        int seqLen = 0;
        int linkerLen = 0;
        LocalAlignment localAligner = new LocalAlignment(linkerLength, linkerLength);
        scoreHist = new int[this.linkerLength + 1];
        Arrays.fill(scoreHist, 0);
        secondBestScoreDiffHist = new int[this.linkerLength * 2 + 1];
        Arrays.fill(secondBestScoreDiffHist, 0);
        for (int i = 0; i < this.linkers.length; i++) {
            localAligner.Align(this.linkers[i], seq);
            seqLen = seq.length();
            if (this.linkers[i].length() != 0) {
                linkerLen = this.linkers[i].length();
            }
            //System.out.println(i);
            int score = localAligner.getScore();
            if (bestScore < score) {
                secondBestScore = bestScore;
                bestScore = score;
                bestIndex = i;
                minI = localAligner.getMinI(); // index in linker
                minJ = localAligner.getMinJ(); // index in sequence
            } else if (secondBestScore < score) {
                secondBestScore = score;
            }
        }
        if ((bestScore >= 0) && (bestScore <= this.linkerLength)) {
            scoreHist[bestScore]++;
        }
        int secondBestScoreDiff = bestScore - secondBestScore;
        if ((secondBestScoreDiff >= 0) && (secondBestScoreDiff <= 2 * this.linkerLength)) {
            secondBestScoreDiffHist[secondBestScoreDiff]++;
        }

        int tag_Start = 0;
        int tag_End = minJ - minI;

        if (tag_End < 0) {
            tag_End = 0;
        }
        if (tag_End < 0) {
            tagLengthDistribution[0]++;
        } else if (tag_End >= maxTagLength) {
            tagLengthDistribution[maxTagLength - 1]++;
        } else {
            tagLengthDistribution[tag_End]++;
        }

        //output format
        // 1: sequence before the linker
        // 2: sequence after the linker (including linker sequence)
        // 3. sequence after the linker
        // 4: index of best linker (if there are multiple linkers; the index starts from 0)
        // 5: best local alignment score
        // 6: difference between the best alignment score and the second-best alignment score
        // 7: start position of the best alignment in the input sequence
        // 8: start position of the best alignment in the linker sequence
        // 9. tag1 Length
        // 10.tag2 Length
        if (tag_End + linkerLen < seqLen) {
            if (flip_tail == 1) {
                return seq.substring(tag_Start, tag_End) + "\t" + String.valueOf(tag_End + linkerLen + 1) + "\t" + revComplement(seq.substring(tag_End + linkerLen)) + "\t" + bestIndex + "\t" + bestScore + "\t" + secondBestScoreDiff + "\t" + minJ + "\t" + minI + "\t" + tag_End + "\t" + (seqLen - tag_End - linkerLen);
            } else {
                return seq.substring(tag_Start, tag_End) + "\t" + String.valueOf(tag_End + linkerLen + 1) + "\t" + seq.substring(tag_End + linkerLen) + "\t" + bestIndex + "\t" + bestScore + "\t" + secondBestScoreDiff + "\t" + minJ + "\t" + minI + "\t" + tag_End + "\t" + (seqLen - tag_End - linkerLen);
            }
        } else {
            return seq.substring(tag_Start, tag_End) + "\tNA\tNA\t" + bestIndex + "\t" + bestScore + "\t" + secondBestScoreDiff + "\t" + minJ + "\t" + minI + "\t" + tag_End + "\t0";
        }
    }

    public void readLinkers() throws IOException {
        BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(this.linkerFile)));
        ArrayList<String> tempLinkers = new ArrayList<String>();
        String line;
        while ((line = fileIn.readLine()) != null) {
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
        nLinkers = linkers.length;
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
}
