package lib.tool;

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
        this(past_file, prefix, linker_list, restriction, type, Opts.Default.MinReadsLen, Opts.Default.MaxReadsLen, min_score, Opts.Phred33);
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
        FastqString.append(S[6] + "\n");
        if (AppendBase(S[0], MatchSeq[0], First)) {
            FastqString.append(S[0] + AppendSeq[0] + "\n");
            FastqString.append(S[8] + "\n");
            FastqString.append(S[9].substring(Integer.parseInt(S[1]) - S[0].length(), Integer.parseInt(S[1])) + AppendQuality[0] + "\n");
        } else {
            FastqString.append(S[0] + "\n");
            FastqString.append(S[8] + "\n");
            FastqString.append(S[9].substring(Integer.parseInt(S[1]) - S[0].length(), Integer.parseInt(S[1])) + "\n");
        }
        return FastqString.toString();
    }

    private String ParseSecond(String[] S) throws NumberFormatException, IndexOutOfBoundsException {
        StringBuilder FastqString = new StringBuilder();
        FastqString.append(S[6] + "\n");
        if (AppendBase(S[3], MatchSeq[1], Second)) {
            if (S[3].length() <= 30) {
                FastqString.append(AppendSeq[1] + S[3] + "\n");
                FastqString.append(S[8] + "\n");
                FastqString.append(AppendQuality[1] + S[9].substring(Integer.parseInt(S[2]) + 1, Integer.parseInt(S[2]) + 1 + S[3].length()) + "\n");
            } else {
                FastqString.append(AppendSeq[1] + S[3].substring(0, MaxReadsLength) + "\n");
                FastqString.append(S[8] + "\n");
                FastqString.append(AppendQuality[1] + S[9].substring(Integer.parseInt(S[2]) + 1, Integer.parseInt(S[2]) + 1 + MaxReadsLength) + "\n");
            }
        } else {
            if (S[3].length() <= 30) {
                FastqString.append(S[3] + "\n");
                FastqString.append(S[8] + "\n");
                FastqString.append(S[9].substring(Integer.parseInt(S[2]) + 1, Integer.parseInt(S[2]) + 1 + S[3].length()) + "\n");
            } else {
                FastqString.append(S[3].substring(0, MaxReadsLength) + "\n");
                FastqString.append(S[8] + "\n");
                FastqString.append(S[9].substring(Integer.parseInt(S[2]) + 1, Integer.parseInt(S[2]) + 1 + MaxReadsLength) + "\n");
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

    /*public DivideLinker(String PastFastq, String[] LinkerFastq, int MinReadsLength, int MaxReadsLength, int MinLinkerFilterQuality, String Restriction, String AddQuality, String Type, int threads) throws IOException {
                BufferedReader fastq_read = new BufferedReader(new FileReader(PastFastq));
                BufferedWriter[] fastq_write = new BufferedWriter[LinkerFastq.length];
                for (int i = 0; i < LinkerFastq.length; i++) {
                    fastq_write[i] = new BufferedWriter(new FileWriter(LinkerFastq[i]));
                }
                String add = AddQuality;
                String MatchRestriction;
                String AddSeq;
                String TempSeq = Restriction.replace("^", "");
                int restrictionSite = Restriction.indexOf("^");
                if (Type.equals("R1")) {
                    if (restrictionSite < TempSeq.length() - restrictionSite) {
                        restrictionSite = TempSeq.length() - restrictionSite;
                    }
                    MatchRestriction = TempSeq.substring(0, restrictionSite);
                    try {
                        AddSeq = TempSeq.substring(restrictionSite);
                    } catch (IndexOutOfBoundsException e) {
                        AddSeq = "";
                    }
                } else if (Type.equals("R2")) {
                    if (restrictionSite > TempSeq.length() - restrictionSite) {
                        restrictionSite = TempSeq.length() - restrictionSite;
                    }
                    MatchRestriction = TempSeq.substring(restrictionSite);
                    try {
                        AddSeq = TempSeq.substring(0, restrictionSite);
                    } catch (IndexOutOfBoundsException e) {
                        AddSeq = "";
                    }
                } else {
                    MatchRestriction = "";
                    AddSeq = "";
                    System.err.println("Wrong Type " + Type);
                    System.exit(0);
                }
                for (int i = 1; i < AddSeq.length(); i++) {
                    AddQuality = AddQuality + add;
                }
                if (Type.equals("R1")) {
                    Thread[] process = new Thread[threads];
                    System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
                    //多线程读取
                    for (int i = 0; i < threads; i++) {
                        String finalAddSeq = AddSeq;
                        String finalAddQuality = AddQuality;
                        process[i] = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String line;
                                String[] str;
                                int len;
                                try {
        //                            System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " begin");
                                    while ((line = fastq_read.readLine()) != null) {
                                        str = line.split("\\t+");
                                        str[0] = str[0].replace("N", "");
                                        for (int j = 0; j < LinkerFastq.length; j++) {
                                            if (str[0].length() >= MinReadsLength && Integer.parseInt(str[5]) >= MinLinkerFilterQuality && Integer.parseInt(str[4]) == j) {
                                                len = Math.max(MaxReadsLength, str[0].length());
                                                if (AppendBase(str[0], MatchRestriction, Type)) {
                                                    synchronized (fastq_write[j]) {
                                                        fastq_write[j].write(str[6] + "\n");
                                                        fastq_write[j].write(str[0].substring(len - MaxReadsLength, str[0].length()) + finalAddSeq + "\n");
                                                        fastq_write[j].write(str[8] + "\n");
                                                        fastq_write[j].write(str[9].substring(len - MaxReadsLength, str[0].length()) + finalAddQuality + "\n");
                                                    }
                                                } else {
                                                    synchronized (fastq_write[j]) {
                                                        fastq_write[j].write(str[6] + "\n");
                                                        fastq_write[j].write(str[0].substring(len - MaxReadsLength, str[0].length()) + "\n");
                                                        fastq_write[j].write(str[8] + "\n");
                                                        fastq_write[j].write(str[9].substring(len - MaxReadsLength, str[0].length()) + "\n");
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
        //                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
                            }
                        });
                        process[i].start();
                    }
                    for (int i = 0; i < threads; i++) {
                        try {
                            process[i].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    fastq_read.close();
                    for (int i = 0; i < LinkerFastq.length; i++) {
                        fastq_write[i].close();
                    }
                    System.out.println(new Date() + "\tEnd to cluster linker\t" + Type);
                } else if (Type.equals("R2")) {
                    Thread[] process = new Thread[threads];
                    System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
                    //多线程读取
                    for (int i = 0; i < threads; i++) {
                        String finalAddSeq = AddSeq;
                        String finalAddQuality = AddQuality;
                        process[i] = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String line;
                                String[] str;
                                int len;
                                try {
        //                            System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " begin");
                                    while ((line = fastq_read.readLine()) != null) {
                                        str = line.split("\\t+");
                                        str[3] = str[3].replace("N", "");
                                        for (int j = 0; j < LinkerFastq.length; j++) {
                                            if (str[3].length() >= MinReadsLength && Integer.parseInt(str[5]) >= MinLinkerFilterQuality && Integer.parseInt(str[4]) == j) {
                                                len = Math.min(str[3].length(), MaxReadsLength);
                                                if (AppendBase(str[3], MatchRestriction, Type)) {
                                                    synchronized (fastq_write[j]) {
                                                        fastq_write[j].write(str[6] + "\n");
                                                        fastq_write[j].write(finalAddSeq + str[3].substring(0, len) + "\n");
                                                        fastq_write[j].write(str[8] + "\n");
                                                        fastq_write[j].write(finalAddQuality + str[9].substring(Integer.parseInt(str[2]) + 1, Integer.parseInt(str[2]) + 1 + len) + "\n");
                                                    }
                                                } else {
                                                    synchronized (fastq_write[j]) {
                                                        fastq_write[j].write(str[6] + "\n");
                                                        fastq_write[j].write(str[3].substring(0, len) + "\n");
                                                        fastq_write[j].write(str[8] + "\n");
                                                        fastq_write[j].write(str[9].substring(Integer.parseInt(str[2]) + 1, Integer.parseInt(str[2]) + 1 + len) + "\n");
                                                    }
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
        //                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
                            }
                        });
                        process[i].start();
                    }
                    for (int i = 0; i < threads; i++) {
                        try {
                            process[i].join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    fastq_read.close();
                    for (int i = 0; i < LinkerFastq.length; i++) {
                        fastq_write[i].close();
                    }
                    System.out.println(new Date() + "\tEnd to cluster linker\t" + Type);
                } else {
                    System.err.println(new Date() + "\tError parameter in cluster linker\t" + Type);
                    System.exit(0);
                }

            }//OK
        */
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
