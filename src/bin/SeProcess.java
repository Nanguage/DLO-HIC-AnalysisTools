package bin;

import java.io.*;
import java.util.Date;
import java.util.Hashtable;

import lib.tool.Tools;
import lib.unit.CustomFile;
import lib.unit.Opts;

import javax.sound.midi.SoundbankResource;

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
    private File IterationDir;
    private String Prefix = Opts.Default.Prefix;
    private File FastqFile;//Fastq文件
    private File IndexPrefix;//比对索引前缀
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
    //    private File SaiFile;
    private CustomFile UniqSamFile;//唯一比对的Sam文件
    private File UnMapSamFile;//未比对上的Sam文件
    private CustomFile MultiSamFile;//多比对文件
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
        Align(FastqFile, SamFile, ReadsType);
        //Sam文件过滤
        SamFilter(SamFile, UniqSamFile, UnMapSamFile, MultiSamFile, MinQuality);
        BufferedReader sam_read = new BufferedReader(new FileReader(UnMapSamFile));
        String Line;
        String[] Str;
        Hashtable<String, String> ReadsList = new Hashtable<>();
        while ((Line = sam_read.readLine()) != null) {
            Str = Line.split("\\s+");
            ReadsList.put(Str[0], Str[9]);
        }
        sam_read.close();
        File[] TempFile = IterationAlignment(ReadsList, Prefix, 1);
        UniqSamFile.Append(TempFile[0]);
        MultiSamFile.Append(TempFile[1]);
        //Sam文件转bed
        Tools.SamToBed(UniqSamFile, BedFile);
        BedFile.SortFile(new int[]{4}, "", "\\s+", SortBedFile);
        System.out.println(new Date() + "\tDelete " + BedFile.getName());
        BedFile.delete();
        Tools.RemoveEmptyFile(OutPath);
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
        IterationDir = new File(OutPath + "/Iteration");
        synchronized (SeProcess.class) {
            if (!OutPath.isDirectory() && !OutPath.mkdirs()) {
                System.err.println("Can't create " + OutPath);
                System.exit(0);
            }
            if (!IterationDir.isDirectory() && !IterationDir.mkdirs()) {
                System.err.println("Can't create Directory " + IterationDir);
                System.exit(1);
            }
        }
        File FilePrefix = new File(OutPath + "/" + Prefix + "." + MisMatchNum);
        SamFile = new File(FilePrefix.getPath() + ".sam");
        UniqSamFile = new CustomFile(FilePrefix + ".uniq.sam");
        UnMapSamFile = new File(FilePrefix + ".unmap.sam");
        MultiSamFile = new CustomFile(FilePrefix + ".multi.sam");
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

    private void Align(File FastqFile, File SamFile, int ReadsType) throws IOException, InterruptedException {
        //比对
        String CommandStr;
        System.out.println(new Date() + "\tBegin to align\t" + FastqFile.getName());
        if (ReadsType == Opts.ShortReads) {
            File SaiFile = new File(FastqFile + ".sai");
            CommandStr = "bwa aln -t " + AlignThreads + " -n " + MisMatchNum + " -f " + SaiFile + " " + IndexPrefix + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            Tools.ExecuteCommandStr(CommandStr);//执行命令行
            System.out.println(new Date() + "\tsai to sam\t" + FastqFile.getName());
            CommandStr = "bwa samse -f " + SamFile + " " + IndexPrefix + " " + SaiFile + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            Tools.ExecuteCommandStr(CommandStr);//执行命令行
            System.out.println(new Date() + "\tDelete " + SaiFile.getName());
            SaiFile.delete();//删除sai文件
        } else if (ReadsType == Opts.LongReads) {
            CommandStr = "bwa mem -t " + AlignThreads + " " + IndexPrefix + " " + FastqFile;
            Opts.CommandOutFile.Append(CommandStr + "\n");
            Tools.ExecuteCommandStr(CommandStr, SamFile.getPath());//执行命令行
        } else {
            System.err.println("Error reads type:" + ReadsType);
            System.exit(1);
        }
        System.out.println(new Date() + "\tEnd align\t" + FastqFile.getName());
    }

    /**
     * @param SamFile
     * @param UniqSamFile
     * @param UnMapSamFile
     * @param MultiSamFile
     * @param MinQuality
     * @return <>count of unique map, unmap and multi map</p>
     * @throws IOException
     * @throws InterruptedException
     */
    private long[] SamFilter(File SamFile, File UniqSamFile, File UnMapSamFile, File MultiSamFile, int MinQuality) throws IOException, InterruptedException {
        //sam文件过滤
        long[] Count = new long[]{0, 0, 0};
        BufferedReader sam_read = new BufferedReader(new FileReader(SamFile));
        BufferedWriter sam_write = new BufferedWriter(new FileWriter(UniqSamFile));
        BufferedWriter unmap_write = new BufferedWriter(new FileWriter(UnMapSamFile));
        BufferedWriter multi_write = new BufferedWriter(new FileWriter(MultiSamFile));
        System.out.println(new Date() + "\tBegin to sam filter\t" + SamFile.getName());
        Thread[] process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        while ((line = sam_read.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[0].matches("^@.+")) {
                                continue;
                            }
                            if (Integer.parseInt(str[4]) >= MinQuality) {
                                synchronized (sam_write) {
                                    Count[0]++;
                                    sam_write.write(line + "\n");
                                }
                            } else if (str[2].equals("*")) {
                                synchronized (unmap_write) {
                                    Count[1]++;
                                    unmap_write.write(line + "\n");
                                }
                            } else {
                                synchronized (multi_write) {
                                    Count[2]++;
                                    multi_write.write(line + "\n");
                                }
                            }
                        }
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
        unmap_write.close();
        multi_write.close();
        System.out.println(new Date() + "\tEnd to sam filter\t" + SamFile);
        return Count;
    }

    /**
     * @param ReadsList
     * @param Prefix
     * @param Num
     * @return <>samfile of unique map and multi map</p>
     * @throws IOException
     * @throws InterruptedException
     */
    public File[] IterationAlignment(Hashtable<String, String> ReadsList, String Prefix, int Num) throws IOException, InterruptedException {
        System.out.println(new Date() + "\tIteration align start " + Num);
        CustomFile UniqSamFile = new CustomFile(IterationDir + "/" + Prefix + ".iteration" + Num + ".uniq.sam");
        CustomFile UnMapSamFile = new CustomFile(IterationDir + "/" + Prefix + ".iteration" + Num + ".unmap.sam");
        CustomFile MultiSamFile = new CustomFile(IterationDir + "/" + Prefix + ".iteration" + Num + ".multi.sam");
        File FastaFile = new File(IterationDir + "/" + Prefix + "." + Num + ".fasta");
        BufferedWriter fasta_write = new BufferedWriter(new FileWriter(FastaFile));
        String Line;
        String[] Str;
        //------------------------------------------fasta file write----------------------------------------------------
        for (String title : ReadsList.keySet()) {
            String Seq = ReadsList.get(title);
            String[] KSeq = Tools.GetKmer(Seq, Num);
            for (int i = 0; i < KSeq.length; i++) {
                fasta_write.write(">" + title + "\n");
                fasta_write.write(KSeq[i] + "\n");
            }
        }
        fasta_write.close();
        //--------------------------------------------------------------------------------------------------------------
        File TempSamFile = new File(IterationDir + "/" + Prefix + ".sam.temp" + Num);
        Align(FastaFile, TempSamFile, ReadsType);// align
        SamFilter(TempSamFile, UniqSamFile, UnMapSamFile, MultiSamFile, MinQuality);//filter
        //delete useless file
        FastaFile.delete();
        UnMapSamFile.delete();
        TempSamFile.delete();
        //remove uniq and multi map reads name
        BufferedReader sam_reader = new BufferedReader(new FileReader(UniqSamFile));
        while ((Line = sam_reader.readLine()) != null) {
            Str = Line.split("\\s+");
            ReadsList.remove(Str[0]);
        }
        sam_reader.close();
        sam_reader = new BufferedReader(new FileReader(MultiSamFile));
        while ((Line = sam_reader.readLine()) != null) {
            Str = Line.split("\\s+");
            ReadsList.remove(Str[0]);
        }
        sam_reader.close();
        //------------------------------------remove multi unique map-----------------------------------------------------
        UniqSamFile.SortFile(new int[]{1}, "", "", new File(UniqSamFile + ".sort"));
        sam_reader = new BufferedReader(new FileReader(UniqSamFile + ".sort"));
        Hashtable<String, Integer> TempHash = new Hashtable<>();
        while ((Line = sam_reader.readLine()) != null) {
            Str = Line.split("\\s+");
            TempHash.put(Str[0], TempHash.getOrDefault(Str[0], 0) + 1);
        }
        sam_reader = new BufferedReader(new FileReader(UniqSamFile + ".sort"));
        BufferedWriter writer = new BufferedWriter(new FileWriter(UniqSamFile));
        while ((Line = sam_reader.readLine()) != null) {
            Str = Line.split("\\s+");
            if (TempHash.get(Str[0]) == 1) {
                writer.write(Line + "\n");
            }
        }
        TempHash.clear();
        writer.close();
        sam_reader.close();
        new File(UniqSamFile + ".sort").delete();
        //--------------------------------------------------------------------------------------------------------------
//        while ((Line = sam_reader.readLine()) != null) {
//            Str = Line.split("\\s+");
//            TempHash.put(Str[0], TempHash.getOrDefault(Str[0], 0) + 1);
//        }
        if (ReadsList.keySet().size() == 0) {
            return new File[]{UniqSamFile, MultiSamFile};
        }
        File[] TempFile = IterationAlignment(ReadsList, Prefix, ++Num);
        UniqSamFile.Append(TempFile[0]);
        MultiSamFile.Append(TempFile[1]);
        TempFile[0].delete();
        TempFile[1].delete();
        return new File[]{UniqSamFile, MultiSamFile};
    }

    public File getBedFile() {
        return BedFile;
    }

    public File getSamFile() {
        return SamFile;
    }

    public File getUniqSamFile() {
        return UniqSamFile;
    }

    public CustomFile getSortBedFile() {
        return SortBedFile;
    }
}
