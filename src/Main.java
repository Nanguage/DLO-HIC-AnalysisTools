import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class Main {
    private final String OptFastqFile = "FastqFile";//fastq文件
    private final String OptGenomeFile = "GenomeFile";//基因组文件
    private final String OptPhred = "Phred";//fastq格式
    private final String OptOutPrefix = "OutPrefix";//输出前缀
    private final String OptOutPath = "OutPath";//输出路径
    private final String OptChromosome = "Chromosome";//染色体名
    private final String OptRestriction = "Restriction";//酶切位点序列
    //    private String[] MatchRestriction = new String[2];//匹配的序列
//    private String[] AddSeq = new String[2];//延长的序列
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
    private final String OptThreads = "Threads";//线程数
    private String[] RequiredParameter = new String[]{OptFastqFile, OptGenomeFile, OptLinkerFile, OptChromosome, OptRestriction, OptLinkersType, OptIndexFile, OptAlignMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptOutPrefix, OptAdapterFile, OptMaxMisMatchLength, OptMinReadsLength, OptMaxReadsLength, OptPhred, OptUseLinker, OptMatchScore, OptMisMatchScore, OptIndelScore, OptAlignMisMatch, OptAlignThread, OptResolution, OptThreads};
    //    private String ConfigureFile;//配置文件
    private Hashtable<String, String> ParameterList;
    private Hashtable<String, Integer> ChrSize;//染色体大小
    public int MinLinkerFilterQuality;
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
        String[] FinalLinkerBedpe = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        String[] LinkerFasqFileR1 = new String[ParameterList.get(OptLinkersType).split("\\s+").length];
        String[] LinkerFasqFileR2 = new String[ParameterList.get(OptLinkersType).split("\\s+").length];
        String[] UseLinkerFasqFileR1 = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        String[] UseLinkerFasqFileR2 = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptLinkersType).split("\\s+").length; i++) {
            LinkerFasqFileR1[i] = SeProcessDir + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptLinkersType).split("\\s+")[i] + ".R1.fastq";
            LinkerFasqFileR2[i] = SeProcessDir + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptLinkersType).split("\\s+")[i] + ".R2.fastq";
        }
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            UseLinkerFasqFileR1[i] = SeProcessDir + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptUseLinker).split("\\s+")[i] + ".R1.fastq";
            UseLinkerFasqFileR2[i] = SeProcessDir + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptUseLinker).split("\\s+")[i] + ".R2.fastq";
        }
        String FinalBedpeFile = BedpeProcessDir + "/" + ParameterList.get(OptOutPrefix) + ".bedpe";
        String InterBedpeFile = BedpeProcessDir + "/" + ParameterList.get(OptOutPrefix) + ".inter.bedpe";
        step.Threads = Integer.parseInt(ParameterList.get(OptThreads));//设置线程数
        //=========================================linker filter==linker 过滤===========================================
        PreProcess preprocess;
        preprocess = new PreProcess(PreProcessDir, ParameterList.get(OptOutPrefix), ParameterList.get(OptFastqFile), ParameterList.get(OptLinkerFile), ParameterList.get(OptAdapterFile), ParameterList.get(OptMatchScore), ParameterList.get(OptMisMatchScore), ParameterList.get(OptIndelScore), String.valueOf(Integer.parseInt(ParameterList.get(OptThreads)) * 4));
        preprocess.Run();
        String PastFile = preprocess.getPastFile();//获取past文件位置
        preprocess = null;
        //=========================================Linker Cluster=======================================================
        Thread cslR1 = ClusterLinker(PastFile, LinkerFasqFileR1, "R1");
        Thread cslR2 = ClusterLinker(PastFile, LinkerFasqFileR2, "R2");
        try {
            cslR1.start();
            cslR2.start();
            cslR1.join();
            cslR2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //=======================================Se Process===单端处理==================================================
        Thread[] sepR1 = new Thread[ParameterList.get(OptUseLinker).split("\\s+").length];
        Thread[] sepR2 = new Thread[ParameterList.get(OptUseLinker).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            sepR1[i] = SeProcess(UseLinkerFasqFileR1[i], UseLinkerFasqFileR1[i].replace(".R1.fastq", ""), "R1");
            sepR2[i] = SeProcess(UseLinkerFasqFileR2[i], UseLinkerFasqFileR1[i].replace(".R2.fastq", ""), "R2");
            sepR1[i].start();
            sepR2[i].start();
        }
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            try {
                sepR1[i].join();
                sepR2[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //==========================================获取酶切片段和染色体大小=============================================
        Thread findenzy = FindRestrictionFragment();
        findenzy.start();
        //=============================================获取排序好的bed文件===============================================
        String[] R1SortBedFile = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        String[] R2SortBedFile = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        String[] SeBedpeFile = new String[ParameterList.get(OptUseLinker).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            R1SortBedFile[i] = new SeProcess(UseLinkerFasqFileR1[i], ParameterList.get(OptIndexFile), ParameterList.get(OptAlignMisMatch), ParameterList.get(OptAlignMinQuality), ParameterList.get(OptOutPath), UseLinkerFasqFileR1[i].replace(".R1.fastq", ""), "R1").getSortBedFile();
            R2SortBedFile[i] = new SeProcess(UseLinkerFasqFileR2[i], ParameterList.get(OptIndexFile), ParameterList.get(OptAlignMisMatch), ParameterList.get(OptAlignMinQuality), ParameterList.get(OptOutPath), UseLinkerFasqFileR1[i].replace(".R2.fastq", ""), "R1").getSortBedFile();
            SeBedpeFile[i] = SeProcessDir + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptUseLinker).split("\\s+")[i] + ".bedpe";
            step.MergeBedToBedpe(R1SortBedFile[i], R2SortBedFile[i], SeBedpeFile[i], 4, "");//合并左右端bed文件，输出bedpe文件
        }
        try {
            findenzy.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //==============================================Bedpe Process====bedpe 处理=====================================
        Thread[] LinkerProcess = new Thread[ParameterList.get(OptUseLinker).split("\\s+").length];//不同linker类型并行
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            LinkerProcess[i] = BedpeProcess(ParameterList.get(OptUseLinker).split("\\s+")[i], SeBedpeFile[i]);
            LinkerProcess[i].start();
            FinalLinkerBedpe[i] = new BedpeProcess(BedpeProcessDir, ParameterList.get(OptOutPrefix), ParameterList.get(OptUseLinker).split("\\s+")[i], ParameterList.get(OptChromosome).split("\\s+"), EnzyFilePrefix, SeBedpeFile[i]).getFinalBedpeFile();
        }
        for (int i = 0; i < ParameterList.get(OptUseLinker).split("\\s+").length; i++) {
            try {
                LinkerProcess[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //=================================================Bedpe To Inter===============================================
        CommonMethod.Merge(FinalLinkerBedpe, FinalBedpeFile);//合并不同linker类型的bedpe文件
        step.BedpeToInter(FinalBedpeFile, InterBedpeFile);//将交互区间转换成交互点
        //=================================================Make Matrix==================================================
        int[] chrSize = new int[ParameterList.get(OptChromosome).split("\\s+").length];
        int i = 0;
        for (String chr : ParameterList.get(OptChromosome).split("\\s+")) {
            chrSize[i++] = ChrSize.get(chr);
        }
        MakeMatrix matrix = new MakeMatrix(MakeMatrixDir, ParameterList.get(OptOutPrefix), InterBedpeFile, ParameterList.get(OptChromosome).split("\\s+"), chrSize, Integer.parseInt(ParameterList.get(OptResolution)));//生成交互矩阵类
        matrix.Run();//运行
    }

    /**
     * <p>创建酶切片段文件，获取染色体大小</p>
     *
     * @return 线程句柄
     */
    private Thread FindRestrictionFragment() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (String chr : ParameterList.get(OptChromosome).split("\\s+")) {
                        ChrSize.put(chr, 0);
                    }
                    Routine step = new Routine();
                    ArrayList<String> list = new ArrayList<>();
                    if (!new File(EnzyPath).isDirectory()) {
                        if (!new File(EnzyPath).mkdir()) {
                            System.out.println(new Date() + "\tCreat " + EnzyPath + " false !");
                        }
                    }
                    Hashtable<String, Integer> temphash = step.FindRestrictionSite(ParameterList.get(OptGenomeFile), ParameterList.get(OptRestriction), EnzyFilePrefix);
                    for (String chr : temphash.keySet()) {
                        if (ChrSize.containsKey(chr)) {
                            ChrSize.put(chr, temphash.get(chr));
                        }
                        list.add(chr + "\t" + temphash.get(chr));
                    }
                    CommonMethod.PrintList(list, EnzyPath + "/" + ParameterList.get(OptOutPrefix) + ".ChrSize");//打印染色体大小信息
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return t;
    }

    private Thread ClusterLinker(String PastFile, String[] LinkerFasqFile, String Type) throws IOException {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    step.ClusterLinker(PastFile, LinkerFasqFile, Integer.parseInt(ParameterList.get(OptMinReadsLength)), Integer.parseInt(ParameterList.get(OptMaxReadsLength)), MinLinkerFilterQuality, ParameterList.get(OptRestriction), AddQuality, Type);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return t;
    }

    private Thread SeProcess(String FastqFile, String Prefix, String Type) {
        SeProcess ssp = new SeProcess(FastqFile, ParameterList.get(OptIndexFile), ParameterList.get(OptAlignMisMatch), ParameterList.get(OptAlignMinQuality), ParameterList.get(OptOutPath), Prefix, Type);//单端处理类
        ssp.SetParameter(ssp.OptThreads, ParameterList.get(OptThreads));//设置线程数
        ssp.SetParameter(ssp.OptAlignThreads, ParameterList.get(OptAlignThread));
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ssp.Run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return t;
    }

    private Thread BedpeProcess(String UseLinker, String SeBedpeFile) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BedpeProcess bedpe = new BedpeProcess(BedpeProcessDir, ParameterList.get(OptOutPrefix), UseLinker, ParameterList.get(OptChromosome).split("\\s+"), EnzyFilePrefix, SeBedpeFile);//bedpe文件处理类
                    bedpe.Threads = Integer.parseInt(ParameterList.get(OptThreads));//设置线程数
                    bedpe.Run();//运行
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return t;
    }

    public void GetOption(String Infile) throws IOException {
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
    }

    public void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! no " + opt);
                System.exit(0);
            }
        }
        if (ParameterList.get(OptOutPrefix).equals("")) {
            try {
                ParameterList.put(OptOutPrefix, ParameterList.get(OptFastqFile).substring(0, ParameterList.get(OptFastqFile).lastIndexOf(".")));
            } catch (IndexOutOfBoundsException e) {
                ParameterList.put(OptOutPrefix, ParameterList.get(OptFastqFile));
            }
        }
        if (ParameterList.get(OptUseLinker).equals("")) {
            ParameterList.put(OptUseLinker, ParameterList.get(OptLinkersType));
        }
        if (!new File(ParameterList.get(OptOutPath)).isDirectory()) {
            System.err.println("Wrong OutPath " + ParameterList.get(OptOutPath) + " is not a directory");
            System.exit(0);
        }
        if (!new File(ParameterList.get(OptGenomeFile)).isFile()) {
            System.err.println("Wrong " + OptGenomeFile + " " + ParameterList.get(OptGenomeFile) + " is not a file");
            System.exit(0);
        }
        if (!new File(ParameterList.get(OptFastqFile)).isFile()) {
            System.err.println("Wrong " + OptFastqFile + " " + ParameterList.get(OptFastqFile) + " is not a file");
            System.exit(0);
        }
        if (!new File(ParameterList.get(OptLinkerFile)).isFile()) {
            System.err.println("Wrong " + OptLinkerFile + " " + ParameterList.get(OptLinkerFile) + " is not a file");
            System.exit(0);
        }
        //=======================================================================
        BufferedReader infile = new BufferedReader(new FileReader(ParameterList.get(OptLinkerFile)));
        int linkerLength = infile.readLine().length();
        infile.close();
        if (ParameterList.get(OptPhred).equals("33")) {
            AddQuality = "I";
        } else {
            AddQuality = "h";
        }
        MinLinkerFilterQuality = (linkerLength - Integer.parseInt(ParameterList.get(OptMaxMisMatchLength))) * Integer.parseInt(ParameterList.get(OptMatchScore)) + Integer.parseInt(ParameterList.get(OptMaxMisMatchLength)) * Integer.parseInt(ParameterList.get(OptMisMatchScore));//设置linkerfilter最小分数
        PreProcessDir = ParameterList.get(OptOutPath) + "/PreProcess";
        SeProcessDir = ParameterList.get(OptOutPath) + "/SeProcess";
        BedpeProcessDir = ParameterList.get(OptOutPath) + "/BedpeProcess";
        MakeMatrixDir = ParameterList.get(OptOutPath) + "/MakeMatrix";
        EnzyPath = ParameterList.get(OptOutPath) + "/EnzySiteFile";
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
        EnzyFilePrefix = EnzyPath + "/" + ParameterList.get(OptOutPrefix) + "." + ParameterList.get(OptRestriction).replace("^", "");
        step.Threads = Integer.parseInt(ParameterList.get(OptThreads));
    }

    public void ShowParameter() {
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
