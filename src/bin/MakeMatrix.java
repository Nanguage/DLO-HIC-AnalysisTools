package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import lib.tool.*;

public class MakeMatrix {
    private String OptOutPath = "OutPath";
    private String OptPrefix = "Prefix";
    private String OptInterBedpeFile = "InterBedpeFile";
    private String OptChromosome = "Chromosome";
    public String OptThread = "Thread";
    private String OptResolution = "Resolution";
    private String OptChrSzieFile = "ChrSzieFile";
    private String OptChromosomeSize = "ChrSize";
    //===============================================================
    private String OutPath;//输出路径
    private String Prefix;//输出前缀
    private String InterBedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    public int Thread;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private String ChrSzieFile;
    private String[] ChrInterBedpeFile;
    //==============================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptInterBedpeFile, OptResolution};
    private String[] OptionalParameter = new String[]{OptChromosome, OptOutPath, OptPrefix, OptThread, OptChrSzieFile};


    public MakeMatrix(String outpath, String outprefix, String validpairs, String[] chrosomose, int[] chrsize, int resolution) throws IOException {
        ParameterInit();
        ParameterList.put(OptOutPath, outpath);
        ParameterList.put(OptPrefix, outprefix);
        ParameterList.put(OptInterBedpeFile, validpairs);
        String temp = "";
        if (chrosomose.length > 0) {
            temp = chrosomose[0];
            for (int i = 1; i < chrosomose.length; i++) {
                temp = temp + " " + chrosomose[i];
            }
        }
        ParameterList.put(OptChromosome, temp);
        temp = "";
        if (chrsize.length > 0) {
            temp = String.valueOf(chrsize[0]);
            for (int i = 1; i < chrsize.length; i++) {
                temp = temp + " " + chrsize[i];
            }
        }
        ParameterList.put(OptChromosomeSize, temp);
        ParameterList.put(OptResolution, String.valueOf(resolution));
        Init();
    }

    MakeMatrix(String ConfigFile) throws IOException {
        ParameterInit();
        GetOption(ConfigFile);
        Init();
    }

    public void Run() throws IOException {
//        Routine P = new Routine();
//        P.Threads = Thread;
        int[][] Matrix = CreatInterActionMatrix(InterBedpeFile, Chromosome, ChromosomeSize, Resolution, InterMatrixPrefix);
        double[][] NormalizeMatrix = MatrixNormalize(Matrix);
        Tools.PrintMatrix(NormalizeMatrix, NormalizeMatrixPrefix + ".2d.matrix", NormalizeMatrixPrefix + ".spare.matrix");
        ChrInterBedpeFile = SeparateInterBedpe(InterBedpeFile, Chromosome, OutPath + "/" + Prefix, "");
        for (int i = 0; i < Chromosome.length; i++) {
            String ChrInterMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".inter";
            String ChrNormalizeMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".normalize";
            Matrix = CreatInterActionMatrix(ChrInterBedpeFile[i], new String[]{Chromosome[i]}, new int[]{ChromosomeSize[i]}, Resolution / 10, ChrInterMatrixPrefix);
            NormalizeMatrix = MatrixNormalize(Matrix);
            Tools.PrintMatrix(NormalizeMatrix, ChrNormalizeMatrixPrefix + ".2d.matrix", ChrNormalizeMatrixPrefix + ".spare.matrix");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp DLO-HIC-AnalysisTools.jar bin.MakeMatrix <Config.txt>");
            System.exit(0);
        }
        MakeMatrix mm = new MakeMatrix(args[0]);
        mm.ShowParameter();
        mm.Run();
    }

    private void GetOption(String optionfile) throws IOException {
        BufferedReader option = new BufferedReader(new FileReader(optionfile));
        String line;
        String[] str;
        while ((line = option.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s*=\\s*|\\s+");
            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
                ParameterList.put(str[0], str[1]);
            }
        }
        option.close();
    }

    public void ParameterInit() {
        for (String opt : RequiredParameter) {
            ParameterList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ParameterList.put(opt, "");
        }
        ParameterList.put(OptOutPath, "./");
        ParameterList.put(OptPrefix, "Matrix");
        ParameterList.put(OptThread, "1");
    }

    public boolean SetParameter(String Key, String Value) {
        if (ParameterList.containsKey(Key)) {
            ParameterList.put(Key, Value);
            return true;
        } else {
            return false;
        }
    }

    public void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
        System.out.println("===============================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
    }

