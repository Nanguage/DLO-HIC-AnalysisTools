package bin;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;

import lib.tool.*;
import lib.unit.CustomFile;
import lib.unit.Opts;

public class MakeMatrix {
    private String OptOutPath = "OutPath";
    private String OptPrefix = "Prefix";
    private String OptBedpeFile = "BedpeFile";
    private String OptChromosome = "Chromosome";
    public String OptThread = "Threads";
    private String OptResolution = "Resolution";
    private String OptChrSzieFile = "ChrSizeFile";
    private String OptChromosomeSize = "ChrSize";
    //===============================================================
    private File OutPath;//输出路径
    private String Prefix;//输出前缀
    private CustomFile BedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    public int Thread;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private File ChrSizeFile;
    private CustomFile[] ChrInterBedpeFile;
    //==============================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptBedpeFile, OptResolution};
    private String[] OptionalParameter = new String[]{OptChromosome, OptOutPath, OptPrefix, OptThread, OptChrSzieFile};


    public MakeMatrix(File outpath, String outprefix, String validpairs, String[] chrosomose, int[] chrsize, int resolution) throws IOException {
        ParameterInit();
        ParameterList.put(OptOutPath, outpath.getPath());
        ParameterList.put(OptPrefix, outprefix);
        ParameterList.put(OptBedpeFile, validpairs);
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

    public void Run() throws IOException, InterruptedException {
        String ImageDir = "image";
//        String PlotScriptFile = Opts.JarFile.getParent() + "/script/PlotHeatmap.py";
        if (!new File(OutPath + "/" + ImageDir).isDirectory()) {
            new File(OutPath + "/" + ImageDir).mkdir();
        }
        Integer[][] Matrix = CreateInterActionMatrix(BedpeFile, Chromosome, ChromosomeSize, Resolution, InterMatrixPrefix);
        String PlotCom = "python " + Opts.PlotScriptFile + " -m A -i " + InterMatrixPrefix + ".2d.matrix -o " + OutPath + "/" + ImageDir + "/" + Prefix + ".png -r " + Resolution + " -c " + InterMatrixPrefix + ".matrix.BinSize" + " -q 98";
        Opts.CommandOutFile.Append(PlotCom + "\n");
        Tools.ExecuteCommandStr(PlotCom, OutPath + "/" + ImageDir + "/" + Prefix + ".plot.out", OutPath + "/" + ImageDir + "/" + Prefix + ".plot.err");
        Double[][] NormalizeMatrix = MatrixNormalize(Matrix);
        Tools.PrintMatrix(NormalizeMatrix, NormalizeMatrixPrefix + ".2d.matrix", NormalizeMatrixPrefix + ".spare.matrix");
        ChrInterBedpeFile = SeparateInterBedpe(BedpeFile, Chromosome, OutPath + "/" + Prefix, "");
        for (int i = 0; i < Chromosome.length; i++) {
            String ChrInterMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".inter";
            String ChrNormalizeMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".normalize";
            Matrix = CreateInterActionMatrix(ChrInterBedpeFile[i], new String[]{Chromosome[i]}, new int[]{ChromosomeSize[i]}, Resolution / 10, ChrInterMatrixPrefix);
            NormalizeMatrix = MatrixNormalize(Matrix);
            Tools.PrintMatrix(NormalizeMatrix, ChrNormalizeMatrixPrefix + ".2d.matrix", ChrNormalizeMatrixPrefix + ".spare.matrix");
            PlotCom = "python " + Opts.PlotScriptFile + " -t localGenome -m A -i " + ChrInterMatrixPrefix + ".2d.matrix -o " + OutPath + "/" + ImageDir + "/" + Prefix + "." + Chromosome[i] + ".png -r " + Resolution / 10 + " -p " + Chromosome[i] + ":0" + ":" + Chromosome[i] + ":0" + "  -q 95";
            Opts.CommandOutFile.Append(PlotCom + "\n");
            Tools.ExecuteCommandStr(PlotCom);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
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
        OutPath = new File(ParameterList.get(OptOutPath));
        Prefix = ParameterList.get(OptPrefix);
        BedpeFile = new CustomFile(ParameterList.get(OptBedpeFile));
        ChrSizeFile = new File(ParameterList.get(OptChrSzieFile));
        Chromosome = ParameterList.get(OptChromosome).split("\\s+");
        ChromosomeSize = new int[ParameterList.get(OptChromosomeSize).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptChromosomeSize).split("\\s+").length; i++) {
            ChromosomeSize[i] = Integer.parseInt(ParameterList.get(OptChromosomeSize).split("\\s+")[i]);
        }
        Resolution = Integer.parseInt(ParameterList.get(OptResolution));
        Thread = Integer.parseInt(ParameterList.get(OptThread));
        //=======================================================
        if (!OutPath.isDirectory()) {
            if (!OutPath.mkdirs()) {
                System.err.println("Can't Create " + OutPath);
                System.exit(0);
            }
        }
//        if (ChrSizeFile.equals("") && (Chromosome.length == 0 || ChromosomeSize.length == 0)) {
//            System.err.println("Error ! No Chromosome or ChromosomeSize");
//            System.exit(0);
        if (Chromosome.length == 0 || ChromosomeSize.length == 0) {
            if (ChrSizeFile.isFile()) {
                ExtractChrSize();
            } else {
                System.err.println("Wrong ChrSizeFile " + ChrSizeFile + "is not a file");
                System.exit(1);
            }
        }
        InterMatrixPrefix = OutPath + "/" + Prefix + ".inter";
        NormalizeMatrixPrefix = OutPath + "/" + Prefix + ".normalize";
    }


    private void ExtractChrSize() throws IOException {
        BufferedReader chrsize = new BufferedReader(new FileReader(ChrSizeFile));
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

    public CustomFile[] getChrInterBedpeFile() throws IOException {
        if (ChrInterBedpeFile == null) {
            ChrInterBedpeFile = SeparateInterBedpe(BedpeFile, Chromosome, OutPath + "/" + Prefix, "");
        }
        return ChrInterBedpeFile;
    }

    public CustomFile[] SeparateInterBedpe(File InterBedpeFile, String[] Chromosome, String Prefix, String Regex) throws
            IOException {
        System.out.println(new Date() + "\tSeparate InterBedpe " + InterBedpeFile.getName());
        BufferedReader interfile = new BufferedReader(new FileReader(InterBedpeFile));
        CustomFile[] SameChrFile = new CustomFile[Chromosome.length];
        BufferedWriter[] chrwrite = new BufferedWriter[Chromosome.length];
        String DiffFile = Prefix + ".diff.bedpe";
        BufferedWriter diffwrite = new BufferedWriter(new FileWriter(DiffFile));
        //------------------------------------------------------------
        Hashtable<String, Integer> ChrIndex = new Hashtable<>();
        for (int i = 0; i < Chromosome.length; i++) {
            ChrIndex.put(Chromosome[i], i);
            SameChrFile[i] = new CustomFile(Prefix + "." + Chromosome[i] + ".same.bedpe");
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
        System.out.println(new Date() + "\tEnd separate InterBedpe " + InterBedpeFile.getName());
        return SameChrFile;
    }

    private Integer[][] CreateInterActionMatrix(CustomFile bedpeFile, String[] chromosome, int[] chrSize, int resolution, String prefix) throws IOException {
        System.out.println(new Date() + "\tBegin to create interaction matrix " + bedpeFile);
        int SumBin = 0;
        int[] ChrBinSize = Statistic.CalculatorBinSize(chrSize, resolution);
        //计算bin的总数
        for (int i = 0; i < ChrBinSize.length; i++) {
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > 50000) {
            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
            System.exit(0);
        }
        Integer[][] InterMatrix = new Integer[SumBin][SumBin];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(bedpeFile));
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
                        if (bedpeFile.BedpeDetect() == 1) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                int hang = Integer.parseInt(str[1]) / resolution;
                                int lie = Integer.parseInt(str[3]) / resolution;
                                for (int j = 0; j < chromosome.length; j++) {
                                    if (str[0].equals(chromosome[j])) {
                                        break;
                                    }
                                    hang = hang + ChrBinSize[j];
                                }
                                if (hang >= finalSumBin) {
                                    continue;
                                }
                                for (int j = 0; j < chromosome.length; j++) {
                                    if (str[2].equals(chromosome[j])) {
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
                        } else if (bedpeFile.BedpeDetect() == 2) {
                            while ((line = infile.readLine()) != null) {
                                str = line.split("\\s+");
                                int hang = (Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2 / resolution;
                                int lie = (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2 / resolution;
                                for (int j = 0; j < chromosome.length; j++) {
                                    if (str[0].equals(chromosome[j])) {
                                        break;
                                    }
                                    hang = hang + ChrBinSize[j];
                                }
                                if (hang >= finalSumBin) {
                                    continue;
                                }
                                for (int j = 0; j < chromosome.length; j++) {
                                    if (str[3].equals(chromosome[j])) {
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
                        } else if (bedpeFile.BedpeDetect() == 0) {
                            System.err.println("Empty file!\t" + bedpeFile.getName());
                        } else {
                            System.err.println("Error format!\t" + bedpeFile.getName());
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
        Tools.PrintMatrix(InterMatrix, prefix + ".2d.matrix", prefix + ".spare.matrix");
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        int temp = 0;
        BufferedWriter outfile = new BufferedWriter(new FileWriter(prefix + ".matrix.BinSize"));
        for (int i = 0; i < chromosome.length; i++) {
            temp = temp + 1;
            outfile.write(chromosome[i] + "\t" + temp + "\t");
            temp = temp + ChrBinSize[i] - 1;
            outfile.write(temp + "\n");
        }
        outfile.close();
        return InterMatrix;
    }//OK

    private Double[][] MatrixNormalize(Integer[][] Matrix) {
        System.out.println(new Date() + "\tNormalize Matrix");
        Double[][] NormalizeMatrix = new Double[Matrix.length][Matrix.length];//定义标准化矩阵
        for (Double[] aNormalizeMatrix : NormalizeMatrix) {
            Arrays.fill(aNormalizeMatrix, 0D);
        }
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
                    NormalizeMatrix[i][j] = 0D;//如果某个距离平均交互数为0，则直接将标准化矩阵对应点设成0
                } else {
                    NormalizeMatrix[i][j] = Matrix[i][j] / Distance[2][Math.abs(i - j)];//用对应距离的交互值除以对应的平均交互值
                }
            }
        }
        System.out.println(new Date() + "\tNormalize Matrix end");
        return NormalizeMatrix;//返回标准化后的矩阵
    }
}
