import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import javafx.scene.input.DataFormat;
import lib.tool.Statistic;
import lib.unit.CustomFile;
import lib.unit.IntegerArrays;
import lib.unit.Opts;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import sun.misc.BASE64Encoder;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;


public class Report {
    private File ReportOutPath;
    public LinkerClass[] UseLinker;
    //    public File RawDataFile = new File("");
    public long RawDataReadsNum;
    public String HalfLinkerA = "", HalfLinkerB = "";
    public String RestrictionSeq = "";
    public String AdapterSequence = "";
    //    public File OutPath = new File("");
//    public String OutPrefix = "";
    public int MatchScore, MisMatchScore, InDelScore;
    public File PreDir, SeDir, BedpeDir, MatrixDir, TransDir;
    public File[] SeLinkerDir, BedpeLinkerDir;
    public File LinkerFile = new File("");
    //    public File GenomeFile = new File("");
    public File GenomeIndex = new File("");
    public File AdapterFile = new File("");

    public ArrayList<String> LinkersType = new ArrayList<>();
    //    public ArrayList<String> UseLinker = new ArrayList<>();
    public ArrayList<String> Chromosome = new ArrayList<>();
    public Long[] LinkersNum;
    public ArrayList<File> FastqR1File = new ArrayList<>();
    public ArrayList<File> FastqR2File = new ArrayList<>();
    //    public ArrayList<Long> FastqR1Num = new ArrayList<>();
//    public ArrayList<Long> FastqR2Num = new ArrayList<>();
    public ArrayList<File> UseBed1 = new ArrayList<>();
    public ArrayList<File> UseBed2 = new ArrayList<>();
    public ArrayList<Long> UniqMapR1Num = new ArrayList<>();
    public ArrayList<Long> UniqMapR2Num = new ArrayList<>();
    public ArrayList<File> BedpeFile = new ArrayList<>();
    public ArrayList<Long> BedpeNum = new ArrayList<>();
    public ArrayList<String> NoRmdupName = new ArrayList<>();
    public ArrayList<Long> NoRmdupNum = new ArrayList<>();
    public ArrayList<String[]> LigationFile = new ArrayList<>();
    public ArrayList<Long[]> LigationNum = new ArrayList<>();
    public CustomFile FinalBedpeName = new CustomFile("");
    public long FinalBedpeNum;
    public long IntraActionNum;
    public long InterActionNum;
    public long LongRegionNum;
    public long ShortRegionNum;
    public int MinUniqueScore;
    public int MinReadsLength;
    public int MaxReadsLength;
    public int[] Resolution;
    public int Thread;

    public Report(File OutPath) {
        ReportOutPath = OutPath;
        File[] CheckFile = new File[]{ReportOutPath, new File(ReportOutPath + "/data"), new File(ReportOutPath + "/image")};
        for (File f : CheckFile) {
            if (!f.isDirectory() && !f.mkdir()) {
                System.err.println(new Date() + ":\tCan't create " + f);
                System.exit(1);
            }
        }
    }

