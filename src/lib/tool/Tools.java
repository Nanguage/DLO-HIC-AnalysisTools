package lib.tool;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * @author snowf
 * @version 1.0
 */

public class Tools {
    private Tools() {

    }

    public static void PrintList(ArrayList<?> List, String OutFile) throws IOException {
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        for (Object s : List) {
            outfile.write(s + "\n");
        }
        outfile.close();
    }

    public static void PrintMatrix(Number[][] Matrix, String TwoDMatrixFile, String SpareMatrix) throws IOException {
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
                if (Matrix[i][j].doubleValue() != 0) {
                    sparefile.write((i + 1) + "\t" + (j + 1) + "\t" + Matrix[i][j] + "\n");
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

    public static int ExecuteCommandStr(String CommandStr, String... args) throws IOException, InterruptedException {
        Process P;
        int ExitValue;
        System.out.println(new Date() + "\t" + CommandStr);
        P = Runtime.getRuntime().exec(CommandStr);
        Thread OutThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getInputStream()));
                    if (args.length >= 1 && args[0] != null) {
                        String OutFile = args[0];
                        BufferedWriter bufferedwriter_out = new BufferedWriter(new FileWriter(OutFile));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            bufferedwriter_out.write(line + "\n");
                        }
                        bufferedReaderIn.close();
                        bufferedwriter_out.close();
                    } else {
                        while (bufferedReaderIn.readLine() != null) ;
                        bufferedReaderIn.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread ErrThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String line;
                    BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getErrorStream()));
                    if (args.length >= 2 && args[1] != null) {
                        String LogFile = args[1];
                        BufferedWriter bufferedwriter_err = new BufferedWriter(new FileWriter(LogFile));
                        while ((line = bufferedReaderIn.readLine()) != null) {
                            bufferedwriter_err.write(line + "\n");
                        }
                        bufferedReaderIn.close();
                        bufferedwriter_err.close();
                    } else {
                        while (bufferedReaderIn.readLine() != null) ;
                        bufferedReaderIn.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        OutThread.start();
        ErrThread.start();
        OutThread.join();
        ErrThread.join();
        ExitValue = P.waitFor();
        return ExitValue;
    }

    public static int RemoveEmptyFile(File Dir) {
        int Count = 0;
        File[] FileList = Dir.listFiles();
        if (FileList == null) {
            return Count;
        }
        for (File file : FileList) {
            if (file.isFile() && file.length() == 0) {
                file.delete();
                Count++;
            } else {
                Count += RemoveEmptyFile(file);
            }
        }
        return Count;
    }

    public static void SamToBed(File SamFile, File BedFile) throws IOException {
        System.out.println(new Date() + "\tBegin\t" + SamFile.getName() + " to " + BedFile.getName());
        BufferedReader reader = new BufferedReader(new FileReader(SamFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(BedFile));
        String Line;
        String[] Str;
        String Orientation;
        while ((Line = reader.readLine()) != null) {
            if (Line.matches("^@.*")) {
                continue;
            }
            Str = Line.split("\\s+");
            Orientation = (Integer.parseInt(Str[1]) & 16) == 16 ? "-" : "+";
            writer.write(Str[2] + "\t" + Str[3] + "\t" + (Integer.parseInt(Str[3]) + CalculateFragLength(Str[5]) - 1) + "\t" + Str[0] + "\t" + Str[4] + "\t" + Orientation + "\n");
        }
        writer.close();
        reader.close();
    }

    private static int CalculateFragLength(String s) {
        int Length = 0;
        StringBuilder Str = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case 'M':
                case 'D':
                case 'N':
                    Length += Integer.parseInt(Str.toString());
                    Str.setLength(0);
                    break;
                case 'I':
                case 'S':
                case 'P':
                case 'H':
                    Str.setLength(0);
                    break;
                default:
                    Str.append(s.charAt(i));
            }
        }
        return Length;
    }

    public static String[] GetKmer(String str, int l) {
        if (l > str.length()) {
            return new String[0];
        }
        String[] Kmer = new String[l + 1];
        int length = str.length() - l;
        for (int i = 0; i < l + 1; i++) {
            Kmer[i] = str.substring(i, i + length);
        }
        return Kmer;
    }
}
