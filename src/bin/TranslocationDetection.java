
package bin;

import kotlin.text.Charsets;
import lib.Command.Execute;
import lib.File.FileTool;
import lib.Image.HeatMap;
import lib.tool.Tools;
import lib.unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.PoissonDistribution;
//import org.opencv.core.Mat;
//import org.opencv.imgcodecs.Imgcodecs;
import script.CreateMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
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
    private Matrix<Integer> InterMatrix;
    private Matrix FilteredMatrix;
    private CustomFile BedpeFile;
    private int Resolution;
    private double Area;
    private double VacancyArea;
    private double Count;
    private String OutPrefix;
    private double P_Value = 0.01;
    private PoissonDistribution BackgroundDistribution = new PoissonDistribution(1);

//    static {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//    }

    public static void main(String[] args) throws ParseException, IOException {
        Options Arguement = new Options();
        Arguement.addOption(Option.builder("chr").hasArgs().argName("name:size").desc("Chromosome name and region (such as chr1:100:500)").build());
        Arguement.addOption(Option.builder("r").required().longOpt("res").hasArg().argName("int").desc("Resolution").build());
        Arguement.addOption(Option.builder("m").required().longOpt("matrix").argName("file").hasArg().desc("Inter action matrix").build());
        Arguement.addOption(Option.builder("f").required().longOpt("bedpe").hasArg().argName("string").desc("Interaction bedpe file").build());
        Arguement.addOption(Option.builder("p").hasArg().argName("string").desc("out prefix").build());
        Arguement.addOption("v", true, "p value");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp " + Opts.JarFile.getAbsolutePath() + " " + TranslocationDetection.class.getName() + " [option]", Arguement);
            System.exit(1);
        }
        CommandLine Comline = new DefaultParser().parse(Arguement, args);
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
        double pvalue = Comline.hasOption("v") ? Double.parseDouble(Comline.getOptionValue("v")) : 0.05;
        File MatrixFile = new File(Comline.getOptionValue("matrix"));
        int Resolution = Integer.parseInt(Comline.getOptionValue("r"));
        CustomFile BedpeFile = new CustomFile(Comline.getOptionValue("f"));
        String Prefix = Comline.hasOption("p") ? Comline.getOptionValue("p") : chr1.Chr.Name + "-" + chr2.Chr.Name;

        ArrayList<InterAction> List = new ArrayList<>();

        TranslocationDetection Tld = new TranslocationDetection(chr1, chr2, new Matrix<>(FileTool.ReadMatrixFile(MatrixFile)), BedpeFile, Resolution, Prefix);
        Tld.setP_Value(pvalue);
        Tld.Run(MatrixFile);
        /*nterActMatrix filteredmatrix = Tld.getFilteredMatrix();
        Tools.PrintMatrix(filteredmatrix.getMatrix(), Prefix + ".filter.2d.matrix", Prefix + ".filter.spare.matrix");
        //===============
        HeatMap map = new HeatMap(Prefix + ".filter.2d.matrix");
//        map.Draw();
        ImageIO.write(map.Draw().getImage(), "png", new File(Prefix + ".filter.png"));*/
    }

    TranslocationDetection(ChrRegion chr1, ChrRegion chr2, Matrix matrix, CustomFile bedpefile, String prefix) {
        Chr1 = chr1;
        Chr2 = chr2;
        InterMatrix = matrix;
        FilteredMatrix = matrix;
        BedpeFile = bedpefile;
        OutPrefix = prefix;
        Resolution = InterMatrix.Resolution;
    }

    TranslocationDetection(ChrRegion chr1, ChrRegion chr2, Matrix matrix, CustomFile bedpefile, int Resolution, String prefix) {
        this(chr1, chr2, matrix, bedpefile, prefix);
        this.setResolution(Resolution);
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
     * @param MatrixFile
     * @throws IOException
     */
    public void Run(File MatrixFile) throws IOException {
        String ComLine = "python " + Opts.JarFile.getParent() + "/script/LongCornerDetect.py -i " + MatrixFile + " -c " + Chr1.toString().replace("\t", ":") + " " + Chr2.toString().replace("\t", ":") + " -r " + Resolution + " -p " + OutPrefix;
        Opts.CommandOutFile.Append(ComLine + "\n");
        new Execute(ComLine);
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
                new Execute(ComLine);
            }

        }
        /*ArrayList<InterAction> ValidList = new ArrayList<>();
        Area = InterMatrix.getSize()[0] * InterMatrix.getSize()[1];
        Count = InterMatrix.Count;
        int Size = 1;
        while (Count / Area * Size * Size <= 50) {
            Size += 1;
        }
        ArrayList<int[]> Index = ChangePointDetect(MatrixFile, OutPrefix);//Edge detect
        int[] RowIndex = Index.get(0);// get row edge
        int[] ColIndex = Index.get(1);// get col edge
        HeatMap map = new HeatMap(InterMatrix.getMatrix());// Create heatmap figure
        BufferedImage HeatMapImage = map.Draw().getImage();
        //draw change point line
        ImageIO.write(HeatMapImage, "png", new File(OutPrefix + ".cpt.png"));//Print heatmap figure
        //==============================================================================================================
        ArrayList<ChrRegion> RowRegion = new ArrayList<>();//Row region about each apartment
        ArrayList<ChrRegion> ColRegion = new ArrayList<>();//Col region about each apartment
        RowRegion.add(new ChrRegion(Chr1.Chr, 1, RowIndex[0] - 1));
        for (int i = 0; i < RowIndex.length - 1; i++) {
            RowRegion.add(new ChrRegion(Chr1.Chr, RowIndex[i], RowIndex[i + 1] - 1));
        }
        RowRegion.add(new ChrRegion(Chr1.Chr, RowIndex[RowIndex.length - 1], InterMatrix.getSize()[0]));
        ColRegion.add(new ChrRegion(Chr2.Chr, 1, ColIndex[0] - 1));
        for (int i = 0; i < ColIndex.length - 1; i++) {
            ColRegion.add(new ChrRegion(Chr2.Chr, ColIndex[i], ColIndex[i + 1] - 1));
        }
        ColRegion.add(new ChrRegion(Chr2.Chr, ColIndex[ColIndex.length - 1], InterMatrix.getSize()[1]));
        //==============================================================================================================
        int max = Confidence(P_Value);//calculate the max density
        for (int i = 0; i < RowRegion.size(); i++) {
            for (int j = 0; j < ColRegion.size(); j++) {
                double[][] tempmatrix = SubMatrix(InterMatrix.getMatrix(), new int[]{RowRegion.get(i).Begin - 1, RowRegion.get(i).Terminal - 1}, new int[]{ColRegion.get(j).Begin - 1, ColRegion.get(j).Terminal - 1});
                double[] rowsum = GetRowSum(tempmatrix);
                double[] colsum = GetColSum(tempmatrix);
                double count = GetCount(tempmatrix);
                double area = (RowRegion.get(i).Terminal - RowRegion.get(i).Begin + 1) * (ColRegion.get(j).Terminal - ColRegion.get(j).Begin + 1);
                if (count / area * Size * Size <= max) {
                    for (int k = RowRegion.get(i).Begin - 1; k <= RowRegion.get(i).Terminal - 1; k++) {
                        for (int l = ColRegion.get(j).Begin - 1; l <= ColRegion.get(j).Terminal - 1; l++) {
                            FilteredMatrix.getMatrix()[k][l] = 0;
                        }
                    }
                } else {
                    double[] rarray = new double[rowsum.length];
                    double[] carray = new double[colsum.length];
                    for (int k = 0; k < rowsum.length; k++) {
                        rarray[k] = k;
                    }
                    for (int k = 0; k < colsum.length; k++) {
                        carray[k] = k;
                    }
                    if (new SpearmansCorrelation().correlation(rarray, rowsum) < 0.2 && new SpearmansCorrelation().correlation(carray, colsum) < 0.2) {
                        for (int k = RowRegion.get(i).Begin - 1; k <= RowRegion.get(i).Terminal - 1; k++) {
                            for (int l = ColRegion.get(j).Begin - 1; l <= ColRegion.get(j).Terminal - 1; l++) {
                                FilteredMatrix.getMatrix()[k][l] = 0;
                            }
                        }
                    } else {
                        ValidList.add(new InterAction(new ChrRegion(Chr1.Chr, Chr1.Begin + (RowRegion.get(i).Begin - 1) * Resolution, Chr1.Begin + (RowRegion.get(i).Terminal - 1) * Resolution), new ChrRegion(Chr2.Chr, Chr2.Begin + (ColRegion.get(j).Begin - 1) * Resolution, Chr2.Begin + (ColRegion.get(j).Terminal - 1) * Resolution)));
                        System.out.println(Chr1.Chr.Name + "\t" + (Chr1.Begin + (RowRegion.get(i).Begin - 1) * Resolution) + "\t" + (Chr1.Begin + (RowRegion.get(i).Terminal - 1) * Resolution) + "\t" + Chr2.Chr.Name + "\t" + (Chr2.Begin + (ColRegion.get(j).Begin - 1) * Resolution) + "\t" + (Chr2.Begin + (ColRegion.get(j).Terminal - 1) * Resolution) + "\t" + String.valueOf(count / area * Size * Size));
                    }
                }
            }
        }
        return ValidList;*/
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
                        AbnormalCount += InterMatrix.getMatrix()[i][j];
                    }
                }
            }
            BackgroundDistribution = new PoissonDistribution((Count - AbnormalCount) * Size * Size / (Area - VacancyArea - A));
        }
    }

    private ArrayList<int[]> ChangePointDetect(String matrixfile, String outprefix) throws IOException {
        String ComLine = "Rscript " + Opts.JarFile.getParent() + "/script/ChangePointDetect.R " + matrixfile + " " + outprefix;
        new Execute(ComLine);
        ArrayList<int[]> ChangePointList = new ArrayList<>();
        List<String> LineStr = FileUtils.readLines(new File(outprefix + ".cpt"), Charset.defaultCharset());
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

    private double[][] SubMatrix(double[][] Matrix, int[] IRegion, int[] JRegion) {
        double[][] submatrix = new double[IRegion[1] - IRegion[0] + 1][JRegion[1] - JRegion[0] + 1];
        for (int i = 0; i < submatrix.length; i++) {
            System.arraycopy(Matrix[IRegion[0] + i], JRegion[0], submatrix[i], 0, submatrix[i].length);
        }
        return submatrix;
    }

    private boolean[][] SearchArea(Matrix Matrix, int Size, double min, double max) throws IOException {
        boolean[][] VMatrix = new boolean[Matrix.getSize()[0]][Matrix.getSize()[1]];//position we want detect
        double[][] Line = new double[Matrix.getSize()[0]][Matrix.getSize()[1] - Size + 1];//value of each col
        double[] Window = new double[Matrix.getSize()[1] - Size + 1];//value of each windows
        for (int i = 0; i < Matrix.getSize()[0]; i++) {
            for (int j = 0; j <= Matrix.getSize()[1] - Size; j++) {
                for (int k = 0; k < Size; k++) {
                    Line[i][j] += (double) Matrix.getMatrix()[i][j + k];
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

//    public void setInterList(ArrayList<InterAction> interList) {
//        InterList = interList;
//    }

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
