package bin;

import lib.Command.Execute;
import lib.File.FileTool;
import lib.Image.Heatmap;
import lib.tool.Tools;
import lib.unit.*;
import org.apache.commons.cli.*;
import org.apache.commons.math3.distribution.PoissonDistribution;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

public class TranslocationDetection {
    private Chromosome Chr1;
    private Chromosome Chr2;
    private InterActMatrix InterMatrix;
    private InterActMatrix FilteredMatrix;
    private ArrayList<InterAction> InterList = new ArrayList<>();
    private int Resolution;
    private double Area;
    private double VacancyArea;
    private double Count;
    private String OutPrefix;
    private double P_Value = 0.01;
    private PoissonDistribution BackgroundDistribution = new PoissonDistribution(1);

    public static void main(String[] args) throws ParseException, IOException {
        Options Arguement = new Options();
        Arguement.addOption(Option.builder("chr").hasArgs().argName("name:size").desc("Chromosome name and size").build());
        Arguement.addOption(Option.builder("r").required().hasArg().argName("int").desc("Resolution").build());
        Arguement.addOption(Option.builder("m").required().longOpt("matrix").argName("file").hasArg().desc("Inter action matrix").build());
        Arguement.addOption(Option.builder("l").required().longOpt("list").argName("file").hasArg().desc("Clustered bedpe file").build());
        Arguement.addOption(Option.builder("p").hasArg().argName("string").desc("out prefix").build());
        Arguement.addOption("v", true, "p value");
        CommandLine Comline = new DefaultParser().parse(Arguement, args);
        Chromosome chr1 = new Chromosome("?"), chr2 = new Chromosome("?");
        if (Comline.hasOption("chr")) {
            String[] s = Comline.getOptionValues("chr");
            if (s[0].split(":").length > 1) {
                chr1 = new Chromosome(s[0].split(":")[0], Integer.parseInt(s[0].split(":")[1]));
            } else {
                chr1 = new Chromosome(s[0].split(":")[0]);
            }
            if (s.length > 1) {
                if (s[1].split(":").length > 1) {
                    chr2 = new Chromosome(s[1].split(":")[0], Integer.parseInt(s[1].split(":")[1]));
                } else {
                    chr2 = new Chromosome(s[1].split(":")[0]);
                }
            } else {
                chr2 = chr1;
            }
        }
        double pvalue = Comline.hasOption("v") ? Double.parseDouble(Comline.getOptionValue("v")) : 0.05;
        String MatrixFile = Comline.getOptionValue("matrix");
        String ListFile = Comline.getOptionValue("list");
        int Resolution = Integer.parseInt(Comline.getOptionValue("r"));
        String Prefix = Comline.hasOption("p") ? Comline.getOptionValue("p") : chr1.Name + "-" + chr2.Name;
//        BufferedReader in = new BufferedReader(new FileReader(ListFile));
        String line;
        String[] str;
        ArrayList<InterAction> List = new ArrayList<>();
//        while ((line = in.readLine()) != null) {
//            str = line.split("\\s+");
//            if (str[0].equals(chr1.Name) && str[3].equals(chr2.Name)) {
//                InterAction inter = new InterAction(new ChrRegion(new String[]{str[0], str[1], str[2]}), new ChrRegion(new String[]{str[3], str[4], str[5]}));
//                inter.Count = Integer.parseInt(str[6]);
//                List.add(inter);
//            } else if (str[3].equals(chr1.Name) && str[0].equals(chr2.Name)) {
//                InterAction inter = new InterAction(new ChrRegion(new String[]{str[3], str[4], str[5]}), new ChrRegion(new String[]{str[0], str[1], str[2]}));
//                inter.Count = Integer.parseInt(str[6]);
//                List.add(inter);
//            }
//        }
        TranslocationDetection Tld = new TranslocationDetection(chr1, chr2, new InterActMatrix(FileTool.ReadMatrixFile(MatrixFile)), List, Resolution, Prefix);
        Tld.setP_Value(pvalue);
        Tld.Run(MatrixFile);
        InterActMatrix filteredmatrix = Tld.getFilteredMatrix();
        Tools.PrintMatrix(filteredmatrix.getMatrix(), Prefix + ".filter.2d.matrix", Prefix + ".filter.spare.matrix");
        //===============
        Heatmap map = new Heatmap(Prefix + ".filter.2d.matrix");
        map.Draw();
        ImageIO.write(map.getImage(), "png", new File(Prefix + ".filter.png"));
    }

