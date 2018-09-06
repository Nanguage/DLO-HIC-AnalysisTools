import java.io.*;
import java.util.*;

import bin.*;
import lib.File.*;
import lib.Image.PlotMatrix;
import lib.tool.*;
import lib.tool.FindRestrictionSite;
import lib.unit.*;
import org.apache.commons.cli.*;
import script.CreateMatrix;

public class Main {
    private final String OptFastqFile = "FastqFile";//fastq文件
    private final String OptGenomeFile = "GenomeFile";//基因组文件
    private final String OptPrefix = "Prefix";//输出前缀
    private final String OptOutPath = "OutPath";//输出路径
    private final String OptChromosome = "Chromosomes";//染色体名
    private final String OptRestriction = "Restriction";//酶切位点序列
    private final String OptLinkerFile = "LinkerFile";//linker文件
    private final String OptAdapterFile = "AdapterFile";//Adapter文件
    private final String OptLinkersType = "LinkersType";//linker类型
    private final String OptUseLinker = "UseLinker";//可用的linker类型
    private final String OptMatchScore = "MatchScore";//linker过滤匹配分数
    private final String OptMisMatchScore = "MisMatchScore";//linker过滤错配分数
    private final String OptIndelScore = "IndelScore";//linker过滤插入缺失分数
    private final String OptMaxMisMatchLength = "MaxMisMatchLength";//linker过滤最大错配数
    private final String OptIndexFile = "Index";//比对索引
    private final String OptReadsType = "ReadsType";// Long or Short
    private final String OptAlignMisMatch = "AlignMisMatch";//bwa等比对最小错配数
    private final String OptAlignThread = "AlignThread";//bwa等比对线程数
    private final String OptAlignMinQuality = "AlignMinQuality";//bwa等比对最小质量
    private final String OptMinReadsLength = "MinReadsLength";//最小reads长度
    private final String OptMaxReadsLength = "MaxReadsLength";//最大reads长度
    private final String OptResolution = "Resolution";//分辨率
    private final String OptDrawResolution = "DrawRes";
    private final String OptThreads = "Threads";//线程数
    private final String OptIteration = "Iteration";//是否迭代
    private final String OptStep = "Step";

    //===================================================================
    private CustomFile[] FastqFile;
    private CustomFile GenomeFile;
    private File OutPath;
    private String Prefix;
    private Chromosome[] Chromosomes;
    //    private ArrayList<String> Chrs = new ArrayList<>();
    private String Restriction;
    private File LinkerFile;
    private File AdapterFile;
    private ArrayList<String> LinkersType = new ArrayList<>();
    private ArrayList<String> UseLinker = new ArrayList<>();
    private File IndexPrefix;
    private int FileType = Opts.Single;
    private int MatchScore;
    private int MisMatchScore;
    private int IndelScore;
    private int ReadsType;
    private int AlignMisMatch;
    private int AlignThread;
    private int AlignMinQuality;
    private int MinReadsLength;
    private int MaxReadsLength;
    private int[] Resolution;
    private int[] DrawResolution;
    private int DetectResolution;
    private boolean Iteration = true;
    private int Threads;
    private Date PreTime, SeTime, BedpeTime, MatrixTime, TransTime, EndTime;
    private ArrayList<String> Step = new ArrayList<>();
    private ArrayList<Thread> SThread = new ArrayList<>();

    //===================================================================
    private String[] RequiredParameter = new String[]{OptFastqFile, OptGenomeFile, OptLinkerFile, OptChromosome, OptRestriction, OptLinkersType, OptAlignMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptIndexFile, OptPrefix, OptAdapterFile, OptMaxMisMatchLength, OptMinReadsLength, OptMaxReadsLength, OptUseLinker, OptMatchScore, OptMisMatchScore, OptIndelScore, OptReadsType, OptAlignMisMatch, OptAlignThread, OptResolution, OptStep, OptIteration, OptThreads};
    private Hashtable<String, String> ArgumentList = new Hashtable<>();//参数列表
    private Hashtable<String, Integer> ChrSize = new Hashtable<>();//染色体大小
    private int MinLinkerFilterQuality;
    private File EnzyPath;//酶切位点文件目录
    private String EnzyFilePrefix;//酶切位点文件前缀
    private File[] ChrEnzyFile;//每条染色体的酶切位点位置文件
    private File PreProcessDir;//预处理输出目录
    private File SeProcessDir;//单端处理输出目录
    private File BedpeProcessDir;//bedpe处理输出目录
    private File MakeMatrixDir;//建立矩阵输出目录
    private File TransLocationDir;//染色体易位输出目录
    //    private CustomFile[] ChrBedpeFile;
    private Report Stat = new Report();

