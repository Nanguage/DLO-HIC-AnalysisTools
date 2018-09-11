import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import lib.tool.Statistic;
import lib.unit.CustomFile;
import lib.unit.IntegerArrays;
import lib.unit.Opts;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


public class Report {
    public LinkerClass AA = new LinkerClass("AA");
    public LinkerClass BB = new LinkerClass("BB");
    public LinkerClass AB = new LinkerClass("AB");
    public LinkerClass BA = new LinkerClass("BA");
    public File RawDataFile = new File("");
    public Long RawDataReadsNum;
    public String HalfLinkerA = "", HalfLinkerB = "";
    public String RestrictionSeq = "";
    public String AdapterSequence = "";
    public File OutPath = new File("");
    public String OutPrefix = "";
    public int MatchScore, MisMatchScore, InDelScore;
    public File PreDir, SeDir, BedpeDir, MatrixDir, TransDir;
    public File[] SeLinkerDir, BedpeLinkerDir;
    public File LinkerFile = new File("");
    public File GenomeFile = new File("");
    public File GenomeIndex = new File("");
    public File AdapterFile = new File("");

    public ArrayList<String> LinkersType = new ArrayList<>();
    public ArrayList<String> UseLinker = new ArrayList<>();
    public ArrayList<String> Chromosome = new ArrayList<>();
    public ArrayList<Long> LinkersNum = new ArrayList<>();
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
    public int MinAlignQuality;
    public int MinReadsLength;
    public int MaxReadsLength;
    public int[] Resolution;
    public int Thread;

    public void Show() {
        System.out.println("\n--------------------------------Statistic----------------------------------");
        System.out.print("Raw data name:\t" + RawDataFile.getName());
        System.out.println("Raw reads number:\t" + new DecimalFormat("#,###").format(RawDataReadsNum));
        System.out.println();
        System.out.println("\n-----------------------------------------\nLinkers type\tReads number\tPercent");
        for (int i = 0; i < LinkersType.size(); i++) {
            System.out.println(LinkersType.get(i) + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)) + "\t" + String.format("%.2f", (double) LinkersNum.get(i) / RawDataReadsNum * 100) + "%");
        }
        System.out.println("\n-----------------------------------------\nFastq file\tReads number\tFastq file\tReads number");
        for (int i = 0; i < FastqR1File.size(); i++) {
            System.out.println(FastqR1File.get(i).getName() + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)) + "\t" + FastqR2File.get(i).getName() + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)));
        }
        System.out.println("\n-----------------------------------------\nBed file\tUniq reads number\tPercent\tBed file\tUniq reads number\tPercent");
        for (int i = 0; i < UseBed1.size(); i++) {
            System.out.println(UseBed1.get(i).getName() + "\t" + new DecimalFormat("#,###").format(UniqMapR1Num.get(i)) + "\t" + String.format("%.2f", (double) UniqMapR1Num.get(i) / LinkersNum.get(i) * 100) + "%" + "\t" + UseBed2.get(i).getName() + "\t" + new DecimalFormat("#,###").format(UniqMapR2Num.get(i)) + "\t" + String.format("%.2f", (double) UniqMapR2Num.get(i) / LinkersNum.get(i) * 100) + "%");
        }
        long sum = 0;
        System.out.println("\n-----------------------------------------\nBedpe file\tReads number\tPercent");
        for (int i = 0; i < LigationFile.size(); i++) {
            sum = sum + BedpeNum.get(i);
            System.out.println("UniqMap\t" + new DecimalFormat("#,###").format(BedpeNum.get(i)));
            for (int j = 0; j < LigationFile.get(i).length; j++) {
                System.out.println(LigationFile.get(i)[j].replaceAll(".*/", "") + "\t" + new DecimalFormat("#,###").format(LigationNum.get(i)[j]) + "\t" + String.format("%.2f", (double) LigationNum.get(i)[j] / BedpeNum.get(i) * 100) + "%");
            }
            System.out.println(NoRmdupName.get(i).replaceAll(".*/", "") + "\t" + new DecimalFormat("#,###").format(NoRmdupNum.get(i)) + "\t" + String.format("%.2f", (double) NoRmdupNum.get(i) / BedpeNum.get(i) * 100) + "%");
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
        context.setVariable("InputFile", RawDataFile.getPath());
        context.setVariable("IndexPrefix", GenomeIndex.getPath());
        context.setVariable("OutPutDir", OutPath.getPath());
        context.setVariable("OutPutPrefix", OutPrefix);
        context.setVariable("GenomeFile", GenomeFile.getPath());
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
        context.setVariable("PreDir", PreDir.getPath());
        for (int i = 0; i < UseLinker.size(); i++) {
            context.setVariable(UseLinker.get(i) + "FastqFileR1", FastqR1File.get(i).getPath());
            context.setVariable(UseLinker.get(i) + "FastqFileR1Num", "Null");
            context.setVariable(UseLinker.get(i) + "FastqFileR1Percent", "Null");
            context.setVariable(UseLinker.get(i) + "FastqFileR2", FastqR2File.get(i).getPath());
            context.setVariable(UseLinker.get(i) + "FastqFileR2Num", "Null");
            context.setVariable(UseLinker.get(i) + "FastqFileR2Percent", "Null");
            context.setVariable(UseLinker.get(i) + "UniqMapFileR1", "Null");
            context.setVariable(UseLinker.get(i) + "UniqMapFileR1Num", UniqMapR1Num.get(i));
            context.setVariable(UseLinker.get(i) + "UniqMapFileR1Percent", "Null");
            context.setVariable(UseLinker.get(i) + "UniqMapFileR2", "Null");
            context.setVariable(UseLinker.get(i) + "UniqMapFileR2Num", UniqMapR2Num.get(i));
            context.setVariable(UseLinker.get(i) + "UniqMapFileR2Percent", "Null");
            context.setVariable(UseLinker.get(i) + "MergeBedFile", BedpeFile.get(i).getPath());
            context.setVariable(UseLinker.get(i) + "MergeBedFileNum", BedpeNum.get(i));
            context.setVariable(UseLinker.get(i) + "MergeBedFilePercent", "Null");
        }


        String html = templateEngine.process("Report", context);
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        out.write(html);
        out.close();
    }

}

class LinkerClass {
    public String LinkerType, LinkerSequence;
    public File SeProcessOutDir, BedpeProcessDir;
    public File FastqFileR1, FastqFileR2;
    public File UniqMapFileR1, UniqMapFileR2;
    public File RawBedpeFile, RawSameBedpeFile, RawDiffBedpeFile;
    public File SelfLigationFile, RelLigationFile, SameValidFile;
    public File SameCleanFile, DiffCleanFile;
    public Long LinkerNum;
    public Long FastqNumR1, FastqNumR2;
    public Long UniqMapNumR1, UniqMapNumR2;
    public Long RawBedpeNum, RawSameBedpeNum, RawDiffBedpeNum;
    public Long SelfLigationNum, RelLigationNum, SameValidNum;
    public Long SameCleanNum, DiffCleanNum;

    LinkerClass(String s) {
        LinkerType = s;
    }

}
