package bin;

import java.io.*;
import java.util.Date;
import java.util.Hashtable;

import lib.Command.Execute;
import lib.unit.CustomFile;

/**
 * @author snowf
 * @version 1.0
 */

public class SeProcess {
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
    private File FastqFile;//不同linker类型的Fastq文件
    private File IndexFile;//比对索引文件
    public int Thread;//线程数
    private int MinQuality;//最小比对质量
    private int MisMatchNum;//错配数，bwa中使用
    public int AlignThreads;//比对线程数
    //========================================================================
    private Hashtable<String, String> ArgumentList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptFastqFile, OptIndexFile, OptMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptMisMatchNum, OptAlignThreads, OptThreads};
    private File SamFile;//Sam文件
    private File FilterSamFile;//过滤后的Sam文件
    private File BamFile;//Bam文件
    private CustomFile BedFile;//Bed文件
    private CustomFile SortBedFile;//排序后的bed文件

    public SeProcess(File fastqfile, File index, int mismatch, int minquality, File outpath, String prefix) {
        ArgumentInit();
        ArgumentList.put(OptFastqFile, fastqfile.getPath());
        ArgumentList.put(OptIndexFile, index.getPath());
        ArgumentList.put(OptMisMatchNum, String.valueOf(mismatch));
        ArgumentList.put(OptMinQuality, String.valueOf(minquality));
        ArgumentList.put(OptOutPath, outpath.getPath());
        ArgumentList.put(OptPrefix, prefix);
//        ArgumentList.put(OptSeType, type);
        Init();
    }

    SeProcess(String ConfigFile) throws IOException {
        ArgumentInit();
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
        //========================================================================================
        //处理可用的linker类型（每个线程处理一种linker类型）
//        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start\t" + LinkersType[finalI]);
        //比对
        Align();
        //Sam文件过滤
        SamFilter();
        //Sam文件转bam再转bed
        SamToBed();
        //对bed文件排序（由于在大量数据下bed文件会比较大，所以为了减少内存消耗，bed文件排序使用串行）
        BedFile.SortFile(new int[]{4}, "", "\\s+", SortBedFile);
//        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end\t" + LinkersType[finalI]);
        //删掉过滤后的sam文件（有bam文件，所以不用留sam文件，节省空间）
        System.out.println(new Date() + "\tDelete " + FilterSamFile.getName());
        FilterSamFile.delete();
        System.out.println(new Date() + "\tDelete " + BedFile.getName());
        BedFile.delete();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage:    java -cp DLO-HIC-AnalysisTools.jar bin.SeProcess Config.txt");
            System.exit(0);
        }
        SeProcess se = new SeProcess(args[0]);
        se.ShowParameter();
        se.Run();
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
            if (ArgumentList.containsKey(str[0]) && str.length >= 2) {
                ArgumentList.put(str[0], str[1]);
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
            if (ArgumentList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
//        if (!ArgumentList.get(OptSeType).equals("R1") && !ArgumentList.get(OptSeType).equals("R2")) {
//            System.err.println("Error ! Unknow Type " + ArgumentList.get(OptSeType) + "\tType should R1 or R2");
//            System.exit(0);
//        }
        //=============================================================================================
        String OutPath = ArgumentList.get(OptOutPath);
        String Prefix = ArgumentList.get(OptPrefix);
        FastqFile = new File(ArgumentList.get(OptFastqFile));
        IndexFile = new File(ArgumentList.get(OptIndexFile));
//        String seType = ArgumentList.get(OptSeType);
        Thread = Integer.parseInt(ArgumentList.get(OptThreads));
        MinQuality = Integer.parseInt(ArgumentList.get(OptMinQuality));
        MisMatchNum = Integer.parseInt(ArgumentList.get(OptMisMatchNum));
        AlignThreads = Integer.parseInt(ArgumentList.get(OptAlignThreads));
        //=============================================================================================
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdirs()) {
                System.err.println("Can't creat " + OutPath);
                System.exit(0);
            }
        }
        SamFile = new File(OutPath + "/" + Prefix + "." + MisMatchNum + ".sam");
