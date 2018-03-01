
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
    //    private final String OptSeType = "Type";//单端类型(R1 or R2)
    private final String OptPrefix = "Prefix";//输出前缀
    public final String OptThreads = "Thread";//线程数
    private final String OptMinQuality = "MinQuality";//最小比对质量
    private final String OptMisMatchNum = "MisMatchNum";//错配数，bwa中使用
    public final String OptAlignThreads = "AlignThreads";//比对线程数
    //========================================================================
    private String FastqFile;//不同linker类型的Fastq文件
    private String IndexFile;//比对索引文件
    private int Thread;//线程数
    private int MinQuality;//最小比对质量
    private int MisMatchNum;//错配数，bwa中使用
    private int AlignThreads;//比对线程数
    //========================================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptFastqFile, OptIndexFile, OptMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptMisMatchNum, OptAlignThreads, OptThreads};
    private String SamFile;//Sam文件
    private String FilterSamFile;//过滤后的Sam文件
    private String BamFile;//Bam文件
    private String BedFile;//Bed文件
    private String SortBedFile;//排序后的bed文件

    SeProcess(String fastqfile, String index, String mismatch, String minquality, String outpath, String prefix) {
        ParameterInit();
        ParameterList.put(OptFastqFile, fastqfile);
        ParameterList.put(OptIndexFile, index);
        ParameterList.put(OptMisMatchNum, mismatch);
        ParameterList.put(OptMinQuality, minquality);
        ParameterList.put(OptOutPath, outpath);
        ParameterList.put(OptPrefix, prefix);
//        ParameterList.put(OptSeType, type);
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
        Se.Threads = Thread;
        //========================================================================================
        //处理可用的linker类型（每个线程处理一种linker类型）
//        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start\t" + LinkersType[finalI]);
        //比对
        Se.Align(IndexFile, FastqFile, SamFile, AlignThreads, MisMatchNum);
        //Sam文件过滤
        Se.SamFilter(SamFile, FilterSamFile, MinQuality);
        //Sam文件转bam再转bed
        Se.SamToBed(FilterSamFile, BamFile, BedFile);
        //对bed文件排序（由于在大量数据下bed文件会比较大，所以为了减少内存消耗，bed文件排序使用串行）
        CommonMethod.SortFile(BedFile, new int[]{4}, "", "\\s+", SortBedFile, Thread);
//        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end\t" + LinkersType[finalI]);
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
//        if (!ParameterList.get(OptSeType).equals("R1") && !ParameterList.get(OptSeType).equals("R2")) {
//            System.err.println("Error ! Unknow Type " + ParameterList.get(OptSeType) + "\tType should R1 or R2");
//            System.exit(0);
//        }
        //=============================================================================================
        String OutPath = ParameterList.get(OptOutPath);
        String Prefix = ParameterList.get(OptPrefix);
        FastqFile = ParameterList.get(OptFastqFile);
        IndexFile = ParameterList.get(OptIndexFile);
//        String seType = ParameterList.get(OptSeType);
        Thread = Integer.parseInt(ParameterList.get(OptThreads));
        MinQuality = Integer.parseInt(ParameterList.get(OptMinQuality));
        MisMatchNum = Integer.parseInt(ParameterList.get(OptMisMatchNum));
        AlignThreads = Integer.parseInt(ParameterList.get(OptAlignThreads));
        //=============================================================================================
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdirs()) {
                System.err.println("Can't creat " + OutPath);
                System.exit(0);
            }
        }
        SamFile = OutPath + "/" + Prefix + "." + MisMatchNum + ".sam";
//        SamFile = FastqFile.replace(".fastq", "." + MisMatchNum + ".sam");
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
        ParameterList.put(OptPrefix, "SeProcess_Out");
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
