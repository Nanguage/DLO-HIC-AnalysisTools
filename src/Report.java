import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

import lib.tool.Statistic;
import lib.unit.CustomFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


public class Report {

    public File[] RawDataFile;
    public File LinkerFile = new File("");
    public File GenomeFile = new File("");
    public File GenomeIndex = new File("");
    public File AdapterFile = new File("");
    public File OutPath = new File("");
    public String OutPrefix = "";

    public Long[] RawDataReadsNum;
    public String RestrictionSeq = "";
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
    public int Resolution;
    public int Thread;

    public void Run() {

    }

    public void Show() {
        System.out.println("\n--------------------------------Statistic----------------------------------");
        System.out.print("Raw data name:\t");
        for (int i = 0; i < RawDataFile.length; i++) {
            System.out.print(RawDataFile[i].getName() + "\t");
        }
        System.out.println();
        System.out.print("Raw reads number:");
        for (int i = 0; i < RawDataReadsNum.length; i++) {
            System.out.print("\t" + new DecimalFormat("#,###").format(RawDataReadsNum[i]));
        }
        System.out.println();
        System.out.println("\n-----------------------------------------\nLinkers type\tReads number\tPercent");
        for (int i = 0; i < LinkersType.size(); i++) {
            System.out.println(LinkersType.get(i) + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)) + "\t" + (double) LinkersNum.get(i) / Statistic.Average(RawDataReadsNum) * 100 + "%");
        }
        System.out.println("\n-----------------------------------------\nFastq file\tReads number\tFastq file\tReads number");
        for (int i = 0; i < FastqR1File.size(); i++) {
            System.out.println(FastqR1File.get(i).getName() + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)) + "\t" + FastqR2File.get(i).getName() + "\t" + new DecimalFormat("#,###").format(LinkersNum.get(i)));
        }
        System.out.println("\n-----------------------------------------\nBed file\tUniq reads number\tPercent\tBed file\tUniq reads number\tPercent");
        for (int i = 0; i < UseBed1.size(); i++) {
            System.out.println(UseBed1.get(i).getName() + "\t" + new DecimalFormat("#,###").format(UniqMapR1Num.get(i)) + "\t" + (double) UniqMapR1Num.get(i) / LinkersNum.get(i) * 100 + "%" + "\t" + UseBed2.get(i).getName() + "\t" + new DecimalFormat("#,###").format(UniqMapR2Num.get(i)) + "\t" + (double) UniqMapR2Num.get(i) / LinkersNum.get(i) * 100 + "%");
        }
        long sum = 0;
        System.out.println("\n-----------------------------------------\nBedpe file\tReads number\tPercent");
        for (int i = 0; i < LigationFile.size(); i++) {
            sum = sum + BedpeNum.get(i);
            System.out.println("UniqMap\t" + new DecimalFormat("#,###").format(BedpeNum.get(i)));
            for (int j = 0; j < LigationFile.get(i).length; j++) {
                System.out.println(LigationFile.get(i)[j].replaceAll(".*/", "") + "\t" + new DecimalFormat("#,###").format(LigationNum.get(i)[j]) + "\t" + (double) LigationNum.get(i)[j] / BedpeNum.get(i) * 100 + "%");
            }
            System.out.println(NoRmdupName.get(i).replaceAll(".*/", "") + "\t" + new DecimalFormat("#,###").format(NoRmdupNum.get(i)) + "\t" + (double) NoRmdupNum.get(i) / BedpeNum.get(i) * 100 + "%");
        }
        System.out.println("\n-------------------------------------------------------------");
        System.out.println("Total action number:\t" + new DecimalFormat("#,###").format(FinalBedpeNum) + "\t" + (double) FinalBedpeNum / Statistic.Average(RawDataReadsNum) * 100 + "%");
        System.out.println("Intra action number:\t" + new DecimalFormat("#,###").format(IntraActionNum) + "\t" + (double) IntraActionNum / FinalBedpeNum * 100 + "%");
        System.out.println("Inter action number:\t" + new DecimalFormat("#,###").format(InterActionNum) + "\t" + (double) InterActionNum / FinalBedpeNum * 100 + "%");
        System.out.println("\n-------------------------------------------------------------");
        if (RestrictionSeq.replace("^", "").length() <= 4) {
            System.out.println("Short region <= 5k :\t" + new DecimalFormat("#,###").format(ShortRegionNum) + "\t" + (double) ShortRegionNum / IntraActionNum * 100 + "%");
            System.out.println("Long region > 5k :\t" + new DecimalFormat("#,###").format(LongRegionNum) + "\t" + (double) LongRegionNum / IntraActionNum * 100 + "%");
        } else {
            System.out.println("Short region <= 20k :\t" + new DecimalFormat("#,###").format(ShortRegionNum) + "\t" + (double) ShortRegionNum / IntraActionNum * 100 + "%");
            System.out.println("Long region > 20k :\t" + new DecimalFormat("#,###").format(LongRegionNum) + "\t" + (double) LongRegionNum / IntraActionNum * 100 + "%");
        }
//        System.out.println("No duplication:\t" + NoRmdupNum * 100 + "%");
//        System.out.println("Ligation type\tPercent");
//        for (int i = 0; i < LigationFile.size(); i++) {
//            System.out.println(LigationFile.get(i) + "\t" + LigationNum.get(i) * 100 + "%");
//        }
//        System.out.println("Inter action:\t" + InterActionNum * 100 + "%");
//        System.out.println("Intra action:\t" + IntraActionNum * 100 + "%");
    }

    public void ReportHtml(String outfile) throws IOException {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix("resource/");
        resolver.setSuffix(".html");
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        Context context = new Context();
        context.setVariable("RawDataFile", RawDataFile);
        context.setVariable("LinkerFile", LinkerFile);
        context.setVariable("RefGenome", GenomeFile);
        context.setVariable("GenomeIndex", GenomeIndex);
        context.setVariable("AdapterFile", AdapterFile);
        context.setVariable("OutPath", OutPath);
        context.setVariable("OutPrefix", OutPrefix);
        context.setVariable("Restriction", RestrictionSeq);
        context.setVariable("LinkerType", String.join(" ", LinkersType.toArray(new String[LinkersType.size()])));
        context.setVariable("UseLink", String.join(" ", UseLinker.toArray(new String[UseLinker.size()])));
        context.setVariable("Chromosome", String.join(" ", Chromosome.toArray(new String[Chromosome.size()])));
        context.setVariable("MinAlignQ", MinAlignQuality);
        context.setVariable("MinReadsLen", MinReadsLength);
        context.setVariable("MaxReadsLen", MaxReadsLength);
        context.setVariable("Resolution", Resolution);
        context.setVariable("Thread", Thread);

        String html = templateEngine.process("Template", context);
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        out.write(html);
        out.close();
    }

}
