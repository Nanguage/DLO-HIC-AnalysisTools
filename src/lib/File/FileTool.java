package lib.File;

import lib.unit.*;

import java.io.*;
import java.util.ArrayList;

public class FileTool {

    public static InputStreamReader GetFileStream(String s) {
        return new InputStreamReader(FileTool.class.getResourceAsStream(s));
    }

    public static void ExtractFile(String InternalFile, File OutFile) throws IOException {
        BufferedReader reader = new BufferedReader(GetFileStream(InternalFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(OutFile));
        String Line;
        while ((Line = reader.readLine()) != null) {
            writer.write(Line + "\n");
        }
        writer.close();
    }

    public static void MergeSamFile(File[] InFile, File MergeFile) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(MergeFile));
        String line;
        BufferedReader gethead = new BufferedReader(new FileReader(InFile[0]));
        while ((line = gethead.readLine()) != null && line.matches("^@.*")) {
            out.write(line + "\n");
        }
        gethead.close();
        for (File file : InFile) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            while ((line = in.readLine()) != null) {
                if (line.matches("^@.*")) {
                    continue;
                }
                out.write(line + "\n");
            }
            in.close();
        }
        out.close();
    }

    public static Double[][] ReadMatrixFile(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<double[]> List = new ArrayList<>();
        while ((line = in.readLine()) != null) {
            List.add(StringArrays.toDouble(line.split("\\s+")));
        }
        in.close();
        Double[][] matrix = new Double[List.size()][];
        for (int i = 0; i < List.size(); i++) {
            matrix[i] = new Double[List.get(i).length];
            for (int j = 0; j < List.get(i).length; j++) {
                matrix[i][j] = List.get(i)[j];
            }
        }
        return matrix;
    }

    public static ArrayList<InterAction> ReadInterActionFile(CustomFile file, int Name, int Count, int Fragment1, int Fragment2) throws IOException {
        String line;
        String[] str;
        ArrayList<InterAction> List = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(file));
        if (file.BedpeDetect() == Opts.FileFormat.BedpePointFormat) {
            while ((line = in.readLine()) != null) {
                str = line.split("\\s+");
                InterAction inter = new InterAction(new ChrRegion(new String[]{str[0], str[1], str[1]}), new ChrRegion(new String[]{str[2], str[3], str[3]}));
                if (Name >= 0) {
                    try {
                        inter.Name = str[Name];
                    } catch (IndexOutOfBoundsException e) {
                        inter.Name = null;
                    }
                }
                if (Count >= 0) {
                    try {
                        inter.Count = Integer.parseInt(str[Count]);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        inter.Count = 1;
                    }
                }
                if (Fragment1 >= 0 && Fragment2 >= 0) {
                    try {
                        inter.LeftFragment = Integer.parseInt(str[Fragment1]);
                        inter.RightFragment = Integer.parseInt(str[Fragment2]);
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    }
                }
                List.add(inter);
            }
        } else if (file.BedpeDetect() == Opts.FileFormat.BedpeRegionFormat) {
            while ((line = in.readLine()) != null) {
                str = line.split("\\s+");
                InterAction inter = new InterAction(new ChrRegion(new String[]{str[0], str[1], str[2]}), new ChrRegion(new String[]{str[3], str[4], str[5]}));
                if (Name >= 0) {
                    try {
                        inter.Name = str[Name];
                    } catch (IndexOutOfBoundsException e) {
                        inter.Name = null;
                    }
                }
                if (Count >= 0) {
                    try {
                        inter.Count = Integer.parseInt(str[Count]);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        inter.Count = 1;
                    }
                }
                if (Fragment1 >= 0 && Fragment2 >= 0) {
                    try {
                        inter.LeftFragment = Integer.parseInt(str[Fragment1]);
                        inter.RightFragment = Integer.parseInt(str[Fragment2]);
                    } catch (IndexOutOfBoundsException | NumberFormatException ignored) {
                    }
                }
                List.add(inter);
            }
        } else {
            System.out.println("Error format");
            System.exit(1);
        }
        in.close();
        return List;
    }
}
