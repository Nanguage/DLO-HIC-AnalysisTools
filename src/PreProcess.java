import java.io.*;
import java.util.Date;

public class PreProcess {
    private String OutPath;//输出路径
    private String OutPrefix;//输出前缀
    private String FastqFile;//Fastq文件
    private String LinkerFile;//linker文件
    private String LinkerFilterOutPrefix;//linker过滤输出前缀
    private int Threads = 1;//线程数，默认1
    private LinkerFiltering lk;//声明一个linkerFiltering类

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        Threads = threads;
        Init();
        //实例化一个LinkerFiltering对象
        lk = new LinkerFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, 0, Threads * 4);
    }

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int matchscore, int mismatchscore, int indelscore, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        Threads = threads;
        Init();
        lk = new LinkerFiltering(FastqFile, LinkerFile, LinkerFilterOutPrefix, matchscore, mismatchscore, indelscore, 0, Threads * 4);
    }

    public void Run() throws IOException {
        System.out.println(new Date() + "\tStart to linkerfilter");
        lk.Run();
    }

    public String getPastFile() {
        return lk.getOutFile();
    }

    private void Init() {
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdir();
        }
        LinkerFilterOutPrefix = OutPath + "/" + OutPrefix + ".linkerfilter";
    }

}
