
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/**
 * @author snowf
 * @version 1.0
 */

class SeProcess {
    private String Prefix;//输出前缀
    private String IndexFile;//比对索引文件
    public int Threads = 1;//线程数
    private int SeNum;//单端类型(R1 or R2)
    private int MisMatch;//错配数，bwa中使用
    private String RestrictionSeq;//需要匹配的酶切位点序列
    private String AddSeq;//需要延长的序列
    private String[] LinkersType;//linker类型
    private String[] UseLinkerType;//有用的linker类型
    private String AddQuality;//延长的序列
    public int Phred = 33;//Fastq格式
    private int MinQuality = 20;//最小比对质量
    private int MaxReadsLength = 20;//最长reads长度
    private int MinReadsLength = 16;//最短reads长度
    private int AlignThreads = 8;//比对线程数
    public int MinLinkerFilterQuality = 34;//最小linker过滤的比对质量
    private String OutPath;//输出目录
    private String PastFile;//输入文件(linker过滤的结果文件)
    private String[] LinkerFastqFile;//不同linker类型的Fastq文件
    private String[] UseLinkerFastqFile;//可用的linker类型的Fastq文件
    private String[] SamFile;//Sam文件
    private String[] FilterSamFile;//过滤后的Sam文件
    private String[] BamFile;//Bam文件
    private String[] BedFile;//Bed文件
    private String[] SortBedFile;//排序后的bed文件

    SeProcess(String outPath, String prefix, String pastfile, String[] linkerstype, String[] uselinker, String index, String restrictionSeq, String addseq, int num, int mismatch) {
        OutPath = outPath;
        Prefix = prefix;
        PastFile = pastfile;
        LinkersType = linkerstype;
        UseLinkerType = uselinker;
        IndexFile = index;
        RestrictionSeq = restrictionSeq;
        AddSeq = addseq;
        SeNum = num;
        MisMatch = mismatch;
        Init();
    }

    SeProcess(String ConfigFile) throws IOException {
        GetOption(ConfigFile);
        Init();
    }

