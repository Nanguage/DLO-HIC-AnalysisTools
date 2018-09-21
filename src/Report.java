import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import lib.unit.CustomFile;
import lib.unit.Opts;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import sun.misc.BASE64Encoder;


public class Report {
    private File ReportOutPath;
    public CommonInfor ComInfor = new CommonInfor();
    public ActionInfor InterAction = new ActionInfor();
    public RunTime RunTime = new RunTime();
    public LinkerClass[] UseLinker;
    public String[] ReadsLengthDisBase64;
    public long RawDataReadsNum;
    public String AdapterSequence = "";
    public File PreDir, SeDir, BedpeDir, MatrixDir, TransDir;
    public File LinkerFile = new File("");
    public File GenomeIndex = new File("");
    public File AdapterFile = new File("");

    public ArrayList<String> LinkersType = new ArrayList<>();
    public ArrayList<String> Chromosome = new ArrayList<>();
    public double[] LinkersNum;
    public CustomFile FinalBedpeName = new CustomFile("");
    public int MinUniqueScore;
    public int[] Resolution;
    public int Thread;
    private File DataDir;
    private File ImageDir;

    public Report(File OutPath) {
        ReportOutPath = OutPath;
        DataDir = new File(ReportOutPath + "/data");
        ImageDir = new File(ReportOutPath + "/image");
        File[] CheckFile = new File[]{ReportOutPath, DataDir, ImageDir};
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
        }
        System.out.println("\n-------------------------------------------------------------");
        System.out.println("Total action number:\t" + new DecimalFormat("#,###").format(InterAction.FinalBedpeNum) + "\t" + String.format("%.2f", InterAction.FinalBedpeNum / RawDataReadsNum * 100) + "%");
        System.out.println("Inter action number:\t" + new DecimalFormat("#,###").format(InterAction.InterActionNum) + "\t" + String.format("%.2f", InterAction.InterActionNum / InterAction.FinalBedpeNum * 100) + "%");
        System.out.println("Intra action number:\t" + new DecimalFormat("#,###").format(InterAction.IntraActionNum) + "\t" + String.format("%.2f", InterAction.IntraActionNum / InterAction.FinalBedpeNum * 100) + "%");
        System.out.println("\n-------------------------------------------------------------");
        if (ComInfor.Restriction.replace("^", "").length() <= 4) {
            System.out.println("Short region <= 5k :\t" + new DecimalFormat("#,###").format(InterAction.ShortRegionNum) + "\t" + String.format("%.2f", InterAction.ShortRegionNum / InterAction.IntraActionNum * 100) + "%");
            System.out.println("Long region > 5k :\t" + new DecimalFormat("#,###").format(InterAction.LongRegionNum) + "\t" + String.format("%.2f", InterAction.LongRegionNum / InterAction.IntraActionNum * 100) + "%");
        } else {
            System.out.println("Short region <= 20k :\t" + new DecimalFormat("#,###").format(InterAction.ShortRegionNum) + "\t" + String.format("%.2f", InterAction.ShortRegionNum / InterAction.IntraActionNum * 100) + "%");
            System.out.println("Long region > 20k :\t" + new DecimalFormat("#,###").format(InterAction.LongRegionNum) + "\t" + String.format("%.2f", InterAction.LongRegionNum / InterAction.IntraActionNum * 100) + "%");
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
        ComInfor.InputFile = Opts.InputFile;
        ComInfor.OutPutDir = Opts.OutPath;
        ComInfor.OutPutPrefix = Opts.Prefix;
        ComInfor.GenomeFile = Opts.GenomeFile;
        context.setVariable("AdapterSeq", AdapterSequence.replaceAll("\\s+", "<br/>"));
        context.setVariable("TotalReads", RawDataReadsNum);
        context.setVariable("ComInformation", ComInfor);
        long Ambiguous = RawDataReadsNum;
        for (int i = 0; i < LinkersType.size(); i++) {
            context.setVariable(LinkersType.get(i) + "LinkerNum", LinkersNum[i]);
            Ambiguous -= LinkersNum[i];
        }
        context.setVariable("AmbiguousLinkerNum", ThousandFormat(Ambiguous));
        context.setVariable("AmbiguousLinkerPercent", PercentFormat((double) Ambiguous / RawDataReadsNum * 100) + "%");
        context.setVariable("PreDir", PreDir.getPath());
        context.setVariable("LinkerClass", UseLinker);
        context.setVariable("Inter", InterAction);
        context.setVariable("RunTime", RunTime);
        context.setVariable("LinkerAliScoreDis", GetBase64(Opts.LinkerScoreDisFile));
        context.setVariable("ReadsLenDiss", ReadsLengthDisBase64);


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

    public String GetBase64(File f) throws IOException {
        FileInputStream image = new FileInputStream(f);
        byte[] data = new byte[image.available()];
        image.read(data);
        image.close();
        return new BASE64Encoder().encode(data);
    }

    public File getDataDir() {
        return DataDir;
    }

    public File getImageDir() {
        return ImageDir;
    }
}

class LinkerClass {
    public String LinkerType, LinkerSequence;
    public File SeProcessOutDir, BedpeProcessOutDir;
    public CustomFile FastqFileR1, FastqFileR2;
    public CustomFile UniqMapFileR1, UniqMapFileR2;
    public CustomFile RawBedpeFile, RawSameBedpeFile, RawDiffBedpeFile;
    public CustomFile SelfLigationFile, RelLigationFile, SameValidFile;
    public CustomFile SameCleanFile, DiffCleanFile;
    public CustomFile MergeCleanFile;
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

class CommonInfor {
    public CustomFile InputFile;
    public CustomFile GenomeFile;
    public File OutPutDir;
    public String OutPutPrefix;
    public int Thread;
    public String HalfLinkerA = "", HalfLinkerB = "";
    public int MatchScore, MisMatchScore, InDelScore;
    public File IndexPrefix = new File("");
    public String Restriction = "";
    public int[] Resolution;
    public int MinReadsLen;
    public int MaxReadsLen;
    public ArrayList<String> Chromosome = new ArrayList<>();
}

class ActionInfor {
    public CustomFile FinalBedpeFile = new CustomFile("");
    public double FinalBedpeNum;
    public double IntraActionNum;
    public double InterActionNum;
    public double LongRegionNum;
    public double ShortRegionNum;
}

class RunTime {
    public String StartTime;
    public String LinkerFilter;
    public String Mapping;
    public String LigationFilter;
    public String MakeMatrix;
    public String TransLocation;
    public String Total;
}
