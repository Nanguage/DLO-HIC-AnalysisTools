import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class Main {
    private String FastqFile;//fastq文件
    private String GenomeFile;//基因组文件
    private int Phred;//fastq格式
    private String OutPrefix;//输出前缀
    private String OutPath;//输出路径
    private String ChromosomePrefix;//染色体前缀
    private String[] Chromosome;//染色体名
    private String RestrictionSeq;//酶切位点序列
    private String[] MatchRestriction = new String[2];//匹配的序列
    private String[] AddSeq = new String[2];//延长的序列
    private String EnzyFilePrefix;//酶切位点文件前缀
    private String AdapterFile;//Adapter文件
    private String[] LinkersType;//linker类型
    private String[] UseLinker;//可用的linker类型
    private String LinkerFile;//linker文件
    private int LinkerLength;//linker长度
    private int MatchScore;//linker过滤匹配分数
    private int MisMatchScore;//linker过滤错配分数
    private int IndelScore;//linker过滤插入缺失分数
    private int MaxMisMatchLength;//linker过滤最大错配数
    private String IndexFile;//比对索引
    private int AlignMisMatch;//bwa等比对最小错配数
    private int AlignThread;//bwa等比对线程数
    private int AlignMinQuality;//bwa等比对最小质量
    private int MinReadsLength;//最小reads长度
    private int MaxReadsLength;//最大reads长度
    private int Resolution;//分辨率
    private int Threads;//线程数
    //    private String ConfigureFile;//配置文件
    private String PreProcessDir;//预处理输出目录
    private String SeProcessDir;//单端处理输出目录
    private String BedpeProcessDir;//bedpe处理输出目录
    private String MakeMatrixDir;//建立矩阵输出目录

    Main(String ConfigFile) {
        try {
            GetOption(ConfigFile);//获取参数
            Init();//初始化参数
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage:    java -jar HiC-test.jar config.txt");
            System.exit(0);
        }
        Main main = new Main(args[0]);
        main.Run();
    }

    public void Run() throws IOException {
        //==============================================测试区==============================

        //===========================================初始化输出文件=======================================
        String[] SeBedpeFile = new String[UseLinker.length];
        String[] FinalLinkerBedpe = new String[UseLinker.length];
        for (int i = 0; i < UseLinker.length; i++) {
            SeBedpeFile[i] = OutPath + "/bedpe/" + OutPrefix + "." + UseLinker[i] + "." + AlignMisMatch + ".bedpe";
        }
        String FinalBedpeFile = BedpeProcessDir + "/" + OutPrefix + ".bedpe";
        String InterBedpeFile = BedpeProcessDir + "/" + OutPrefix + ".inter.bedpe";
        Routine step = new Routine();
        step.Threads = Threads;//设置线程数
        //=========================================linker filter==linker 过滤=============================================
        PreProcess preprocess;
        if (AdapterFile != null) {
            preprocess = new PreProcess(PreProcessDir, OutPrefix, FastqFile, LinkerFile, AdapterFile, MatchScore, MisMatchScore, IndelScore, Threads);
            preprocess.Run();
        } else {
            preprocess = new PreProcess(PreProcessDir, OutPrefix, FastqFile, LinkerFile, MatchScore, MisMatchScore, IndelScore, Threads);
            preprocess.Run();//运行预处理
        }
        String PastFile = preprocess.getPastFile();//获取past文件位置
        preprocess = null;
        //=======================================Se Process===单端处理=============================================
        SeProcess seleft = new SeProcess(SeProcessDir, OutPrefix, PastFile, LinkersType, UseLinker, IndexFile, MatchRestriction[0], AddSeq[0], 1, AlignMisMatch);//左端处理类
        SeProcess seright = new SeProcess(SeProcessDir, OutPrefix, PastFile, LinkersType, UseLinker, IndexFile, MatchRestriction[1], AddSeq[1], 2, AlignMisMatch);//右端处理类
        seleft.Threads = Threads;//设置线程数
        seleft.Phred = Phred;//设置fastq文件格式
        seleft.MinLinkerFilterQuality = LinkerLength * MatchScore + MaxMisMatchLength * MisMatchScore;//设置linkerfilter最小分数
        seright.Threads = Threads;
        seright.Phred = Phred;
        seright.MinLinkerFilterQuality = LinkerLength * MatchScore + MaxMisMatchLength * MisMatchScore;
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    seleft.Run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    seright.Run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //=======================================================提取染色体大小信息=================================================================================
        ArrayList<String> list = new ArrayList<>();
        int[] ChrSize = new int[Chromosome.length];
        if (EnzyFilePrefix == null) {
            EnzyFilePrefix = OutPath + "/EnzySiteFile";
            if (!new File(EnzyFilePrefix).isDirectory()) {
                new File(EnzyFilePrefix).mkdir();
            }
            EnzyFilePrefix = EnzyFilePrefix + "/" + OutPrefix + "." + RestrictionSeq.replace("^", "");
            Hashtable<String, Integer> temphash = step.FindRestrictionSite(GenomeFile, RestrictionSeq, EnzyFilePrefix);
            for (int i = 0; i < Chromosome.length; i++) {
                ChrSize[i] = temphash.get(Chromosome[i]);
                list.add(Chromosome[i] + "\t" + ChrSize[i]);
            }
        } else {
            Hashtable<String, Integer> temphash = CommonMethod.GetChromosomeSize(IndexFile + ".ann");//获取染色体大小
            for (int i = 0; i < Chromosome.length; i++) {
                ChrSize[i] = temphash.get(Chromosome[i]);
                list.add(Chromosome[i] + "\t" + ChrSize[i]);
            }
        }
        CommonMethod.PrintList(list, SeProcessDir + "/" + OutPrefix + ".ChrSize");//打印染色体大小信息
        list = null;
        //==============================================================================================================================================================
        String[] Test1SortBedFile = seleft.getSortBedFile();//获取排序好的bed文件
        String[] Test2SortBedFile = seright.getSortBedFile();
        if (!new File(OutPath + "/bedpe").isDirectory()) {
            new File(OutPath + "/bedpe").mkdir();
        }
        for (int i = 0; i < UseLinker.length; i++) {
            step.MergeBedToBedpe(Test1SortBedFile[i], Test2SortBedFile[i], SeBedpeFile[i], 4, "");//合并左右端bed文件，输出bedpe文件
        }
        //==============================================Bedpe Process====bedpe 处理=========================================================================================
        Thread[] LinkerProcess = new Thread[UseLinker.length];//不同linker类型并行
        BedpeProcess[] bedpe = new BedpeProcess[UseLinker.length];//bedpe文件处理类
        for (int i = 0; i < UseLinker.length; i++) {
            int finalI = i;
            LinkerProcess[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        bedpe[finalI] = new BedpeProcess(BedpeProcessDir, OutPrefix, UseLinker[finalI], Chromosome, EnzyFilePrefix, SeBedpeFile[finalI]);
                        bedpe[finalI].Threads = Threads;//设置线程数
                        bedpe[finalI].Run();//运行
                        FinalLinkerBedpe[finalI] = bedpe[finalI].getFinalBedpeFile();//获取最终的bedpe文件
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            LinkerProcess[i].start();
        }
        for (int i = 0; i < UseLinker.length; i++) {
            try {
                LinkerProcess[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //=================================================Bedpe To Inter==================================================
        CommonMethod.Merge(FinalLinkerBedpe, FinalBedpeFile);//合并不同linker类型的bedpe文件
        step.BedpeToInter(FinalBedpeFile, InterBedpeFile);//将交互区间转换成交互点
        //=================================================Make Matrix========================================================================
        MakeMatrix matrix = new MakeMatrix(MakeMatrixDir, OutPrefix, InterBedpeFile, Chromosome, ChrSize, Resolution);//生成交互矩阵类
        matrix.Run();//运行
    }

    private void GetOption(String Infile) throws IOException {
        String line;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(Infile));
        while ((line = infile.readLine()) != null) {
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s+");
            try {
                switch (str[0]) {

                    case "FastqFile":
                        FastqFile = str[2];
                        System.out.println("FastqFile:\t" + FastqFile);
                        break;
                    case "GenomeFile":
                        GenomeFile = str[2];
                        System.out.println("GenomeFile:\t" + GenomeFile);
                        break;
                    case "OutPrefix":
                        OutPrefix = str[2];
                        System.out.println("OutPrefix:\t" + OutPrefix);
                        break;
                    case "OutPath":
                        OutPath = str[2];
                        System.out.println("OutPath:\t" + OutPath);
                        break;
                    case "ChromosomePrefix":
                        ChromosomePrefix = str[2];
                        System.out.println("ChromosomePrefix:\t" + ChromosomePrefix);
                        break;
                    case "Chromosome":
                        Chromosome = new String[str.length - 2];
                        System.arraycopy(str, 2, Chromosome, 0, str.length - 2);
                        break;
                    case "RestrictionSeq":
                        RestrictionSeq = str[2];
                        System.out.println("MatchRestriction sequence:\t" + RestrictionSeq);
                        break;
                    case "LinkerFile":
                        LinkerFile = str[2];
                        System.out.println("Linker file:\t" + LinkerFile);
                        break;
                    case "Index":
                        IndexFile = str[2];
                        System.out.println("Index:\t" + IndexFile);
                        break;
                    case "Thread":
                        Threads = Integer.parseInt(str[2]);
                        System.out.println("Thread:\t" + Threads);
                        break;
                    case "AlignMisMatch":
                        AlignMisMatch = Integer.parseInt(str[2]);
                        System.out.println("CreatMatrix MisMatch:\t" + AlignMisMatch);
                        break;
                    case "Phred":
                        Phred = Integer.parseInt(str[2]);
                        System.out.println("Phred:\t" + Phred);
                        break;
                    case "MatchScore":
                        MatchScore = Integer.parseInt(str[2]);
                        System.out.println("Match Score:\t" + MatchScore);
                        break;
                    case "MisMatchScore":
                        MisMatchScore = Integer.parseInt(str[2]);
                        System.out.println("MisMatch Score:\t" + MisMatchScore);
                        break;
                    case "IndelScore":
                        IndelScore = Integer.parseInt(str[2]);
                        System.out.println("Indel Score:\t" + IndelScore);
                        break;
                    case "MaxMisMatchLength":
                        MaxMisMatchLength = Integer.parseInt(str[2]);
                        System.out.println("Max MisMatch Length:\t" + MaxMisMatchLength);
                        break;
                    case "AlignThread":
                        AlignThread = Integer.parseInt(str[2]);
                        System.out.println("CreatMatrix Thread:\t" + AlignThread);
                        break;
                    case "MinReadsLength":
                        MinReadsLength = Integer.parseInt(str[2]);
                        System.out.println("Min Reads Length:\t" + MinReadsLength);
                        break;
                    case "MaxReadsLength":
                        MaxReadsLength = Integer.parseInt(str[2]);
                        System.out.println("Max Reads Length:\t" + MaxReadsLength);
                        break;
                    case "AlignMinQuality":
                        AlignMinQuality = Integer.parseInt(str[2]);
                        System.out.println("CreatMatrix Min Quality:\t" + AlignMinQuality);
                        break;
                    case "EnzyFilePrefix":
                        EnzyFilePrefix = str[2];
                        System.out.println("Enzy File Prefix:\t" + EnzyFilePrefix);
                        break;
                    case "Resolution":
                        Resolution = Integer.parseInt(str[2]);
                        System.out.println("Resolution:\t" + Resolution);
                        break;
                    case "LinkersType":
                        LinkersType = new String[str.length - 2];
                        System.arraycopy(str, 2, LinkersType, 0, str.length - 2);
                        System.out.print("LinkersType:\t");
                        for (String s : LinkersType) {
                            System.out.print(" " + s);
                        }
                        System.out.println();
                        break;
                    case "UseLinker":
                        UseLinker = new String[str.length - 2];
                        System.arraycopy(str, 2, UseLinker, 0, str.length - 2);
                        System.out.print("UseLinker:\t");
                        for (String s : UseLinker) {
                            System.out.print(" " + s);
                        }
                        System.out.println();
                        break;
                    case "AdapterFile":
                        AdapterFile = str[2];
                        System.out.println("AdapterFile:\t" + AdapterFile);
                        break;
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
        infile.close();
    }

    private void Init() throws IOException {
        if (EnzyFilePrefix == null && GenomeFile == null) {
            System.err.println("Error ! no EnzyFilePrefix or GenomeFile");
            System.exit(0);
        }
        if (FastqFile == null) {
            System.err.println("Error ! no FastqFile");
            System.exit(0);
        }
        if (Chromosome == null) {
            System.err.println("Error ! no Chromosome");
            System.exit(0);
        }
        if (RestrictionSeq == null) {
            System.err.println("Error ! no RestrictionSeq");
            System.exit(0);
        }
        if (IndexFile == null) {
            System.err.println("Error ! no IndexFile");
            System.exit(0);
        }
        if (LinkerFile == null) {
            System.err.println("Error ! no LinkerFile");
            System.exit(0);
        }
        if (MaxMisMatchLength == 0) {
            System.out.println("Error ! no MaxMisMatchLength");
            System.exit(0);
        }
        if (MaxReadsLength == 0) {
            System.out.println("Error ! no MaxReadsLength");
            System.exit(0);
        }
        if (AlignMinQuality == 0) {
            System.out.println("Error ! no AlignMinQuality");
            System.exit(0);
        }
        if (LinkersType == null) {
            System.out.println("Error ! no LinkersType");
            System.exit(0);
        }
        //=======================================================================
        if (OutPrefix == null) {
            try {
                OutPrefix = FastqFile.substring(0, FastqFile.lastIndexOf("."));
            } catch (IndexOutOfBoundsException e) {
                OutPrefix = FastqFile;
            }
            System.out.println("Out Prefix:\t" + OutPrefix);
        }
        if (OutPath == null) {
            OutPath = "./";
            System.out.println("Out Path:\t" + OutPath);
        }
        if (UseLinker == null) {
            UseLinker = LinkersType;
            System.out.print("UseLinker:\t");
            for (String s : UseLinker) {
                System.out.print(" " + s);
            }
            System.out.println();
        }
        if (ChromosomePrefix == null) {
            ChromosomePrefix = "";
            System.out.println("Chromosome Prefix:\t" + ChromosomePrefix);
        }
        if (Threads == 0) {
            Threads = 1;
            System.out.println("Thread:\t" + Threads);
        }
        if (Phred == 0) {
            Phred = 33;
            System.out.println("Phred:\t" + Phred);
        }
        if (MatchScore == 0) {
            MatchScore = 1;
            System.out.println("Match Score:\t" + MatchScore);
        }
        if (MisMatchScore == 0) {
            MisMatchScore = -2;
            System.out.println("MisMatch Score:\t" + MisMatchScore);
        }
        if (IndelScore == 0) {
            IndelScore = -2;
            System.out.println("Indel Score:\t" + IndelScore);
        }
        if (AlignThread == 0) {
            AlignThread = 4;
            System.out.println("CreatMatrix Thread:\t" + AlignThread);
        }
        if (MinReadsLength == 0) {
            MinReadsLength = 16;
            System.out.println("Min Reads Length:\t" + MinReadsLength);
        }
        if (Resolution == 0) {
            Resolution = 1000000;
            System.out.println("Resolution:\t" + Resolution);
        }
        //============================================================================
        System.out.print("Chromosome:\t");
        for (int i = 0; i < Chromosome.length; i++) {
            Chromosome[i] = ChromosomePrefix + Chromosome[i];
            System.out.print(Chromosome[i] + " ");
        }
        System.out.println();
        int restrictionSite = RestrictionSeq.indexOf("^");
        String TempSeq = RestrictionSeq.replace("^", "");
        MatchRestriction[0] = TempSeq.substring(0, TempSeq.length() - restrictionSite);
        MatchRestriction[1] = TempSeq.substring(restrictionSite);
        try {
            AddSeq[0] = TempSeq.substring(TempSeq.length() - restrictionSite);
            AddSeq[1] = TempSeq.substring(0, restrictionSite);
        } catch (IndexOutOfBoundsException e) {
            AddSeq = new String[]{"", ""};
        }

        System.out.println("MatchRestriction one:\t" + MatchRestriction[0]);
        System.out.println("MatchRestriction two:\t" + MatchRestriction[1]);
        BufferedReader infile = new BufferedReader(new FileReader(LinkerFile));
        LinkerLength = infile.readLine().length();
        infile.close();
        PreProcessDir = OutPath + "/PreProcess";
        SeProcessDir = OutPath + "/SeProcess";
        BedpeProcessDir = OutPath + "/BedpeProcess";
        MakeMatrixDir = OutPath + "/MakeMatrix";
        if (!new File(OutPath).isDirectory()) {
            System.out.println(new Date() + "+\tError output path");
            System.exit(0);
        }
    }
}