    /**
     * <p>单端数据处理</p>
     * <p>1. linker聚类（区分不同的linker）</p>
     * <p>2. 比对</p>
     * <p>3. sam文件过滤</p>
     * <p>4. sam转bed</p>
     * <p>5. bed文件排序</p>
     *
     * @throws IOException
     */
    public void Run() throws IOException {
        Routine Se = new Routine();
        Se.Threads = Threads;
        //========================================================================================
        //区分不同类型的linker，并放到不同的文件中（中间会过滤掉比对质量较低，和无法延长的序列）
        Se.ClusterLinker(PastFile, LinkerFastqFile, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, RestrictionSeq, AddSeq, AddQuality, SeNum);
        Thread[] process = new Thread[UseLinkerType.length];
        //处理可用的linker类型（每个线程处理一种linker类型）
        for (int i = 0; i < UseLinkerType.length; i++) {
            int finalI = i;
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start\t" + LinkersType[finalI]);
                        //比对
                        Se.Align(IndexFile, UseLinkerFastqFile[finalI], SamFile[finalI], AlignThreads, MisMatch);
                        //Sam文件过滤
                        Se.SamFilter(SamFile[finalI], FilterSamFile[finalI], MinQuality);
                        //Sam文件转bam再转bed
                        Se.SamToBed(FilterSamFile[finalI], BamFile[finalI], BedFile[finalI]);
                        synchronized (process) {
                            //对bed文件排序（由于在大量数据下bed文件会比较大，所以为了减少内存消耗，bed文件排序使用串行）
                            CommonMethod.SortFile(BedFile[finalI], new int[]{4}, "", "\\s+", SortBedFile[finalI], Threads);
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end\t" + LinkersType[finalI]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
        }
        for (int i = 0; i < process.length; i++) {
            try {
                process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //删掉过滤后的sam文件（有bam文件，所以不用留sam文件，节省空间）
        for (String samfile : FilterSamFile) {
            System.out.println(new Date() + "\tDelete " + samfile);
            new File(samfile).delete();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage:    java -cp HiC-test.jar SeProcess Config.txt");
            System.exit(0);
        }
        SeProcess se = new SeProcess(args[0]);
        se.Run();
    }

    public String[] getSortBedFile() {
        return SortBedFile;
    }

    private void GetOption(String ConfigFile) throws IOException {
        BufferedReader file = new BufferedReader(new FileReader(ConfigFile));
        String line;
        String[] str;
        while ((line = file.readLine()) != null) {
            str = line.split("\\s+");
            switch (str[0]) {
                case "PastFile":
                    PastFile = str[2];
                    System.out.println("PastFile:\t" + PastFile);
                    break;
                case "OutPath":
                    OutPath = str[2];
                    System.out.println("OutPath:\t" + OutPath);
                    break;
                case "OutPrefix":
                    Prefix = str[2];
                    System.out.println("OutPrefix:\t" + Prefix);
                    break;
                case "Index":
                    IndexFile = str[2];
                    System.out.println("Index:\t" + IndexFile);
                    break;
                case "Phred":
                    Phred = Integer.parseInt(str[2]);
                    System.out.println("Phred:\t" + Phred);
                    break;
                case "AlignMinQuality":
                    MinQuality = Integer.parseInt(str[2]);
                    System.out.println("CreatMatrix Min Quality:\t" + MinQuality);
                    break;
                case "AlignThread":
                    AlignThreads = Integer.parseInt(str[2]);
                    System.out.println("CreatMatrix Thread:\t" + AlignThreads);
                    break;
                case "Thread":
                    Threads = Integer.parseInt(str[2]);
                    System.out.println("Thread:\t" + Threads);
                    break;
                case "AlignMisMatch":
                    MisMatch = Integer.parseInt(str[2]);
                    System.out.println("CreatMatrix MisMatch:\t" + MisMatch);
                    break;
                case "RestrictionSeq":
                    RestrictionSeq = str[2];
                    System.out.println("Restriction Seq:\t" + RestrictionSeq);
                    break;
                case "AddSeq":
                    AddSeq = str[2];
                    System.out.println("Add Seq:\t" + AddSeq);
                    break;
                case "Type":
                    SeNum = Integer.parseInt(str[2]);
                    System.out.println("Type is :\t" + SeNum);
                    break;
                case "MinReadsLength":
                    MinReadsLength = Integer.parseInt(str[2]);
                    System.out.println("MinReadsLength:\t" + MinReadsLength);
                    break;
                case "MaxReadsLength":
                    MaxReadsLength = Integer.parseInt(str[2]);
                    System.out.println("MaxReadsLength:\t" + MaxReadsLength);
                    break;
                case "MinLinkerFilterQuality":
                    MinLinkerFilterQuality = Integer.parseInt(str[2]);
                    System.out.println("MinLinkerFilterQuality:\t" + MinLinkerFilterQuality);
                    break;
            }
        }
        file.close();
        if (PastFile == null) {
            System.out.println("Error ! No PastFile");
            System.exit(0);
        }
        if (OutPath == null) {
            OutPath = "./";
            System.out.println("Out path is:\t" + OutPath);
        }
        if (Prefix == null) {
            if (PastFile.lastIndexOf(".") != -1) {
                Prefix = PastFile.substring(0, PastFile.lastIndexOf("."));
            } else {
                Prefix = PastFile;
            }
            System.out.println("Prefix is:\t" + Prefix);
        }
        if (IndexFile == null) {
            System.out.println("Error ! No IndexFile");
            System.exit(0);
        }
        if (RestrictionSeq == null) {
            System.out.println("Error ! No RestrictionSeq");
            System.exit(0);
        }
        if (AddSeq == null) {
            System.out.println("Error ! No AddSeq");
            System.exit(0);
        }
        if (SeNum != 1 && SeNum != 2) {
            System.out.println("Error ! Unknow Type " + SeNum + "\tType should 1 or 2");
            System.exit(0);
        }
    }

    /**
     * <p>类的初始化</p>
     * <p>检测输出路径，判断单端类型（哪一端），构建输出文件</p>
     */
    private void Init() {
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdirs();
        }
        String dir = "";
        if (SeNum == 1) {
            dir = "R1";
        } else if (SeNum == 2) {
            dir = "R2";
        } else {
            System.err.println("Error Single Type!\t" + SeNum);
            System.exit(0);
        }
        if (!new File(OutPath + "/" + dir).isDirectory()) {
            new File(OutPath + "/" + dir).mkdir();
        }
        LinkerFastqFile = new String[LinkersType.length];
        UseLinkerFastqFile = new String[UseLinkerType.length];
        SamFile = new String[UseLinkerType.length];
        FilterSamFile = new String[UseLinkerType.length];
        BamFile = new String[UseLinkerType.length];
        BedFile = new String[UseLinkerType.length];
        SortBedFile = new String[UseLinkerType.length];
        int restrictionLength = RestrictionSeq.length() + AddSeq.length();
        for (int i = 0; i < LinkersType.length; i++) {
            LinkerFastqFile[i] = OutPath + "/" + dir + "/" + Prefix + "." + SeNum + "." + restrictionLength + "linker." + LinkersType[i] + ".fastq";
        }
        for (int i = 0; i < UseLinkerType.length; i++) {
            UseLinkerFastqFile[i] = OutPath + "/" + dir + "/" + Prefix + "." + SeNum + "." + restrictionLength + "linker." + UseLinkerType[i] + ".fastq";
            SamFile[i] = UseLinkerFastqFile[i].replace(".fastq", "." + MisMatch + ".sam");
            FilterSamFile[i] = SamFile[i].replace(".sam", ".uniq.sam");
            BamFile[i] = FilterSamFile[i].replace(".uniq.sam", ".bam");
            BedFile[i] = BamFile[i].replace(".bam", ".bed");
            SortBedFile[i] = BedFile[i].replace(".bed", ".sort.bed");
        }
        if (Phred == 33) {
            AddQuality = "I";
        } else {
            AddQuality = "h";
        }
    }
}