    public void Show() {
        System.out.println("\n--------------------------------Statistic----------------------------------");
        System.out.print("Raw data name:\t" + Opts.InputFile.getName());
        System.out.println("Raw reads number:\t" + new DecimalFormat("#,###").format(RawDataReadsNum));
        System.out.println();
        System.out.println("\n-----------------------------------------\nLinkers type\tReads number\tPercent");
        for (int i = 0; i < LinkersType.size(); i++) {
            System.out.println(LinkersType.get(i) + "\t" + new DecimalFormat("#,###").format(LinkersNum[i]) + "\t" + String.format("%.2f", (double) LinkersNum[i] / RawDataReadsNum * 100) + "%");
        }
        System.out.println("\n-----------------------------------------\nFastq file\tReads number\tFastq file\tReads number");
        for (int i = 0; i < UseLinker.length; i++) {
            System.out.println(UseLinker[i].FastqFileR1.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].FastqNumR1) + "\t" + UseLinker[i].FastqFileR2.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].FastqNumR2));
        }
        System.out.println("\n-----------------------------------------\nBed file\tUniq reads number\tPercent\tBed file\tUniq reads number\tPercent");
        for (int i = 0; i < UseLinker.length; i++) {
            System.out.println(UseLinker[i].UniqMapFileR1.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].UniqMapNumR1) + "\t" + String.format("%.2f", (double) UseLinker[i].UniqMapNumR1 / UseLinker[i].FastqNumR1 * 100) + "%" + "\t" + UseLinker[i].UniqMapFileR2.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].UniqMapNumR2) + "\t" + String.format("%.2f", (double) UseLinker[i].UniqMapNumR2 / UseLinker[i].FastqNumR2 * 100) + "%");
        }
        double sum = 0;
        System.out.println("\n-----------------------------------------\nBedpe file\tReads number\tPercent");
        for (int i = 0; i < UseLinker.length; i++) {
            sum = sum + UseLinker[i].SameCleanNum + UseLinker[i].DiffCleanNum;
            System.out.println("UniqMap\t" + new DecimalFormat("#,###").format(UseLinker[i].RawBedpeNum));
            System.out.println(UseLinker[i].SelfLigationFile.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].SelfLigationNum) + "\t" + String.format("%.2f", UseLinker[i].SelfLigationNum / UseLinker[i].RawBedpeNum * 100) + "%");
            System.out.println(UseLinker[i].RelLigationFile.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].RelLigationNum) + "\t" + String.format("%.2f", UseLinker[i].RelLigationNum / UseLinker[i].RawBedpeNum * 100) + "%");
            System.out.println(UseLinker[i].SameValidFile.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].SameValidNum) + "\t" + String.format("%.2f", UseLinker[i].SameValidNum / UseLinker[i].RawBedpeNum * 100) + "%");
            System.out.println(UseLinker[i].RawDiffBedpeFile.getName() + "\t" + new DecimalFormat("#,###").format(UseLinker[i].RawDiffBedpeNum) + "\t" + String.format("%.2f", UseLinker[i].RawDiffBedpeNum / UseLinker[i].RawBedpeNum * 100) + "%");