    private void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
        //=======================================================
        OutPath = ParameterList.get(OptOutPath);
        Prefix = ParameterList.get(OptPrefix);
        InterBedpeFile = ParameterList.get(OptInterBedpeFile);
        ChrSzieFile = ParameterList.get(OptChrSzieFile);
        Chromosome = ParameterList.get(OptChromosome).split("\\s+");
        ChromosomeSize = new int[ParameterList.get(OptChromosomeSize).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptChromosomeSize).split("\\s+").length; i++) {
            ChromosomeSize[i] = Integer.parseInt(ParameterList.get(OptChromosomeSize).split("\\s+")[i]);
        }
        Resolution = Integer.parseInt(ParameterList.get(OptResolution));
        Thread = Integer.parseInt(ParameterList.get(OptThread));
        //=======================================================
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdirs()) {
                System.err.println("Can't Creat " + OutPath);
                System.exit(0);
            }
        }
        if (ChrSzieFile.equals("") && (Chromosome.length == 0 || ChromosomeSize.length == 0)) {
            System.err.println("Error ! No Chromosome or ChromosomeSize");
            System.exit(0);
        } else if (Chromosome.length == 0 || ChromosomeSize.length == 0) {
            if (!new File(ChrSzieFile).isFile()) {
                System.err.println("Wrong ChrSzieFile " + ChrSzieFile + "is not a file");
                System.exit(0);
            }
            ExtractChrSize();
        }
        InterMatrixPrefix = OutPath + "/" + Prefix + ".inter";
        NormalizeMatrixPrefix = OutPath + "/" + Prefix + ".normalize";
    }


    public void ExtractChrSize() throws IOException {
        BufferedReader chrsize = new BufferedReader(new FileReader(ChrSzieFile));
        ArrayList<String[]> list = new ArrayList<>();
        String line;
        String[] str;
        while ((line = chrsize.readLine()) != null) {
            str = line.split("\\s+");
            list.add(str);
        }
        Chromosome = new String[list.size()];
        ChromosomeSize = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Chromosome[i] = list.get(i)[0];
            ChromosomeSize[i] = Integer.parseInt(list.get(i)[1]);
        }
    }

    public String[] getChrInterBedpeFile() throws IOException {
        if (ChrInterBedpeFile == null) {
//            Routine P = new Routine();
            ChrInterBedpeFile = SeparateInterBedpe(InterBedpeFile, Chromosome, OutPath + "/" + Prefix, "");
        }
        return ChrInterBedpeFile;
    }

    public String[] SeparateInterBedpe(String InterBedpeFile, String[] Chromosome, String Prefix, String Regex) throws IOException {
        System.out.println(new Date() + "\tSeperate InterBedpe " + InterBedpeFile);
        BufferedReader interfile = new BufferedReader(new FileReader(InterBedpeFile));
        String[] SameChrFile = new String[Chromosome.length];
        BufferedWriter[] chrwrite = new BufferedWriter[Chromosome.length];
        String DiffFile = Prefix + ".diff.bedpe";
        BufferedWriter diffwrite = new BufferedWriter(new FileWriter(DiffFile));
        //------------------------------------------------------------
        Hashtable<String, Integer> ChrIndex = new Hashtable<>();
        for (int i = 0; i < Chromosome.length; i++) {
            ChrIndex.put(Chromosome[i], i);
            SameChrFile[i] = Prefix + "." + Chromosome[i] + ".same.bedpe";
            chrwrite[i] = new BufferedWriter(new FileWriter(SameChrFile[i]));
        }
        if (Regex.isEmpty()) {
            Regex = "\\s+";
        }
        //================================================================
        Thread[] Process = new Thread[Thread];
        for (int i = 0; i < Thread; i++) {
            String finalRegex = Regex;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
                        while ((line = interfile.readLine()) != null) {
                            str = line.split(finalRegex);
                            if (str[0].equals(str[2])) {
                                synchronized (Process) {
                                    chrwrite[ChrIndex.get(str[0])].write(line + "\n");
                                }
                            } else {
                                synchronized (Process) {
                                    diffwrite.write(line + "\n");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        for (int i = 0; i < Thread; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < Chromosome.length; i++) {
            chrwrite[i].close();
        }
        diffwrite.close();
        System.out.println(new Date() + "\tEnd seperate InterBedpe " + InterBedpeFile);
        return SameChrFile;
    }

    public int[][] CreatInterActionMatrix(String InterBedpeFile, String[] Chromosome, int[] ChrSize, int Resolution, String Prefix) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + InterBedpeFile);
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
        BufferedReader infile = new BufferedReader(new FileReader(InterBedpeFile));
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
        return InterMatrix;
    }//OK

    public double[][] MatrixNormalize(int[][] Matrix) {
        System.out.println(new Date() + "\tNormalize Matrix");
        double[][] NormalizeMatrix = new double[Matrix.length][Matrix.length];//定义标准化矩阵
        double[][] Distance = new double[3][Matrix.length];//定义距离数组
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = i; j < Matrix.length; j++) {
                Distance[0][j - i]++;//计算相同距离的交互点的个数
                Distance[1][j - i] += Matrix[i][j];//计算相同距离的交互点的总数
            }
        }
        for (int i = 0; i < Matrix.length; i++) {
            Distance[2][i] = Distance[1][i] / Distance[0][i];//计算平均交互数
        }
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix.length; j++) {
                if (Distance[2][Math.abs(i - j)] == 0) {
                    NormalizeMatrix[i][j] = 0;//如果某个距离平均交互数为0，则直接将标准化矩阵对应点设成0
                } else {
                    NormalizeMatrix[i][j] = Matrix[i][j] / Distance[2][Math.abs(i - j)];//用对应距离的交互值除以对应的平均交互值
                }
            }
        }
        System.out.println(new Date() + "\tNormalize Matrix end");
        return NormalizeMatrix;//返回标准化后的矩阵
    }
}
