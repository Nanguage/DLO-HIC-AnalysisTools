package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import lib.tool.*;
import lib.unit.*;
import script.BedpeFilter;
import script.CreateMatrix;

import javax.sound.midi.SoundbankResource;

public class MakeMatrix {
    //    private String OptOutPath = "OutPath";
//    private String OptPrefix = "Prefix";
//    private String OptBedpeFile = "BedpeFile";
//    private String OptChromosome = "Chromosomes";
//    public String OptThread = "Threads";
//    private String OptResolution = "Resolution";
//    private String OptChrSzieFile = "ChrSizeFile";
//    private String OptChromosomeSize = "ChrSize";
    //===============================================================
    private File OutPath;//输出路径
    private String Prefix;//输出前缀
    private CustomFile BedpeFile;
    private Chromosome[] Chromosomes;
    //    private int[] ChromosomeSize;
    private int Resolution;
    //    private int[] DrawResolution;
    private int Threads;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private File ChrSizeFile;
    private CustomFile[] ChrBedpeFile;
    //==============================================================
    private String[] ChrMatrixPrefix;
    private File TwoDMatrixFile, SpareMAtrixFile;
    private File[] ChrTwoDMatrixFile, ChrSpareMAtrixFile, ChrNormalizeTwoDMatrix, ChrNormalizeSpareMAtrixFile;
    private File ConfigureFile;
    private Properties Config = new Properties();
//    private Hashtable<String, String> ParameterList = new Hashtable<>();
//    private String[] RequiredParameter = new String[]{OptBedpeFile, OptResolution};
//    private String[] OptionalParameter = new String[]{OptChromosome, OptOutPath, OptPrefix, OptThread, OptChrSzieFile};


    public MakeMatrix(File outpath, String outprefix, CustomFile validpairs, CustomFile[] ChrBedpeFile, Chromosome[] chrosomose, int resolution, int thread) throws IOException {
        OutPath = outpath;
        Prefix = outprefix;
        BedpeFile = validpairs;
        this.ChrBedpeFile = ChrBedpeFile;
        Chromosomes = chrosomose;
        Resolution = resolution;
        Threads = thread;
//        DrawResolution = drawresolution;
        Init();
//        ParameterInit();
//        ParameterList.put(OptOutPath, outpath.getPath());
//        ParameterList.put(OptPrefix, outprefix);
//        ParameterList.put(OptBedpeFile, validpairs);
//        String temp = "";
//        if (chrosomose.length > 0) {
//            temp = chrosomose[0];
//            for (int i = 1; i < chrosomose.length; i++) {
//                temp = temp + " " + chrosomose[i];
//            }
//        }
//        ParameterList.put(OptChromosome, temp);
//        temp = "";
//        if (chrsize.length > 0) {
//            temp = String.valueOf(chrsize[0]);
//            for (int i = 1; i < chrsize.length; i++) {
//                temp = temp + " " + chrsize[i];
//            }
//        }
//        ParameterList.put(OptChromosomeSize, temp);
//        ParameterList.put(OptResolution, String.valueOf(resolution));
    }

    MakeMatrix(File ConfigFile) throws IOException {
//        ParameterInit();
        GetOption(ConfigFile);
        Init();
    }

