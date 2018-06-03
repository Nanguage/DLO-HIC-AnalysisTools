package lib.tool;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


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

    public static void PrintMatrix(boolean[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
        BufferedWriter twodfile = new BufferedWriter(new FileWriter(TwoDMatrixFile));
        BufferedWriter sparefile = new BufferedWriter(new FileWriter(SpareMatrix));
        //打印二维矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j]) {
                    twodfile.write(1 + "\t");
                } else {
                    twodfile.write(0 + "\t");
                }
            }
            twodfile.write("\n");
        }
        //打印稀疏矩阵
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix[i].length; j++) {
                if (Matrix[i][j]) {
                    sparefile.write(String.valueOf(i + 1) + "\t" + String.valueOf(j + 1) + "\t" + 1 + "\n");
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
            infile.close();
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
                infile.close();
                return 0;
            }
            infile.close();
            return 1;
        }
        infile.close();
        return 2;
    }

    public static String DateFormat(long Date) {
        return Date / 3600 + "H" + (Date % 3600) / 60 + "M" + (Date % 3600) % 60 + "S";
    }

    public static double UnitTrans(double Num, String PrimaryUint, String TransedUint) {
        String[] Unit = new String[]{"B", "b", "K", "k", "M", "m", "G", "g"};
        Double[] Value = new Double[]{1.0, 1.0, 1000.0, 1000.0, 1000000.0, 1000000.0, 1000000000.0, 1000000000.0};
        HashMap<String, Double> UnitMap = new HashMap<>();
        for (int i = 0; i < Unit.length; i++) {
            UnitMap.put(Unit[i], Value[i]);
        }
        return Num * UnitMap.get(PrimaryUint) / UnitMap.get(TransedUint);
    }

}