//            System.out.println(NoRmdupName.get(i).replaceAll(".*/", "") + "\t" + new DecimalFormat("#,###").format(NoRmdupNum.get(i)) + "\t" + String.format("%.2f", (double) NoRmdupNum.get(i) / BedpeNum.get(i) * 100) + "%");
        }
        System.out.println("\n-------------------------------------------------------------");
        System.out.println("Total action number:\t" + new DecimalFormat("#,###").format(FinalBedpeNum) + "\t" + String.format("%.2f", (double) FinalBedpeNum / RawDataReadsNum * 100) + "%");
        System.out.println("Inter action number:\t" + new DecimalFormat("#,###").format(InterActionNum) + "\t" + String.format("%.2f", (double) InterActionNum / FinalBedpeNum * 100) + "%");
        System.out.println("Intra action number:\t" + new DecimalFormat("#,###").format(IntraActionNum) + "\t" + String.format("%.2f", (double) IntraActionNum / FinalBedpeNum * 100) + "%");
        System.out.println("\n-------------------------------------------------------------");
        if (RestrictionSeq.replace("^", "").length() <= 4) {
            System.out.println("Short region <= 5k :\t" + new DecimalFormat("#,###").format(ShortRegionNum) + "\t" + String.format("%.2f", (double) ShortRegionNum / IntraActionNum * 100) + "%");
            System.out.println("Long region > 5k :\t" + new DecimalFormat("#,###").format(LongRegionNum) + "\t" + String.format("%.2f", (double) LongRegionNum / IntraActionNum * 100) + "%");
        } else {
            System.out.println("Short region <= 20k :\t" + new DecimalFormat("#,###").format(ShortRegionNum) + "\t" + String.format("%.2f", (double) ShortRegionNum / IntraActionNum * 100) + "%");
            System.out.println("Long region > 20k :\t" + new DecimalFormat("#,###").format(LongRegionNum) + "\t" + String.format("%.2f", (double) LongRegionNum / IntraActionNum * 100) + "%");
        }
    }

    public void ReportHtml(String outfile) throws IOException {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("resource/");
        resolver.setSuffix(".html");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("Date", DateFormat.getDateTimeInstance().format(new Date()));
        context.setVariable("InputFile", Opts.InputFile.getPath());
        context.setVariable("IndexPrefix", GenomeIndex.getPath());
        context.setVariable("OutPutDir", Opts.OutPath.getPath());
        context.setVariable("OutPutPrefix", Opts.Prefix);
        context.setVariable("GenomeFile", Opts.GenomeFile.getPath());
        context.setVariable("LinkerA", HalfLinkerA);
        context.setVariable("LinkerB", HalfLinkerB);
        context.setVariable("MatchScore", MatchScore);
        context.setVariable("MisMatchScore", MisMatchScore);
        context.setVariable("InDelScore", InDelScore);
        context.setVariable("Restriction", RestrictionSeq);
        context.setVariable("MinReadsLen", MinReadsLength);
        context.setVariable("MaxReadsLen", MaxReadsLength);
        context.setVariable("Resolution", String.join(" ", IntegerArrays.toString(Resolution)));
        context.setVariable("Thread", Thread);
        context.setVariable("AdapterSeq", AdapterSequence.replaceAll("\\s+", "<br/>"));
        context.setVariable("TotalReads", RawDataReadsNum);
        long Ambiguous = RawDataReadsNum;
        for (int i = 0; i < LinkersType.size(); i++) {
            context.setVariable(LinkersType.get(i) + "LinkerNum", LinkersNum[i]);
//            context.setVariable(LinkersType.get(i) + "LinkerPercent", String.format("%.2f", (double) LinkersNum[i] / RawDataReadsNum * 100) + "%");
            Ambiguous -= LinkersNum[i];
        }
        context.setVariable("AmbiguousLinkerNum", ThousandFormat(Ambiguous));
        context.setVariable("AmbiguousLinkerPercent", PercentFormat((double) Ambiguous / RawDataReadsNum * 100) + "%");
        context.setVariable("PreDir", PreDir.getPath());
//        AA.LinkerType="AA";AA.FastqFileR1=new File("./Se/AAR1.fastq");AA.FastqNumR1=1000;AA.SeProcessOutDir=new File("./Se");
//        AA.SelfLigationFile=new File("./Self");AA.SelfLigationNum=200;AA.RelLigationFile=new File("./Rel");AA.RelLigationNum=200;
//        BB.SelfLigationFile=new File("./Self");BB.SelfLigationNum=200;BB.RelLigationFile=new File("./Rel");BB.RelLigationNum=200;
//        BB.FastqNumR1=1000;
//        BB.LinkerType="BB";BB.FastqFileR2=new File("./Se/BBR2.fastq");BB.FastqNumR2=900;BB.SeProcessOutDir=new File("./Se");
        context.setVariable("LinkerClass", UseLinker);
        context.setVariable("LinkerAliScoreDis", GetBase64(Opts.LinkerScoreDisFile));


        //========================================test=============================


//        String[][][] LinkerLigationData = new String[2][6][3];
//        LinkerLigationData[0][0][0] = "AA";
//        LinkerLigationData[1][0][0] = "BB";
//        for (int i = 0; i < LinkerLigationData.length; i++) {
//            for (int j = 1; j < LinkerLigationData[i].length - 1; j++) {
//                for (int k = 0; k < LinkerLigationData[i][j].length; k++) {
//                    LinkerLigationData[i][j][k] = "Test" + i + "-" + j + "-" + k;
//                }
//            }
//        }
//        context.setVariable("LinkerLigationData", LinkerLigationData);

//==================================
        String html = templateEngine.process("Report", context);
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        out.write(html);
        out.close();
    }

    private String ThousandFormat(Number n) {
        return new DecimalFormat("#,###").format(n);
    }

    private String PercentFormat(Number n) {
        return String.format("%.2f", n);
    }

    public void LinkerClassInit(int i) {
        UseLinker = new LinkerClass[i];
        for (int j = 0; j < i; j++) {
            UseLinker[j] = new LinkerClass();
        }
    }

    private String GetBase64(File f) throws IOException {
        FileInputStream image = new FileInputStream(f);
        byte[] data = new byte[image.available()];
        image.read(data);
        image.close();
        return new BASE64Encoder().encode(data);
    }
}

class LinkerClass {
    public String LinkerType, LinkerSequence;
    public File SeProcessOutDir, BedpeProcessDir;
    public CustomFile FastqFileR1, FastqFileR2;
    public CustomFile UniqMapFileR1, UniqMapFileR2;
    public CustomFile RawBedpeFile, RawSameBedpeFile, RawDiffBedpeFile;
    public CustomFile SelfLigationFile, RelLigationFile, SameValidFile;
    public CustomFile SameCleanFile, DiffCleanFile;
    public double LinkerNum;
    public double FastqNumR1, FastqNumR2;
    public double UniqMapNumR1, UniqMapNumR2;
    public double RawBedpeNum, RawSameBedpeNum, RawDiffBedpeNum;
    public double SelfLigationNum, RelLigationNum, SameValidNum;
    public double SameCleanNum, DiffCleanNum;

    LinkerClass(String s) {
        LinkerType = s;
    }

    LinkerClass() {
    }

}
