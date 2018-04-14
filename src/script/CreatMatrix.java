package script;

import lib.tool.PrintMatrix;
import lib.tool.Statistic;
import lib.tool.Tools;
import lib.unit.*;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

public class CreatMatrix {
    private String BedpeFile;
    private String[] Chromosome;
    private int[] ChrSize;
    private int Resolution;
    private String Prefix;
    private int Thread;

    CreatMatrix(String BedpeFile, String[] Chromosome, int[] ChrSize, int Resolution, String Prefix, int Thread) throws IOException {
        this.BedpeFile = BedpeFile;
        this.Chromosome = Chromosome;
        this.ChrSize = ChrSize;
        this.Resolution = Resolution;
        this.Prefix = Prefix;
        this.Thread = Thread;
    }

    public static void main(String[] args) throws IOException {
        Options Arguement = new Options();
//        Arguement.addOption("h", "help", false, "print help message");
        Arguement.addOption(Option.builder("f").hasArg().argName("file").desc("[required] bedpefile").build());
        Arguement.addOption(Option.builder("c").longOpt("chr").hasArgs().argName("strings").desc("[required, need't when region has set] the chromosome name that you want to calculator").build());
        Arguement.addOption(Option.builder("s").longOpt("size").hasArgs().argName("ints").desc("[required, need't when region has set] the chromosome size that you want to calculator").build());
        Arguement.addOption(Option.builder("r").longOpt("res").hasArg().argName("int").desc("resolution (default 1M)").build());
        Arguement.addOption(Option.builder("region").hasArgs().argName("strings").desc("(sample chr1:0:100 chr4:100:400) region you want to calculator, if not set, will calculator chromosome size").build());
        Arguement.addOption("t", true, "thread (default 1)");
        Arguement.addOption("p", true, "out prefix (default bedpefile)");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreatMatrix [option]", Arguement);
            System.exit(1);
        }
        try {
            CommandLine line = new DefaultParser().parse(Arguement, args);
            String BedpeFile = line.getOptionValue("f");
            String[] Chromosome = line.hasOption("chr") ? line.getOptionValues("chr") : null;
            int[] ChrSize = line.hasOption("size") ? StringArrays.toInteger(line.getOptionValues("size")) : null;
            int Resolution = line.hasOption("res") ? Integer.parseInt(line.getOptionValue("res")) : 1000000;
            String Prefix = line.hasOption("p") ? line.getOptionValue("p") : BedpeFile;
            int Thread = line.hasOption("t") ? Integer.parseInt(line.getOptionValue("t")) : 1;
            ChrRegion Chr1 = line.hasOption("region") ? new ChrRegion(line.getOptionValue("region").split(":")) : null;
            ChrRegion Chr2 = line.hasOption("region") && line.getOptionValues("region").length > 1 ? new ChrRegion(line.getOptionValues("region")[1].split(":")) : Chr1;
            if (Chr1 == null) {
                new CreatMatrix(BedpeFile, Chromosome, ChrSize, Resolution, Prefix, Thread).Run();
            } else {
                new CreatMatrix(BedpeFile, Chromosome, ChrSize, Resolution, Prefix, Thread).Run(Chr1, Chr2);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreatMatrix [option]", Arguement);
            System.exit(1);
        }
    }

    public void Run() throws IOException {
        if (Chromosome == null || ChrSize == null) {
            System.err.println("Error! no -chr and -size arguement");
            System.exit(0);
        }
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile);
        int[] ChrBinSize;
        int SumBin = 0;
        //计算bin的总数
        ChrBinSize = Statistic.CalculatorBinSize(ChrSize, Resolution);
        for (int i = 0; i < ChrBinSize.length; i++) {
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > 50000) {
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
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                        while ((line = infile.readLine()) != null) {
                            str = line.split("\\s+");
                            int hang = Integer.parseInt(str[1]) / Resolution;
                            int lie = Integer.parseInt(str[3]) / Resolution;
                            for (int j = 0; j < Chromosome.length; j++) {
                                if (str[0].equals(Chromosome[j])) {
                                    break;
                                }
                                hang = hang + ChrBinSize[j];
                            }
                            if (hang >= finalSumBin) {
                                continue;
                            }
                            for (int j = 0; j < Chromosome.length; j++) {
                                if (str[2].equals(Chromosome[j])) {
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
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
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

    public void Run(ChrRegion chr1, ChrRegion chr2) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile);
        int[] ChrBinSize;
        ChrBinSize = Statistic.CalculatorBinSize(new int[]{chr1.Terminal - chr1.Begin, chr2.Terminal - chr2.Begin}, Resolution);
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
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                        if (Tools.BedpeDetect(BedpeFile) == 1) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                ChrRegion left = new ChrRegion(new String[]{str[0], str[1], str[1]});
                                ChrRegion right = new ChrRegion(new String[]{str[2], str[3], str[3]});
                                if (left.IsBelong(chr1) && right.IsBelong(chr2)) {
                                    int hang = (left.Begin - chr1.Begin) / Resolution + 1;
                                    int lie = (right.Begin - chr2.Begin) / Resolution + 1;
                                    synchronized (Process) {
                                        InterMatrix[hang - 1][lie - 1]++;
                                    }
                                }
                            }
                        } else if (Tools.BedpeDetect(BedpeFile) == 2) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                ChrRegion left = new ChrRegion(new String[]{str[0], str[1], str[2]});
                                ChrRegion right = new ChrRegion(new String[]{str[3], str[4], str[5]});
                                if (left.IsBelong(chr1) && right.IsBelong(chr2)) {
                                    int hang = ((left.Begin + left.Terminal) / 2 - chr1.Begin) / Resolution + 1;
                                    int lie = ((right.Begin + right.Terminal) / 2 - chr2.Begin) / Resolution + 1;
                                    synchronized (Process) {
                                        InterMatrix[hang - 1][lie - 1]++;
                                    }
                                }
                            }
                        } else {
                            System.err.println("Error foramt!");
                            System.exit(1);
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
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
    }
}
