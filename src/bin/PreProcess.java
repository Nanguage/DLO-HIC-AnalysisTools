package bin;

import lib.tool.SequenceFiltering;
import lib.unit.Opts;

import java.io.*;
import java.util.Date;
import java.util.Hashtable;

public class PreProcess {
    //    private final String OptOutPath = "OutPath";//输出路径
//    private final String OptOutPrefix = "OutPrefix";//输出前缀
//    private final String OptFastqFile = "FastqFile";//Fastq文件
//    private final String OptLinkerFile = "LinkerFile";//linker文件
//    private final String OptAdapterFile = "AdapterFile";//Adapter文件
//    private final String OptMatchScore = "MatchScore";//匹配分数
//    private final String OptMisMatchScore = "MisMatchScore";//错配分数
//    private final String OptIndelScore = "IndelScore";//插入缺失分数
//    private final String OptThreads = "Thread";//线程数，默认1
    private File OutPath;//输出目录
    private String Prefix;
    private File[] FastqFile;//Fastq文件
    private File LinkerFile;//linker文件
    private File AdapterFile;//Adapter文件
    private int Type = Opts.Single;
    private int MatchScore = 1;//匹配分数
    private int MisMatchScore = -2;//错配分数
    private int IndelScore = -2;//插入缺失分数
    private int Threads;//线程数，默认1
    private String LinkerFilterOutPrefix;//linker过滤输出前缀
    //    private int ScoreNum;
    private Hashtable<String, String> OptionList = new Hashtable<>();
//    private String[] RequiredParameter = new String[]{OptFastqFile, OptLinkerFile};
//    private String[] OptionalParameter = new String[]{OptOutPath, OptOutPrefix, OptAdapterFile, OptMatchScore, OptMisMatchScore, OptIndelScore, OptThreads};

    PreProcess(String ConfigFile) throws IOException {
        OptionListInit();
        GetOption(ConfigFile);
        Init();
    }

    public PreProcess() {
    }

    public PreProcess(File outpath, String outprefix, File[] fastqfile, File linkerfile, File adapterfile, int matchscore, int mismatchscore, int indelscore, int type, int threads) throws IOException {
        OptionListInit();
        OutPath = outpath;
        Prefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        AdapterFile = adapterfile;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Type = type;
        Threads = threads;
//        OptionList.put(OptOutPath, outpath);
//        OptionList.put(OptOutPrefix, outprefix);
//        OptionList.put(OptFastqFile,String.join(" ",fastqfile));
//        OptionList.put(OptLinkerFile, linkerfile);
//        OptionList.put(OptAdapterFile, adapterfile);
//        OptionList.put(OptMatchScore, String.valueOf(matchscore));
//        OptionList.put(OptMisMatchScore, String.valueOf(mismatchscore));
//        OptionList.put(OptIndelScore, String.valueOf(indelscore));
//        OptionList.put(OptThreads, String.valueOf(threads));
        Init();
    }

    public void Run() throws IOException, InterruptedException {
        System.out.println(new Date() + "\tStart to linkerfilter");
        if (Type == Opts.Single) {
            SequenceFiltering lk;//声明一个linkerFiltering类
            lk = new SequenceFiltering(FastqFile[0], LinkerFile, AdapterFile, LinkerFilterOutPrefix, MatchScore, MisMatchScore, IndelScore, 0, Threads);
            lk.Run();
        } else if (Type == Opts.PairEnd) {
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SequenceFiltering lkleft;
                        lkleft = new SequenceFiltering(FastqFile[0], LinkerFile, AdapterFile, LinkerFilterOutPrefix + ".R1", MatchScore, MisMatchScore, IndelScore, 0, Threads);
                        lkleft.Run();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SequenceFiltering lkright;
                        lkright = new SequenceFiltering(FastqFile[1], LinkerFile, AdapterFile, LinkerFilterOutPrefix + ".R2", MatchScore, MisMatchScore, IndelScore, 0, Threads);
                        lkright.Run();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
        } else {
            System.out.println("Error Type" + Type);
            System.exit(1);
        }
    }

