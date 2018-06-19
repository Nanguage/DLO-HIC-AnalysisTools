import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import bin.*;
import lib.Command.Execute;
import lib.File.*;
import lib.tool.*;
import lib.unit.CustomFile;
import lib.unit.Opts;

public class Main {
    private final String OptFastqFile = "FastqFile";//fastq文件
    //    private final String OptFileType = "FileType";
    private final String OptGenomeFile = "GenomeFile";//基因组文件
    private final String OptPrefix = "Prefix";//输出前缀
    private final String OptOutPath = "OutPath";//输出路径
    private final String OptChromosome = "Chromosome";//染色体名
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
    private final String OptThreads = "Threads";//线程数
    private final String OptStep = "Step";
    //===================================================================
    private CustomFile[] FastqFile;
    private CustomFile GenomeFile;
    private File OutPath;
    private String Prefix;
    private ArrayList<String> Chromosome = new ArrayList<>();
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
    private int Resolution;
    private int Threads;
    private Date PreTime, SeTime, BedpeTime, MatrixTime, TransTime, EndTime;
    private ArrayList<String> Step = new ArrayList<>();
    private ArrayList<Thread> SThread = new ArrayList<>();

    //===================================================================
    private String[] RequiredParameter = new String[]{OptFastqFile, OptGenomeFile, OptLinkerFile, OptChromosome, OptRestriction, OptLinkersType, OptAlignMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptIndexFile, OptPrefix, OptAdapterFile, OptMaxMisMatchLength, OptMinReadsLength, OptMaxReadsLength, OptUseLinker, OptMatchScore, OptMisMatchScore, OptIndelScore, OptReadsType, OptAlignMisMatch, OptAlignThread, OptResolution, OptStep, OptThreads};
    private Hashtable<String, String> ArgumentList = new Hashtable<>();
    private Hashtable<String, Integer> ChrSize = new Hashtable<>();//染色体大小
    private int MinLinkerFilterQuality;
    //    private String AddQuality;
    private File EnzyPath;//酶切位点文件目录
    private String EnzyFilePrefix;//酶切位点文件前缀
    private File PreProcessDir;//预处理输出目录
    private File SeProcessDir;//单端处理输出目录
    private File BedpeProcessDir;//bedpe处理输出目录
    private File MakeMatrixDir;//建立矩阵输出目录
    //    private Routine step = new Routine();
    private Report Stat = new Report();

    Main(File ConfigFile) throws IOException {
        OptionListInit();
        GetOption(ConfigFile);//获取参数
        Init();
    }

    Main() {
        OptionListInit();
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        //==============================================测试区==========================================================


        //==============================================================================================================
        if (args.length < 1) {
            System.out.println("Usage:    java -jar DLO-HIC-AnalysisTools.jar <config.txt>");
            System.exit(0);
        }
        Main main = new Main(new File(args[0]));
        main.ShowParameter();
        main.Run();
    }