    TranslocationDetection(Chromosome chr1, Chromosome chr2, InterActMatrix matrix, ArrayList<InterAction> list, String prefix) {
        Chr1 = chr1;
        Chr2 = chr2;
        InterMatrix = matrix;
        FilteredMatrix = matrix;
        InterList = list;
        OutPrefix = prefix;
        Resolution = InterMatrix.Resolution;
    }

    TranslocationDetection(Chromosome chr1, Chromosome chr2, InterActMatrix matrix, ArrayList<InterAction> list, int Resolution, String prefix) {
        this(chr1, chr2, matrix, list, prefix);
        this.setResolution(Resolution);
    }

    public void Run(String MatrixFile) throws IOException {
        Area = InterMatrix.getSize()[0] * InterMatrix.getSize()[1];
        Count = InterMatrix.Count;
        int Size = 1000000 / Resolution;
        VacancyArea = GetMatrixArea(SearchArea(15, 0, 0));
        CreatBackground(Size);
        ChangePointDetect(MatrixFile, OutPrefix);
        BufferedReader reader = new BufferedReader(new FileReader(OutPrefix + ".row.cpt"));
        String line = reader.readLine();
        int[] RowIndex = StringArrays.toInteger(line.split("\\s+"));
        reader = new BufferedReader(new FileReader(OutPrefix + ".col.cpt"));
        line = reader.readLine();
        int[] ColIndex = StringArrays.toInteger(line.split("\\s+"));
        Heatmap map = new Heatmap(InterMatrix.getMatrix());
        map.Draw();
        BufferedImage HeatMapImage = map.getImage();
        for (int i = 0; i < RowIndex.length; i++) {
            for (int j = 0; j < HeatMapImage.getWidth(); j++) {
                HeatMapImage.setRGB(j, RowIndex[i] - 1, Color.GREEN.getRGB());
            }
        }
        for (int i = 0; i < ColIndex.length; i++) {
            for (int j = 0; j < HeatMapImage.getHeight(); j++) {
                HeatMapImage.setRGB(ColIndex[i] - 1, j, Color.GREEN.getRGB());
            }
        }
        ImageIO.write(HeatMapImage, "png", new File(OutPrefix + ".cpt.png"));
        ArrayList<ChrRegion> RowRegion = new ArrayList<>();
        ArrayList<ChrRegion> ColRegion = new ArrayList<>();
        RowRegion.add(new ChrRegion(Chr1, 1, RowIndex[0] - 1));
        for (int i = 0; i < RowIndex.length - 1; i++) {
            RowRegion.add(new ChrRegion(Chr1, RowIndex[i], RowIndex[i + 1] - 1));
        }
        RowRegion.add(new ChrRegion(Chr1, RowIndex[RowIndex.length - 1], InterMatrix.getSize()[0]));
        ColRegion.add(new ChrRegion(Chr2, 1, ColIndex[0] - 1));
        for (int i = 0; i < ColIndex.length - 1; i++) {
            ColRegion.add(new ChrRegion(Chr2, ColIndex[i], ColIndex[i + 1] - 1));
        }
        ColRegion.add(new ChrRegion(Chr2, ColIndex[ColIndex.length - 1], InterMatrix.getSize()[1]));
        int max = Confidence(P_Value);
        for (int i = 0; i < RowRegion.size(); i++) {
            for (int j = 0; j < ColRegion.size(); j++) {
                double[][] tempmatrix = SubMatrix(InterMatrix.getMatrix(), new int[]{RowRegion.get(i).Begin - 1, RowRegion.get(i).Terminal - 1}, new int[]{ColRegion.get(j).Begin - 1, ColRegion.get(j).Terminal - 1});
                double count = GetCount(tempmatrix);
                double area = (RowRegion.get(i).Terminal - RowRegion.get(i).Begin + 1) * (ColRegion.get(j).Terminal - ColRegion.get(j).Begin + 1);
                if (count / area * Size * Size <= max) {
                    for (int k = RowRegion.get(i).Begin - 1; k <= RowRegion.get(i).Terminal - 1; k++) {
                        for (int l = ColRegion.get(j).Begin - 1; l <= ColRegion.get(j).Terminal - 1; l++) {
                            FilteredMatrix.getMatrix()[k][l] = 0;
                        }
                    }
//                    System.out.println(RowRegion.get(i).Chr.Name + "\t" + RowRegion.get(i).Begin + "\t" + RowRegion.get(i).Terminal + "\t" + ColRegion.get(j).Chr.Name + "\t" + ColRegion.get(j).Begin + "\t" + ColRegion.get(j).Terminal + "\t" + String.valueOf(count / area * 12 * 12));
                }
            }
        }
        //        BufferedWriter out = new BufferedWriter(new FileWriter(OutPrefix + ".sig.cluster"));
//        for (InterAction inter : InterList) {
//            double probability = BackgroundDistribution.cumulativeProbability(inter.Count * Resolution * Resolution / (inter.getLeft().Length * inter.getRight().Length));
//            if (probability > 1 - P_Value) {
//                out.write(inter.getLeft().toString() + "\t" + inter.getRight().toString() + "\t" + inter.Count + "\t" + probability + "\n");
//            }
//        }
//        out.close();
    }

