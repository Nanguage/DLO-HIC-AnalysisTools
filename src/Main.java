import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

public class Main {
    private final String OptFastqFile = "FastqFile";//fastq文件
    private final String OptGenomeFile = "GenomeFile";//基因组文件
    private final String OptPhred = "Phred";//fastq格式
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
    private final String OptAlignMisMatch = "AlignMisMatch";//bwa等比对最小错配数
    private final String OptAlignThread = "AlignThread";//bwa等比对线程数
    private final String OptAlignMinQuality = "AlignMinQuality";//bwa等比对最小质量
    private final String OptMinReadsLength = "MinReadsLength";//最小reads长度
    private final String OptMaxReadsLength = "MaxReadsLength";//最大reads长度
    private final String OptResolution = "Resolution";//分辨率
    private final String OptThreads = "Thread";//线程数
    private final String OptStep = "Step";
    //===================================================================
    private String FastqFile;
    private String GenomeFile;
    private String Prefix;
    private ArrayList<String> Chromosome = new ArrayList<>();
    private String Restriction;
    private String LinkerFile;
    private String AdapterFile;
    private ArrayList<String> LinkersType = new ArrayList<>();
    private ArrayList<String> UseLinker = new ArrayList<>();
    private int MatchScore;
    private int MisMatchScore;
    private int IndelScore;
    private String IndexFile;
    private int AlignMisMatch;
    private int AlignThread;
    private int AlignMinQuality;
    private int MinReadsLength;
    private int MaxReadsLength;
    private int Resolution;
    private int Thread;
    private ArrayList<String> Step = new ArrayList<>();

    //===================================================================
    private String[] RequiredParameter = new String[]{OptFastqFile, OptGenomeFile, OptLinkerFile, OptChromosome, OptRestriction, OptLinkersType, OptIndexFile, OptAlignMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptAdapterFile, OptMaxMisMatchLength, OptMinReadsLength, OptMaxReadsLength, OptPhred, OptUseLinker, OptMatchScore, OptMisMatchScore, OptIndelScore, OptAlignMisMatch, OptAlignThread, OptResolution, OptStep, OptThreads};
    private Hashtable<String, String> ParameterList;
    private Hashtable<String, Integer> ChrSize;//染色体大小
    private int MinLinkerFilterQuality;
    private String AddQuality;
    private String EnzyPath;//酶切位点文件目录
    private String EnzyFilePrefix;//酶切位点文件前缀
    private String PreProcessDir;//预处理输出目录
    private String SeProcessDir;//单端处理输出目录
    private String BedpeProcessDir;//bedpe处理输出目录
    private String MakeMatrixDir;//建立矩阵输出目录
    private Routine step = new Routine();

    Main(String ConfigFile) throws IOException {
        ParameterList = new Hashtable<>();
        ChrSize = new Hashtable<>();
        OptionListInit();
        GetOption(ConfigFile);//获取参数
        Init();
    }

    Main() {
        ParameterList = new Hashtable<>();
        ChrSize = new Hashtable<>();
        OptionListInit();
    }

    public static void main(String args[]) throws IOException {
        //==============================================测试区==========================================================

        //==============================================================================================================
        if (args.length < 1) {
            System.out.println("Usage:    java -jar DLO-HIC-AnalysisTools.jar <config.txt>");
            System.exit(0);
        }
        Main main = new Main(args[0]);
        main.ShowParameter();
        main.Run();
    }

