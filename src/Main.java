import java.io.*;
import java.text.DateFormat;
import java.util.*;

import bin.*;
import kotlin.text.Charsets;
import lib.File.*;
import lib.Image.PlotMatrix;
import lib.tool.*;
import lib.tool.FindRestrictionSite;
import lib.unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import script.CreateMatrix;

public class Main {

    //===================================================================
    private Chromosome[] Chromosomes;
    private String[] HalfLinker, LinkerSeq;
    private String LinkerA, LinkerB;
    private String Restriction;
    private File LinkerFile;
    private File AdapterFile;
    private String[] AdapterSeq;
    private ArrayList<String> LinkersType = new ArrayList<>();
    private ArrayList<String> UseLinker = new ArrayList<>();
    private File IndexPrefix;
    private int MatchScore, MisMatchScore, InDelScore;
    private int ReadsType;
    private int AlignMisMatch;
    private int AlignThread;
    private int MinUniqueScore;
    private int MinReadsLength, MaxReadsLength;
    private int[] Resolution;
    private int[] DrawResolution;
    private int DetectResolution;
    private int LinkerLength, MinLinkerLength;
    private boolean Iteration = true;
    private int Threads;
    private Date PreTime, SeTime, BedpeTime, MatrixTime, TransTime, EndTime;
    private ArrayList<String> Step = new ArrayList<>();
    private ArrayList<Thread> SThread = new ArrayList<>();
    private Properties Config = new Properties();
    //===================================================================
    private int MinLinkerFilterQuality;
    private File EnzyPath;//酶切位点文件目录
    private String EnzyFilePrefix;//酶切位点文件前缀
    private File[] ChrEnzyFile;//每条染色体的酶切位点位置文件
    private File PreProcessDir;//预处理输出目录
    private File SeProcessDir;//单端处理输出目录
    private File BedpeProcessDir;//bedpe处理输出目录
    private File MakeMatrixDir;//建立矩阵输出目录
    private File TransLocationDir;//染色体易位输出目录
    private File ReportDir;//生成报告目录
    private Report Stat;

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
        //获取配置文件和高级配置文件
        CustomFile ConfigureFile = ComLine.hasOption("conf") ? new CustomFile(ComLine.getOptionValue("conf")) : Opts.ConfigFile;
        CustomFile AdvConfigFile = ComLine.hasOption("adv") ? new CustomFile(ComLine.getOptionValue("adv")) : Opts.AdvConfigFile;
        GetOption(ConfigureFile, AdvConfigFile);//读取配置信息
        //获取命令行参数信息
        if (ComLine.hasOption("i")) {
            Config.setProperty(Require.InputFile.toString(), String.join(" ", ComLine.getOptionValues("i")));
        }
        if (ComLine.hasOption("p")) {
            Config.setProperty(Optional.Prefix.toString(), ComLine.getOptionValue("p"));
        }
        if (ComLine.hasOption("o")) {
            Config.setProperty(Optional.OutPath.toString(), ComLine.getOptionValue("o"));
        }
        if (ComLine.hasOption("s")) {
            Config.setProperty(Optional.Step.toString(), ComLine.getOptionValue("s"));
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
        Stat.RunTime.StartTime = DateFormat.getDateTimeInstance().format(new Date());
        Stat.ComInfor.HalfLinkerA = LinkerA;
        Stat.ComInfor.HalfLinkerB = LinkerB;
        Stat.ComInfor.MatchScore = MatchScore;
        Stat.ComInfor.MisMatchScore = MisMatchScore;
        Stat.ComInfor.InDelScore = InDelScore;
        Stat.ComInfor.Restriction = Restriction;
        Stat.ComInfor.IndexPrefix = IndexPrefix;
        Stat.MinUniqueScore = MinUniqueScore;
        Stat.ComInfor.MinReadsLen = MinReadsLength;
        Stat.ComInfor.MaxReadsLen = MaxReadsLength;
        Stat.ComInfor.Resolution = Resolution;
        Stat.ComInfor.Thread = Threads;
        Stat.LinkersType = LinkersType;
        Stat.LinkerClassInit(UseLinker.size());
        Stat.PreDir = PreProcessDir;
        Stat.SeDir = SeProcessDir;
        Stat.BedpeDir = BedpeProcessDir;
        Stat.MatrixDir = MakeMatrixDir;
        for (lib.unit.Chromosome Chromosome : Chromosomes) {
            Stat.Chromosome.add(Chromosome.Name);
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            Stat.UseLinker[i].LinkerType = UseLinker.get(i);
            Stat.UseLinker[i].SeProcessOutDir = SeProcessDir;
        }
//        Stat.RawDataFile = Opts.InputFile;
//        Stat.RawDataReadsNum = new Long[InputFile.length];
        //==============================================================================================================
        Opts.CommandOutFile.delete();
        Thread ST;
        Thread[] STS;
        //==========================================Create Index========================================================
        Thread createindex = new Thread();
        if (IndexPrefix == null || IndexPrefix.getName().equals("")) {
            createindex = CreateIndex(Opts.GenomeFile);
            createindex.start();
        }
        //=========================================linker filter==linker 过滤===========================================
        PreTime = new Date();
        //-----------------------------------Adapter序列处理------------------------------
        AdapterFile.delete();//删除原来的Adapter文件
        FileUtils.touch(AdapterFile);//创建新的Adapter文件
        if (AdapterSeq != null) {
            //若Adapter序列不为空
            if (AdapterSeq[0].equals("Auto")) {
                //标记为自动识别Adapter
                AdapterSeq = new String[1];
                AdapterSeq[0] = Opts.InputFile.AdapterDetect(new File(PreProcessDir + "/" + Opts.Prefix), MinLinkerLength + LinkerLength);
                System.out.println(new Date() + "\tDetected adapter seq:\t" + AdapterSeq[0]);
            }
            //将Adapter序列输出到文件中
            FileUtils.write(AdapterFile, String.join("\n", AdapterSeq), Charsets.UTF_8);
            Stat.AdapterSequence = String.join(" ", AdapterSeq);
        }
        //---------------------------------保存linker序列--------------------------------
        LinkerFile.delete();
        FileUtils.touch(LinkerFile);
        FileUtils.write(LinkerFile, String.join("\n", LinkerSeq), Charsets.UTF_8);
        //-----------------------------------------------------------------------------
        PreProcess preprocess;
        preprocess = new PreProcess(PreProcessDir, Opts.Prefix, Opts.InputFile, LinkerFile, AdapterFile, MatchScore, MisMatchScore, InDelScore, Threads * 4);
        if (StepCheck("LinkerFilter")) {
            preprocess.Run();
        }
        //==============================================================================================================
        File PastFile = preprocess.getPastFile();//获取past文件位置
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Stat.RawDataReadsNum = Opts.InputFile.CalculatorLineNumber() / 4;
                    //calculate linker count
                    Stat.LinkersNum = Statistic.CalculateLinkerCount(PastFile, LinkersType.toArray(new String[0]), MinLinkerFilterQuality, Threads);
                    File LinkerDisFile = new File(Stat.getDataDir() + "/LinkerScoreDis.data");
                    Statistic.CalculateLinkerScoreDistribution(PastFile, LinkerLength * MatchScore, LinkerDisFile);
                    Opts.LinkerScoreDisFile = new File(Stat.getImageDir() + "/" + LinkerDisFile.getName().replace(".data", ".png"));
                    String ComLine = "python " + Opts.StatisticPlotFile + " -i " + LinkerDisFile + " -t bar -o " + Opts.LinkerScoreDisFile;
                    Tools.ExecuteCommandStr(ComLine, null, null);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ST.start();
        SThread.add(ST);
        preprocess = null;
        //==========================================Divide Linker=======================================================

        DivideLinker divideLinker = new DivideLinker(PastFile, SeProcessDir + "/" + Opts.Prefix, LinkersType.toArray(new String[0]), Restriction, DivideLinker.All, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, Opts.InputFile.FastqPhred());
        divideLinker.setThreads(Threads);
        LinkerFastqFileR1 = divideLinker.getR1FastqFile();
        LinkerFastqFileR2 = divideLinker.getR2FastqFile();
        Stat.ReadsLengthDisBase64 = new String[LinkersType.size()];
        if (StepCheck("DivideLinker")) {
            divideLinker.Run();
        }
        //=========================================calculate reads length===============================================
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < LinkersType.size(); i++) {
                        double[][] dis = new double[2][];
                        dis[0] = Statistic.ReadsLengthDis(LinkerFastqFileR1[i], null);
                        dis[1] = Statistic.ReadsLengthDis(LinkerFastqFileR2[i], null);
                        File OutFile = new File(Stat.getDataDir() + "/" + Opts.Prefix + "." + LinkersType.get(i) + ".reads_length_distribution.data");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(OutFile));
                        writer.write("Length\tR1\tR2\n");
                        for (int j = 0; j < Math.max(dis[0].length, dis[1].length); j++) {
                            writer.write(j + "\t");
                            if (j > dis[0].length - 1) {
                                writer.write("0\t");
                            } else {
                                writer.write(dis[0][j] + "\t");
                            }
                            if (j > dis[1].length - 1) {
                                writer.write("0\n");
                            } else {
                                writer.write(dis[1][j] + "\n");
                            }
                        }
                        writer.close();
                        File PngFile = new File(Stat.getImageDir() + "/" + OutFile.getName().replace(".data", ".png"));
                        String ComLine = Opts.Python + " " + Opts.StatisticPlotFile + " -t bar -y Count --title " + LinkersType.get(i) + " -i " + OutFile + " -o " + PngFile;
                        Tools.ExecuteCommandStr(ComLine, null, null);
                        Stat.ReadsLengthDisBase64[i] = Stat.GetBase64(PngFile);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ST.start();
        SThread.add(ST);
        //==============================================================================================================
        for (int i = 0; i < UseLinker.size(); i++) {
            UseLinkerFasqFileR1[i] = new CustomFile(LinkerFastqFileR1[LinkersType.indexOf(UseLinker.get(i))]);
            UseLinkerFasqFileR2[i] = new CustomFile(LinkerFastqFileR2[LinkersType.indexOf(UseLinker.get(i))]);
            Stat.UseLinker[i].FastqFileR1 = UseLinkerFasqFileR1[i];
            Stat.UseLinker[i].FastqFileR2 = UseLinkerFasqFileR2[i];
        }
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
            R1SortBedFile[i] = new CustomFile(new SeProcess(UseLinkerFasqFileR1[i], IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, UseLinkerFasqFileR1[i].getName().replace(".fastq", ""), ReadsType).getSortBedFile());
            R2SortBedFile[i] = new CustomFile(new SeProcess(UseLinkerFasqFileR2[i], IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, UseLinkerFasqFileR2[i].getName().replace(".fastq", ""), ReadsType).getSortBedFile());
            SeBedpeFile[i] = new CustomFile(SeProcessDir + "/" + Opts.Prefix + "." + UseLinker.get(i) + ".bedpe");
            if (StepCheck("Bed2BedPe")) {
                new MergeBedToBedpe(R1SortBedFile[i], R2SortBedFile[i], SeBedpeFile[i], 4, "");//合并左右端bed文件，输出bedpe文件
            }
            //==========================================================================================================
            Stat.UseLinker[i].UniqMapFileR1 = R1SortBedFile[i];
            Stat.UseLinker[i].UniqMapFileR2 = R2SortBedFile[i];
            Stat.UseLinker[i].RawBedpeFile = SeBedpeFile[i];
//            Stat.UseBed1.add(R1SortBedFile[i]);
//            Stat.UseBed2.add(R2SortBedFile[i]);
            int finalI = i;
            ST = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Stat.UseLinker[finalI].FastqNumR1 = Stat.UseLinker[finalI].FastqFileR1.CalculatorLineNumber() / 4;
                        Stat.UseLinker[finalI].FastqNumR2 = Stat.UseLinker[finalI].FastqFileR2.CalculatorLineNumber() / 4;
                        Stat.UseLinker[finalI].UniqMapNumR1 = Stat.UseLinker[finalI].UniqMapFileR1.CalculatorLineNumber();
                        Stat.UseLinker[finalI].UniqMapNumR2 = Stat.UseLinker[finalI].UniqMapFileR2.CalculatorLineNumber();
                        Stat.UseLinker[finalI].RawBedpeNum = Stat.UseLinker[finalI].RawBedpeFile.CalculatorLineNumber();
//                        Stat.UniqMapR1Num.add(R1SortBedFile[finalI].CalculatorLineNumber());
//                        Stat.UniqMapR2Num.add(R2SortBedFile[finalI].CalculatorLineNumber());
//                        Stat.BedpeFile.add(SeBedpeFile[finalI]);
//                        Stat.BedpeNum.add(SeBedpeFile[finalI].CalculatorLineNumber());
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
            BedpeProcess Temp = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker.get(i)), Opts.Prefix + "." + UseLinker.get(i), Chromosomes, ChrEnzyFile, SeBedpeFile[i]);
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
                        BedpeProcess Temp = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker.get(finalI)), Opts.Prefix + "." + UseLinker.get(finalI), Chromosomes, ChrEnzyFile, SeBedpeFile[finalI]);
                        Stat.UseLinker[finalI].BedpeProcessOutDir = new File(BedpeProcessDir + "/" + UseLinker.get(finalI));
                        Stat.UseLinker[finalI].SelfLigationFile = Temp.getSelfLigationFile();
                        Stat.UseLinker[finalI].RelLigationFile = Temp.getReLigationFile();
                        Stat.UseLinker[finalI].SameValidFile = Temp.getValidFile();
                        Stat.UseLinker[finalI].RawSameBedpeFile = Temp.getSameFile();
                        Stat.UseLinker[finalI].RawDiffBedpeFile = Temp.getDiffFile();
                        Stat.UseLinker[finalI].SameCleanFile = Temp.getSameNoDumpFile();
                        Stat.UseLinker[finalI].DiffCleanFile = Temp.getDiffNoDumpFile();
                        Stat.UseLinker[finalI].MergeCleanFile = Temp.getFinalFile();
                        Stat.UseLinker[finalI].SelfLigationNum = Temp.getSelfLigationFile().CalculatorLineNumber();
                        Stat.UseLinker[finalI].RelLigationNum = Temp.getReLigationFile().CalculatorLineNumber();
                        Stat.UseLinker[finalI].SameValidNum = Temp.getValidFile().CalculatorLineNumber();
                        Stat.UseLinker[finalI].RawSameBedpeNum = Stat.UseLinker[finalI].SelfLigationNum + Stat.UseLinker[finalI].RelLigationNum + Stat.UseLinker[finalI].SameValidNum;
                        Stat.UseLinker[finalI].RawDiffBedpeNum = Temp.getDiffFile().CalculatorLineNumber();
                        Stat.UseLinker[finalI].SameCleanNum = Temp.getSameNoDumpFile().CalculatorLineNumber();
                        Stat.UseLinker[finalI].DiffCleanNum = Temp.getDiffNoDumpFile().CalculatorLineNumber();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            STS[i].start();
            SThread.add(STS[i]);
        }
        //=================================================BedpeFile To Inter===========================================
        CustomFile FinalBedpeFile = new CustomFile(BedpeProcessDir + "/" + Opts.Prefix + ".clean.bedpe");
        CustomFile SameBedpeFile = new CustomFile(BedpeProcessDir + "/" + Opts.Prefix + ".same.clean.bedpe");
        CustomFile DiffBedpeFile = new CustomFile(BedpeProcessDir + "/" + Opts.Prefix + ".diff.clean.bedpe");
        CustomFile[] ChrBedpeFile = new CustomFile[Chromosomes.length];
        for (int i = 0; i < Chromosomes.length; i++) {
            ChrBedpeFile[i] = new CustomFile(BedpeProcessDir + "/" + Opts.Prefix + "." + Chromosomes[i].Name + ".same.clean.bedpe");
        }
        CustomFile InterBedpeFile = new CustomFile(BedpeProcessDir + "/" + Opts.Prefix + ".inter.clean.bedpe");
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

        if (StepCheck("MakeMatrix")) {
            for (Chromosome s : Chromosomes) {
                if (s.Size == 0) {
                    findenzy.start();
                    findenzy.join();
                }
            }
            Thread[] mmt = new Thread[Resolution.length];
            for (int i = 0; i < Resolution.length; i++) {
                int finalI = i;
                mmt[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MakeMatrix matrix = new MakeMatrix(new File(MakeMatrixDir + "/" + Resolution[finalI]), Opts.Prefix, InterBedpeFile, ChrBedpeFile, Chromosomes, Resolution[finalI], Threads);//生成交互矩阵类
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
                MakeMatrix matrix = new MakeMatrix(new File(MakeMatrixDir + "/" + DrawResolution[i]), Opts.Prefix, InterBedpeFile, ChrBedpeFile, Chromosomes, DrawResolution[i], Threads);//生成交互矩阵类
                if (!new File(MakeMatrixDir + "/" + DrawResolution[i]).isDirectory()) {
                    matrix.Run();
                }
                File[] TwoDMatrixFile = matrix.getChrTwoDMatrixFile();
                for (int j = 0; j < Chromosomes.length; j++) {
                    new PlotMatrix(TwoDMatrixFile[j], new File(OutDir + "/" + Opts.Prefix + "." + Chromosomes[j].Name + "." + Tools.UnitTrans(DrawResolution[i], "B", "M") + "M.png"), DrawResolution[i]).Run(new String[]{Chromosomes[j].Name + ":0", Chromosomes[j].Name + ":0"});
                }
            }
        }
        //==============================================================================================================
        ST = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Stat.InterAction.FinalBedpeFile = FinalBedpeFile;
                    Stat.InterAction.FinalBedpeNum = FinalBedpeFile.CalculatorLineNumber();
                    Stat.InterAction.IntraActionNum = SameBedpeFile.CalculatorLineNumber();
                    Stat.InterAction.InterActionNum = Stat.InterAction.FinalBedpeNum - Stat.InterAction.IntraActionNum;
                    if (Stat.ComInfor.Restriction.replace("^", "").length() <= 4) {
                        Stat.InterAction.ShortRegionNum = Statistic.RangeCount(SameBedpeFile, 0, 5000, 4);
                    } else {
                        Stat.InterAction.ShortRegionNum += Statistic.RangeCount(SameBedpeFile, 0, 20000, 4);
                    }
                    Stat.InterAction.LongRegionNum = Stat.InterAction.IntraActionNum - Stat.InterAction.ShortRegionNum;
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
            for (Chromosome s : Chromosomes) {
                if (s.Size == 0) {
                    findenzy.start();
                    findenzy.join();
                }
            }
//            TransLocationDetection(Chromosomes, FinalBedpeFile, DetectResolution, Threads);
        }
        EndTime = new Date();
        Stat.RunTime.LinkerFilter = Tools.DateFormat((SeTime.getTime() - PreTime.getTime()) / 1000);
        Stat.RunTime.Mapping = Tools.DateFormat((BedpeTime.getTime() - SeTime.getTime()) / 1000);
        Stat.RunTime.LigationFilter = Tools.DateFormat((MatrixTime.getTime() - BedpeTime.getTime()) / 1000);
        Stat.RunTime.MakeMatrix = Tools.DateFormat((TransTime.getTime() - MatrixTime.getTime()) / 1000);
        Stat.RunTime.TransLocation = Tools.DateFormat((EndTime.getTime() - TransTime.getTime()) / 1000);
        Stat.RunTime.Total = Tools.DateFormat((EndTime.getTime() - PreTime.getTime()) / 1000);
        System.out.println("\n-------------------------------Time----------------------------------------");
        System.out.println("PreProcess:\t" + Stat.RunTime.LinkerFilter);
        System.out.println("SeProcess:\t" + Stat.RunTime.Mapping);
        System.out.println("BedpeProcess:\t" + Stat.RunTime.LigationFilter);
        System.out.println("MakeMatrix:\t" + Stat.RunTime.MakeMatrix);
        System.out.println("Translocation:\t" + Stat.RunTime.TransLocation);
        System.out.println("Total:\t" + Stat.RunTime.Total);
        //===================================Report=====================================================================

        for (Thread t : SThread) {
            t.join();
        }
        Stat.Show();
        new File(Opts.OutPath + "/Report").mkdir();
        Stat.ReportHtml(Opts.OutPath + "/Report/Test.index.html");
    }

    /**
     * Create reference genome index
     *
     * @param genomefile genome file
     * @return process thread
     */
    private Thread CreateIndex(File genomefile) {
        File IndexDir = new File(Opts.OutPath + "/" + Opts.IndexDir);
        if (!IndexDir.isDirectory() && !IndexDir.mkdir()) {
            System.out.println("Create " + IndexDir + " false");
            System.exit(1);
        }
        IndexPrefix = new File(IndexDir + "/" + genomefile.getName());
        Stat.ComInfor.IndexPrefix = IndexPrefix;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String ComLine = "bwa index -p " + IndexPrefix + " " + genomefile;
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
                    ArrayList<String> list = new ArrayList<>();
                    if (!EnzyPath.isDirectory() && !EnzyPath.mkdir()) {
                        System.err.println(new Date() + "\tCreate " + EnzyPath + " false !");
                    }
                    FindRestrictionSite fr = new FindRestrictionSite(Opts.GenomeFile, EnzyPath, Restriction, EnzyFilePrefix);
                    ArrayList<Chromosome> TempChrs = fr.Run();
                    File[] TempChrEnzyFile = fr.getChrFragmentFile();
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
                    }
                    System.out.println(new Date() + "\tEnd find restriction fragment");
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
        File SamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, Prefix, ReadsType).getSamFile();
        File UniqSamFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, Prefix, ReadsType).getUniqSamFile();
        CustomFile SortBedFile = new SeProcess(FastqFile, IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, Prefix, ReadsType).getSortBedFile();
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
                            SeProcess ssp = new SeProcess(InFile, IndexPrefix, AlignMisMatch, MinUniqueScore, SeProcessDir, Prefix + ".split" + finalI, ReadsType);//单端处理类
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
                    BedpeProcess bedpe = new BedpeProcess(new File(BedpeProcessDir + "/" + UseLinker), Opts.Prefix + "." + UseLinker, Chromosomes, ChrEnzyFile, SeBedpeFile);//bedpe文件处理类
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
        Config.setProperty(Optional.OutPath.toString(), Default.OutPath);
        Config.setProperty(Optional.Prefix.toString(), Default.Prefix);
        Config.setProperty(Optional.Resolutions.toString(), String.valueOf(Default.Resolution));
        Config.setProperty(Optional.DrawResolution.toString(), String.valueOf(Default.Resolution));
        Config.setProperty(Optional.Thread.toString(), String.valueOf(Default.Thread));
        Config.setProperty(Optional.Step.toString(), Default.Step);
        Config.setProperty(Advance.MatchScore.toString(), String.valueOf(Default.MatchScore));
        Config.setProperty(Advance.MisMatchScore.toString(), String.valueOf(Default.MisMatchScore));
        Config.setProperty(Advance.InDelScore.toString(), String.valueOf(Default.InDelScore));
        Config.setProperty(Advance.MinReadsLength.toString(), String.valueOf(Default.MinReadsLen));
        Config.setProperty(Advance.MaxReadsLength.toString(), String.valueOf(Default.MaxReadsLen));
        Config.setProperty(Advance.AlignThread.toString(), String.valueOf(Default.AlignThread));
        Config.setProperty(Advance.AlignMisMatch.toString(), String.valueOf(Default.AlignMisMatchNum));
        Config.setProperty(Advance.Iteration.toString(), Default.Iteration);
        Config.setProperty(Advance.AlignType.toString(), String.valueOf(Default.AlignType));
        Config.load(new FileReader(AdvConfFile));
        Config.load(new FileReader(ConfFile));
    }


    private void Init() {
        for (Require opt : Require.values()) {
            if (Config.getProperty(opt.toString()) == null) {
                System.err.println("Error ! no " + opt);
                System.exit(1);
            } else if (Config.getProperty(opt.toString()).equals("")) {
                System.err.println("Error ! no " + opt);
                System.exit(1);
            }
        }
        //----------------------------------------必要参数赋值-----------------------------------------------------------
        String[] tempstrs;
        Opts.InputFile = new CustomFile(Config.getProperty(Require.InputFile.toString()));
        Opts.GenomeFile = new CustomFile(Config.getProperty(Require.GenomeFile.toString()));
        Restriction = Config.getProperty(Require.Restriction.toString());
        HalfLinker = Config.getProperty(Require.HalfLinker.toString()).split("\\s+");
        LinkerA = HalfLinker[0];
        LinkerLength = LinkerA.length();
        if (HalfLinker.length > 1) {
            LinkerB = HalfLinker[1];
            LinkerLength += LinkerB.length();
        } else {
            LinkerLength += LinkerA.length();
        }

        //-----------------------------------------可选参数赋值----------------------------------------------------------
        Opts.OutPath = new File(Config.getProperty(Optional.OutPath.toString()));
        Opts.Prefix = Config.getProperty(Optional.Prefix.toString());
        Resolution = StringArrays.toInteger(Config.getProperty(Optional.Resolutions.toString()).split("\\s+"));
        Threads = Integer.parseInt(Config.getProperty(Optional.Thread.toString()));
        DrawResolution = StringArrays.toInteger(Config.getProperty(Optional.DrawResolution.toString()).split("\\s+"));
        Step.addAll(Arrays.asList(Config.getProperty(Optional.Step.toString()).split("\\s+")));
        if (Config.getProperty(Optional.AdapterSeq.toString()) != null && !Config.getProperty(Optional.AdapterSeq.toString()).equals("")) {
            AdapterSeq = Config.getProperty(Optional.AdapterSeq.toString()).split("\\s+");
        }
        if (Config.getProperty(Optional.Index.toString()) != null && !Config.getProperty(Optional.Index.toString()).equals("")) {
            IndexPrefix = new File(Config.getProperty(Optional.Index.toString()));
        }
        if (Config.getProperty(Optional.Chromosomes.toString()) != null && !Config.getProperty(Optional.Chromosomes.toString()).equals("")) {
            tempstrs = Config.getProperty(Optional.Chromosomes.toString()).split("\\s+");
            Chromosomes = new Chromosome[tempstrs.length];
            for (int i = 0; i < Chromosomes.length; i++) {
                Chromosomes[i] = new Chromosome(tempstrs[i]);
            }
            ChrEnzyFile = new File[Chromosomes.length];
        }

        //-------------------------------------------高级参数赋值--------------------------------------------------------
        MatchScore = Integer.parseInt(Config.getProperty(Advance.MatchScore.toString(), String.valueOf(Default.MatchScore)));
        MisMatchScore = Integer.parseInt(Config.getProperty(Advance.MisMatchScore.toString(), String.valueOf(Default.MisMatchScore)));
        InDelScore = Integer.parseInt(Config.getProperty(Advance.InDelScore.toString(), String.valueOf(Default.InDelScore)));
        MinReadsLength = Integer.parseInt(Config.getProperty(Advance.MinReadsLength.toString(), String.valueOf(Default.MinReadsLen)));
        MaxReadsLength = Integer.parseInt(Config.getProperty(Advance.MaxReadsLength.toString(), String.valueOf(Default.MaxReadsLen)));
        AlignThread = Integer.parseInt(Config.getProperty(Advance.AlignThread.toString(), String.valueOf(Default.AlignThread)));
        AlignMisMatch = Integer.parseInt(Config.getProperty(Advance.AlignMisMatch.toString(), String.valueOf(Default.AlignMisMatchNum)));
        Iteration = Boolean.valueOf(Config.getProperty(Advance.Iteration.toString(), Default.Iteration));
        ReadsType = Config.getProperty(Advance.AlignType.toString()).equals("Short") ? Opts.ShortReads : Config.getProperty(Advance.AlignType.toString()).equals("Long") ? Opts.LongReads : Opts.ErrorFormat;
        //设置唯一比对分数
        if (ReadsType == Opts.ShortReads) {
            MinUniqueScore = 20;
        } else if (ReadsType == Opts.LongReads) {
            MinUniqueScore = 30;
        }
        Config.setProperty(Advance.MinUniqueScore.toString(), String.valueOf(MinUniqueScore));
        if (Config.getProperty(Advance.MinLinkerLen.toString()) == null) {
            Config.setProperty(Advance.MinLinkerLen.toString(), String.valueOf((int) (LinkerLength * 0.9)));
        }
        MinLinkerLength = Integer.parseInt(Config.getProperty(Advance.MinLinkerLen.toString()));
        //================================================
        if (!Opts.OutPath.isDirectory()) {
            System.err.println("Error, " + Opts.OutPath + " is not a directory");
            System.exit(1);
        }
        if (!Opts.GenomeFile.isFile()) {
            System.err.println("Error, " + Opts.GenomeFile + " is not a file");
            System.exit(1);
        }
        if (!Opts.InputFile.isFile()) {
            System.err.println("Error, " + Opts.InputFile + " is not a file");
            System.exit(1);
        }
        //=======================================================================;
        PreProcessDir = new File(Opts.OutPath + "/" + Opts.PreDir);
        SeProcessDir = new File(Opts.OutPath + "/" + Opts.SeDir);
        BedpeProcessDir = new File(Opts.OutPath + "/" + Opts.BedpeDir);
        MakeMatrixDir = new File(Opts.OutPath + "/" + Opts.MatrixDir);
        TransLocationDir = new File(Opts.OutPath + "/" + Opts.TransDir);
        ReportDir = new File(Opts.OutPath + "/" + Opts.ReportDir);
        EnzyPath = new File(Opts.OutPath + "/" + Opts.EnzyFragDir);
        File[] CheckDir = new File[]{PreProcessDir, SeProcessDir, BedpeProcessDir, MakeMatrixDir, TransLocationDir, EnzyPath};
        for (File s : CheckDir) {
            if (!s.isDirectory() && !s.mkdir()) {
                System.err.println("Can't create " + s);
                System.exit(1);
            }
        }
        tempstrs = new String[]{"A", "B"};
        //构建Linker序列
        LinkerSeq = new String[HalfLinker.length * HalfLinker.length];
        for (int i = 0; i < HalfLinker.length; i++) {
            for (int j = 0; j < HalfLinker.length; j++) {
                LinkerSeq[i * HalfLinker.length + j] = HalfLinker[i] + Tools.ReverseComple(HalfLinker[j]);
                LinkersType.add(tempstrs[i] + tempstrs[j]);
                if (i == j) {
                    UseLinker.add(tempstrs[i] + tempstrs[j]);
                }
            }
        }
        MinLinkerFilterQuality = MinLinkerLength * MatchScore + (LinkerLength - MinLinkerLength) * MisMatchScore;//设置linkerfilter最小分数
        EnzyFilePrefix = Opts.Prefix + "." + Restriction.replace("^", "");
        LinkerFile = new File(PreProcessDir + "/" + Opts.Prefix + ".linker");
        AdapterFile = new File(PreProcessDir + "/" + Opts.Prefix + ".adapter");
        Stat = new Report(ReportDir);
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
        for (Require opt : Require.values()) {
            System.out.println(opt + ":\t" + Config.getProperty(opt.toString()));
        }
        System.out.println("======================================================================================");
        for (Optional opt : Optional.values()) {
            System.out.println(opt + ":\t" + Config.getProperty(opt.toString()));
        }
        System.out.println("======================================================================================");
        for (Advance opt : Advance.values()) {
            System.out.println(opt + ":\t" + Config.getProperty(opt.toString()));
        }
    }

}

enum Require {
    InputFile("InputFile"), Restriction("Restriction"), HalfLinker("HalfLinker"), GenomeFile("GenomeFile");
    private String Str;

    Require(String s) {
        this.Str = s;
    }

    @Override
    public String toString() {
        return Str;
    }
}

enum Optional {
    OutPath("OutPath"), Prefix("Prefix"), Index("Index"), Chromosomes("Chromosomes"), AdapterSeq("AdapterSeq"), Resolutions("Resolutions"), Thread("Thread"), DrawResolution("DrawResolution"), Step("Step");
    private String Str;

    Optional(String s) {
        this.Str = s;
    }

    @Override
    public String toString() {
        return Str;
    }
}

enum Advance {
    MatchScore("MatchScore"), MisMatchScore("MisMatchScore"), InDelScore("InDelScore"), MinLinkerLen("MinLinkerLen"), MinReadsLength("MinReadsLength"), MaxReadsLength("MaxReadsLength"), AlignThread("AlignThread"), AlignType("AlignType"), AlignMisMatch("AlignMisMatch"), MinUniqueScore("MinUniqueScore"), Iteration("Iteration");
    private String Str;

    Advance(String s) {
        this.Str = s;
    }

    @Override
    public String toString() {
        return Str;
    }
}
