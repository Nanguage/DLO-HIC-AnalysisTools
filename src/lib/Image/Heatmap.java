package lib.Image;

import lib.File.FileTool;
import lib.unit.Chromosome;
import org.apache.commons.cli.*;
import org.apache.commons.math3.distribution.PoissonDistribution;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Heatmap {
    private Chromosome Chr1;
    private Chromosome Chr2;
    private double[][] Matrix;
    private int Width;
    private int Height;
    private int Count;
    private int ColorType = 1;
    private double[] RowCount;
    private double[] ColCount;
    private double MaxValue = 0;
    private BufferedImage HeatmapImage;

    public Heatmap(String matrixfile) throws IOException {
        Matrix = FileTool.ReadMatrixFile(matrixfile);
        Init();
    }

    public Heatmap(int height, int width) {
        Matrix = new double[height][width];
        Init();
    }

    public Heatmap(double[][] matrix) {
        Matrix = matrix;
        Init();
    }

    private void Init() {
        Height = Matrix.length;
        Width = Matrix[0].length;
        RowCount = new double[Height];
        ColCount = new double[Width];
        for (int i = 0; i < Height; i++) {
            for (int j = 0; j < Width; j++) {
                RowCount[i] += Matrix[i][j];
                ColCount[j] += Matrix[i][j];
                if (Matrix[i][j] > MaxValue) {
                    MaxValue = Matrix[i][j];
                }
            }
            Count += RowCount[i];
        }
    }

    public void Draw() {
        HeatmapImage = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < Height; i++) {
            for (int j = 0; j < Width; j++) {
                if (Matrix[i][j] == 0) {
                    HeatmapImage.setRGB(j, i, Color.WHITE.getRGB());
                } else {
//                    im.setRGB(j, i, new Color(255, 255 - (int) (Math.sqrt(Matrix[i][j] / Max) * 255), 255 - (int) (Math.sqrt(Matrix[i][j] / Max) * 255)).getRGB());
                    HeatmapImage.setRGB(j , i, new Color(255, 0, 0).getRGB());
                }
            }
        }
    }

    public void Test() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("Test.out"));
        for (int i = 0; i < RowCount.length; i++) {
            writer.write(RowCount[i] + "\t");
        }
        writer.write("\n");
        for (int i = 0; i < ColCount.length; i++) {
            writer.write(ColCount[i] + "\t");
        }
        writer.write("\n");
        writer.close();
    }

    public static void main(String[] args) throws IOException, ParseException {
        Options Argument = new Options();
        Argument.addOption("f", true, "Matrix file");
        Argument.addOption("o", true, "Out file");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp DLO-HiC-AnalysisTools.jar lib.Image.Heatmap [option]", Argument);
            System.exit(1);
        }
        CommandLine Comline = new DefaultParser().parse(Argument, args);
        String MatrixFile = Comline.getOptionValue("f");
        String OutFile = Comline.getOptionValue("o");
        Heatmap map = new Heatmap(MatrixFile);
        map.Draw();
        map.Test();
        BufferedImage im = map.getImage();
        ImageIO.write(im, "png", new File(OutFile));
    }

    public BufferedImage getImage() {
        return HeatmapImage;
    }

    private void setRegionColor(int x1, int y1, int x2, int y2, Color c) {
        for (int i = Math.min(x1, x2); i <= Math.max(x1, x2); i++) {
            for (int j = Math.min(y1, y2); j <= Math.max(y1, y2); j++) {
                HeatmapImage.setRGB(j, i, c.getRGB());
            }
        }
    }


    public void setChr1(Chromosome chr1) {
        Chr1 = chr1;
    }

    public void setChr2(Chromosome chr2) {
        Chr2 = chr2;
    }

    public void setMatrix(double[][] matrix) {
        Matrix = matrix;
    }

    public void Set(int x, int y, double v) {
        Matrix[x][y] = v;
    }

}
