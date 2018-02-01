import java.io.*;
import java.util.Date;

public class PreProcess {
    private String OutPath;//输出路径
    private String OutPrefix;//输出前缀
    private String FastqFile;//Fastq文件
    private String LinkerFile;//linker文件
    private String AdapterFile;//Adapter文件
    private int MatchScore;//匹配分数
    private int MisMatchScore;//错配分数
    private int IndelScore;//插入缺失分数
    private String LinkerFilterOutPrefix;//linker过滤输出前缀
    private int ScoreNum;
    private int Threads = 1;//线程数，默认1
    private SequenceFiltering lk;//声明一个linkerFiltering类

    PreProcess() {
    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        Threads = threads;
        Init();
    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, String adapterfile, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        AdapterFile = adapterfile;
        Threads = threads;
        Init();
    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int matchscore, int mismatchscore, int indelscore, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Threads = threads;
        Init();
    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, String adapterfile, int matchscore, int mismatchscore, int indelscore, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        AdapterFile = adapterfile;
        MatchScore = matchscore;
        MisMatchScore = mismatchscore;
        IndelScore = indelscore;
        Threads = threads;
        Init();
    }

    public void Run() throws IOException {
        System.out.println(new Date() + "\tStart to linkerfilter");
        lk.Run();
    }

    public String getPastFile() {
        return lk.getOutFile();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage:  java -cp DLO-HIC-AnalysisTools.jar PreProcess <Config.txt>");
            System.exit(0);
        } else {
            PreProcess p = new PreProcess();
            p.GetOption(args[0]);
            p.Run();
        }
    }

    private void GetOption(String optionfile) throws IOException {
        BufferedReader optfile = new BufferedReader(new FileReader(optionfile));
        String line;
        String[] str;
        while ((line = optfile.readLine()) != null) {
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s+");
            switch (str[0]) {
                case "FastqFile":
                    FastqFile = str[2];
                    System.out.println("FastqFile:\t" + FastqFile);
                    break;
                case "OutPrefix":
                    OutPrefix = str[2];
                    System.out.println("OutPrefix:\t" + OutPrefix);
                    break;
                case "OutPath":
                    OutPath = str[2];
                    System.out.println("OutPath:\t" + OutPath);
                    break;
                case "LinkerFile":
                    LinkerFile = str[2];
                    System.out.println("Linker file:\t" + LinkerFile);
                    break;
                case "MatchScore":
                    MatchScore = Integer.parseInt(str[2]);
                    ScoreNum++;
                    System.out.println("Match Score:\t" + MatchScore);
                    break;
                case "MisMatchScore":
                    MisMatchScore = Integer.parseInt(str[2]);
                    ScoreNum++;
                    System.out.println("MisMatch Score:\t" + MisMatchScore);
                    break;
                case "IndelScore":
                    IndelScore = Integer.parseInt(str[2]);
                    ScoreNum++;
                    System.out.println("Indel Score:\t" + IndelScore);
                    break;
                case "AdapterFile":
                    AdapterFile = str[2];
                    System.out.println("AdapterFile:\t" + AdapterFile);
                    break;
                case "Thread":
                    Threads = Integer.parseInt(str[2]);
                    System.out.println("Thread:\t" + Threads);
                    break;
            }
        }
        optfile.close();
        Init();
    }

    private void Init() throws IOException {
        if (FastqFile == null) {
            System.out.println("Error ! No FastqFile");
            System.exit(0);
        }
        if (LinkerFile == null) {
            System.out.println("Error ! No LinkerFile");
            System.exit(0);
        }
        if (OutPath == null) {
            OutPath = "./";
            System.out.println("OutPath:\t" + OutPath);
        }
        if (OutPrefix == null) {
            OutPrefix = "pre.out";
            System.out.println("OutPrefix:\t" + OutPrefix);
        }
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdir();
        }
        LinkerFilterOutPrefix = OutPath + "/" + OutPrefix + ".linkerfilter";
        if (AdapterFile != null) {
            if (ScoreNum == 3) {
                lk = new SequenceFiltering(FastqFile, LinkerFile, AdapterFile, LinkerFilterOutPrefix, MatchScore, MisMatchScore, IndelScore, 0, Threads * 4);
            } else {
                lk = new SequenceFiltering(FastqFile, LinkerFile, AdapterFile, LinkerFilterOutPrefix, 0, Threads * 4);
            }
        } else if (ScoreNum == 3) {
            lk = new SequenceFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, MatchScore, MisMatchScore, IndelScore, 0, Threads * 4);
        } else {
            lk = new SequenceFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, 0, Threads * 4);
        }
    }

}