    public void Run() throws IOException, InterruptedException {
//        String ImageDir = "image";
//        for (int i = 0; i < Resolution.length; i++) {
        Integer[][] Matrix;
//        File ImagePath = new File(OutPath + "/image_" + Tools.UnitTrans(Resolution, "B", "M") + "M");
//        if (!ImagePath.isDirectory()) {
//            ImagePath.mkdirs();
//        }
////            Integer[][] Matrix = CreateInterActionMatrix(BedpeFile, Chromosomes, Resolution[i], InterMatrixPrefix);
//        for (File s : ChrBedpeFile){
//            System.out.println(s);
//        }
//        for (Chromosome i : Chromosomes){
//            System.out.println(i);
//        }
//        System.out.println(BedpeFile);
//        System.out.println(InterMatrixPrefix);
//        for (Chromosome s : Chromosomes){
//            System.out.println(s);
//        }
//        System.out.println(Resolution);
        CreateMatrix cm = new CreateMatrix(BedpeFile, Chromosomes, Resolution, InterMatrixPrefix, Threads);
        cm.Run();
//        PngFile = new File(ImagePath + "/" + Prefix + ".png");
//        PlotMatrix plt = new PlotMatrix(ChrTwoDMatrixFile, PngFile, Resolution);
//        plt.Run(cm.getBinSizeFile());
//        String PlotCom = plt.getComLine();
//            String PlotCom = "python " + Opts.PlotScriptFile + " -m A -i " + InterMatrixPrefix + ".2d.matrix -o " + ImagePath + "/" + Prefix + ".png -r " + Resolution[i] + " -c " + InterMatrixPrefix + ".matrix.BinSize" + " -q 98";
//        Opts.CommandOutFile.Append(PlotCom + "\n");
//        Tools.ExecuteCommandStr(PlotCom, new File(ImagePath + "/" + Prefix + ".plot.out"), new File(ImagePath + "/" + Prefix + ".plot.err"));
//        Double[][] NormalizeMatrix = MatrixNormalize(Matrix);
//        Tools.PrintMatrix(NormalizeMatrix, new File(NormalizeMatrixPrefix + ".2d.matrix"), new File(NormalizeMatrixPrefix + ".spare.matrix"));
//        ChrBedpeFile = BedpeFile.SeparateInterBedpe(Chromosomes, OutPath + "/" + Prefix, ,Threads);
        for (int j = 0; j < Chromosomes.length; j++) {
//            String ChrNormalizeMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosomes[j].Name + ".normalize";
            cm = new CreateMatrix(ChrBedpeFile[j], new Chromosome[]{Chromosomes[j]}, Resolution / 10, ChrMatrixPrefix[j], Threads);
            Matrix = cm.Run();
//            Matrix = CreateInterActionMatrix(ChrBedpeFile[j], new Chromosomes[]{Chromosomes[j]}, Resolution / 10, ChrInterMatrixPrefix);
            Tools.PrintMatrix(MatrixNormalize(Matrix), ChrNormalizeTwoDMatrix[j], ChrNormalizeSpareMAtrixFile[j]);
//            PlotCom = "python " + Opts.PlotScriptFile + " -t localGenome -m A -i " + ChrInterMatrixPrefix + ".2d.matrix -o " + ImagePath + "/" + Prefix + "." + Chromosomes[j].Name + ".png -r " + Resolution / 10 + " -p " + Chromosomes[j].Name + ":0" + ":" + Chromosomes[j].Name + ":0" + "  -q 95";
//            Opts.CommandOutFile.Append(PlotCom + "\n");
//            Tools.ExecuteCommandStr(PlotCom, null, null);
        }
//        }
//        String PlotScriptFile = Opts.JarFile.getParent() + "/script/PlotHeatmap.py";


    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp DLO-HIC-AnalysisTools.jar bin.MakeMatrix <Config.txt>");
            System.exit(0);
        }
        MakeMatrix mm = new MakeMatrix(new File(args[0]));
        mm.ShowParameter();
        mm.Run();
    }

    private void GetOption(File conf_file) throws IOException {
        ConfigureFile = conf_file;
        Config.load(new FileReader(conf_file));
        OutPath = new File(Config.getProperty("OutPath", "./"));
        Prefix = Config.getProperty("Prefix", "MM_Out");
        BedpeFile = Config.getProperty("BedpeFile") != null ? new CustomFile(Config.getProperty("BedpeFile")) : null;
        String[] Chr = Config.getProperty("Chromosomes") != null ? Config.getProperty("Chromosomes").split("\\s+") : null;
        int[] ChrSize = Config.getProperty("ChrSize") != null ? StringArrays.toInteger(Config.getProperty("ChrSize").split("\\s+")) : null;
        Threads = Integer.parseInt(Config.getProperty("Threads", "1"));
        ChrSizeFile = Config.getProperty("ChrSizeFile") != null ? new File(Config.getProperty("ChrSizeFile")) : null;

//        BufferedReader option = new BufferedReader(new FileReader(optionfile));
//        String line;
//        String[] str;
//        while ((line = option.readLine()) != null) {
//            line = line.trim();
//            if (line.equals("")) {
//                continue;
//            }
//            str = line.split("\\s*=\\s*|\\s+");
//            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
//                ParameterList.put(str[0], str[1]);
//            }
//        }
//        option.close();
//    }
//
//    public void ParameterInit() {
//        for (String opt : RequiredParameter) {
//            ParameterList.put(opt, "");
//        }
//        for (String opt : OptionalParameter) {
//            ParameterList.put(opt, "");
//        }
//        ParameterList.put(OptOutPath, "./");
//        ParameterList.put(OptPrefix, "Matrix");
//        ParameterList.put(OptThread, "1");
    }

 /*   public boolean SetParameter(String Key, String Value) {
        if (ParameterList.containsKey(Key)) {
            ParameterList.put(Key, Value);
            return true;
        } else {
            return false;
        }
    }*/

    public void ShowParameter() {
        Config.stringPropertyNames();
//        for (String opt : RequiredParameter) {
//            System.out.println(opt + ":\t" + ParameterList.get(opt));
//        }
//        System.out.println("===============================================================================");
//        for (String opt : OptionalParameter) {
//            System.out.println(opt + ":\t" + ParameterList.get(opt));
//        }
    }

    private void Init() throws IOException {
//        for (String opt : RequiredParameter) {
//            if (ParameterList.get(opt).equals("")) {
//                System.err.println("Error ! No " + opt);
//                System.exit(0);
//            }
//        }
        //=======================================================
//        OutPath = new File(ParameterList.get(OptOutPath));
//        Prefix = ParameterList.get(OptPrefix);
//        BedpeFile = new CustomFile(ParameterList.get(OptBedpeFile));
//        ChrSizeFile = new File(ParameterList.get(OptChrSzieFile));
//        Chromosomes = ParameterList.get(OptChromosome).split("\\s+");
//        ChromosomeSize = new int[ParameterList.get(OptChromosomeSize).split("\\s+").length];
//        for (int i = 0; i < ParameterList.get(OptChromosomeSize).split("\\s+").length; i++) {
//            ChromosomeSize[i] = Integer.parseInt(ParameterList.get(OptChromosomeSize).split("\\s+")[i]);
//        }
//        Resolution = Integer.parseInt(ParameterList.get(OptResolution));
//        Threads = Integer.parseInt(ParameterList.get(OptThread));
        //=======================================================
//        synchronized (MakeMatrix.class) {
        if (!OutPath.isDirectory() && !OutPath.mkdir()) {
            System.err.println("Can't Create " + OutPath);
            System.exit(1);
        }
//        }

//        if (ChrSizeFile.equals("") && (Chromosomes.length == 0 || ChromosomeSize.length == 0)) {
//            System.err.println("Error ! No Chromosomes or ChromosomeSize");
//            System.exit(0);
        if (Chromosomes.length == 0) {
            if (ChrSizeFile.isFile()) {
                ExtractChrSize();
            } else {
                System.err.println("Wrong ChrSizeFile " + ChrSizeFile + "is not a file");
                System.exit(1);
            }
        }
        CreateMatrix cm;
        InterMatrixPrefix = OutPath + "/" + Prefix + "_" + Tools.UnitTrans(Resolution, "B", "M") + "M";
//        NormalizeMatrixPrefix = OutPath + "/" + Prefix + ".normalize";
        ChrTwoDMatrixFile = new File[Chromosomes.length];
        ChrSpareMAtrixFile = new File[Chromosomes.length];
        ChrNormalizeTwoDMatrix = new File[Chromosomes.length];
        ChrNormalizeSpareMAtrixFile = new File[Chromosomes.length];
        ChrMatrixPrefix = new String[Chromosomes.length];
        cm = new CreateMatrix(BedpeFile, Chromosomes, Resolution, InterMatrixPrefix, Threads);
        TwoDMatrixFile = cm.getTwoDMatrixFile();
        SpareMAtrixFile = cm.getSpareMatrixFile();
//        System.out.println(TwoDMatrixFile+"\t"+SpareMAtrixFile);
        for (int i = 0; i < Chromosomes.length; i++) {
            ChrMatrixPrefix[i] = OutPath + "/" + Prefix + "." + Chromosomes[i].Name + "_" + Tools.UnitTrans(Resolution / 10, "B", "M") + "M";
            cm = new CreateMatrix(ChrBedpeFile[i], new Chromosome[]{Chromosomes[i]}, Resolution / 10, ChrMatrixPrefix[i], Threads);
            ChrTwoDMatrixFile[i] = cm.getTwoDMatrixFile();
            ChrSpareMAtrixFile[i] = cm.getSpareMatrixFile();
            ChrNormalizeTwoDMatrix[i] = new File(OutPath + "/" + Prefix + "." + Chromosomes[i].Name + "_" + Tools.UnitTrans(Resolution / 10, "B", "M") + "M" + ".normalize.2d.matrix");
            ChrNormalizeSpareMAtrixFile[i] = new File(OutPath + "/" + Prefix + "." + Chromosomes[i].Name + "_" + Tools.UnitTrans(Resolution / 10, "B", "M") + "M" + ".normalize.spare.matrix");
        }
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
        Chromosomes = new Chromosome[list.size()];
//        ChromosomeSize = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Chromosomes[i] = new Chromosome(list.get(i));
//            ChromosomeSize[i] = Integer.parseInt(list.get(i)[1]);
        }
    }

