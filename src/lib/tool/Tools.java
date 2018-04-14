package lib.tool;

import java.io.*;
import java.util.ArrayList;

public class Tools {
    public static void PrintList(ArrayList<String> List, String OutFile) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        for (String s : List) {
            outfile.write(s + "\n");
        }
        outfile.close();
    }

    public static void PrintMatrix(int[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
        BufferedWriter twodfile = new BufferedWriter(new FileWriter(TwoDMatrixFile));
        BufferedWriter sparefile = new BufferedWriter(new FileWriter(SpareMatrix));
        //打印二维矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                twodfile.write(Matrix[i][j] + "\t");
            }
            twodfile.write("\n");
        }
        //打印稀疏矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j] != 0) {
                    sparefile.write(String.valueOf(i + 1) + "\t" + String.valueOf(j + 1) + "\t" + Matrix[i][j] + "\n");
                }
            }
        }
        sparefile.close();
        twodfile.close();
    }

    public static void PrintMatrix(double[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
        BufferedWriter twodfile = new BufferedWriter(new FileWriter(TwoDMatrixFile));
        BufferedWriter sparefile = new BufferedWriter(new FileWriter(SpareMatrix));
        //打印二维矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                twodfile.write(Matrix[i][j] + "\t");
            }
            twodfile.write("\n");
        }
        //打印稀疏矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j] != 0) {
                    sparefile.write(String.valueOf(i + 1) + "\t" + String.valueOf(j + 1) + "\t" + Matrix[i][j] + "\n");
                }
            }
        }
        sparefile.close();
        twodfile.close();
    }

    public static int BedpeDetect(String BedpeFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        String line;
        String[] str;
        line = infile.readLine();
        if (line == null) {
            return 0;
        }
        str = line.split("\\s+");
        try {
            Integer.parseInt(str[1]);
            Integer.parseInt(str[2]);
            Integer.parseInt(str[4]);
            Integer.parseInt(str[5]);
        } catch (IndexOutOfBoundsException | NumberFormatException i) {
            try {
                Integer.parseInt(str[1]);
                Integer.parseInt(str[3]);
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }
            return 1;
        }
        return 2;
    }
}
