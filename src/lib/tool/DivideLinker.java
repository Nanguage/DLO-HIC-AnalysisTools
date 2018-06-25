package lib.tool;

import lib.unit.Default;
import lib.unit.Opts;

import java.io.*;
import java.util.Date;

public class DivideLinker {
    private File PastFile;
    private String Prefix;
    private String[] LinkerList;
    private Long[] LinkerCount;
    private File[] R1FastqFile;
    private File[] R2FastqFile;
    private int MinReadsLength;
    private int MaxReadsLength;
    private int MinScore;
    private String Restriction;
    private int Phred;
    private int Type;
    private String[] MatchSeq = new String[2];
    private String[] AppendSeq = new String[2];
    private String[] AppendQuality = new String[2];
    private int Threads = 1;
    public static final int First = 1;
    public static final int Second = 2;
    public static final int All = 3;

    public DivideLinker(File past_file, String prefix, String[] linker_list, String restriction, int min_score, int type) {
        this(past_file, prefix, linker_list, restriction, type, Default.MinReadsLen, Default.MaxReadsLen, min_score, Opts.Phred33);
    }

    public DivideLinker(File past_file, String prefix, String[] linker_list, String restriction, int type, int min_reads_length, int max_reads_length, int min_score, int phred) {
        PastFile = past_file;
        Prefix = prefix;
        LinkerList = linker_list;
        Restriction = restriction;
        Type = type;
        MinReadsLength = min_reads_length;
        MaxReadsLength = max_reads_length;
        MinScore = min_score;
        Phred = phred;
        Init();
    }

    private void Init() {
        R1FastqFile = new File[LinkerList.length];
        R2FastqFile = new File[LinkerList.length];
        LinkerCount = new Long[LinkerList.length];
        for (int i = 0; i < LinkerCount.length; i++) {
            LinkerCount[i] = 0L;
        }
        for (int i = 0; i < LinkerList.length; i++) {
            R1FastqFile[i] = new File(Prefix + "." + LinkerList[i] + ".R1.fastq");
            R2FastqFile[i] = new File(Prefix + "." + LinkerList[i] + ".R2.fastq");
        }
        int[] length = new int[]{Restriction.indexOf("^"), Restriction.replace("^", "").length() - Restriction.indexOf("^")};
        MatchSeq[0] = Restriction.replace("^", "").substring(0, Math.max(length[0], length[1]));
        MatchSeq[1] = Restriction.replace("^", "").substring(Math.min(length[0], length[1]), Restriction.replace("^", "").length());
        AppendSeq[0] = Restriction.replace("^", "").substring(Math.max(length[0], length[1]), Restriction.replace("^", "").length());
        AppendSeq[1] = Restriction.replace("^", "").substring(0, Math.min(length[0], length[1]));
        if (Phred == Opts.Phred33) {
            AppendQuality[0] = AppendSeq[0].replaceAll(".", "I");
            AppendQuality[1] = AppendSeq[1].replaceAll(".", "I");
        } else if (Phred == Opts.Phred64) {
            AppendQuality[0] = AppendSeq[0].replaceAll(".", "h");
            AppendQuality[1] = AppendSeq[1].replaceAll(".", "h");
        } else {
            System.out.println("Error Phred:\t" + Phred);
        }
        if (!PastFile.isFile()) {
            System.err.println("No such file " + PastFile);
            System.exit(1);
        }
    }