//    public CustomFile[] getChrBedpeFile() throws IOException {
//        if (ChrBedpeFile == null) {
//            ChrBedpeFile = SeparateInterBedpe(BedpeFile, Chromosomes, OutPath + "/" + Prefix, "");
//        }
//        return ChrBedpeFile;
//    }

//    private CustomFile[] SeparateInterBedpe(File InterBedpeFile, Chromosomes[] Chromosomes, String Prefix, String Regex) throws IOException {
//        System.out.println(new Date() + "\tSeparate InterBedpe " + InterBedpeFile.getName());
//        BufferedReader interfile = new BufferedReader(new FileReader(InterBedpeFile));
//        CustomFile[] SameChrFile = new CustomFile[Chromosomes.length];
//        BufferedWriter[] chrwrite = new BufferedWriter[Chromosomes.length];
//        String DiffFile = Prefix + ".diff.bedpe";
//        BufferedWriter diffwrite = new BufferedWriter(new FileWriter(DiffFile));
//        //------------------------------------------------------------
//        Hashtable<String, Integer> ChrIndex = new Hashtable<>();
//        for (int i = 0; i < Chromosomes.length; i++) {
//            ChrIndex.put(Chromosomes[i].Name, i);
//            SameChrFile[i] = new CustomFile(Prefix + "." + Chromosomes[i] + ".same.bedpe");
//            chrwrite[i] = new BufferedWriter(new FileWriter(SameChrFile[i]));
//        }
//        if (Regex.isEmpty()) {
//            Regex = "\\s+";
//        }
//        //================================================================
//        Thread[] Process = new Thread[Threads];
//        for (int i = 0; i < Threads; i++) {
//            String finalRegex = Regex;
//            Process[i] = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    String line;
//                    String[] str;
//                    try {
//                        while ((line = interfile.readLine()) != null) {
//                            str = line.split(finalRegex);
//                            if (str[0].equals(str[2])) {
//                                synchronized (Process) {
//                                    chrwrite[ChrIndex.get(str[0])].write(line + "\n");
//                                }
//                            } else {
//                                synchronized (Process) {
//                                    diffwrite.write(line + "\n");
//                                }
//                            }
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            Process[i].start();
//        }
//        for (int i = 0; i < Threads; i++) {
//            try {
//                Process[i].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        for (int i = 0; i < Chromosomes.length; i++) {
//            chrwrite[i].close();
//        }
//        diffwrite.close();
//        System.out.println(new Date() + "\tEnd separate InterBedpe " + InterBedpeFile.getName());
//        return SameChrFile;
//    }

    private Integer[][] CreateInterActionMatrix(CustomFile bedpeFile, Chromosome[] chromosome, int resolution, String prefix) throws IOException {
        CreateMatrix cm = new CreateMatrix(bedpeFile, chromosome, resolution, prefix, Threads);
        return cm.Run();
//        System.out.println(new Date() + "\tBegin to create interaction matrix ");
//        int SumBin = 0;
//        int[] ChrSize = new int[chromosome.length];
//        for (int i = 0; i < chromosome.length; i++) {
//            ChrSize[i] = chromosome[i].Size;
//        }
//        int[] ChrBinSize = Statistic.CalculatorBinSize(ChrSize, resolution);
//        //计算bin的总数
//        for (int i = 0; i < ChrBinSize.length; i++) {
//            SumBin = SumBin + ChrBinSize[i];
//        }
//        if (SumBin > Opts.MaxBinNum) {
//            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
//            System.exit(1);
//        }
//        Integer[][] InterMatrix = new Integer[SumBin][SumBin];
//        for (int i = 0; i < InterMatrix.length; i++) {
//            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
//        }
//        BufferedReader infile = new BufferedReader(new FileReader(bedpeFile));
//        Thread[] Process = new Thread[Threads];
//        //----------------------------------------------------------------------------
//        for (int i = 0; i < Threads; i++) {
//            int finalSumBin = SumBin;
//            Process[i] = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        String line;
//                        String[] str;
//                        if (bedpeFile.BedpeDetect() == Opts.BedpePointFormat) {
//                            while ((line = infile.readLine()) != null) {
//                                str = line.split("\\s+");
//                                int hang = Integer.parseInt(str[1]) / resolution;
//                                int lie = Integer.parseInt(str[3]) / resolution;
//                                for (int j = 0; j < chromosome.length; j++) {
//                                    if (str[0].equals(chromosome[j])) {
//                                        break;
//                                    }
//                                    hang = hang + ChrBinSize[j];
//                                }
//                                if (hang >= finalSumBin) {
//                                    continue;
//                                }
//                                for (int j = 0; j < chromosome.length; j++) {
//                                    if (str[2].equals(chromosome[j])) {
//                                        break;
//                                    }
//                                    lie = lie + ChrBinSize[j];
//                                }
//                                if (lie >= finalSumBin) {
//                                    continue;
//                                }
//                                synchronized (Process) {
//                                    InterMatrix[hang][lie]++;
//                                    if (hang != lie) {
//                                        InterMatrix[lie][hang]++;
//                                    }
//                                }
//                            }
//                        } else if (bedpeFile.BedpeDetect() == Opts.BedpeRegionFormat) {
//                            while ((line = infile.readLine()) != null) {
//                                str = line.split("\\s+");
//                                int hang = (Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2 / resolution;
//                                int lie = (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2 / resolution;
//                                for (int j = 0; j < chromosome.length; j++) {
//                                    if (str[0].equals(chromosome[j])) {
//                                        break;
//                                    }
//                                    hang = hang + ChrBinSize[j];
//                                }
//                                if (hang >= finalSumBin) {
//                                    continue;
//                                }
//                                for (int j = 0; j < chromosome.length; j++) {
//                                    if (str[3].equals(chromosome[j])) {
//                                        break;
//                                    }
//                                    lie = lie + ChrBinSize[j];
//                                }
//                                if (lie >= finalSumBin) {
//                                    continue;
//                                }
//                                synchronized (Process) {
//                                    InterMatrix[hang][lie]++;
//                                    if (hang != lie) {
//                                        InterMatrix[lie][hang]++;
//                                    }
//                                }
//                            }
//                        } else if (bedpeFile.BedpeDetect() == 0) {
//                            System.err.println("Empty file!\t" + bedpeFile.getName());
//                        } else {
//                            System.err.println("Error format!\t" + bedpeFile.getName());
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            Process[i].start();
//        }
//        //-------------------------------------------------
//        for (int i = 0; i < Threads; i++) {
//            try {
//                Process[i].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        infile.close();
//        //--------------------------------------------------------
//        //打印矩阵
//        Tools.PrintMatrix(InterMatrix, new File(prefix + ".2d.matrix"), new File(prefix + ".spare.matrix"));
//        System.out.println(new Date() + "\tEnd to creat interaction matrix");
//        //--------------------------------------------------------------------
//        int temp = 0;
//        BufferedWriter outfile = new BufferedWriter(new FileWriter(prefix + ".matrix.BinSize"));
//        for (int i = 0; i < chromosome.length; i++) {
//            temp = temp + 1;
//            outfile.write(chromosome[i] + "\t" + temp + "\t");
//            temp = temp + ChrBinSize[i] - 1;
//            outfile.write(temp + "\n");
//        }
//        outfile.close();
//        return InterMatrix;
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

    public File getTwoDMatrixFile() {
        return TwoDMatrixFile;
    }

    public File[] getChrSpareMAtrixFile() {
        return ChrSpareMAtrixFile;
    }

    public File[] getChrTwoDMatrixFile() {
        return ChrTwoDMatrixFile;
    }
}
