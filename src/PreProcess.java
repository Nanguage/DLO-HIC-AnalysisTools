import java.io.*;
import java.util.Date;

public class PreProcess {
    private String OutPath;
    private String OutPrefix;
    private String FastqFile;
    private String LinkerFile;
    private String LinkerFilterOutPrefix;
    private int Threads = 1;
    private LinkerFiltering lk;

    PreProcess(String outpath, String outprefix, String fastqfile, String linkerfile, int threads) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        Threads = threads;
        Init();
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