    private Main(String[] args) throws IOException {
        Options Argument = new Options();
        Argument.addOption(Option.builder("i").hasArgs().argName("file").desc("input file").build());//输入文件
        Argument.addOption(Option.builder("conf").hasArg().argName("file").desc("Configure file").build());//配置文件
        Argument.addOption(Option.builder("p").hasArg().argName("string").desc("Prefix").build());//输出前缀(不需要包括路径)
        Argument.addOption(Option.builder("adv").hasArg().argName("file").desc("Advanced configure file").build());//高级配置文件(一般不用修改)
        Argument.addOption(Option.builder("o").longOpt("out").hasArg().argName("dir").desc("Out put directory").build());//输出路径
        Argument.addOption(Option.builder("s").longOpt("step").hasArgs().argName("string").desc("same as \"Step\" in configure file").build());
        final String helpHeader = "Version: " + Opts.Version + "\nAuthor: " + Opts.Author + "\nContact: " + Opts.Email;
        final String helpFooter = "Note: Have a good day!\n      JVM can get " + String.format("%.2f", Opts.MaxMemory / Math.pow(10, 9)) + "G memory";
        if (args.length == 0) {
            //没有参数时打印帮助信息
            new HelpFormatter().printHelp("java -jar Path/" + Opts.JarFile.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        CommandLine ComLine = null;
        try {
            ComLine = new DefaultParser().parse(Argument, args);
        } catch (ParseException e) {
            //缺少参数时打印帮助信息
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp("java -jar Path/" + Opts.JarFile.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        OptionListInit();
        //获取配置文件和高级配置文件
        CustomFile ConfigureFile = ComLine.hasOption("conf") ? new CustomFile(ComLine.getOptionValue("conf")) : Opts.ConfigFile;
        CustomFile AdvConfigFile = ComLine.hasOption("adv") ? new CustomFile(ComLine.getOptionValue("adv")) : Opts.AdvConfigFile;
        GetOption(ConfigureFile, AdvConfigFile);//读取配置信息
        //获取命令行参数信息
        if (ComLine.hasOption("i")) {
            ArgumentList.put(OptFastqFile, String.join(" ", ComLine.getOptionValues("i")));
        }
        if (ComLine.hasOption("p")) {
            ArgumentList.put(OptPrefix, ComLine.getOptionValue("p"));
        }
        if (ComLine.hasOption("o")) {
            ArgumentList.put(OptOutPath, ComLine.getOptionValue("o"));
        }
        if (ComLine.hasOption("s")) {
            ArgumentList.put(OptStep, String.join(" ", ComLine.getOptionValues("s")));
        }
        Init();//变量初始化
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //==============================================测试区==========================================================
//        int i =0;
//        switch (new CustomFile("chr3-chr9-500k.2d.matrix").MatrixDetect()){
//            case TwoDMatrixFormat:
//                System.out.println(2);
//                break;
//            case SpareMatrixFormat:
//                System.out.println(3);
//                break;
//        }
//        Properties ConfigFile = new Properties();
//        ConfigFile.load(new FileReader(Opts.ConfigFile));
//        String Temp = ConfigFile.getProperty("");

        //==============================================================================================================

        Main main = new Main(args);
        main.ShowParameter();//显示参数
        main.Run();
    }

    public void Run() throws IOException, InterruptedException {
        //============================================print system information==========================================
        System.out.println("===============Welcome to use " + Opts.JarFile.getName() + "===================");
        System.out.println("Version:\t" + Opts.Version);
        System.out.println("Author:\t" + Opts.Author);
        System.out.println("Max Memory:\t" + String.format("%.2f", Opts.MaxMemory / Math.pow(10, 9)) + "G");
        System.out.println("-------------------------------------------------------------------------------");
        //===========================================初始化输出文件======================================================
        File[] LinkerFastqFileR1;
        File[] LinkerFastqFileR2;
        CustomFile[] UseLinkerFasqFileR1 = new CustomFile[UseLinker.size()];
        CustomFile[] UseLinkerFasqFileR2 = new CustomFile[UseLinker.size()];
        //==============================================================================================================
        Stat.RestrictionSeq = Restriction;
        Stat.LinkerFile = LinkerFile;
        Stat.AdapterFile = AdapterFile;
        Stat.GenomeFile = GenomeFile;
        Stat.GenomeIndex = IndexPrefix;
        Stat.OutPrefix = Prefix;
        Stat.MinAlignQuality = AlignMinQuality;
        Stat.MinReadsLength = MinReadsLength;
        Stat.MaxReadsLength = MaxReadsLength;
        Stat.Resolution = Resolution;
        Stat.Thread = Threads;
        Stat.LinkersType = LinkersType;
        Stat.UseLinker = UseLinker;
        for (int i = 0; i < Chromosomes.length; i++) {
            Stat.Chromosome.add(Chromosomes[i].Name);
        }
        Stat.RawDataFile = FastqFile;
        Stat.RawDataReadsNum = new Long[FastqFile.length];
        //==============================================================================================================
        Opts.CommandOutFile.delete();
        Thread ST;
        Thread[] STS;
        //==========================================Create Index========================================================
        Thread createindex = new Thread();
        if (IndexPrefix == null || IndexPrefix.getName().equals("")) {
            createindex = CreateIndex(GenomeFile);
            createindex.start();
        }
        //=========================================linker filter==linker 过滤===========================================
        PreTime = new Date();
        PreProcess preprocess;
        preprocess = new PreProcess(PreProcessDir, Prefix, FastqFile, LinkerFile, AdapterFile, MatchScore, MisMatchScore, IndelScore, FileType, Threads * 4);
        if (StepCheck("LinkerFilter")) {
            preprocess.Run();
        }
        //==============================================================================================================
        File[] PastFile = preprocess.getPastFile();//获取past文件位置
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < FastqFile.length; i++) {
                        Stat.RawDataReadsNum[i] = FastqFile[i].CalculatorLineNumber() / 4;
                    }
                    //calculate linker count
                    Stat.LinkersNum.addAll(Arrays.asList(Statistic.CalculateLinkerCount(PastFile[0], LinkersType.toArray(new String[0]), MinLinkerFilterQuality, Threads)));
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ST.start();
        SThread.add(ST);
        preprocess = null;
        //==========================================Divide Linker=======================================================
        if (PastFile.length > 1) {
            DivideLinker divideLinker1 = new DivideLinker(PastFile[0], SeProcessDir + "/" + Prefix, LinkersType.toArray(new String[0]), Restriction, DivideLinker.First, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, FastqFile[0].FastqPhred());
            divideLinker1.setThreads(Threads);
            LinkerFastqFileR1 = divideLinker1.getR1FastqFile();
            DivideLinker divideLinker2 = new DivideLinker(PastFile[1], SeProcessDir + "/" + Prefix, LinkersType.toArray(new String[0]), Restriction, DivideLinker.First, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, FastqFile[0].FastqPhred());
            divideLinker2.setThreads(Threads);
            LinkerFastqFileR2 = divideLinker2.getR1FastqFile();
            if (StepCheck("DivideLinker")) {
                Thread r1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            divideLinker1.Run();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Thread r2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            divideLinker2.Run();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                r1.start();
                r2.start();
                r1.join();
                r2.join();
            }
        } else {
            DivideLinker divideLinker = new DivideLinker(PastFile[0], SeProcessDir + "/" + Prefix, LinkersType.toArray(new String[0]), Restriction, DivideLinker.All, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, FastqFile[0].FastqPhred());
            divideLinker.setThreads(Threads);
            LinkerFastqFileR1 = divideLinker.getR1FastqFile();
            LinkerFastqFileR2 = divideLinker.getR2FastqFile();
            if (StepCheck("DivideLinker")) {
                divideLinker.Run();
            }
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            UseLinkerFasqFileR1[i] = new CustomFile(LinkerFastqFileR1[LinkersType.indexOf(UseLinker.get(i))]);
            UseLinkerFasqFileR2[i] = new CustomFile(LinkerFastqFileR2[LinkersType.indexOf(UseLinker.get(i))]);
        }
        Stat.FastqR1File.addAll(Arrays.asList(LinkerFastqFileR1));
        Stat.FastqR2File.addAll(Arrays.asList(LinkerFastqFileR2));
        createindex.join();
        //=======================================Se Process===单端处理==================================================
        SeTime = new Date();
        Thread[] sepR1 = new Thread[UseLinker.size()];
        Thread[] sepR2 = new Thread[UseLinker.size()];
        for (int i = 0; i < UseLinker.size(); i++) {
            int finalI = i;
            sepR1[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SeProcess(UseLinkerFasqFileR1[finalI], UseLinkerFasqFileR1[finalI].getName().replace(".fastq", ""));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            int finalI1 = i;
            sepR2[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SeProcess(UseLinkerFasqFileR2[finalI1], UseLinkerFasqFileR2[finalI1].getName().replace(".fastq", ""));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            if (StepCheck("SeProcess")) {
                System.out.println(new Date() + "\tStart SeProcess");
                sepR1[i].start();
                sepR2[i].start();
            }
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            try {
                sepR1[i].join();
                sepR2[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //=============================================获取排序好的bed文件===============================================
        CustomFile[] R1SortBedFile = new CustomFile[UseLinker.size()];
        CustomFile[] R2SortBedFile = new CustomFile[UseLinker.size()];
        CustomFile[] SeBedpeFile = new CustomFile[UseLinker.size()];
        for (int i = 0; i < UseLinker.size(); i++) {
            R1SortBedFile[i] = new CustomFile(new SeProcess(UseLinkerFasqFileR1[i], IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, UseLinkerFasqFileR1[i].getName().replace(".fastq", ""), ReadsType).getSortBedFile());
            R2SortBedFile[i] = new CustomFile(new SeProcess(UseLinkerFasqFileR2[i], IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, UseLinkerFasqFileR2[i].getName().replace(".fastq", ""), ReadsType).getSortBedFile());
            SeBedpeFile[i] = new CustomFile(SeProcessDir + "/" + Prefix + "." + UseLinker.get(i) + ".bedpe");
            if (StepCheck("Bed2BedPe")) {
                new MergeBedToBedpe(R1SortBedFile[i], R2SortBedFile[i], SeBedpeFile[i], 4, "");//合并左右端bed文件，输出bedpe文件
            }
            //==========================================================================================================
            Stat.UseBed1.add(R1SortBedFile[i]);
            Stat.UseBed2.add(R2SortBedFile[i]);
            int finalI = i;
            ST = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Stat.UniqMapR1Num.add(R1SortBedFile[finalI].CalculatorLineNumber());
                        Stat.UniqMapR2Num.add(R2SortBedFile[finalI].CalculatorLineNumber());
                        Stat.BedpeFile.add(SeBedpeFile[finalI]);
                        Stat.BedpeNum.add(SeBedpeFile[finalI].CalculatorLineNumber());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            ST.start();
            SThread.add(ST);
            //==========================================================================================================
        }
        //==========================================获取酶切片段和染色体大小=============================================
        BedpeTime = new Date();
        Thread findenzy = FindRestrictionFragment();
        if (StepCheck("BedPeProcess")) {
            findenzy.start();
        }
        findenzy.join();
        //==============================================BedpeFile Process====bedpe 处理=====================================
        CustomFile[][] LinkerChrSameCleanBedpeFile = new CustomFile[UseLinker.size()][];
        CustomFile[] LinkerFinalSameCleanBedpeFile = new CustomFile[UseLinker.size()];
        CustomFile[] LinkerFinalDiffCleanBedpeFile = new CustomFile[UseLinker.size()];
        File[] FinalLinkerBedpe = new File[UseLinker.size()];//有效的bedpe文件,每种linker一个文件
        Thread[] LinkerProcess = new Thread[UseLinker.size()];//不同linker类型并行
        for (int i = 0; i < LinkerProcess.length; i++) {
            LinkerProcess[i] = BedpeProcess(UseLinker.get(i), SeBedpeFile[i]);
            if (StepCheck("BedPeProcess")) {
                LinkerProcess[i].start();
            }
            BedpeProcess Temp = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker.get(i)), Prefix + "." + UseLinker.get(i), Chromosomes, ChrEnzyFile, SeBedpeFile[i]);
            FinalLinkerBedpe[i] = Temp.getFinalFile();
            LinkerFinalSameCleanBedpeFile[i] = Temp.getSameNoDumpFile();
            LinkerFinalDiffCleanBedpeFile[i] = Temp.getDiffNoDumpFile();
            LinkerChrSameCleanBedpeFile[i] = Temp.getChrSameNoDumpFile();
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            LinkerProcess[i].join();
        }
        //==============================================================================================================
        STS = new Thread[UseLinker.size()];
        for (int i = 0; i < UseLinker.size(); i++) {
            int finalI = i;
            STS[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        BedpeProcess Temp = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker.get(finalI)), Prefix + "." + UseLinker.get(finalI), Chromosomes, ChrEnzyFile, SeBedpeFile[finalI]);
                        long selfnum = Temp.getSelfLigationFile().CalculatorLineNumber();
                        long renum = Temp.getReLigationFile().CalculatorLineNumber();
                        long validnum = Temp.getValidFile().CalculatorLineNumber();
                        long nodupnum = Temp.getFinalFile().CalculatorLineNumber();
                        long diffnum = Temp.getDiffFile().CalculatorLineNumber();
                        synchronized (STS) {
                            Stat.LigationFile.add(new String[]{Temp.getSelfLigationFile().getPath(), Temp.getReLigationFile().getPath(), Temp.getValidFile().getPath(), Temp.getDiffFile().getPath()});
                            Stat.LigationNum.add(new Long[]{selfnum, renum, validnum, diffnum});
                            Stat.NoRmdupName.add(Temp.getFinalFile().getPath());
                            Stat.NoRmdupNum.add(nodupnum);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            STS[i].start();
            SThread.add(STS[i]);
        }
        //=================================================BedpeFile To Inter===========================================
        CustomFile FinalBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".clean.bedpe");
        CustomFile SameBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".same.clean.bedpe");
        CustomFile DiffBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".diff.clean.bedpe");
        CustomFile[] ChrBedpeFile = new CustomFile[Chromosomes.length];
        for (int i = 0; i < Chromosomes.length; i++) {
            ChrBedpeFile[i] = new CustomFile(BedpeProcessDir + "/" + Prefix + "." + Chromosomes[i].Name + ".same.clean.bedpe");
        }
        CustomFile InterBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".inter.clean.bedpe");
        //--------------------------------------------------------------------------------------------------------------
        if (StepCheck("BedPeProcess")) {
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SameBedpeFile.Merge(LinkerFinalSameCleanBedpeFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t1.start();
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DiffBedpeFile.Merge(LinkerFinalDiffCleanBedpeFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t2.start();
            Thread t3 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FinalBedpeFile.Merge(FinalLinkerBedpe);//合并不同linker类型的bedpe文件
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t3.start();
            //合并不同linker的染色体内的交互，作为构建矩阵的输入文件
            Thread t4 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < Chromosomes.length; i++) {
                            for (int j = 0; j < UseLinker.size(); j++) {
                                ChrBedpeFile[i].Append(LinkerChrSameCleanBedpeFile[j][i]);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t4.start();
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        }
        if (StepCheck("BedPe2Inter")) {
            new BedpeToInter(FinalBedpeFile.getPath(), InterBedpeFile.getPath());//将交互区间转换成交互点
        }
        //=================================================Make Matrix==================================================
        MatrixTime = new Date();
        for (Chromosome s : Chromosomes) {
            if (s.Size == 0) {
                findenzy.start();
                findenzy.join();
            }
        }
        if (StepCheck("MakeMatrix")) {
            Thread[] mmt = new Thread[Resolution.length];
            for (int i = 0; i < Resolution.length; i++) {
                int finalI = i;
                mmt[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MakeMatrix matrix = new MakeMatrix(new File(MakeMatrixDir + "/" + Resolution[finalI]), Prefix, InterBedpeFile, ChrBedpeFile, Chromosomes, Resolution[finalI], Threads);//生成交互矩阵类
                            matrix.Run();//运行
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                mmt[i].start();
            }
            for (int i = 0; i < Resolution.length; i++) {
                mmt[i].join();
            }
            //------------------------------------------------------画热图-----------------------------------------
            for (int i = 0; i < DrawResolution.length; i++) {
                File OutDir = new File(MakeMatrixDir + "/img_" + Tools.UnitTrans(DrawResolution[i], "B", "M") + "M");
                if (!OutDir.isDirectory() && !OutDir.mkdir()) {
                    System.err.println(new Date() + "\tWarning! Can't Create " + OutDir);
                }
                MakeMatrix matrix = new MakeMatrix(new File(MakeMatrixDir + "/" + DrawResolution[i]), Prefix, InterBedpeFile, ChrBedpeFile, Chromosomes, DrawResolution[i], Threads);//生成交互矩阵类
                if (!new File(MakeMatrixDir + "/" + DrawResolution[i]).isDirectory()) {
                    matrix.Run();
                }
                File[] TwoDMatrixFile = matrix.getChrTwoDMatrixFile();
                for (int j = 0; j < Chromosomes.length; j++) {
                    new PlotMatrix(TwoDMatrixFile[j], new File(OutDir + "/" + Prefix + "." + Chromosomes[j].Name + "." + Tools.UnitTrans(DrawResolution[i], "B", "M") + "M.png"), DrawResolution[i]).Run(new String[]{Chromosomes[j].Name + ":0", Chromosomes[j].Name + ":0"});
                }
            }
        }
        //==============================================================================================================
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Stat.FinalBedpeName = FinalBedpeFile;
                    Stat.FinalBedpeNum = Stat.FinalBedpeName.CalculatorLineNumber();
                    Stat.IntraActionNum = SameBedpeFile.CalculatorLineNumber();
                    if (Stat.RestrictionSeq.replace("^", "").length() <= 4) {
                        Stat.ShortRegionNum = Statistic.RangeCount(SameBedpeFile, 0, 5000, 4);
                    } else {
                        Stat.ShortRegionNum += Statistic.RangeCount(SameBedpeFile, 0, 20000, 4);
                    }
                    Stat.InterActionNum = Stat.FinalBedpeNum - Stat.IntraActionNum;
                    Stat.LongRegionNum = Stat.IntraActionNum - Stat.ShortRegionNum;
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ST.start();
        SThread.add(ST);
        //=============================================TransLocation Detection==========================================
        TransTime = new Date();
        if (StepCheck("TransLocationDetection")) {
            TransLocationDetection(Chromosomes, FinalBedpeFile, DetectResolution, Threads);
        }
        EndTime = new Date();
        System.out.println("\n-------------------------------Time----------------------------------------");
        System.out.println("PreProcess:\t" + Tools.DateFormat((SeTime.getTime() - PreTime.getTime()) / 1000));
        System.out.println("SeProcess:\t" + Tools.DateFormat((BedpeTime.getTime() - SeTime.getTime()) / 1000));
        System.out.println("BedpeProcess:\t" + Tools.DateFormat((MatrixTime.getTime() - BedpeTime.getTime()) / 1000));
        System.out.println("MakeMatrix:\t" + Tools.DateFormat((TransTime.getTime() - MatrixTime.getTime()) / 1000));
        System.out.println("Translocation:\t" + Tools.DateFormat((EndTime.getTime() - TransTime.getTime()) / 1000));
        System.out.println("Total:\t" + Tools.DateFormat((EndTime.getTime() - PreTime.getTime()) / 1000));
        //===================================Report=====================================================================

        for (Thread t : SThread) {
            t.join();
        }
        Stat.Show();
//        Stat.ReportHtml(ArgumentList.get(OptOutPath) + "/result.html");
    }

    /**
     * Create reference genome index
     *
     * @param fastfile genome file
     * @return process thread
     */
    private Thread CreateIndex(File fastfile) {
        File IndexDir = new File(OutPath + "/" + Opts.IndexDir);
        if (!IndexDir.isDirectory() && !IndexDir.mkdirs()) {
            System.out.println("Create " + IndexDir + " false");
            System.exit(1);
        }
        IndexPrefix = new File(IndexDir + "/" + fastfile.getName());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ComLine = "bwa index -p " + IndexPrefix + " " + fastfile;
                    Opts.CommandOutFile.Append(ComLine + "\n");
                    Tools.ExecuteCommandStr(ComLine, null, null);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return t;
    }

    /**
     * <p>创建酶切片段文件，获取染色体大小</p>
     *
     * @return 线程句柄
     */
    private Thread FindRestrictionFragment() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(new Date() + "\tStart to find restriction fragment");
//                    Routine step = new Routine();
                    ArrayList<String> list = new ArrayList<>();
                    if (!EnzyPath.isDirectory() && !EnzyPath.mkdir()) {
                        System.err.println(new Date() + "\tCreate " + EnzyPath + " false !");
                    }
                    FindRestrictionSite fr = new FindRestrictionSite(GenomeFile, EnzyPath, Restriction, EnzyFilePrefix);
                    ArrayList<Chromosome> TempChrs = fr.Run();
                    File[] TempChrEnzyFile = fr.getChrFragmentFile();
//                     = Statistic.FindRestrictionSite(GenomeFile.getPath(), Restriction, EnzyFilePrefix);
                    for (int i = 0; i < Chromosomes.length; i++) {
                        boolean flag = false;
                        for (int j = 0; j < TempChrs.size(); j++) {
                            if (TempChrs.get(j).Name.equals(Chromosomes[i].Name)) {
                                Chromosomes[i] = TempChrs.get(j);
                                ChrEnzyFile[i] = TempChrEnzyFile[j];
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            System.err.println(new Date() + "\tWarning! No " + Chromosomes[i].Name + " in genomic file");
                        }
//                        if (temphash.containsKey(chr.Name)) {
//                            Chromosomes
////                            ChrSize.put(chr.Name, temphash.get(chr.Name));
//                        }
//                        list.add(chr + "\t" + temphash.get(chr));
                    }
                    System.out.println(new Date() + "\tEnd find restriction fragment");
//                    Tools.PrintList(list, new File(EnzyPath + "/" + Prefix + ".ChrSize"));//打印染色体大小信息
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Single end process
     *
     * @param FastqFile
     * @param Prefix
     * @return
     */
    private void SeProcess(CustomFile FastqFile, String Prefix) throws IOException, InterruptedException {
        ArrayList<File> SplitFastqFile = FastqFile.SplitFile(FastqFile.getPath(), 100000000);//1亿行作为一个单位拆分
        ArrayList<File> TempSplitFastq = new ArrayList<>(SplitFastqFile);
        File SamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getSamFile();
        File UniqSamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getUniqSamFile();
        CustomFile SortBedFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getSortBedFile();
        File[] SplitSamFile = new File[SplitFastqFile.size()];
        File[] SplitFilterSamFile = new File[SplitFastqFile.size()];
        File[] SplitSortBedFile = new File[SplitFastqFile.size()];
        Thread[] t2 = new Thread[Default.MaxThreads];
        int[] Index = new int[]{0};
        for (int i = 0; i < t2.length; i++) {
            t2[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (TempSplitFastq.size() > 0) {
                        int finalI;
                        File InFile;
                        synchronized (t2) {
                            try {
                                InFile = TempSplitFastq.remove(0);
                                finalI = Index[0];
                                Index[0]++;
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                        }
                        try {
                            SeProcess ssp = new SeProcess(InFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix + ".split" + finalI, ReadsType);//单端处理类
                            ssp.Threads = Threads;//设置线程数
                            ssp.AlignThreads = AlignThread;
                            ssp.setIteration(Iteration);
                            ssp.Run();
                            SplitSamFile[finalI] = ssp.getSamFile();
                            SplitFilterSamFile[finalI] = ssp.getUniqSamFile();
                            SplitSortBedFile[finalI] = ssp.getSortBedFile();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t2[i].start();
        }
        for (Thread aT2 : t2) {
            aT2.join();
        }
        for (File s : SplitFastqFile) {
            if (Opts.DeBugLevel < 1) {
                s.delete();
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileTool.MergeSamFile(SplitSamFile, SamFile);
                    FileTool.MergeSamFile(SplitFilterSamFile, UniqSamFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                for (File s : SplitSamFile) {
                    if (Opts.DeBugLevel < 1) {
                        s.delete();
                    }
                }
                for (File s : SplitFilterSamFile) {
                    if (Opts.DeBugLevel < 1) {
                        s.delete();
                    }
                }
            }
        }).start();
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SortBedFile.MergeSortFile(SplitSortBedFile, new int[]{4}, "", "\\s+");
                    for (File s : SplitSortBedFile) {
                        if (Opts.DeBugLevel < 1) {
                            s.delete();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t3.start();
        t3.join();
    }

    private Thread BedpeProcess(String UseLinker, CustomFile SeBedpeFile) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BedpeProcess bedpe = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker), Prefix + "." + UseLinker, Chromosomes, ChrEnzyFile, SeBedpeFile);//bedpe文件处理类
                    bedpe.Threads = Threads;//设置线程数
                    bedpe.Run();//运行
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //======================================developing===========================================

    /**
     * @param Chromosomes 染色体列表
     * @param BedPeFile   bedpe文件
     * @param Resolution  分辨率
     * @throws InterruptedException
     * @throws IOException
     */
    private void TransLocationDetection(Chromosome[] Chromosomes, CustomFile BedPeFile, int Resolution, int Threads) throws InterruptedException, IOException {
        //创建列表
        ArrayList<String> Prefix = new ArrayList<>();
        ArrayList<Chromosome> Chr1 = new ArrayList<>();
        ArrayList<Chromosome> Chr2 = new ArrayList<>();
        for (int i = 0; i < Chromosomes.length - 1; i++) {
            for (int j = i + 1; j < Chromosomes.length; j++) {
                Chr1.add(Chromosomes[i]);
                Chr2.add(Chromosomes[j]);
                Prefix.add(TransLocationDir + "/" + Chromosomes[i].Name + "-" + Chromosomes[j].Name + "." + Tools.UnitTrans(Resolution, "b", "M") + "M");
            }
        }
        Thread[] t = new Thread[Threads];//创建多个线程
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (Prefix.size() > 0) {
                        Chromosome chr1, chr2;
                        String prefix;
                        synchronized (t) {
                            try {
                                chr1 = Chr1.remove(0);
                                chr2 = Chr2.remove(0);
                                prefix = Prefix.remove(0);
                            } catch (IndexOutOfBoundsException e) {
                                break;
                            }
                        }
                        try {
                            //如果存在矩阵文件就不创建，节省时间
                            if (!new File(prefix + ".2d.matrix").exists()) {
                                new CreateMatrix(BedPeFile, Chromosomes, Default.Resolution / 2, prefix, 1).Run(new ChrRegion(chr1, 0, chr1.Size), new ChrRegion(chr2, 0, chr2.Size));
                            }
                            //开始识别
                            TranslocationDetection Trans = new TranslocationDetection(new ChrRegion(chr1, 0, chr1.Size), new ChrRegion(chr2, 0, chr2.Size), new File(prefix + ".2d.matrix"), BedPeFile, Default.Resolution / 2, prefix);
                            Trans.Run();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            t[i].start();
        }
        for (Thread aT : t) {
            aT.join();
        }
    }

    //==============================
    private void GetOption(File ConfFile, File AdvConfFile) throws IOException {
        ReadConfFile(ConfFile);
        ReadConfFile(AdvConfFile);
    }

    private void ReadConfFile(File file) throws IOException {
        String line;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(file));
        while ((line = infile.readLine()) != null) {
            line = line.trim();
            if (line.equals("") || line.matches("^/.*|^#.*")) {
                continue;
            }
            str = line.split("\\s*=\\s*", 2);
            if (ArgumentList.containsKey(str[0]) && str.length >= 2) {
                ArgumentList.put(str[0], str[1]);
            }
        }
        infile.close();
    }

    private void OptionListInit() {
        for (String opt : RequiredParameter) {
            ArgumentList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ArgumentList.put(opt, "");
        }
        ArgumentList.put(OptOutPath, Default.OutPath);
        ArgumentList.put(OptMaxMisMatchLength, "3");
        ArgumentList.put(OptMinReadsLength, "16");
        ArgumentList.put(OptMaxReadsLength, "20");
        ArgumentList.put(OptMatchScore, "1");
        ArgumentList.put(OptMisMatchScore, "-2");
        ArgumentList.put(OptIndelScore, "-2");
        ArgumentList.put(OptReadsType, "Short");
        ArgumentList.put(OptAlignMisMatch, "0");
        ArgumentList.put(OptAlignThread, "8");
        ArgumentList.put(OptResolution, String.valueOf(Default.Resolution));
        ArgumentList.put(OptDrawResolution, String.valueOf(Default.Resolution));
        ArgumentList.put(OptThreads, "4");
        ArgumentList.put(OptStep, "-");
    }

    private void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ArgumentList.get(opt).equals("")) {
                System.err.println("Error ! no " + opt);
                System.exit(1);
            }
        }
        //================================================
        FastqFile = new CustomFile[ArgumentList.get(OptFastqFile).split("\\s+").length];
        for (int i = 0; i < FastqFile.length; i++) {
            FastqFile[i] = new CustomFile(ArgumentList.get(OptFastqFile).split("\\s+")[i]);
        }
        if (FastqFile.length >= 2) {
            FileType = Opts.PairEnd;
        }
        GenomeFile = new CustomFile(ArgumentList.get(OptGenomeFile));
        Prefix = ArgumentList.get(OptPrefix);
        OutPath = new File(ArgumentList.get(OptOutPath));
        ArrayList<String> Chrs = new ArrayList<>();
        Chrs.addAll(Arrays.asList(ArgumentList.get(OptChromosome).split("\\s+")));
        Restriction = ArgumentList.get(OptRestriction);
        LinkerFile = new CustomFile(ArgumentList.get(OptLinkerFile));
        AdapterFile = !ArgumentList.get(OptAdapterFile).equals("") ? new CustomFile(ArgumentList.get(OptAdapterFile)) : null;
        LinkersType.addAll(Arrays.asList(ArgumentList.get(OptLinkersType).split("\\s+")));
        UseLinker.addAll(Arrays.asList(ArgumentList.get(OptUseLinker).split("\\s+")));
        MatchScore = Integer.parseInt(ArgumentList.get(OptMatchScore));
        MisMatchScore = Integer.parseInt(ArgumentList.get(OptMisMatchScore));
        IndelScore = Integer.parseInt(ArgumentList.get(OptIndelScore));
        int MaxMisMatchLength = Integer.parseInt(ArgumentList.get(OptMaxMisMatchLength));
        IndexPrefix = new File(ArgumentList.get(OptIndexFile));
        ReadsType = ArgumentList.get(OptReadsType).equals("Short") ? Opts.ShortReads : ArgumentList.get(OptReadsType).equals("Long") ? Opts.LongReads : Opts.ErrorFormat;
        AlignMisMatch = Integer.parseInt(ArgumentList.get(OptAlignMisMatch));
        AlignThread = Integer.parseInt(ArgumentList.get(OptAlignThread));
        AlignMinQuality = Integer.parseInt(ArgumentList.get(OptAlignMinQuality));
        MinReadsLength = Integer.parseInt(ArgumentList.get(OptMinReadsLength));
        MaxReadsLength = Integer.parseInt(ArgumentList.get(OptMaxReadsLength));
        Resolution = StringArrays.toInteger(ArgumentList.get(OptResolution).split("\\s+"));
        DrawResolution = StringArrays.toInteger(ArgumentList.get(OptDrawResolution).split("\\s+"));
        Threads = Integer.parseInt(ArgumentList.get(OptThreads));
        Iteration = Boolean.valueOf(ArgumentList.get(OptIteration));
        Step.addAll(Arrays.asList(ArgumentList.get(OptStep).split("\\s+")));
        Chromosomes = new Chromosome[Chrs.size()];
        ChrEnzyFile = new File[Chrs.size()];
        for (int i = 0; i < Chromosomes.length; i++) {
            Chromosomes[i] = new Chromosome(Chrs.get(i));
        }

        //================================================
        if (Prefix.equals("")) {
            ArgumentList.put(OptPrefix, Default.Prefix);
        }
        if (UseLinker.size() == 0) {
            UseLinker = LinkersType;
        }
        if (!OutPath.isDirectory()) {
            System.err.println("Error, " + OutPath + " is not a directory");
            System.exit(0);
        }
        if (!GenomeFile.isFile()) {
            System.err.println("Error, " + GenomeFile + " is not a file");
            System.exit(0);
        }
        for (int i = 0; i < FastqFile.length; i++) {
            if (!FastqFile[i].isFile()) {
                System.err.println("Error, " + FastqFile[i] + " is not a file");
                System.exit(0);
            }
        }
        if (!LinkerFile.isFile()) {
            System.err.println("Error, " + LinkerFile + " is not a file");
            System.exit(0);
        }
        //=======================================================================
        BufferedReader infile = new BufferedReader(new FileReader(LinkerFile));
        int linkerLength = infile.readLine().length();
        infile.close();
        MinLinkerFilterQuality = (linkerLength - MaxMisMatchLength) * MatchScore + MaxMisMatchLength * MisMatchScore;//设置linkerfilter最小分数
        PreProcessDir = new File(OutPath + "/" + Opts.PreDir);
        SeProcessDir = new File(OutPath + "/" + Opts.SeDir);
        BedpeProcessDir = new File(OutPath + "/" + Opts.BedpeDir);
        MakeMatrixDir = new File(OutPath + "/" + Opts.MatrixDir);
        TransLocationDir = new File(OutPath + "/" + Opts.TransDir);
        EnzyPath = new File(OutPath + "/" + Opts.EnzyFragDir);
        if (!PreProcessDir.isDirectory() && !PreProcessDir.mkdirs()) {
            System.err.println("Can't create " + PreProcessDir);
            System.exit(1);
        }
        if (!SeProcessDir.isDirectory() && !SeProcessDir.mkdirs()) {
            System.err.println("Can't create " + SeProcessDir);
            System.exit(1);
        }
        if (!BedpeProcessDir.isDirectory() && !BedpeProcessDir.mkdirs()) {
            System.err.println("Can't create " + BedpeProcessDir);
            System.exit(1);
        }
        if (!MakeMatrixDir.isDirectory() && !MakeMatrixDir.mkdirs()) {
            System.err.println("Can't create " + MakeMatrixDir);
            System.exit(1);
        }
        if (!TransLocationDir.isDirectory() && !TransLocationDir.mkdirs()) {
            System.err.println("Can't create " + TransLocationDir);
            System.exit(1);
        }
        if (!EnzyPath.isDirectory() && !EnzyPath.mkdirs()) {
            System.err.println("Can't create " + EnzyPath);
            System.exit(1);
        }
        EnzyFilePrefix = Prefix + "." + Restriction.replace("^", "");
        Stat.OutPath = OutPath;
    }

    private boolean StepCheck(String step) {
        if (Step.size() > 0) {
            if (Step.get(0).equals(step)) {
                Step.remove(0);
                return true;
            } else if (Step.get(0).equals("-") && Step.size() > 1) {
                if (Step.get(1).equals(step)) {
                    Step.remove(0);
                    Step.remove(0);
                }
                return true;
            } else return Step.get(0).equals("-") && Step.size() == 1;
        }
        return false;
    }

    private void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ArgumentList.get(opt));
        }
        System.out.println("======================================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ArgumentList.get(opt));
        }
    }

    public Hashtable<String, String> getArgumentList() {
        return ArgumentList;
    }

    public String[] getRequiredParameter() {
        return RequiredParameter;
    }

    public String[] getOptionalParameter() {
        return OptionalParameter;
    }
}
