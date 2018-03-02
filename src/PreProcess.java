import java.io.*;
import java.util.Date;
import java.util.Hashtable;

public class PreProcess {
    private final String OptOutPath = "OutPath";//输出路径
    private final String OptOutPrefix = "OutPrefix";//输出前缀
    private final String OptFastqFile = "FastqFile";//Fastq文件
    private final String OptLinkerFile = "LinkerFile";//linker文件
    private final String OptAdapterFile = "AdapterFile";//Adapter文件
    private final String OptMatchScore = "MatchScore";//匹配分数
    private final String OptMisMatchScore = "MisMatchScore";//错配分数
    private final String OptIndelScore = "IndelScore";//插入缺失分数
    private final String OptThreads = "Thread";//线程数，默认1
    private String FastqFile;//Fastq文件
    private String LinkerFile;//linker文件
    private String AdapterFile;//Adapter文件
    private int MatchScore;//匹配分数
    private int MisMatchScore;//错配分数
    private int IndelScore;//插入缺失分数
    private int Threads;//线程数，默认1
    private String LinkerFilterOutPrefix;//linker过滤输出前缀
    //    private int ScoreNum;
    private Hashtable<String, String> OptionList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptFastqFile, OptLinkerFile};
    private String[] OptionalParameter = new String[]{OptOutPath, OptOutPrefix, OptAdapterFile, OptMatchScore, OptMisMatchScore, OptIndelScore, OptThreads};

    PreProcess(String ConfigFile) throws IOException {
        OptionListInit();
        GetOption(ConfigFile);
        Init();
    }

    PreProcess() {
    }
//    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int threads) throws IOException {
//        OptionList.put(OptOutPath, outpath);
//        OptionList.put(OptOutPrefix, outprefix);
//        OptionList.put(OptFastqFile, fastqfile);
//        OptionList.put(OptLinkerFile, linkerfile);
//        OptionList.put(OptThreads, String.valueOf(threads));
//        Init();
//    }
//
//    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, String adapterfile, int threads) throws IOException {
//        OptionList.put(OptOutPath, outpath);
//        OptionList.put(OptOutPrefix, outprefix);
//        OptionList.put(OptFastqFile, fastqfile);
//        OptionList.put(OptLinkerFile, linkerfile);
//        OptionList.put(OptAdapterFile, adapterfile);
//        OptionList.put(OptThreads, String.valueOf(threads));
//        Init();
//    }
//
//    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int matchscore, int mismatchscore, int indelscore, int threads) throws IOException {
//        OutPath = outpath;
//        OutPrefix = outprefix;
//        FastqFile = fastqfile;
//        LinkerFile = linkerfile;
//        MatchScore = matchscore;
//        MisMatchScore = mismatchscore;
//        IndelScore = indelscore;
//        Thread = threads;
//        Init();
//    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, String adapterfile, int matchscore, int mismatchscore, int indelscore, int threads) throws IOException {
        OptionListInit();
        OptionList.put(OptOutPath, outpath);
        OptionList.put(OptOutPrefix, outprefix);
        OptionList.put(OptFastqFile, fastqfile);
        OptionList.put(OptLinkerFile, linkerfile);
        OptionList.put(OptAdapterFile, adapterfile);
        OptionList.put(OptMatchScore, String.valueOf(matchscore));
        OptionList.put(OptMisMatchScore, String.valueOf(mismatchscore));
        OptionList.put(OptIndelScore, String.valueOf(indelscore));
        OptionList.put(OptThreads, String.valueOf(threads));
        Init();
    }

    public void Run() throws IOException {
        SequenceFiltering lk;//声明一个linkerFiltering类
        lk = new SequenceFiltering(FastqFile, LinkerFile, AdapterFile, LinkerFilterOutPrefix, MatchScore, MisMatchScore, IndelScore, 0, Threads);
        System.out.println(new Date() + "\tStart to linkerfilter");
        lk.Run();
    }

    public String getPastFile() throws IOException {
        return new SequenceFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, 0, 1).getOutFile();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage:  java -cp DLO-HIC-AnalysisTools.jar PreProcess <Config.txt>");
            System.exit(0);
        } else {
            PreProcess p = new PreProcess(args[0]);
            p.ShowParameter();
            p.Run();
        }
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
        for (String opt : RequiredParameter) {
            if (OptionList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
        //=================================================================
        String OutPath = OptionList.get(OptOutPath);
        String Prefix = OptionList.get(OptOutPrefix);
        FastqFile = OptionList.get(OptFastqFile);
        LinkerFile = OptionList.get(OptLinkerFile);
        AdapterFile = OptionList.get(OptAdapterFile);
        MatchScore = Integer.parseInt(OptionList.get(OptMatchScore));
        MisMatchScore = Integer.parseInt(OptionList.get(OptMisMatchScore));
        IndelScore = Integer.parseInt(OptionList.get(OptIndelScore));
        Threads = Integer.parseInt(OptionList.get(OptThreads));
        //===================================================================
        if (!new File(FastqFile).isFile()) {
            System.err.println("Wrong " + OptFastqFile + " " + FastqFile + " is not a file");
            System.exit(0);
        }
        if (!new File(LinkerFile).isFile()) {
            System.err.println("Wrong " + OptLinkerFile + " " + LinkerFile + " is not a file");
            System.exit(0);
        }
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdir()) {
                System.err.println("Can't creat " + OutPath);
                System.exit(0);
            }
        }
        LinkerFilterOutPrefix = OutPath + "/" + Prefix + ".linkerfilter";
    }

    public void OptionListInit() {
        for (String opt : RequiredParameter) {
            OptionList.put(opt, "");
        }
        OptionList.put(OptOutPath, "./");
        OptionList.put(OptOutPrefix, "Pre.out");
        OptionList.put(OptAdapterFile, "");
        OptionList.put(OptMatchScore, "1");
        OptionList.put(OptMisMatchScore, "-2");
        OptionList.put(OptIndelScore, "-2");
        OptionList.put(OptThreads, "12");
    }

    public void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + OptionList.get(opt));
        }
        System.out.println("======================================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + OptionList.get(opt));
        }
    }

    public Hashtable<String, String> getOptionList() {
        return OptionList;
    }

    public String[] getRequiredParameter() {
        return RequiredParameter;
    }

    public String[] getOptionalParameter() {
        return OptionalParameter;
    }
}
