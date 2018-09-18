package bin;

import kotlin.text.Charsets;
import lib.tool.SequenceFiltering;
import lib.unit.CustomFile;
import lib.unit.Opts;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
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
//    private final String OptThreads = "Threads";//线程数，默认1
    private File OutPath;//输出目录
    private String Prefix;
    private CustomFile FastqFile;//Fastq文件
    private File LinkerFile;//linker文件
    private File AdapterFile;//Adapter文件
    private int MatchScore = 1;//匹配分数
    private int MisMatchScore = -2;//错配分数
    private int IndelScore = -2;//插入缺失分数
    private int Threads;//线程数，默认1
    //    private String AdapterSeq = "";
    private String LinkerFilterOutPrefix;//linker过滤输出前缀
    //    private int ScoreNum;
    private Hashtable<String, String> OptionList = new Hashtable<>();
//    private String[] RequiredParameter = new String[]{OptFastqFile, OptLinkerFile};
//    private String[] OptionalParameter = new String[]{OptOutPath, OptOutPrefix, OptAdapterFile, OptMatchScore, OptMisMatchScore, OptIndelScore, OptThreads};

    PreProcess(String ConfigFile) throws IOException {
        GetOption(ConfigFile);
        Init();
    }

    public PreProcess() {
    }

    public PreProcess(File outpath, String outprefix, CustomFile fastqfile, File linkerfile, File adapterfile, int matchscore, int mismatchscore, int indelscore,int threads) throws IOException {
        OutPath = outpath;
        Prefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        AdapterFile = adapterfile;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Threads = threads;
        Init();
    }

    public void Run() throws IOException, InterruptedException {
        System.out.println(new Date() + "\tStart to linkerfilter");
        SequenceFiltering lk;//声明一个linkerFiltering类
        lk = new SequenceFiltering(FastqFile, LinkerFile, AdapterFile, LinkerFilterOutPrefix, MatchScore, MisMatchScore, IndelScore, 0, Threads);
        lk.Run();
    }

    public File getPastFile() throws IOException {
        return new SequenceFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, 0, 1).getOutFile();
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
        //===================================================================
        if (!FastqFile.isFile()) {
            System.err.println("Wrong " + FastqFile + " is not a file");
            System.exit(0);
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


    public Hashtable<String, String> getOptionList() {
        return OptionList;
    }




}
