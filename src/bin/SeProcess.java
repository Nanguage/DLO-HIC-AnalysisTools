package bin;

import java.io.*;
import java.util.Date;
import java.util.Hashtable;

import lib.Command.Execute;
import lib.unit.CustomFile;
import lib.unit.Opts;
import org.apache.commons.io.FileUtils;

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
    public final String OptThreads = "Threads";//线程数
    private final String OptMinQuality = "MinQuality";//最小比对质量
    private final String OptMisMatchNum = "MisMatchNum";//错配数，bwa中使用
    public final String OptAlignThreads = "AlignThreads";//比对线程数
    //========================================================================
    private File OutPath;//输出路径
    private String Prefix = Opts.Default.Prefix;
    private File FastqFile;//Fastq文件
    private File IndexPrefix;//比对索引文件
    private int ReadsType = Opts.ShortReads;//reads类型Long or Short
    public int Threads = Opts.Default.Thread;//线程数
    private int MinQuality;//最小比对质量
    private int MisMatchNum;//错配数，bwa中使用
    public int AlignThreads = 2;//比对线程数
    //========================================================================
    private Hashtable<String, String> ArgumentList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptFastqFile, OptIndexFile, OptMinQuality};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptMisMatchNum, OptAlignThreads, OptThreads};
    private File SamFile;//Sam文件
    private File SaiFile;
    private File FilterSamFile;//过滤后的Sam文件
    //    private File BamFile;//Bam文件
    private CustomFile BedFile;//Bed文件
    private CustomFile SortBedFile;//排序后的bed文件

    public SeProcess(File fastqfile, File index, int mismatch, int minquality, File outpath, String prefix, int readstype) {
//        ArgumentInit();
        FastqFile = fastqfile;
        IndexPrefix = index;
        MisMatchNum = mismatch;
        MinQuality = minquality;
        OutPath = outpath;
        Prefix = prefix;
        ReadsType = readstype;
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
    public void Run() throws IOException, InterruptedException {
        //========================================================================================
        //比对
        Align(ReadsType);
        //Sam文件过滤
        SamFilter(MinQuality);
        //Sam文件转bam再转bed
        SamToBed();
        //对bed文件排序（由于在大量数据下bed文件会比较大，所以为了减少内存消耗，bed文件排序使用串行）
        BedFile.SortFile(new int[]{4}, "", "\\s+", SortBedFile);
        System.out.println(new Date() + "\tDelete " + BedFile.getName());
        BedFile.delete();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
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
        OutPath = new File(ArgumentList.get(OptOutPath));
        Prefix = ArgumentList.get(OptPrefix);
        FastqFile = new File(ArgumentList.get(OptFastqFile));
        IndexPrefix = new File(ArgumentList.get(OptIndexFile));
//        String seType = ArgumentList.get(OptSeType);
        Threads = Integer.parseInt(ArgumentList.get(OptThreads));
        MinQuality = Integer.parseInt(ArgumentList.get(OptMinQuality));
        MisMatchNum = Integer.parseInt(ArgumentList.get(OptMisMatchNum));
        AlignThreads = Integer.parseInt(ArgumentList.get(OptAlignThreads));
        for (String opt : RequiredParameter) {
            if (ArgumentList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
    }

    /**
     * <p>类的初始化</p>
     * <p>检测输出路径，判断单端类型（哪一端），构建输出文件</p>
     */
    private void Init() {
        //=============================================================================================
        if (!OutPath.isDirectory() && !OutPath.mkdirs()) {
            System.err.println("Can't create " + OutPath);
            System.exit(0);
        }
        File FilePrefix = new File(OutPath + "/" + Prefix + "." + MisMatchNum);
        SamFile = new File(FilePrefix.getPath() + ".sam");
        SaiFile = new File(FilePrefix.getPath() + ".sai");
        FilterSamFile = new File(FilePrefix + ".uniq.sam");
//        BamFile = new File(FilterSamFile.getPath().replace(".uniq.sam", ".bam"));
        BedFile = new CustomFile(FilePrefix + ".bed");
        SortBedFile = new CustomFile(FilePrefix + ".sort.bed");

    }

    private void ArgumentInit() {
        for (String opt : RequiredParameter) {
            ArgumentList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ArgumentList.put(opt, "");
        }
        ArgumentList.put(OptPrefix, Opts.Default.Prefix);
        ArgumentList.put(OptOutPath, Opts.Default.OutPath);
        ArgumentList.put(OptMisMatchNum, "0");
        ArgumentList.put(OptAlignThreads, "2");
        ArgumentList.put(OptThreads, String.valueOf(Opts.Default.Thread));
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

    private void Align(int ReadsType) throws IOException {
        //比对
        String CommandStr;
        System.out.println(new Date() + "\tBegin to align\t" + FastqFile.getName());
        if (ReadsType == Opts.ShortReads) {
            CommandStr = "bwa aln -t " + AlignThreads + " -n " + MisMatchNum + " -f " + SaiFile + " " + IndexPrefix + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            new Execute(CommandStr);//执行命令行
            System.out.println(new Date() + "\tsai to sam\t" + FastqFile.getName());
            CommandStr = "bwa samse -f " + SamFile + " " + IndexPrefix + " " + SaiFile + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            new Execute(CommandStr);//执行命令行
            System.out.println(new Date() + "\tDelete " + SaiFile.getName());
            SaiFile.delete();//删除sai文件
        } else if (ReadsType == Opts.LongReads) {
            CommandStr = "bwa mem -t " + AlignThreads + " " + IndexPrefix + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            new Execute(CommandStr, SamFile.getPath());//执行命令行
        } else {
            System.err.println("Error reads type:" + ReadsType);
            System.exit(1);
        }
        System.out.println(new Date() + "\tEnd align\t" + FastqFile.getName());
    }

    private void SamFilter(int MinQuality) throws IOException, InterruptedException {
        //sam文件过滤
        BufferedReader sam_read = new BufferedReader(new FileReader(SamFile));
        BufferedWriter sam_write = new BufferedWriter(new FileWriter(FilterSamFile));
        System.out.println(new Date() + "\tBegin to sam filter\t" + SamFile.getName());
        Thread[] process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " begin");
                        while ((line = sam_read.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[0].matches("^@.+") || (Integer.parseInt(str[4]) >= MinQuality)) {
                                synchronized (process) {
                                    sam_write.write(line + "\n");
                                }
                            }
                        }
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
            process[i].join();
        }
        sam_read.close();
        sam_write.close();
        System.out.println(new Date() + "\tEnd to sam filter\t" + SamFile);
    }

    private void SamToBed() throws IOException {
        System.out.println(new Date() + "\tBegin\t" + FilterSamFile.getName() + " to " + BedFile.getName());
        BufferedReader reader = new BufferedReader(new FileReader(FilterSamFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(BedFile));
        String Line;
        String[] Str;
        String Orientation;
        while ((Line = reader.readLine()) != null) {
            if (Line.matches("^@.*")) {
                continue;
            }
            Str = Line.split("\\s+");
            Orientation = (Integer.parseInt(Str[1]) & 16) == 16 ? "-" : "+";
            writer.write(Str[2] + "\t" + Str[3] + "\t" + (Integer.parseInt(Str[3]) + CalculateFragLength(Str[5]) - 1) + "\t" + Str[0] + "\t" + Str[4] + "\t" + Orientation + "\n");
        }
        writer.close();
        reader.close();
    }//OK

    private int CalculateFragLength(String s) {
        int Length = 0;
        StringBuilder Str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case 'M':
                    Length += Integer.parseInt(Str.toString());
                    Str.setLength(0);
                    break;
                case 'D':
                    Length += Integer.parseInt(Str.toString());
                    Str.setLength(0);
                    break;
                case 'I':
                    Str.setLength(0);
                    break;
                default:
                    Str.append(s.charAt(i));
            }
        }
        return Length;
    }

    /*private void SamToBed() throws IOException {
        System.out.println(new Date() + "\tBegin\t" + FilterSamFile.getName() + " to " + BedFile.getName());
        String CommandStr = "samtools view -Sb -o " + BamFile + " " + FilterSamFile;
        new Execute(CommandStr, FilterSamFile + ".log");
        CommandStr = "bedtools bamtobed -i " + BamFile;
        new Execute(CommandStr, BedFile.getPath(), BedFile + ".log");
        System.out.println(new Date() + "\tEnd\t" + FilterSamFile + " to " + BedFile);
    }//OK*/

    public File getBedFile() {
        return BedFile;
    }

//    public File getBamFile() {
////        return BamFile;
////    }

    public File getSamFile() {
        return SamFile;
    }

    public File getFilterSamFile() {
        return FilterSamFile;
    }

    public CustomFile getSortBedFile() {
        return SortBedFile;
    }
}
