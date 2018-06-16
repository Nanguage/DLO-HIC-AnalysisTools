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
    private Chromosome[] Chromosome;
    //    private String[] Chromosome;
//    private int[] ChrSize;
    private int Resolution;
    private String Prefix;
    private int Thread;

    public CreateMatrix(CustomFile BedpeFile, Chromosome[] Chr, int Resolution, String Prefix, int Thread) throws IOException {
        this.BedpeFile = BedpeFile;
        Chromosome = Chr;
//        this.Chromosome = Chromosome;
//        this.ChrSize = ChrSize;
        this.Resolution = Resolution;
        this.Prefix = Prefix;
        this.Thread = Thread;
    }

    public static void main(String[] args) throws IOException {
        Options Argument = new Options();
        Argument.addOption(Option.builder("f").hasArg().argName("file").desc("[required] bedpefile").build());
        Argument.addOption(Option.builder("s").hasArg().argName("file").longOpt("size").desc("Chromosome size file").build());
        Argument.addOption(Option.builder("chr").hasArgs().argName("strings").desc("[required, need't when region has set] the chromosome name that you want to calculator").build());
//        Argument.addOption(Option.builder("size").hasArgs().argName("ints").desc("[required, need't when region has set] the chromosome size that you want to calculator").build());
        Argument.addOption(Option.builder("res").hasArg().argName("int").desc("resolution (default 1M)").build());
        Argument.addOption(Option.builder("region").hasArgs().argName("strings").desc("(sample chr1:0:100 chr4:100:400) region you want to calculator, if not set, will calculator chromosome size").build());
        Argument.addOption("t", true, "thread (default 1)");
        Argument.addOption("p", true, "out prefix (default bedpefile)");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreateMatrix [option]", Argument);
            System.exit(1);
        }
        CommandLine ComLine = null;
        try {
            ComLine = new DefaultParser().parse(Argument, args);
        } catch (ParseException e) {
            e.printStackTrace();
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreateMatrix [option]", Argument);
            System.exit(1);
        }
        CustomFile BedpeFile = new CustomFile(ComLine.getOptionValue("f"));
        String[] Chr = ComLine.hasOption("chr") ? ComLine.getOptionValues("chr") : null;
        Chromosome[] Chromosome = null;
        if (Chr != null) {
            Chromosome = new Chromosome[Chr.length];
            for (int i = 0; i < Chr.length; i++) {
                Chromosome[i] = new Chromosome(Chr[i].split(":"));
            }
        }
        String SizeFile = ComLine.hasOption("size") ? ComLine.getOptionValue("size") : null;
        int Resolution = ComLine.hasOption("res") ? Integer.parseInt(ComLine.getOptionValue("res")) : 1000000;
        String Prefix = ComLine.hasOption("p") ? ComLine.getOptionValue("p") : BedpeFile.getPath();
        int Thread = ComLine.hasOption("t") ? Integer.parseInt(ComLine.getOptionValue("t")) : 1;
        ChrRegion Reg1 = ComLine.hasOption("region") ? new ChrRegion(ComLine.getOptionValue("region").split(":")) : null;
        ChrRegion Reg2 = ComLine.hasOption("region") && ComLine.getOptionValues("region").length > 1 ? new ChrRegion(ComLine.getOptionValues("region")[1].split(":")) : Reg1;
        if (SizeFile != null) {
            List<String> ChrSizeList = FileUtils.readLines(new File(SizeFile), Charsets.UTF_8);
            if (Chromosome == null) {
                Chromosome = new Chromosome[ChrSizeList.size()];
                for (int i = 0; i < Chromosome.length; i++) {
                    Chromosome[i] = new Chromosome(ChrSizeList.get(i).split("\\s+"));
                }
            } else {
                for (String aChrSizeList : ChrSizeList) {
                    for (lib.unit.Chromosome aChromosome : Chromosome) {
                        if (aChromosome.Name.equals(aChrSizeList.split("\\s+")[0])) {
                            aChromosome.Size = Integer.parseInt(aChrSizeList.split("\\s+")[1]);
                            break;
                        }
                    }
                }
            }
        }
        if (Reg1 == null) {
            new CreateMatrix(BedpeFile, Chromosome, Resolution, Prefix, Thread).Run();
        } else {
            new CreateMatrix(BedpeFile, Chromosome, Resolution, Prefix, Thread).Run(Reg1, Reg2);
        }
    }

    public void Run() throws IOException {
        if (Chromosome == null) {
            System.err.println("Error! no -chr  argument");
            System.exit(1);
        }
        int[] ChrSize = new int[Chromosome.length];
        System.out.print(new Date() + "\tBegin to creat interaction matrix ");
        for (int i = 0; i < Chromosome.length; i++) {
            System.out.print(Chromosome[i].toString() + " ");
            ChrSize[i] = Chromosome[i].Size;
        }
        System.out.println();
        int SumBin = 0;
        int[] ChrBinSize = Statistic.CalculatorBinSize(ChrSize, Resolution);
        //计算bin的总数
        for (int i = 0; i < ChrBinSize.length; i++) {
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > Opts.MaxBinNum) {
            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
            System.exit(0);
        }
        int[][] InterMatrix = new int[SumBin][SumBin];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        Thread[] Process = new Thread[Thread];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Thread; i++) {
            int finalSumBin = SumBin;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
                        if (BedpeFile.BedpeDetect() == Opts.BedpePointFormat) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                int hang = Integer.parseInt(str[1]) / Resolution;
                                int lie = Integer.parseInt(str[3]) / Resolution;
                                for (int j = 0; j < Chromosome.length; j++) {
                                    if (str[0].equals(Chromosome[j].Name)) {
                                        break;
                                    }
                                    hang = hang + ChrBinSize[j];
                                }
                                if (hang >= finalSumBin) {
                                    continue;
                                }
                                for (int j = 0; j < Chromosome.length; j++) {
                                    if (str[2].equals(Chromosome[j].Name)) {
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
                        } else if (BedpeFile.BedpeDetect() == Opts.BedpeRegionFormat) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                int hang = (Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2 / Resolution;
                                int lie = (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2 / Resolution;
                                for (int j = 0; j < Chromosome.length; j++) {
                                    if (str[0].equals(Chromosome[j].Name)) {
                                        break;
                                    }
                                    hang = hang + ChrBinSize[j];
                                }
                                if (hang >= finalSumBin) {
                                    continue;
                                }
                                for (int j = 0; j < Chromosome.length; j++) {
                                    if (str[3].equals(Chromosome[j].Name)) {
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
                        } else {
                            System.err.println("Error format!");
//                            System.exit(1);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        for (int i = 0; i < Thread; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Tools.PrintMatrix(InterMatrix, Prefix + ".2d.matrix", Prefix + ".spare.matrix");
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        int temp = 0;
        BufferedWriter outfile = new BufferedWriter(new FileWriter(Prefix + ".matrix.BinSize"));
        for (int i = 0; i < Chromosome.length; i++) {
            temp = temp + 1;
            outfile.write(Chromosome[i] + "\t" + temp + "\t");
            temp = temp + ChrBinSize[i] - 1;
            outfile.write(temp + "\n");
        }
        outfile.close();
    }//OK

    public void Run(ChrRegion reg1, ChrRegion reg2) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + reg1.toString().replace("\t", ":") + " " + reg2.toString().replace("\t", ":"));
        int[] ChrBinSize;
        ChrBinSize = Statistic.CalculatorBinSize(new int[]{reg1.Length, reg2.Length}, Resolution);
        if (Math.max(ChrBinSize[0], ChrBinSize[1]) > 50000) {
            System.err.println("Error ! too many bins, there are " + Math.max(ChrBinSize[0], ChrBinSize[1]) + " bins.");
            System.exit(0);
        }
        int[][] InterMatrix = new int[ChrBinSize[0]][ChrBinSize[1]];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        Thread[] Process = new Thread[Thread];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Thread; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " start");
                        if (BedpeFile.BedpeDetect() == Opts.BedpePointFormat) {
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
                        } else if (BedpeFile.BedpeDetect() == Opts.BedpeRegionFormat) {
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
                        } else {
                            System.err.println("Error foramt!");
//                            System.exit(1);
                        }
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        for (int i = 0; i < Thread; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        Tools.PrintMatrix(InterMatrix, Prefix + ".2d.matrix", Prefix + ".spare.matrix");
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        BufferedWriter outfile = new BufferedWriter(new FileWriter(Prefix + ".matrix.Region"));
        outfile.write(reg1.toString() + "\n");
        outfile.write(reg2.toString() + "\n");
        outfile.close();
    }
}
