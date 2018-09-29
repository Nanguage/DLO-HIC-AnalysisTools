package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import lib.tool.*;
import lib.unit.*;
import script.CreateMatrix;

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
    private int Resolution;
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
    private File BinSizeFile;
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
        Init();
    }

    MakeMatrix(File ConfigFile) throws IOException {
//        ParameterInit();
        GetOption(ConfigFile);
        Init();
    }

    public void Run() throws IOException {
        Integer[][] Matrix;
        CreateMatrix cm = new CreateMatrix(BedpeFile, Chromosomes, Resolution, InterMatrixPrefix, Threads);
        cm.Run();
        for (int j = 0; j < Chromosomes.length; j++) {
            cm = new CreateMatrix(ChrBedpeFile[j], new Chromosome[]{Chromosomes[j]}, Resolution / 10, ChrMatrixPrefix[j], Threads);
            Matrix = cm.Run();
            Tools.PrintMatrix(MatrixNormalize(Matrix), ChrNormalizeTwoDMatrix[j], ChrNormalizeSpareMAtrixFile[j]);
        }
    }

    public static void main(String[] args) throws IOException {
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
    }


    public void ShowParameter() {
        Config.stringPropertyNames();
    }

    private void Init() throws IOException {

        if (!OutPath.isDirectory() && !OutPath.mkdir()) {
            System.err.println("Can't Create " + OutPath);
            System.exit(1);
        }
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
        BinSizeFile = cm.getBinSizeFile();
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

    private Integer[][] CreateInterActionMatrix(CustomFile bedpeFile, Chromosome[] chromosome, int resolution, String prefix) throws IOException {
        CreateMatrix cm = new CreateMatrix(bedpeFile, chromosome, resolution, prefix, Threads);
        return cm.Run();
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

    public void setChrSizeFile(File chrSizeFile) {
        ChrSizeFile = chrSizeFile;
    }

    public File getBinSizeFile() {
        return BinSizeFile;
    }
}
