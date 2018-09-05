package script;

import kotlin.text.Charsets;
import lib.tool.Statistic;
import lib.tool.Tools;
import lib.unit.*;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

public class CreateMatrix {
    private CustomFile BedpeFile;
    private Chromosome[] Chromosomes;
    private ChrRegion Region1;
    private ChrRegion Region2;
    private int Resolution;
    private String Prefix;
    private File TwoDMatrixFile;
    private File SpareMatrixFile;
    private File RegionFile;
    private File BinSizeFile;
    private int Threads;
    private float Version = 1.0f;

    public CreateMatrix(CustomFile BedpeFile, Chromosome[] Chrs, int Resolution, String Prefix, int Threads) {
        this.BedpeFile = BedpeFile;
        this.Chromosomes = Chrs;
        this.Resolution = Resolution;
        this.Prefix = Prefix;
        this.Threads = Threads;
        Init();
    }

    private CreateMatrix(String[] args) throws IOException {
        Options Argument = new Options();
        Argument.addOption(Option.builder("f").hasArg().argName("file").required().desc("[required] bedpefile").build());
        Argument.addOption(Option.builder("s").hasArg().longOpt("size").argName("file").desc("Chromosomes size file").build());
        Argument.addOption(Option.builder("chr").hasArgs().argName("strings").desc("The chromosome name which you want to calculator").build());
        Argument.addOption(Option.builder("res").hasArg().argName("int").desc("Resolution (default 1M)").build());
        Argument.addOption(Option.builder("region").hasArgs().argName("strings").desc("(sample chr1:0:100 chr4:100:400) region you want to calculator, if not set, will calculator chromosome size").build());
        Argument.addOption(Option.builder("t").hasArg().argName("int").desc("Threads (default 1)").build());
        Argument.addOption(Option.builder("p").hasArg().argName("string").desc("out prefix (default bedpefile)").build());
        final String helpHeader = "Version: " + Version + "\nAuthor: " + Opts.Author;
        final String helpFooter = "Note:\n" +
                "you can set -chr like \"Chr:ChrSize\" or use -s to define the \"ChrSize\"\n" +
                "If you set -s, you can set -chr like \"Chr\"\n" +
                "The file format of option -s is \"Chromosomes    Size\" for each row\n" +
                "We will calculate all chromosome in Chromosomes size file if you don't set -chr\n" +
                "You needn't set -s and -chr if you set -region";
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp Path/" + Opts.JarFile.getName() + " " + CreateMatrix.class.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        CommandLine ComLine = null;
        try {
            ComLine = new DefaultParser().parse(Argument, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp("java -cp Path/" + Opts.JarFile.getName() + " " + CreateMatrix.class.getName(), helpHeader, Argument, helpFooter, true);
            System.exit(1);
        }
        BedpeFile = new CustomFile(ComLine.getOptionValue("f"));
        String[] Chr = ComLine.hasOption("chr") ? ComLine.getOptionValues("chr") : null;
        if (Chr != null) {
            Chromosomes = new Chromosome[Chr.length];
            for (int i = 0; i < Chr.length; i++) {
                Chromosomes[i] = new Chromosome(Chr[i].split(":"));
            }
        }
        String SizeFile = ComLine.hasOption("size") ? ComLine.getOptionValue("size") : null;
        Resolution = ComLine.hasOption("res") ? Integer.parseInt(ComLine.getOptionValue("res")) : Default.Resolution;
        Prefix = ComLine.hasOption("p") ? ComLine.getOptionValue("p") : BedpeFile.getPath();
        Threads = ComLine.hasOption("t") ? Integer.parseInt(ComLine.getOptionValue("t")) : 1;
        Region1 = ComLine.hasOption("region") ? new ChrRegion(ComLine.getOptionValue("region").split(":")) : null;
        Region2 = ComLine.hasOption("region") && ComLine.getOptionValues("region").length > 1 ? new ChrRegion(ComLine.getOptionValues("region")[1].split(":")) : Region1;
        if (SizeFile != null) {
            List<String> ChrSizeList = FileUtils.readLines(new File(SizeFile), Charsets.UTF_8);
            if (Chromosomes == null) {
                Chromosomes = new Chromosome[ChrSizeList.size()];
                for (int i = 0; i < Chromosomes.length; i++) {
                    Chromosomes[i] = new Chromosome(ChrSizeList.get(i).split("\\s+"));
                }
            } else {
                for (String aChrSizeList : ChrSizeList) {
                    for (Chromosome aChromosome : Chromosomes) {
                        if (aChromosome.Name.equals(aChrSizeList.split("\\s+")[0])) {
                            aChromosome.Size = Integer.parseInt(aChrSizeList.split("\\s+")[1]);
                            break;
                        }
                    }
                }
            }
        }
        Init();
    }

    private void Init() {
        if (Chromosomes == null) {
            System.err.println("Error! no -chr  argument");
            System.exit(1);
        }
        TwoDMatrixFile = new File(Prefix + ".2d.matrix");
        SpareMatrixFile = new File(Prefix + ".spare.matrix");
        RegionFile = new File(Prefix + ".matrix.Region");
        BinSizeFile = new File(Prefix + ".matrix.BinSize");
    }

    public static void main(String[] args) throws IOException {

        new CreateMatrix(args).Run();

    }

    public Integer[][] Run() throws IOException {
        if (Region1 != null) {
            return Run(Region1, Region2);
        }
        int[] ChrSize = new int[Chromosomes.length];
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile.getName() + " Resolution=" + Resolution + " Threads=" + Threads);
        for (int i = 0; i < Chromosomes.length; i++) {
//            System.out.print(Chromosomes[i].Name + " ");
            ChrSize[i] = Chromosomes[i].Size;
        }
//        System.out.println();
        int SumBin = 0;
        int[] ChrBinSize = Statistic.CalculatorBinSize(ChrSize, Resolution);
        //计算bin的总数
        for (int i = 0; i < ChrBinSize.length; i++) {
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > Opts.MaxBinNum) {
            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
            System.exit(1);
        }
        Integer[][] InterMatrix = new Integer[SumBin][SumBin];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        Thread[] Process = new Thread[Threads];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            int finalSumBin = SumBin;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        switch (BedpeFile.BedpeDetect()) {
                            case BedpePointFormat:
//                                System.out.println("1");
                                while ((line = infile.readLine()) != null) {
                                    str = line.split("\\s+");
                                    int hang = Integer.parseInt(str[1]) / Resolution;
                                    int lie = Integer.parseInt(str[3]) / Resolution;
                                    for (int j = 0; j < Chromosomes.length; j++) {
                                        if (str[0].equals(Chromosomes[j].Name)) {
                                            break;
                                        }
                                        hang = hang + ChrBinSize[j];
                                    }
                                    if (hang >= finalSumBin) {
                                        continue;
                                    }
                                    for (int j = 0; j < Chromosomes.length; j++) {
                                        if (str[2].equals(Chromosomes[j].Name)) {
                                            break;
                                        }
                                        lie = lie + ChrBinSize[j];
                                    }
                                    if (lie >= finalSumBin) {
                                        continue;
                                    }
                                    synchronized (Process) {
                                        InterMatrix[hang][lie]++;
                                        if (hang != lie) {
                                            InterMatrix[lie][hang]++;
                                        }
                                    }
                                }
                                break;
                            case BedpeRegionFormat:
//                                System.out.println("2");
                                while ((line = infile.readLine()) != null) {
                                    str = line.split("\\s+");
                                    int hang = (Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2 / Resolution;
                                    int lie = (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2 / Resolution;
                                    for (int j = 0; j < Chromosomes.length; j++) {
                                        if (str[0].equals(Chromosomes[j].Name)) {
                                            break;
                                        }
                                        hang = hang + ChrBinSize[j];
                                    }
                                    if (hang >= finalSumBin) {
                                        continue;
                                    }
                                    for (int j = 0; j < Chromosomes.length; j++) {
                                        if (str[3].equals(Chromosomes[j].Name)) {
                                            break;
                                        }
                                        lie = lie + ChrBinSize[j];
                                    }
                                    if (lie >= finalSumBin) {
                                        continue;
                                    }
                                    synchronized (Process) {
                                        InterMatrix[hang][lie]++;
                                        if (hang != lie) {
                                            InterMatrix[lie][hang]++;
                                        }
                                    }
                                }
                                break;
                            default:
                                System.err.println("Error format!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Tools.PrintMatrix(InterMatrix, TwoDMatrixFile, SpareMatrixFile);
        System.out.println(new Date() + "\tEnd to create interaction matrix");
        //--------------------------------------------------------------------
        int temp = 0;
        BufferedWriter outfile = new BufferedWriter(new FileWriter(BinSizeFile));
        for (int i = 0; i < Chromosomes.length; i++) {
            temp = temp + 1;
            outfile.write(Chromosomes[i].Name + "\t" + temp + "\t");
            temp = temp + ChrBinSize[i] - 1;
            outfile.write(temp + "\n");
        }
        outfile.close();
        return InterMatrix;
    }//OK

    public Integer[][] Run(ChrRegion reg1, ChrRegion reg2) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + reg1.toString().replace("\t", ":") + " " + reg2.toString().replace("\t", ":"));
        int[] ChrBinSize;
        ChrBinSize = Statistic.CalculatorBinSize(new int[]{reg1.Length, reg2.Length}, Resolution);
        if (Math.max(ChrBinSize[0], ChrBinSize[1]) > 50000) {
            System.err.println("Error ! too many bins, there are " + Math.max(ChrBinSize[0], ChrBinSize[1]) + " bins.");
            System.exit(0);
        }
        Integer[][] InterMatrix = new Integer[ChrBinSize[0]][ChrBinSize[1]];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        Thread[] Process = new Thread[Threads];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        switch (BedpeFile.BedpeDetect()) {
                            case BedpePointFormat:
                                while ((line = infile.readLine()) != null) {
                                    str = line.split("\\s+");
                                    ChrRegion left = new ChrRegion(new String[]{str[0], str[1], str[1]});
                                    ChrRegion right = new ChrRegion(new String[]{str[2], str[3], str[3]});
                                    if (left.IsBelong(reg1) && right.IsBelong(reg2)) {
                                        int hang = (left.Begin - reg1.Begin) / Resolution + 1;
                                        int lie = (right.Begin - reg2.Begin) / Resolution + 1;
                                        synchronized (Process) {
                                            InterMatrix[hang - 1][lie - 1]++;
                                        }
                                    } else if (right.IsBelong(reg1) && left.IsBelong(reg2)) {
                                        int hang = (right.Begin - reg1.Begin) / Resolution + 1;
                                        int lie = (left.Begin - reg2.Begin) / Resolution + 1;
                                        synchronized (Process) {
                                            InterMatrix[hang - 1][lie - 1]++;
                                        }
                                    }
                                }
                                break;
                            case BedpeRegionFormat:
                                while ((line = infile.readLine()) != null) {
                                    str = line.split("\\s+");
                                    ChrRegion left = new ChrRegion(new String[]{str[0], str[1], str[2]});
                                    ChrRegion right = new ChrRegion(new String[]{str[3], str[4], str[5]});
                                    if (left.IsBelong(reg1) && right.IsBelong(reg2)) {
                                        int hang = ((left.Begin + left.Terminal) / 2 - reg1.Begin) / Resolution + 1;
                                        int lie = ((right.Begin + right.Terminal) / 2 - reg2.Begin) / Resolution + 1;
                                        synchronized (Process) {
                                            InterMatrix[hang - 1][lie - 1]++;
                                        }
                                    } else if (right.IsBelong(reg1) && left.IsBelong(reg2)) {
                                        int hang = ((right.Begin + right.Terminal) / 2 - reg1.Begin) / Resolution + 1;
                                        int lie = ((left.Begin + left.Terminal) / 2 - reg2.Begin) / Resolution + 1;
                                        synchronized (Process) {
                                            InterMatrix[hang - 1][lie - 1]++;
                                        }
                                    }
                                }
                                break;
                            default:
                                System.err.println("Error foramt!");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Tools.PrintMatrix(InterMatrix, TwoDMatrixFile, SpareMatrixFile);
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        BufferedWriter outfile = new BufferedWriter(new FileWriter(RegionFile));
        outfile.write(reg1.toString() + "\n");
        outfile.write(reg2.toString() + "\n");
        outfile.close();
        return InterMatrix;
    }

    public File getSpareMatrixFile() {
        return SpareMatrixFile;
    }

    public File getTwoDMatrixFile() {
        return TwoDMatrixFile;
    }

    public File getBinSizeFile() {
        return BinSizeFile;
    }
}
