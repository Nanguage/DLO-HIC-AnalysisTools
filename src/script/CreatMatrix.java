package script;

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
        Options Argument = new Options();
        Argument.addOption(Option.builder("f").hasArg().argName("file").desc("[required] bedpefile").build());
        Argument.addOption(Option.builder("chr").hasArgs().argName("strings").desc("[required, need't when region has set] the chromosome name that you want to calculator").build());
        Argument.addOption(Option.builder("size").hasArgs().argName("ints").desc("[required, need't when region has set] the chromosome size that you want to calculator").build());
        Argument.addOption(Option.builder("res").hasArg().argName("int").desc("resolution (default 1M)").build());
        Argument.addOption(Option.builder("region").hasArgs().argName("strings").desc("(sample chr1:0:100 chr4:100:400) region you want to calculator, if not set, will calculator chromosome size").build());
        Argument.addOption("t", true, "thread (default 1)");
        Argument.addOption("p", true, "out prefix (default bedpefile)");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreatMatrix [option]", Argument);
            System.exit(1);
        }
        try {
            CommandLine line = new DefaultParser().parse(Argument, args);
            String BedpeFile = line.getOptionValue("f");
            String[] Chromosome = line.hasOption("chr") ? line.getOptionValues("chr") : null;
            int[] ChrSize = line.hasOption("size") ? StringArrays.toInteger(line.getOptionValues("size")) : null;
            int Resolution = line.hasOption("res") ? Integer.parseInt(line.getOptionValue("res")) : 1000000;
            String Prefix = line.hasOption("p") ? line.getOptionValue("p") : BedpeFile;
            int Thread = line.hasOption("t") ? Integer.parseInt(line.getOptionValue("t")) : 1;
            ChrRegion Reg1 = line.hasOption("region") ? new ChrRegion(line.getOptionValue("region").split(":")) : null;
            ChrRegion Reg2 = line.hasOption("region") && line.getOptionValues("region").length > 1 ? new ChrRegion(line.getOptionValues("region")[1].split(":")) : Reg1;
            if (Reg1 == null) {
                new CreatMatrix(BedpeFile, Chromosome, ChrSize, Resolution, Prefix, Thread).Run();
            } else {
                new CreatMatrix(BedpeFile, Chromosome, ChrSize, Resolution, Prefix, Thread).Run(Reg1, Reg2);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.CreatMatrix [option]", Argument);
            System.exit(1);
        }
    }

    public void Run() throws IOException {
        if (Chromosome == null || ChrSize == null) {
            System.err.println("Error! no -chr and -size arguement");
            System.exit(1);
        }
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile);
        int SumBin = 0;
        int[] ChrBinSize = Statistic.CalculatorBinSize(ChrSize, Resolution);
        //计算bin的总数
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
                        if (Tools.BedpeDetect(BedpeFile) == 1) {
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
                        } else if (Tools.BedpeDetect(BedpeFile) == 2) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                int hang = (Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2 / Resolution;
                                int lie = (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2 / Resolution;
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
                                    if (str[3].equals(Chromosome[j])) {
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
                            System.exit(1);
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
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + BedpeFile);
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
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                        if (Tools.BedpeDetect(BedpeFile) == 1) {
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
                        } else if (Tools.BedpeDetect(BedpeFile) == 2) {
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
        BufferedWriter outfile = new BufferedWriter(new FileWriter(Prefix + ".matrix.Region"));
        outfile.write(reg1.toString() + "\n");
        outfile.write(reg2.toString() + "\n");
        outfile.close();
    }
}
