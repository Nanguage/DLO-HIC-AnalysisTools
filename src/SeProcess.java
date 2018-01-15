
import java.io.*;
import java.util.ArrayList;
import java.util.Date;

class SeProcess {
    private String Prefix;
    private String IndexFile;
    public int Threads = 1;
    private int SeNum;
    private int MisMatch;
    private String RestrictionSeq;
    private String AddSeq;
    private String[] LinkersType;
    private String[] UseLinkerType;
    private int[] Index;
    private String AddQuality;
    private int RestrictionLength;
    public int Phred = 33;
    public int MinQuality = 20;
    public int MaxReadsLength = 20;
    public int MinReadsLength = 16;
    public int AlignThreads = 8;
    public int MinLinkerFilterQuality = 34;
    private String OutPath;
    private String PastFile;
    private int[] ChrSize;
    //--------------------------------------------------------------------------
    private String[] LinkerFastqFile;
    private String[] SamFile;
    private String[] FilterSamFile;
    private String[] BamFile;
    private String[] BedFile;
    private String[] SortBedFile;

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

    SeProcess(String ConfigFile) throws IOException{
            GetOption(ConfigFile);
            Init();
    }

    public void Run() throws IOException {
        Routine Se = new Routine();
        Se.Threads = Threads;
        //========================================================================================
        Se.ClusterLinker(PastFile, LinkerFastqFile, Index, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, RestrictionSeq, AddSeq, AddQuality, SeNum);
        Thread[] process = new Thread[UseLinkerType.length];
        for (int i = 0; i < UseLinkerType.length; i++) {
            int finalI = i;
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start\t" + LinkersType[finalI]);
                        Se.Align(IndexFile, LinkerFastqFile[finalI], SamFile[finalI], AlignThreads, MisMatch);
                        Se.SamFilter(SamFile[finalI], FilterSamFile[finalI], MinQuality);
                        Se.SamToBed(FilterSamFile[finalI], BamFile[finalI], BedFile[finalI]);
                        synchronized (process) {
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
        for (String samfile : FilterSamFile) {
            System.out.println(new Date() + "\tDelete " + samfile);
            new File(samfile).delete();
        }
        //--------------------------------------------------------------------------------------------
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

    public int[] GetChrSize(String[] Chromosome) {
        try {
            ArrayList<String[]> list = CommonMethod.GetChromosomeSize(SamFile[0]);
            ChrSize = new int[Chromosome.length];
            for (int i = 0; i < list.size(); i++) {
                for (int j = 0; j < Chromosome.length; j++) {
                    if (list.get(i)[0].equals(Chromosome[j])) {
                        ChrSize[j] = Integer.parseInt(list.get(i)[1]);
                        break;
                    }
                }
                ChrSize[i] = Integer.parseInt(list.get(i)[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ChrSize;
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
                    System.out.println("Align Min Quality:\t" + MinQuality);
                    break;
                case "AlignThread":
                    AlignThreads = Integer.parseInt(str[2]);
                    System.out.println("Align Thread:\t" + AlignThreads);
                    break;
                case "Thread":
                    Threads = Integer.parseInt(str[2]);
                    System.out.println("Thread:\t" + Threads);
                    break;
                case "AlignMisMatch":
                    MisMatch = Integer.parseInt(str[2]);
                    System.out.println("Align MisMatch:\t" + MisMatch);
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

    private void Init() {
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdirs();
        }
        String dir;
        if (SeNum == 1) {
            dir = "left";
            if (!new File(OutPath + "/left").isDirectory()) {
                new File(OutPath + "/left").mkdir();
            }
        } else if (SeNum == 2) {
            dir = "right";
            if (!new File(OutPath + "/right").isDirectory()) {
                new File(OutPath + "/right").mkdir();
            }
        } else {
            dir = "";
        }
        LinkerFastqFile = new String[UseLinkerType.length];
        SamFile = new String[UseLinkerType.length];
        FilterSamFile = new String[UseLinkerType.length];
        BamFile = new String[UseLinkerType.length];
        BedFile = new String[UseLinkerType.length];
        SortBedFile = new String[UseLinkerType.length];
        RestrictionLength = RestrictionSeq.length() + AddSeq.length();
        Index = new int[UseLinkerType.length];
        for (int i = 0; i < UseLinkerType.length; i++) {
            LinkerFastqFile[i] = OutPath + "/" + dir + "/" + Prefix + "." + SeNum + "." + RestrictionLength + "linker." + UseLinkerType[i] + ".fastq";
            SamFile[i] = LinkerFastqFile[i].replace(".fastq", "." + MisMatch + ".sam");
            FilterSamFile[i] = SamFile[i].replace(".sam", ".uniq.sam");
            BamFile[i] = FilterSamFile[i].replace(".uniq.sam", ".bam");
            BedFile[i] = BamFile[i].replace(".bam", ".bed");
            SortBedFile[i] = BedFile[i].replace(".bed", ".sort.bed");
            for (int j = 0; j < LinkersType.length; j++) {
                if (LinkersType[j].equals(UseLinkerType[i])) {
                    Index[i] = j;
                    break;
                }
            }
        }
        if (Phred == 33) {
            AddQuality = "I";
        } else {
            AddQuality = "h";
        }
    }
}