//        SamFile = FastqFile.replace(".fastq", "." + MisMatchNum + ".sam");
        FilterSamFile = new File(SamFile.getPath().replace(".sam", ".uniq.sam"));
        BamFile = new File(FilterSamFile.getPath().replace(".uniq.sam", ".bam"));
        BedFile = new CustomFile(BamFile.getPath().replace(".bam", ".bed"));
        SortBedFile = new CustomFile(BedFile.getPath().replace(".bed", ".sort.bed"));

    }

    public void ArgumentInit() {
        for (String opt : RequiredParameter) {
            ArgumentList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ArgumentList.put(opt, "");
        }
        ArgumentList.put(OptPrefix, "SeProcess_Out");
        ArgumentList.put(OptOutPath, "./");
        ArgumentList.put(OptMisMatchNum, "0");
        ArgumentList.put(OptAlignThreads, "8");
        ArgumentList.put(OptThreads, "1");
    }

    public boolean SetParameter(String Key, String Value) {
        if (ArgumentList.containsKey(Key)) {
            ArgumentList.put(Key, Value);
            return true;
        } else {
            return false;
        }
    }

    private void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ArgumentList.get(opt));
        }
        System.out.println("===============================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ArgumentList.get(opt));
        }
    }

    private void Align() throws IOException {
        //比对
        System.out.println(new Date() + "\tBegin to align\t" + FastqFile.getName());
        String CommandStr = "bwa aln -t " + AlignThreads + " -n " + MisMatchNum + " -f " + FastqFile + ".sai " + IndexFile + " " + FastqFile;
        new Execute(CommandStr);//执行命令行
        System.out.println(new Date() + "\tsai to sam\t" + FastqFile.getName());
        CommandStr = "bwa samse -f " + SamFile + " " + IndexFile + " " + FastqFile + ".sai " + FastqFile;
        new Execute(CommandStr);//执行命令行
        System.out.println(new Date() + "\tDelete " + FastqFile.getName() + ".sai ");
        new File(FastqFile + ".sai").delete();//删除sai文件
        System.out.println(new Date() + "\tEnd align\t" + FastqFile.getName());
    }//OK

    private void SamFilter() throws IOException {
        //sam文件过滤
        BufferedReader sam_read = new BufferedReader(new FileReader(SamFile));
        BufferedWriter sam_write = new BufferedWriter(new FileWriter(FilterSamFile));
        System.out.println(new Date() + "\tBegin to sam filter\t" + SamFile.getName());
        Thread[] process = new Thread[Thread];
        for (int i = 0; i < Thread; i++) {
            process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " begin");
                        while ((line = sam_read.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[0].matches("^@.+") || (Integer.parseInt(str[4]) >= MinQuality)) {
                                synchronized (process) {
                                    sam_write.write(line + "\n");
                                }
                            }
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
            try {
                process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sam_read.close();
        sam_write.close();
        System.out.println(new Date() + "\tEnd to sam filter\t" + SamFile);
    }//OK

    private void SamToBed() throws IOException {
        System.out.println(new Date() + "\tBegin\t" + FilterSamFile.getName() + " to " + BedFile.getName());
        String CommandStr = "samtools view -Sb -o " + BamFile + " " + FilterSamFile;
        new Execute(CommandStr, FilterSamFile + ".log");
        CommandStr = "bedtools bamtobed -i " + BamFile;
        new Execute(CommandStr, BedFile.getPath(), BedFile + ".log");
        System.out.println(new Date() + "\tEnd\t" + FilterSamFile + " to " + BedFile);
    }//OK

    public File getBedFile() {
        return BedFile;
    }

    public File getBamFile() {
        return BamFile;
    }

    public File getSamFile() {
        return SamFile;
    }

    public CustomFile getSortBedFile() {
        return SortBedFile;
    }
}