    public void Run() throws IOException {

        //===========================================初始化输出文件======================================================
        String[] FinalLinkerBedpe = new String[UseLinker.size()];
        String[] LinkerFasqFileR1 = new String[LinkersType.size()];
        String[] LinkerFasqFileR2 = new String[LinkersType.size()];
        String[] UseLinkerFasqFileR1 = new String[UseLinker.size()];
        String[] UseLinkerFasqFileR2 = new String[UseLinker.size()];
        for (int i = 0; i < LinkersType.size(); i++) {
            LinkerFasqFileR1[i] = SeProcessDir + "/" + Prefix + "." + LinkersType.get(i) + ".R1.fastq";
            LinkerFasqFileR2[i] = SeProcessDir + "/" + Prefix + "." + LinkersType.get(i) + ".R2.fastq";
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            UseLinkerFasqFileR1[i] = SeProcessDir + "/" + Prefix + "." + UseLinker.get(i) + ".R1.fastq";
            UseLinkerFasqFileR2[i] = SeProcessDir + "/" + Prefix + "." + UseLinker.get(i) + ".R2.fastq";
        }
        String FinalBedpeFile = BedpeProcessDir + "/" + Prefix + ".bedpe";
        String InterBedpeFile = BedpeProcessDir + "/" + Prefix + ".inter.bedpe";
        step.Threads = Thread;//设置线程数
        //=========================================linker filter==linker 过滤===========================================
        PreProcess preprocess;
        preprocess = new PreProcess(PreProcessDir, Prefix, FastqFile, LinkerFile, AdapterFile, MatchScore, MisMatchScore, IndelScore, Thread * 4);
        if (StepCheck("LinkerFilter")) {
            preprocess.Run();
        }
        String PastFile = preprocess.getPastFile();//获取past文件位置
        preprocess = null;
        //=========================================Linker Cluster=======================================================
        Thread cslR1 = ClusterLinker(PastFile, LinkerFasqFileR1, "R1");
        Thread cslR2 = ClusterLinker(PastFile, LinkerFasqFileR2, "R2");
        try {
            if (StepCheck("ClusterLinker")) {
                cslR1.start();
                cslR2.start();
            }
            cslR1.join();
            cslR2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //=======================================Se Process===单端处理==================================================
        Thread[] sepR1 = new Thread[UseLinker.size()];
        Thread[] sepR2 = new Thread[UseLinker.size()];
        for (int i = 0; i < UseLinker.size(); i++) {
            sepR1[i] = SeProcess(UseLinkerFasqFileR1[i], UseLinkerFasqFileR1[i].replaceAll(".*/", "").replace(".fastq", ""));
            sepR2[i] = SeProcess(UseLinkerFasqFileR2[i], UseLinkerFasqFileR2[i].replaceAll(".*/", "").replace(".fastq", ""));
            if (StepCheck("SeProcess")) {
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
        String[] R1SortBedFile = new String[UseLinker.size()];
        String[] R2SortBedFile = new String[UseLinker.size()];
        String[] SeBedpeFile = new String[UseLinker.size()];
        for (int i = 0; i < UseLinker.size(); i++) {
            R1SortBedFile[i] = new SeProcess(UseLinkerFasqFileR1[i], IndexFile, AlignMisMatch, AlignMinQuality, SeProcessDir, UseLinkerFasqFileR1[i].replaceAll(".*/", "").replace(".fastq", "")).getSortBedFile();
            R2SortBedFile[i] = new SeProcess(UseLinkerFasqFileR2[i], IndexFile, AlignMisMatch, AlignMinQuality, SeProcessDir, UseLinkerFasqFileR2[i].replaceAll(".*/", "").replace(".fastq", "")).getSortBedFile();
            SeBedpeFile[i] = SeProcessDir + "/" + Prefix + "." + UseLinker.get(i) + ".bedpe";
            if (StepCheck("Bed2BedPe")) {
                step.MergeBedToBedpe(R1SortBedFile[i], R2SortBedFile[i], SeBedpeFile[i], 4, "");//合并左右端bed文件，输出bedpe文件
            }
        }
        //==========================================获取酶切片段和染色体大小=============================================
        Thread findenzy = FindRestrictionFragment();
        if (StepCheck("BedPeProcess")) {
            findenzy.start();
        }
        try {
            findenzy.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //==============================================Bedpe Process====bedpe 处理=====================================
        Thread[] LinkerProcess = new Thread[UseLinker.size()];//不同linker类型并行
        for (int i = 0; i < UseLinker.size(); i++) {
            LinkerProcess[i] = BedpeProcess(UseLinker.get(i), SeBedpeFile[i]);
            if (StepCheck("BedPeProcess")) {
                LinkerProcess[i].start();
            }
            FinalLinkerBedpe[i] = new BedpeProcess(BedpeProcessDir, Prefix + "." + UseLinker.get(i), Chromosome.toArray(new String[Chromosome.size()]), EnzyFilePrefix, SeBedpeFile[i]).getFinalBedpeFile();
        }
        for (int i = 0; i < UseLinker.size(); i++) {
            try {
                LinkerProcess[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //=================================================Bedpe To Inter===============================================
        if (StepCheck("BedPeProcess")) {
            CommonMethod.Merge(FinalLinkerBedpe, FinalBedpeFile);//合并不同linker类型的bedpe文件
        }
        if (StepCheck("BedPe2Inter")) {
            step.BedpeToInter(FinalBedpeFile, InterBedpeFile);//将交互区间转换成交互点
        }
        //=================================================Make Matrix==================================================
        int[] chrSize = new int[Chromosome.size()];
        int i = 0;
        for (String chr : Chromosome) {
            chrSize[i++] = ChrSize.get(chr);
        }
        MakeMatrix matrix = new MakeMatrix(MakeMatrixDir, Prefix, InterBedpeFile, Chromosome.toArray(new String[Chromosome.size()]), chrSize, Resolution);//生成交互矩阵类
        if (StepCheck("MakeMatrix")) {
            matrix.Run();//运行
        }
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
                    for (String chr : Chromosome) {
                        ChrSize.put(chr, 0);
                    }
                    Routine step = new Routine();
                    ArrayList<String> list = new ArrayList<>();
                    if (!new File(EnzyPath).isDirectory()) {
                        if (!new File(EnzyPath).mkdir()) {
                            System.out.println(new Date() + "\tCreat " + EnzyPath + " false !");
                        }
                    }
                    Hashtable<String, Integer> temphash = step.FindRestrictionSite(GenomeFile, Restriction, EnzyFilePrefix);
                    for (String chr : temphash.keySet()) {
                        if (ChrSize.containsKey(chr)) {
                            ChrSize.put(chr, temphash.get(chr));
                        }
                        list.add(chr + "\t" + temphash.get(chr));
                    }
                    CommonMethod.PrintList(list, EnzyPath + "/" + Prefix + ".ChrSize");//打印染色体大小信息
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Thread ClusterLinker(String PastFile, String[] LinkerFasqFile, String Type) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    step.ClusterLinker(PastFile, LinkerFasqFile, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, Restriction, AddQuality, Type);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Thread SeProcess(String FastqFile, String Prefix) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SeProcess ssp = new SeProcess(FastqFile, IndexFile, AlignMisMatch, AlignMinQuality, SeProcessDir, Prefix);//单端处理类
                    ssp.Thread = Thread;//设置线程数
                    ssp.AlignThreads = AlignThread;
                    ssp.Run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Thread BedpeProcess(String UseLinker, String SeBedpeFile) {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BedpeProcess bedpe = new BedpeProcess(BedpeProcessDir, Prefix + "." + UseLinker, Chromosome.toArray(new String[Chromosome.size()]), EnzyFilePrefix, SeBedpeFile);//bedpe文件处理类
                    bedpe.Thread = Thread;//设置线程数
                    bedpe.Run();//运行
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void GetOption(String Infile) throws IOException {
        String line;
        String[] str;
        BufferedReader infile = new BufferedReader(new FileReader(Infile));
        while ((line = infile.readLine()) != null) {
            line = line.trim();
            if (line.equals("") || line.matches("^/.*|^#.*")) {
                continue;
            }
            str = line.split("\\s*=\\s*", 2);
            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
                ParameterList.put(str[0], str[1]);
            }
        }
        infile.close();
    }

    public void OptionListInit() {
        for (String opt : RequiredParameter) {
            ParameterList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ParameterList.put(opt, "");
        }
        ParameterList.put(OptOutPath, "./");
        ParameterList.put(OptMaxMisMatchLength, "3");
        ParameterList.put(OptMinReadsLength, "16");
        ParameterList.put(OptMaxReadsLength, "20");
        ParameterList.put(OptPhred, "33");
        ParameterList.put(OptMatchScore, "1");
        ParameterList.put(OptMisMatchScore, "-2");
        ParameterList.put(OptIndelScore, "-2");
        ParameterList.put(OptAlignMisMatch, "0");
        ParameterList.put(OptAlignThread, "8");
        ParameterList.put(OptResolution, "1000000");
        ParameterList.put(OptThreads, "4");
        ParameterList.put(OptStep, "-");
    }

    private void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! no " + opt);
                System.exit(0);
            }
        }
        //================================================
        FastqFile = ParameterList.get(OptFastqFile);
        GenomeFile = ParameterList.get(OptGenomeFile);
        String Phred = ParameterList.get(OptPhred);
        Prefix = ParameterList.get(OptPrefix);
        String OutPath = ParameterList.get(OptOutPath);
        Chromosome.addAll(Arrays.asList(ParameterList.get(OptChromosome).split("\\s+")));
        Restriction = ParameterList.get(OptRestriction);
        LinkerFile = ParameterList.get(OptLinkerFile);
        AdapterFile = ParameterList.get(OptAdapterFile);
        LinkersType.addAll(Arrays.asList(ParameterList.get(OptLinkersType).split("\\s+")));
        UseLinker.addAll(Arrays.asList(ParameterList.get(OptUseLinker).split("\\s+")));
        MatchScore = Integer.parseInt(ParameterList.get(OptMatchScore));
        MisMatchScore = Integer.parseInt(ParameterList.get(OptMisMatchScore));
        IndelScore = Integer.parseInt(ParameterList.get(OptIndelScore));
        int MaxMisMatchLength = Integer.parseInt(ParameterList.get(OptMaxMisMatchLength));
        IndexFile = ParameterList.get(OptIndexFile);
        AlignMisMatch = Integer.parseInt(ParameterList.get(OptAlignMisMatch));
        AlignThread = Integer.parseInt(ParameterList.get(OptAlignThread));
        AlignMinQuality = Integer.parseInt(ParameterList.get(OptAlignMinQuality));
        MinReadsLength = Integer.parseInt(ParameterList.get(OptMinReadsLength));
        MaxReadsLength = Integer.parseInt(ParameterList.get(OptMaxReadsLength));
        Resolution = Integer.parseInt(ParameterList.get(OptResolution));
        Thread = Integer.parseInt(ParameterList.get(OptThreads));
        Step.addAll(Arrays.asList(ParameterList.get(OptStep).split("\\s+")));
        //================================================
        if (Prefix.equals("")) {
            try {
                ParameterList.put(OptPrefix, FastqFile.substring(0, FastqFile.lastIndexOf(".")));
            } catch (IndexOutOfBoundsException e) {
                ParameterList.put(OptPrefix, FastqFile);
            }
        }
        if (UseLinker.size() == 0) {
            UseLinker = LinkersType;
        }
        if (!new File(OutPath).isDirectory()) {
            System.err.println("Wrong OutPath " + OutPath + " is not a directory");
            System.exit(0);
        }
        if (!new File(GenomeFile).isFile()) {
            System.err.println("Wrong " + OptGenomeFile + " " + GenomeFile + " is not a file");
            System.exit(0);
        }
        if (!new File(FastqFile).isFile()) {
            System.err.println("Wrong " + OptFastqFile + " " + FastqFile + " is not a file");
            System.exit(0);
        }
        if (!new File(LinkerFile).isFile()) {
            System.err.println("Wrong " + OptLinkerFile + " " + LinkerFile + " is not a file");
            System.exit(0);
        }
        //=======================================================================
        BufferedReader infile = new BufferedReader(new FileReader(LinkerFile));
        int linkerLength = infile.readLine().length();
        infile.close();
        if (Phred.equals("33")) {
            AddQuality = "I";
        } else {
            AddQuality = "h";
        }
        MinLinkerFilterQuality = (linkerLength - MaxMisMatchLength) * MatchScore + MaxMisMatchLength * MisMatchScore;//设置linkerfilter最小分数
        PreProcessDir = OutPath + "/PreProcess";
        SeProcessDir = OutPath + "/SeProcess";
        BedpeProcessDir = OutPath + "/BedpeProcess";
        MakeMatrixDir = OutPath + "/MakeMatrix";
        EnzyPath = OutPath + "/EnzySiteFile";
        if (!new File(PreProcessDir).isDirectory() && !new File(PreProcessDir).mkdirs()) {
            System.err.println("Can't creat " + PreProcessDir);
        }
        if (!new File(SeProcessDir).isDirectory() && !new File(SeProcessDir).mkdirs()) {
            System.err.println("Can't creat " + SeProcessDir);
        }
        if (!new File(BedpeProcessDir).isDirectory() && !new File(BedpeProcessDir).mkdirs()) {
            System.err.println("Can't creat " + BedpeProcessDir);
        }
        if (!new File(MakeMatrixDir).isDirectory() && !new File(MakeMatrixDir).mkdirs()) {
            System.err.println("Can't creat " + MakeMatrixDir);
        }
        if (!new File(EnzyPath).isDirectory() && !new File(EnzyPath).mkdirs()) {
            System.err.println("Can't creat " + EnzyPath);
        }
        EnzyFilePrefix = EnzyPath + "/" + Prefix + "." + Restriction.replace("^", "");
        step.Threads = Thread;
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
            } else if (Step.get(0).equals("-") && Step.size() == 1) {
                return true;
            }
        }
        return false;
    }

    private void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
        System.out.println("======================================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
    }

    public Hashtable<String, String> getParameterList() {
        return ParameterList;
    }

    public String[] getRequiredParameter() {
        return RequiredParameter;
    }

    public String[] getOptionalParameter() {
        return OptionalParameter;
    }
}