    public File[] getPastFile() throws IOException {
        if (Type == Opts.Single) {
            return new File[]{new SequenceFiltering(FastqFile[0], LinkerFile, LinkerFilterOutPrefix, 0, 1).getOutFile()};
        } else if (Type == Opts.PairEnd) {
            File File1 = new SequenceFiltering(FastqFile[0], LinkerFile, LinkerFilterOutPrefix + ".R1", 0, 1).getOutFile();
            File File2 = new SequenceFiltering(FastqFile[0], LinkerFile, LinkerFilterOutPrefix + ".R2", 0, 1).getOutFile();
            return new File[]{File1, File2};
        } else {
            return new File[]{};
        }
    }

    public static void main(String[] args) throws IOException {
//        if (args.length < 1) {
//            System.out.println("Usage:  java -cp DLO-HIC-AnalysisTools.jar bin.PreProcess <Config.txt>");
//            System.exit(0);
//        } else {
//            PreProcess p = new PreProcess(args[0]);
////            p.ShowParameter();
//            p.Run();
//        }
    }

    private void GetOption(String optionfile) throws IOException {
        BufferedReader optfile = new BufferedReader(new FileReader(optionfile));
        String line;
        String[] str;
        while ((line = optfile.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s*=\\s*");
            if (str.length >= 2 && OptionList.containsKey(str[0])) {
                OptionList.put(str[0], str[1]);
            }
        }
        optfile.close();
    }

    private void Init() {
//        for (String opt : RequiredParameter) {
//            if (OptionList.get(opt).equals("")) {
//                System.err.println("Error ! No " + opt);
//                System.exit(0);
//            }
//        }
//        //=================================================================
//        String OutPath = OptionList.get(OptOutPath);
//        String Prefix = OptionList.get(OptOutPrefix);
//        FastqFile = OptionList.get(OptFastqFile).split("\\s+");
//        LinkerFile = OptionList.get(OptLinkerFile);
//        AdapterFile = OptionList.get(OptAdapterFile);
//        MatchScore = Integer.parseInt(OptionList.get(OptMatchScore));
//        MisMatchScore = Integer.parseInt(OptionList.get(OptMisMatchScore));
//        IndelScore = Integer.parseInt(OptionList.get(OptIndelScore));
//        Threads = Integer.parseInt(OptionList.get(OptThreads));
        //===================================================================
        for (int i = 0; i < FastqFile.length; i++) {
            if (!FastqFile[i].isFile()) {
                System.err.println("Wrong " + FastqFile[i] + " is not a file");
                System.exit(0);
            }
        }
        if (!LinkerFile.isFile()) {
            System.err.println("Wrong " + LinkerFile + " is not a file");
            System.exit(0);
        }
        if (!OutPath.isDirectory()) {
            if (!OutPath.mkdir()) {
                System.err.println("Can't creat " + OutPath);
                System.exit(0);
            }
        }
        LinkerFilterOutPrefix = OutPath + "/" + Prefix + ".linkerfilter";
    }

    public void OptionListInit() {
//        for (String opt : RequiredParameter) {
//            OptionList.put(opt, "");
//        }
//        OptionList.put(OptOutPath, "./");
//        OptionList.put(OptOutPrefix, "Pre.out");
//        OptionList.put(OptAdapterFile, "");
//        OptionList.put(OptMatchScore, "1");
//        OptionList.put(OptMisMatchScore, "-2");
//        OptionList.put(OptIndelScore, "-2");
//        OptionList.put(OptThreads, "12");
    }

//    public void ShowParameter() {
////        for (String opt : RequiredParameter) {
////            System.out.println(opt + ":\t" + OptionList.get(opt));
////        }
////        System.out.println("======================================================================================");
////        for (String opt : OptionalParameter) {
////            System.out.println(opt + ":\t" + OptionList.get(opt));
////        }
//    }

    public Hashtable<String, String> getOptionList() {
        return OptionList;
    }

//    public String[] getRequiredParameter() {
//        return RequiredParameter;
//    }
//
//    public String[] getOptionalParameter() {
//        return OptionalParameter;
//    }
}
