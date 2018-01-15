import java.io.*;
import java.util.Date;

public class ReProcess {
    private String OutPath;
    private String OutPrefix;
    private String FastqFile;
    private String SeqFile;
    private String OneLineFile;
    private String LinkerFile;
    private String LinkerFilterOutPrefix;
    private String PastFile;
    public int Threads = 1;

    ReProcess(String outpath, String outprefix, String fastqfile, String linkerfile) {
        OutPath = outpath;
        OutPrefix = outprefix;
        FastqFile = fastqfile;
        LinkerFile = linkerfile;
        Init();
    }

    public void Run() throws IOException {
        Routine reprocess = new Routine();
        reprocess.Threads = Threads;
        reprocess.FastqToSeqAndOneLine(FastqFile, SeqFile, OneLineFile);
        System.out.println(new Date() + "\tStart to linkerfilter");
        new LinkerFiltering(SeqFile, LinkerFile, LinkerFilterOutPrefix, 0, Threads * 4);
        CommonMethod.Paste(new String[]{LinkerFilterOutPrefix + ".output.txt", OneLineFile}, PastFile);
    }

    public String getPastFile() {
        return PastFile;
    }

    private void Init() {
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdir();
        }
        SeqFile = OutPath + "/" + OutPrefix + ".seq";
        OneLineFile = OutPath + "/" + OutPrefix + ".oneline";
        LinkerFilterOutPrefix = OutPath + "/" + OutPrefix + ".linkerfilter";
        PastFile = OutPath + "/" + OutPrefix + ".past";
    }
}