    private void CreatBackground(int Size) throws IOException {
        BackgroundDistribution = new PoissonDistribution(Count * Size * Size / (Area - VacancyArea));
        boolean[][] AbnormalMatrix;
        double A = 0;
        int AbnormalCount = 0;
        BackgroundDistribution = new PoissonDistribution((Count - AbnormalCount) * Size * Size / (Area - VacancyArea - A));
        for (int k = 0; k < 1; k++) {
            int max = Confidence(P_Value);
            AbnormalMatrix = SearchArea(Size, max, Double.MAX_VALUE);
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

    private void ChangePointDetect(String matrixfile, String outprefix) throws IOException {
        String ComLine = "Rscript " + FileTool.GetJarFile().getParent() + "/script/ChangePointDetect.R " + matrixfile + " " + outprefix;
        new Execute(ComLine);
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

    private boolean[][] SearchArea(int Size, double min, double max) throws IOException {
        boolean[][] VMatrix = new boolean[InterMatrix.getSize()[0]][InterMatrix.getSize()[1]];
        double[][] Line = new double[InterMatrix.getSize()[0]][InterMatrix.getSize()[1] - Size + 1];
        double[] Window = new double[InterMatrix.getSize()[1] - Size + 1];
        for (int i = 0; i < InterMatrix.getSize()[0]; i++) {
            for (int j = 0; j <= InterMatrix.getSize()[1] - Size; j++) {
                for (int k = 0; k < Size; k++) {
                    Line[i][j] += InterMatrix.getMatrix()[i][j + k];
                }
            }
        }
        for (int i = 0; i < Size - 1; i++) {
            for (int j = 0; j <= InterMatrix.getSize()[1] - Size; j++) {
                Window[j] += Line[i][j];
            }
        }
        BufferedWriter out = new BufferedWriter(new FileWriter("TlD.distribution"));
        for (int i = Size - 1; i < InterMatrix.getSize()[0]; i++) {
            for (int j = 0; j <= InterMatrix.getSize()[1] - Size; j++) {
                out.write((int) (Window[j] + Line[i][j]) + "\n");
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
        out.close();
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

    public void setInterList(ArrayList<InterAction> interList) {
        InterList = interList;
    }

    public void setInterMatrix(InterActMatrix interMatrix) {
        InterMatrix = interMatrix;
    }

    public InterActMatrix getFilteredMatrix() {
        return FilteredMatrix;
    }

    public void setP_Value(double p_Value) {
        P_Value = p_Value;
    }

    public void setResolution(int resolution) {
        Resolution = resolution;
    }
}