    public void Run() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new FileReader(PastFile));
        BufferedWriter[] r1_writer = new BufferedWriter[R1FastqFile.length];//R1 output file list
        BufferedWriter[] r2_writer = new BufferedWriter[R2FastqFile.length];//R2 output file list
        switch (Type) {
            case First://only output R1
                for (int i = 0; i < R1FastqFile.length; i++) {
                    r1_writer[i] = new BufferedWriter(new FileWriter(R1FastqFile[i]));
                }
                break;
            case Second://only output R2
                for (int i = 0; i < R2FastqFile.length; i++) {
                    r2_writer[i] = new BufferedWriter(new FileWriter(R2FastqFile[i]));
                }
                break;
            case All://output R1 and R2
                for (int i = 0; i < R1FastqFile.length; i++) {
                    r1_writer[i] = new BufferedWriter(new FileWriter(R1FastqFile[i]));
                }
                for (int i = 0; i < R2FastqFile.length; i++) {
                    r2_writer[i] = new BufferedWriter(new FileWriter(R2FastqFile[i]));
                }
                break;
            default:
                System.out.println("Error Type:\t" + Type);
                System.exit(1);
        }
        System.out.println(new Date() + "\tBegin to divide linker");
        System.out.print(new Date() + "\tLinker type:\t");
        for (int i = 0; i < LinkerList.length; i++) {
            System.out.print(LinkerList[i] + "\t");
        }
        System.out.println();
        Thread[] Process = new Thread[Threads];//multi-thread
        for (int i = 0; i < Process.length; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String Line;
                    String[] Str;
                    String OutString;
                    String OutString1;
                    String OutString2;
                    try {
                        while ((Line = reader.readLine()) != null) {
                            Str = Line.split("\t");
                            for (int j = 0; j < LinkerList.length; j++) {
                                //find out which kind of linker belong to
                                if (Integer.parseInt(Str[4]) == j && Integer.parseInt(Str[5]) >= MinScore) {
                                    synchronized (LinkerList[j]) {
                                        LinkerCount[j]++;
                                    }
                                    if (Type == First) {
                                        try {
                                            OutString = ParseFirst(Str);
                                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                            break;
                                        }
                                        synchronized (LinkerList[j]) {
                                            r1_writer[j].write(OutString);
                                        }
                                    } else if (Type == Second) {
                                        try {
                                            OutString = ParseSecond(Str);
                                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                            break;
                                        }
                                        synchronized (LinkerList[j]) {
                                            r2_writer[j].write(OutString);
                                        }
                                    } else {
                                        try {
                                            OutString1 = ParseFirst(Str);
                                            OutString2 = ParseSecond(Str);
                                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                            break;
                                        }
                                        synchronized (LinkerList[j]) {
                                            r1_writer[j].write(OutString1);
                                            r2_writer[j].write(OutString2);
                                        }
                                    }
                                    break;
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
        for (Thread Proces : Process) {
            Proces.join();
        }
        switch (Type) {
            case First://only output R1
                for (int i = 0; i < R1FastqFile.length; i++) {
                    r1_writer[i].close();
                }
                break;
            case Second://only output R2
                for (int i = 0; i < R2FastqFile.length; i++) {
                    r2_writer[i].close();
                }
                break;
            case All://output R1 and R2
                for (int i = 0; i < R1FastqFile.length; i++) {
                    r1_writer[i].close();
                }
                for (int i = 0; i < R2FastqFile.length; i++) {
                    r2_writer[i].close();
                }
                break;
        }
        System.out.println(new Date() + "\tDivide " + PastFile + " end");
    }

    private String ParseFirst(String[] S) throws NumberFormatException, IndexOutOfBoundsException {
        StringBuilder FastqString = new StringBuilder();
        String ReadsTitle = S[6];
        String ReadsSeq = S[0].replace("N", "");
        String Orientation = S[8];
        String Quality = S[9].substring(Integer.parseInt(S[1]) - ReadsSeq.length(), Integer.parseInt(S[1]));
        //---------------------------------------------------
        FastqString.append(ReadsTitle + "\n");//reads title
        if (AppendBase(ReadsSeq, MatchSeq[0], First)) {
            if (ReadsSeq.length() <= MaxReadsLength + 10) {
                FastqString.append(ReadsSeq + AppendSeq[0] + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality + AppendQuality[0] + "\n");
            } else {
                FastqString.append(ReadsSeq.substring(ReadsSeq.length() - MaxReadsLength, ReadsSeq.length()) + AppendSeq[0] + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality.substring(Quality.length() - MaxReadsLength, Quality.length()) + AppendQuality[0] + "\n");
            }
        } else {
            if (ReadsSeq.length() <= MaxReadsLength + 10) {
                FastqString.append(ReadsSeq + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality + "\n");
            } else {
                FastqString.append(ReadsSeq.substring(ReadsSeq.length() - MaxReadsLength, ReadsSeq.length()) + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality.substring(Quality.length() - MaxReadsLength, Quality.length()) + "\n");
            }
        }
        return FastqString.toString();
    }

    private String ParseSecond(String[] S) throws NumberFormatException, IndexOutOfBoundsException {
        StringBuilder FastqString = new StringBuilder();
        String ReadsTitle = S[6];
        String ReadsSeq = S[3].replace("N", "");
        String Orientation = S[8];
        String Quality = S[9].substring(Integer.parseInt(S[2]) + 1, Integer.parseInt(S[2]) + 1 + ReadsSeq.length());
        //------------------------------------------------------
        FastqString.append(ReadsTitle + "\n");
        if (AppendBase(ReadsSeq, MatchSeq[1], Second)) {
            if (ReadsSeq.length() <= MaxReadsLength + 10) {
                FastqString.append(AppendSeq[1] + ReadsSeq + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(AppendQuality[1] + Quality + "\n");
            } else {
                FastqString.append(AppendSeq[1] + ReadsSeq.substring(0, MaxReadsLength) + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(AppendQuality[1] + Quality.substring(0, MaxReadsLength) + "\n");
            }
        } else {
            if (ReadsSeq.length() <= MaxReadsLength + 10) {
                FastqString.append(ReadsSeq + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality + "\n");
            } else {
                FastqString.append(ReadsSeq.substring(0, MaxReadsLength) + "\n");
                FastqString.append(Orientation + "\n");
                FastqString.append(Quality.substring(0, MaxReadsLength) + "\n");
            }
        }
        return FastqString.toString();
    }

    public void setThreads(int threads) {
        Threads = threads;
    }

    public Long[] getLinkerCount() {
        return LinkerCount;
    }

    public File[] getR1FastqFile() {
        return R1FastqFile;
    }

    public File[] getR2FastqFile() {
        return R2FastqFile;
    }

    private Boolean AppendBase(String Sequence, String Restriction, int Type) {
        switch (Type) {
            case First:
                return Sequence.substring(Sequence.length() - Restriction.length(), Sequence.length()).equals(Restriction);
            case Second:
                return Sequence.substring(0, Restriction.length()).equals(Restriction);
            default:
                System.err.println(new Date() + "\tError parameter in  append one base\t" + Type);
                System.exit(0);
                return false;
        }
    }//OK
}