    public void Run() throws IOException, InterruptedException {

        //===========================================初始化输出文件======================================================
        File[] FinalLinkerBedpe = new File[UseLinker.size()];
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
        Stat.Chromosome = Chromosome;
        Stat.RawDataFile = FastqFile;
        Stat.RawDataReadsNum = new Long[FastqFile.length];
        //==============================================================================================================
        Opts.CommandOutFile.delete();
        CustomFile FinalBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".bedpe");
        CustomFile InterBedpeFile = new CustomFile(BedpeProcessDir + "/" + Prefix + ".inter.bedpe");
        Thread ST;
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
            sepR1[i] = SeProcess(UseLinkerFasqFileR1[i], UseLinkerFasqFileR1[i].getPath().replaceAll(".*/", "").replace(".fastq", ""));
            sepR2[i] = SeProcess(UseLinkerFasqFileR2[i], UseLinkerFasqFileR2[i].getPath().replaceAll(".*/", "").replace(".fastq", ""));
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
        try {
            findenzy.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //==============================================BedpeFile Process====bedpe 处理=====================================
        Thread[] LinkerProcess = new Thread[UseLinker.size()];//不同linker类型并行
        for (int i = 0; i < UseLinker.size(); i++) {
            LinkerProcess[i] = BedpeProcess(UseLinker.get(i), SeBedpeFile[i]);
            if (StepCheck("BedPeProcess")) {
                LinkerProcess[i].start();
            }
            BedpeProcess Temp = new BedpeProcess(BedpeProcessDir, Prefix + "." + UseLinker.get(i), Chromosome.toArray(new String[Chromosome.size()]), EnzyFilePrefix, SeBedpeFile[i]);
            FinalLinkerBedpe[i] = Temp.getFinalBedpeFile();
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            LinkerProcess[i].join();
        }
        //==============================================================================================================
        for (int i = 0; i < UseLinker.size(); i++) {
            BedpeProcess Temp = new BedpeProcess(BedpeProcessDir, Prefix + "." + UseLinker.get(i), Chromosome.toArray(new String[Chromosome.size()]), EnzyFilePrefix, SeBedpeFile[i]);
            Stat.LigationFile.add(new String[]{Temp.getSelfLigationFile().getPath(), Temp.getReLigationFile().getPath(), Temp.getValidBedpeFile().getPath()});
            Stat.NoRmdupName.add(Temp.getFinalBedpeFile().getPath());
            ST = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Stat.LigationNum.add(new Long[]{Temp.getSelfLigationFile().CalculatorLineNumber(), Temp.getReLigationFile().CalculatorLineNumber(), Temp.getValidBedpeFile().CalculatorLineNumber()});
                        Stat.NoRmdupNum.add(Temp.getFinalBedpeFile().CalculatorLineNumber());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            ST.start();
            SThread.add(ST);
        }
        //=================================================BedpeFile To Inter===========================================
        if (StepCheck("BedPeProcess")) {
            FinalBedpeFile.Merge(FinalLinkerBedpe);//合并不同linker类型的bedpe文件
        }
        if (StepCheck("BedPe2Inter")) {
            new BedpeToInter(FinalBedpeFile.getPath(), InterBedpeFile.getPath());//将交互区间转换成交互点
        }
        //=================================================Make Matrix==================================================
        MatrixTime = new Date();
        int i = 0;
        if (ChrSize.keySet().size() == 0) {
            findenzy.start();
            try {
                findenzy.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int[] chrSize = new int[ChrSize.size()];
        for (String chr : Chromosome) {
            if (ChrSize.containsKey(chr)) {
                chrSize[i++] = ChrSize.get(chr);
            }
        }
        MakeMatrix matrix = new MakeMatrix(MakeMatrixDir, Prefix, InterBedpeFile.getPath(), Chromosome.toArray(new String[Chromosome.size()]), chrSize, Resolution);//生成交互矩阵类
        if (StepCheck("MakeMatrix")) {
            matrix.Run();//运行
        }
        CustomFile[] IntraActionFile = matrix.getChrInterBedpeFile();
        //==============================================================================================================
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Stat.FinalBedpeName = FinalBedpeFile;
                    Stat.FinalBedpeNum = Stat.FinalBedpeName.CalculatorLineNumber();
                    for (CustomFile s : IntraActionFile) {
                        Stat.IntraActionNum = Stat.IntraActionNum + s.CalculatorLineNumber();
                        if (Stat.RestrictionSeq.replace("^", "").length() <= 4) {
                            Stat.ShortRegionNum += Statistic.RangeCount(s, 0, 5000, 4);
                        } else {
                            Stat.ShortRegionNum += Statistic.RangeCount(s, 0, 20000, 4);
                        }
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
        //=============================================Cluster==========================================================
        TransTime = new Date();
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
                    new Execute(ComLine);
                } catch (IOException e) {
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
//                    Routine step = new Routine();
                    ArrayList<String> list = new ArrayList<>();
                    if (!EnzyPath.isDirectory() && !EnzyPath.mkdir()) {
                        System.out.println(new Date() + "\tCreate " + EnzyPath + " false !");
                    }
                    Hashtable<String, Integer> temphash = Statistic.FindRestrictionSite(GenomeFile.getPath(), Restriction, EnzyFilePrefix);
                    for (String chr : Chromosome) {
                        if (temphash.containsKey(chr)) {
                            ChrSize.put(chr, temphash.get(chr));
                        }
                        list.add(chr + "\t" + temphash.get(chr));
                    }
                    Tools.PrintList(list, EnzyPath + "/" + Prefix + ".ChrSize");//打印染色体大小信息
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
    private Thread SeProcess(CustomFile FastqFile, String Prefix) {
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<File> SplitFile = new ArrayList<>();
                try {
                    SplitFile = FastqFile.SplitFile(FastqFile.getPath(), 200000000);//2亿行作为一个单位拆分
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File SamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getSamFile();
                File FilterSamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getFilterSamFile();
//                File BamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix).getBamFile();
                CustomFile SortBedFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix, ReadsType).getSortBedFile();
                Thread[] t2 = new Thread[SplitFile.size()];
                File[] SplitSamFile = new File[SplitFile.size()];
                File[] SplitFilterSamFile = new File[SplitFile.size()];
                File[] SplitSortBedFile = new File[SplitFile.size()];
                for (int i = 0; i < SplitFile.size(); i++) {
                    int finalI = i;
                    File finalSplitFile = SplitFile.get(finalI);
                    t2[i] = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                SeProcess ssp = new SeProcess(finalSplitFile, IndexPrefix, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix + "." + finalI, ReadsType);//单端处理类
                                ssp.Threads = Threads;//设置线程数
                                ssp.AlignThreads = AlignThread;
                                ssp.Run();
                                SplitSamFile[finalI] = ssp.getSamFile();
                                SplitFilterSamFile[finalI] = ssp.getFilterSamFile();
                                SplitSortBedFile[finalI] = ssp.getSortBedFile();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    t2[i].start();
                }
                for (int i = 0; i < SplitFile.size(); i++) {
                    try {
                        t2[i].join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (File s : SplitFile) {
                    s.delete();
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileTool.MergeSamFile(SplitSamFile, SamFile);
                            FileTool.MergeSamFile(SplitFilterSamFile, FilterSamFile);
//                            FileTool.Merge(SplitBamFile, BamFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        for (File s : SplitSamFile) {
                            s.delete();
                        }
                        for (File s : SplitFilterSamFile) {
                            s.delete();
                        }
//                        for (File s : SplitBamFile) {
//                            s.delete();
//                        }
                    }
                }).start();
                Thread t3 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SortBedFile.MergeSortFile(SplitSortBedFile, new int[]{4}, "", "\\s+");
                            for (File s : SplitSortBedFile) {
                                s.delete();
                            }
//                            FileTool.Merge(SplitBedFile, BedFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                t3.start();
                try {
                    t3.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return t1;
    }

    private Thread BedpeProcess(String UseLinker, File SeBedpeFile) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BedpeProcess bedpe = new BedpeProcess(BedpeProcessDir, Prefix + "." + UseLinker, Chromosome.toArray(new String[Chromosome.size()]), EnzyFilePrefix, SeBedpeFile);//bedpe文件处理类
                    bedpe.Threads = Threads;//设置线程数
                    bedpe.Run();//运行
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void GetOption(File ConfFile) throws IOException {
        String line;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(ConfFile));
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
        ArgumentList.put(OptOutPath, Opts.Default.OutPath);
        ArgumentList.put(OptMaxMisMatchLength, "3");
        ArgumentList.put(OptMinReadsLength, "16");
        ArgumentList.put(OptMaxReadsLength, "20");
//        ArgumentList.put(OptFileType, "Single");
        ArgumentList.put(OptMatchScore, "1");
        ArgumentList.put(OptMisMatchScore, "-2");
        ArgumentList.put(OptIndelScore, "-2");
        ArgumentList.put(OptReadsType, String.valueOf(Opts.ShortReads));
        ArgumentList.put(OptAlignMisMatch, "0");
        ArgumentList.put(OptAlignThread, "8");
        ArgumentList.put(OptResolution, String.valueOf(Opts.Default.Resolution));
        ArgumentList.put(OptThreads, "4");
        ArgumentList.put(OptStep, "-");
    }

    private void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ArgumentList.get(opt).equals("")) {
                System.err.println("Error ! no " + opt);
                System.exit(0);
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
        Chromosome.addAll(Arrays.asList(ArgumentList.get(OptChromosome).split("\\s+")));
        Restriction = ArgumentList.get(OptRestriction);
        LinkerFile = new CustomFile(ArgumentList.get(OptLinkerFile));
        AdapterFile = new CustomFile(ArgumentList.get(OptAdapterFile));
        LinkersType.addAll(Arrays.asList(ArgumentList.get(OptLinkersType).split("\\s+")));
        UseLinker.addAll(Arrays.asList(ArgumentList.get(OptUseLinker).split("\\s+")));
        MatchScore = Integer.parseInt(ArgumentList.get(OptMatchScore));
        MisMatchScore = Integer.parseInt(ArgumentList.get(OptMisMatchScore));
        IndelScore = Integer.parseInt(ArgumentList.get(OptIndelScore));
        int MaxMisMatchLength = Integer.parseInt(ArgumentList.get(OptMaxMisMatchLength));
        IndexPrefix = new File(ArgumentList.get(OptIndexFile));
        ReadsType = Integer.parseInt(ArgumentList.get(OptReadsType));
        AlignMisMatch = Integer.parseInt(ArgumentList.get(OptAlignMisMatch));
        AlignThread = Integer.parseInt(ArgumentList.get(OptAlignThread));
        AlignMinQuality = Integer.parseInt(ArgumentList.get(OptAlignMinQuality));
        MinReadsLength = Integer.parseInt(ArgumentList.get(OptMinReadsLength));
        MaxReadsLength = Integer.parseInt(ArgumentList.get(OptMaxReadsLength));
        Resolution = Integer.parseInt(ArgumentList.get(OptResolution));
        Threads = Integer.parseInt(ArgumentList.get(OptThreads));
        Step.addAll(Arrays.asList(ArgumentList.get(OptStep).split("\\s+")));
        //================================================
        if (Prefix.equals("")) {
            ArgumentList.put(OptPrefix, Opts.Default.Prefix);
        }
        if (UseLinker.size() == 0) {
            UseLinker = LinkersType;
        }
        if (!OutPath.isDirectory()) {
            System.err.println("Wrong OutPath " + OutPath + " is not a directory");
            System.exit(0);
        }
        if (!GenomeFile.isFile()) {
            System.err.println("Wrong " + OptGenomeFile + " " + GenomeFile + " is not a file");
            System.exit(0);
        }
        for (int i = 0; i < FastqFile.length; i++) {
            if (!FastqFile[i].isFile()) {
                System.err.println("Wrong " + FastqFile[i] + " is not a file");
                System.exit(0);
            }
        }
        if (!LinkerFile.isFile()) {
            System.err.println("Wrong " + OptLinkerFile + " " + LinkerFile + " is not a file");
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
        if (!EnzyPath.isDirectory() && !EnzyPath.mkdirs()) {
            System.err.println("Can't create " + EnzyPath);
            System.exit(1);
        }
        EnzyFilePrefix = EnzyPath + "/" + Prefix + "." + Restriction.replace("^", "");
//        step.Threads = Threads;
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
