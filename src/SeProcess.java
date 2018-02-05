
import java.io.*;
import java.util.Date;
import java.util.Hashtable;

/**
 * @author snowf
 * @version 1.0
 */

class SeProcess {
    private final String OptFastqFile = "FastqFile";//不同linker类型的Fastq文件
    private final String OptIndexFile = "Index";//比对索引文件
    private final String OptOutPath = "OutPath";//输出目录
    private final String OptSeType = "Type";//单端类型(R1 or R2)
    private final String OptPrefix = "Prefix";//输出前缀
    public final String OptThreads = "Thread";//线程数
    //    private String OptRestriction;//酶切位点（such as A^AGCTT）
//    private String RestrictionSeq;//需要匹配的酶切位点序列
//    private String AddSeq;//需要延长的序列
//    private String AddQuality;//延长的序列质量
//    private String LinkersType;//linker类型
//    private String OptUseLinkerType;//有用的linker类型
//    public int Phred = 33;//Fastq格式
    private final String OptMinQuality = "MinQuality";//最小比对质量
    //    private int MaxReadsLength = 20;//最长reads长度
//    private int MinReadsLength = 16;//最短reads长度
    private final String OptMisMatchNum = "MisMatchNum";//错配数，bwa中使用
    public final String OptAlignThreads = "AlignThreads";//比对线程数
    //    public int MinLinkerFilterQuality = 34;//最小linker过滤的比对质量
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptFastqFile, OptIndexFile, OptSeType, OptMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptMisMatchNum, OptAlignThreads, OptThreads};
    //    private String PastFile;//输入文件(linker过滤的结果文件)
    //    private String[] UseLinkerFastqFile;//可用的linker类型的Fastq文件
    private String SamFile;//Sam文件
    private String FilterSamFile;//过滤后的Sam文件
    private String BamFile;//Bam文件
    private String BedFile;//Bed文件
    private String SortBedFile;//排序后的bed文件

    SeProcess(String fastqfile, String index, String mismatch, String minquality, String outpath, String prefix, String type) {
        ParameterInit();
        ParameterList.put(OptFastqFile, fastqfile);
        ParameterList.put(OptIndexFile, index);
        ParameterList.put(OptMisMatchNum, mismatch);
        ParameterList.put(OptMinQuality, minquality);
        ParameterList.put(OptOutPath, outpath);
        ParameterList.put(OptPrefix, prefix);
        ParameterList.put(OptSeType, type);
        Init();
    }

    SeProcess(String ConfigFile) throws IOException {
        ParameterInit();
        GetOption(ConfigFile);
        Init();
    }

    /**
     * <p>单端数据处理</p>
     * <p>1. 比对</p>
     * <p>2. sam文件过滤</p>
     * <p>3. sam转bed</p>
     * <p>4. bed文件排序</p>
     *
     * @throws IOException
     */
    public void Run() throws IOException {
        Routine Se = new Routine();
        Se.Threads = Integer.parseInt(ParameterList.get(OptThreads));
        //========================================================================================
        //区分不同类型的linker，并放到不同的文件中（中间会过滤掉比对质量较低，和无法延长的序列）
//        Se.ClusterLinker(PastFile, LinkerFastqFile, MinReadsLength, MaxReadsLength, MinLinkerFilterQuality, RestrictionSeq, AddSeq, AddQuality, SeType);
        Thread process;
        //处理可用的linker类型（每个线程处理一种linker类型）
        process = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start\t" + LinkersType[finalI]);
                    //比对
                    Se.Align(ParameterList.get(OptIndexFile), ParameterList.get(OptFastqFile), SamFile, Integer.parseInt(ParameterList.get(OptAlignThreads)), Integer.parseInt(ParameterList.get(OptMisMatchNum)));
                    //Sam文件过滤
                    Se.SamFilter(SamFile, FilterSamFile, Integer.parseInt(ParameterList.get(OptMinQuality)));
                    //Sam文件转bam再转bed
                    Se.SamToBed(FilterSamFile, BamFile, BedFile);
                    //对bed文件排序（由于在大量数据下bed文件会比较大，所以为了减少内存消耗，bed文件排序使用串行）
                    CommonMethod.SortFile(BedFile, new int[]{4}, "", "\\s+", SortBedFile, Integer.parseInt(ParameterList.get(OptThreads)));
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end\t" + LinkersType[finalI]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        process.start();
        try {
            process.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //删掉过滤后的sam文件（有bam文件，所以不用留sam文件，节省空间）
        System.out.println(new Date() + "\tDelete " + FilterSamFile);
        new File(FilterSamFile).delete();

    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage:    java -cp DLO-HIC-AnalysisTools.jar SeProcess Config.txt");
            System.exit(0);
        }
        SeProcess se = new SeProcess(args[0]);
        se.ShowParameter();
        se.Run();
    }

    public String getSortBedFile() {
        return SortBedFile;
    }

    private void GetOption(String ConfigFile) throws IOException {
        BufferedReader file = new BufferedReader(new FileReader(ConfigFile));
        String line;
        String[] str;
        while ((line = file.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s*=\\s*");
            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
                ParameterList.put(str[0], str[1]);
            }
        }
        file.close();
    }

    /**
     * <p>类的初始化</p>
     * <p>检测输出路径，判断单端类型（哪一端），构建输出文件</p>
     */
    private void Init() {
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
        if (!ParameterList.get(OptSeType).equals("R1") && !ParameterList.get(OptSeType).equals("R2")) {
            System.err.println("Error ! Unknow Type " + ParameterList.get(OptSeType) + "\tType should R1 or R2");
            System.exit(0);
        }
        //=============================================================================================
        if (ParameterList.get(OptPrefix).equals("")) {
            if (ParameterList.get(OptFastqFile).lastIndexOf(".") != -1) {
                ParameterList.put(OptPrefix, ParameterList.get(OptFastqFile).substring(0, ParameterList.get(OptFastqFile).lastIndexOf(".")));
            } else {
                ParameterList.put(OptPrefix, OptFastqFile);
            }
        }
        if (!new File(ParameterList.get(OptOutPath) + "/" + ParameterList.get(OptSeType)).isDirectory()) {
            if (!new File(ParameterList.get(OptOutPath) + "/" + ParameterList.get(OptSeType)).mkdirs()) {
                System.err.println("Can't creat " + ParameterList.get(OptOutPath) + "/" + ParameterList.get(OptSeType));
                System.exit(0);
            }
        }
        SamFile = ParameterList.get(OptFastqFile).replace(".fastq", "." + ParameterList.get(OptMisMatchNum) + ".sam");
        FilterSamFile = SamFile.replace(".sam", ".uniq.sam");
        BamFile = FilterSamFile.replace(".uniq.sam", ".bam");
        BedFile = BamFile.replace(".bam", ".bed");
        SortBedFile = BedFile.replace(".bed", ".sort.bed");

    }

    public void ParameterInit() {
        for (String opt : RequiredParameter) {
            ParameterList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ParameterList.put(opt, "");
        }
        ParameterList.put(OptOutPath, "./");
        ParameterList.put(OptMisMatchNum, "0");
        ParameterList.put(OptAlignThreads, "8");
        ParameterList.put(OptThreads, "1");
    }

    public boolean SetParameter(String Key, String Value) {
        if (ParameterList.containsKey(Key)) {
            ParameterList.put(Key, Value);
            return true;
        } else {
            return false;
        }
    }

    public void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
        System.out.println("===============================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
    }
}
