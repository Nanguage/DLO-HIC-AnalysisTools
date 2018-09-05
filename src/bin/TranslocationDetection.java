
package bin;

import com.sun.istack.internal.NotNull;
import kotlin.text.Charsets;
import lib.File.FileTool;
import lib.Image.HeatMap;
import lib.tool.Tools;
import lib.unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.PoissonDistribution;
import script.CreateMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author snowf
 * @version 1.0
 */
public class TranslocationDetection {
    private ChrRegion Chr1;
    private ChrRegion Chr2;
    private File MatrixFile;
    private Matrix<?> InterMatrix;
    private Matrix FilteredMatrix;
    private CustomFile BedpeFile;
    private int Resolution;
    private double Area;
    private double VacancyArea;
    private double Count;
    private String OutPrefix;
    private double P_Value = 0.01;
    private PoissonDistribution BackgroundDistribution = new PoissonDistribution(1);
    public static final float Version = 1.0f;

//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }

    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        TranslocationDetection Tld = new TranslocationDetection(args);
        Tld.Run();
    }

    private TranslocationDetection(ChrRegion chr1, ChrRegion chr2, File matrixfile, CustomFile bedpefile, String prefix) {
        Chr1 = chr1;
        Chr2 = chr2;
//        InterMatrix = matrix;
//        FilteredMatrix = matrix;
        MatrixFile = matrixfile;
        BedpeFile = bedpefile;
        OutPrefix = prefix;
//        Resolution = InterMatrix.getResolution();
    }

    public TranslocationDetection(ChrRegion chr1, ChrRegion chr2, File matrixfile, CustomFile bedpefile, int Resolution, String prefix) {
        this(chr1, chr2, matrixfile, bedpefile, prefix);
        this.setResolution(Resolution);
    }

    private TranslocationDetection(String[] args) throws IOException {
        Options Arguement = new Options();
        Arguement.addOption(Option.builder("chr").hasArgs().argName("name:size").desc("Chromosome name and region (such as chr1:100:500)").build());
        Arguement.addOption(Option.builder("r").required().longOpt("res").hasArg().argName("int").desc("Resolution").build());
        Arguement.addOption(Option.builder("m").required().longOpt("matrix").argName("file").hasArg().desc("Inter action matrix").build());
        Arguement.addOption(Option.builder("f").required().longOpt("bedpe").hasArg().argName("string").desc("Interaction bedpe file").build());
        Arguement.addOption(Option.builder("p").hasArg().argName("string").desc("out prefix").build());
        Arguement.addOption("v", true, "p value");
        final String Helpheader = "Version: " + Version;
        final String Helpfooter = "";
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TranslocationDetection.class.getName(), Helpheader, Arguement, Helpfooter, true);
            System.exit(1);
        }
        CommandLine Comline = null;
        try {
            Comline = new DefaultParser().parse(Arguement, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("java -cp path/" + Opts.JarFile.getName() + " " + TranslocationDetection.class.getName(), Helpheader, Arguement, Helpfooter, true);
            System.exit(1);
        }
        ChrRegion chr1 = new ChrRegion(new Chromosome("?"), 0, 0), chr2 = new ChrRegion(new Chromosome("?"), 0, 0);
        if (Comline.hasOption("chr")) {
            String[] s = Comline.getOptionValues("chr");
            chr1 = new ChrRegion(new Chromosome(s[0].split(":")[0]), Integer.parseInt(s[0].split(":")[1]), Integer.parseInt(s[0].split(":")[2]));
            if (s.length > 1) {
                chr2 = new ChrRegion(new Chromosome(s[1].split(":")[0]), Integer.parseInt(s[1].split(":")[1]), Integer.parseInt(s[1].split(":")[2]));
            } else {
                chr2 = chr1;
            }
        }
        P_Value = Comline.hasOption("v") ? Double.parseDouble(Comline.getOptionValue("v")) : P_Value;
        MatrixFile = new File(Comline.getOptionValue("matrix"));
        Resolution = Integer.parseInt(Comline.getOptionValue("r"));
        BedpeFile = new CustomFile(Comline.getOptionValue("f"));
        OutPrefix = Comline.hasOption("p") ? Comline.getOptionValue("p") : chr1.Chr.Name + "-" + chr2.Chr.Name;
        InterMatrix = new Matrix<>(FileTool.ReadMatrixFile(MatrixFile));
    }

    /***
     *
     * 李老师的cluster算法
     * @param BedpeClusterFile
     * @param CountValue
     * @param ExtendLength
     * @throws IOException
     */
    public void Run(String BedpeClusterFile, int CountValue, int ExtendLength) throws IOException {
        BufferedReader cluster_read = new BufferedReader(new FileReader(BedpeClusterFile));
        String Line = cluster_read.readLine();
        int count = Integer.parseInt(Line.split("\\s+")[6]);
        while (count >= CountValue) {
            String[] str = Line.split("\\s+");
            ChrRegion chr1 = new ChrRegion(new Chromosome(str[0]), Integer.parseInt(str[1]) - ExtendLength, Integer.parseInt(str[2]) + ExtendLength);
            ChrRegion chr2 = new ChrRegion(new Chromosome(str[3]), Integer.parseInt(str[4]) - ExtendLength, Integer.parseInt(str[5]) + ExtendLength);
            String prefix = chr1.Chr.Name + "-" + new DecimalFormat(".00").format(Tools.UnitTrans((chr1.Begin + chr1.Terminal) / 2, "B", "M")) + "M-" + chr2.Chr.Name + "-" + new DecimalFormat(".00").format(Tools.UnitTrans((chr2.Begin + chr2.Terminal) / 2, "B", "M")) + "M";
            new CreateMatrix(BedpeFile, null, Resolution, prefix, 4).Run(chr1, chr2);
            BufferedImage heatmap = new HeatMap(new File(prefix + ".2d.matrix")).Draw().getImage();
            ImageIO.write(heatmap, "png", new File(prefix + ".png"));
            if ((Line = cluster_read.readLine()) == null) {
                break;
            }
            count = Integer.parseInt(Line.split("\\s+")[6]);
        }
    }

    /***
     * 我的图像识别算法
     *
     * @throws IOException
     */
    public void Run() throws IOException, InterruptedException {
        String ComLine = "python " + Opts.JarFile.getParent() + "/script/LongCornerDetect.py -i " + MatrixFile + " -c " + Chr1.toString().replace("\t", ":") + " " + Chr2.toString().replace("\t", ":") + " -r " + Resolution + " -p " + OutPrefix;
        Opts.CommandOutFile.Append(ComLine + "\n");
        Tools.ExecuteCommandStr(ComLine,null,null);
        List<String> PointList = FileUtils.readLines(new File(OutPrefix + ".HisD.point"), Charsets.UTF_8);
        for (String point : PointList) {
            String[] str = point.split("\\s+");
            double p_value = Double.parseDouble(str[6]) + Double.parseDouble(str[7]);
            if (p_value < P_Value) {
                int chr1index = Integer.parseInt(str[3]);
                int chr2index = Integer.parseInt(str[5]);
                ChrRegion region1 = new ChrRegion(new Chromosome(str[2]), chr1index - 2 * Resolution, chr1index + 3 * Resolution);
                ChrRegion region2 = new ChrRegion(new Chromosome(str[4]), chr2index - 2 * Resolution, chr2index + 3 * Resolution);
                String prefix = OutPrefix + "." + region1.Chr.Name + "-" + new DecimalFormat(".00").format(Tools.UnitTrans(chr1index, "B", "M")) + "M," + region2.Chr.Name + "-" + new DecimalFormat(".00").format(Tools.UnitTrans(chr2index, "B", "M")) + "M." + new DecimalFormat().format(Tools.UnitTrans(Resolution / 50, "B", "K")) + "k";
                if (!new File(prefix + ".2d.matrix").exists()) {
                    new CreateMatrix(BedpeFile, null, Resolution / 50, prefix, 4).Run(region1, region2);
                }
                ComLine = "python " + Opts.JarFile.getParent() + "/script/ShortCornerDetect.py -i " + prefix + ".2d.matrix -r " + (Resolution / 50) + " -c " + region1.Chr.Name + ":" + region1.Begin + " " + region2.Chr.Name + ":" + region2.Begin + " -q " + str[8] + " -p " + prefix;
                Opts.CommandOutFile.Append(ComLine + "\n");
                Tools.ExecuteCommandStr(ComLine,null,null);
            }

        }
    }

    /**
     * @param Size windows size
     * @throws IOException
     */
    private void CreatBackground(int Size) throws IOException {
        BackgroundDistribution = new PoissonDistribution(Count * Size * Size / (Area - VacancyArea));
        boolean[][] AbnormalMatrix;
        double A = 0;
        int AbnormalCount = 0;
        BackgroundDistribution = new PoissonDistribution((Count - AbnormalCount) * Size * Size / (Area - VacancyArea - A));
        for (int k = 0; k < 0; k++) {
            int max = Confidence(P_Value);
            AbnormalMatrix = SearchArea(InterMatrix, Size, max, Double.MAX_VALUE);
            A = GetMatrixArea(AbnormalMatrix);
            AbnormalCount = 0;
            for (int i = 0; i < AbnormalMatrix.length; i++) {
                for (int j = 0; j < AbnormalMatrix[i].length; j++) {
                    if (AbnormalMatrix[i][j]) {
                        AbnormalCount += InterMatrix.get(i, j).intValue();
                    }
                }
            }
            BackgroundDistribution = new PoissonDistribution((Count - AbnormalCount) * Size * Size / (Area - VacancyArea - A));
        }
    }

    private ArrayList<int[]> ChangePointDetect(String matrixfile, String outprefix) throws IOException, InterruptedException {
        String ComLine = "Rscript " + Opts.JarFile.getParent() + "/script/ChangePointDetect.R " + matrixfile + " " + outprefix;
        Opts.CommandOutFile.Append(ComLine + "\n");
        Tools.ExecuteCommandStr(ComLine,null,null);
        ArrayList<int[]> ChangePointList = new ArrayList<>();
        List<String> LineStr = FileUtils.readLines(new File(outprefix + ".cpt"), Charsets.UTF_8);
        for (String line : LineStr) {
            ChangePointList.add(StringArrays.toInteger(line.split("\\s+")));
        }
        return ChangePointList;
    }

    private int Confidence(double p_value) {
        int max = 0;
        while (BackgroundDistribution.cumulativeProbability(max) <= 1 - p_value) {
            max++;
        }
        return max;
    }

    private double GetCount(double[][] Matrix) {
        double Count = 0;
        for (double[] aMatrix : Matrix) {
            for (double anAMatrix : aMatrix) {
                Count += anAMatrix;
            }
        }
        return Count;
    }


    private boolean[][] SearchArea(Matrix Matrix, int Size, double min, double max) throws IOException {
        boolean[][] VMatrix = new boolean[Matrix.getSize()[0]][Matrix.getSize()[1]];//position we want detect
        double[][] Line = new double[Matrix.getSize()[0]][Matrix.getSize()[1] - Size + 1];//value of each col
        double[] Window = new double[Matrix.getSize()[1] - Size + 1];//value of each windows
        for (int i = 0; i < Matrix.getSize()[0]; i++) {
            for (int j = 0; j <= Matrix.getSize()[1] - Size; j++) {
                for (int k = 0; k < Size; k++) {
                    Line[i][j] += (double) Matrix.get(i, j + k);
                }
            }
        }
        for (int i = 0; i < Size - 1; i++) {
            for (int j = 0; j <= Matrix.getSize()[1] - Size; j++) {
                Window[j] += Line[i][j];
            }
        }
//        BufferedWriter out = new BufferedWriter(new FileWriter("TlD.distribution"));
        for (int i = Size - 1; i < InterMatrix.getSize()[0]; i++) {
            for (int j = 0; j <= InterMatrix.getSize()[1] - Size; j++) {
//                out.write((int) (Window[j] + Line[i][j]) + "\n");
                if (Window[j] + Line[i][j] <= max && Window[j] + Line[i][j] >= min) {
                    for (int k = 0; k < Size; k++) {
                        for (int l = 0; l < Size; l++) {
                            VMatrix[i + 1 - Size + k][j + l] = true;
                        }
                    }
                }
                Window[j] = Window[j] + Line[i][j] - Line[i + 1 - Size][j];
            }
        }
//        out.close();
//        Tools.PrintMatrix(VMatrix, "Vacancy.2d.matrix", "Vacancy.spare.matrix");
        return VMatrix;
    }

    private double GetMatrixArea(boolean[][] Matrix) {
        double Area = 0;
        for (boolean[] aMatrix : Matrix) {
            for (boolean anAMatrix : aMatrix) {
                if (anAMatrix) {
                    Area++;
                }
            }
        }
        return Area;
    }

    private double[] GetRowSum(double[][] Matrix) {
        double[] RowSum = new double[Matrix.length];
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                RowSum[i] += Matrix[i][j];
            }
        }
        return RowSum;
    }

    private double[] GetColSum(double[][] Matrix) {
        double[] ColSum = new double[Matrix[0].length];
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                ColSum[j] += Matrix[i][j];
            }
        }
        return ColSum;
    }

    public void setInterMatrix(Matrix interMatrix) {
        InterMatrix = interMatrix;
    }

    public Matrix getFilteredMatrix() {
        return FilteredMatrix;
    }

    public void setP_Value(double p_Value) {
        P_Value = p_Value;
    }

    public void setResolution(int resolution) {
        Resolution = resolution;
    }
}
