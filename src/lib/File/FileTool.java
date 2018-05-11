package lib.File;

import lib.tool.Tools;
import lib.unit.ChrRegion;
import lib.unit.InterActMatrix;
import lib.unit.InterAction;
import lib.unit.StringArrays;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public class FileTool {
    public static File GetJarFile() {
        return new File(FileTool.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    }

    public static String GetClassPath(Class c) {
        return (c.getPackage() == null ? "" : c.getPackage().getName() + ".") + c.getName();
    }

    public static InputStreamReader GetFileStream(String s) {
        return new InputStreamReader(FileTool.class.getResourceAsStream(s));
    }

    public static int FastqPhred(String FastqFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(FastqFile));
        int[] FormatEdge = new int[]{(int) '9', (int) 'a'};
        int[] Count = new int[2];
        String line;
        int LineNum = 0;
        while ((line = infile.readLine()) != null && LineNum <= 400) {
            if (++LineNum % 4 != 0) {
                continue;
            }
            for (int i = 0; i < line.length(); i++) {
                if ((int) line.charAt(i) <= FormatEdge[0]) {
                    Count[0]++;
                } else if ((int) line.charAt(i) >= FormatEdge[1]) {
                    Count[1]++;
                }
            }
        }
        return Count[0] >= Count[1] ? 33 : 64;
    }

    public static void Append(String File1, String File2) throws IOException {
        System.out.println(new Date() + "\tAppend " + File1 + " to " + File2);
        String line;
        BufferedReader read = new BufferedReader(new FileReader(File1));
        BufferedWriter write = new BufferedWriter(new FileWriter(File2, true));
        while ((line = read.readLine()) != null) {
            write.write(line + "\n");
        }
        read.close();
        write.close();
        System.out.println(new Date() + "\tEnd append " + File1 + " to " + File2);
    }

    public static void Merge(String[] File, String MergeFile) throws IOException {
        BufferedWriter write_file = new BufferedWriter(new FileWriter(MergeFile));
        String line;
        for (String x : File) {
            System.out.println(new Date() + "\tMerge " + x + " to " + MergeFile);
            BufferedReader read_file = new BufferedReader(new FileReader(x));
            while ((line = read_file.readLine()) != null) {
                write_file.write(line + "\n");
            }
            read_file.close();
        }
        write_file.close();
        System.out.println(new Date() + "\tDone merge");
    }

    public static void MergeSamFile(String[] InFile, String MergeFile) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(MergeFile));
        String line;
        BufferedReader gethead = new BufferedReader(new FileReader(InFile[0]));
        while ((line = gethead.readLine()) != null && line.matches("^@.*")) {
            out.write(line + "\n");
        }
        gethead.close();
        for (String file : InFile) {
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

/*    public static int[][] ReadMatrixFile(String file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<int[]> List = new ArrayList<>();
        while ((line = in.readLine()) != null) {
            List.add(StringArrays.toInteger(line.split("\\s+")));
        }
        in.close();
        int[][] Matrix = new int[List.size()][];
        for (int i = 0; i < List.size(); i++) {
            System.arraycopy(List.get(i), 0, Matrix[i], 0, List.get(i).length);
        }
        return Matrix;
    }*/

    public static double[][] ReadMatrixFile(String file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        ArrayList<double[]> List = new ArrayList<>();
        while ((line = in.readLine()) != null) {
            List.add(StringArrays.toDouble(line.split("\\s+")));
        }
        in.close();
        double[][] matrix = new double[List.size()][];
        for (int i = 0; i < List.size(); i++) {
            matrix[i] = new double[List.get(i).length];
            System.arraycopy(List.get(i), 0, matrix[i], 0, List.get(i).length);
        }
        return matrix;
    }

    public static ArrayList<InterAction> ReadInterActionFile(String file, int Name, int Count, int Fragment1, int Fragment2) throws IOException {
        String line;
        String[] str;
        ArrayList<InterAction> List = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(file));
        if (Tools.BedpeDetect(file) == 1) {
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
        } else if (Tools.BedpeDetect(file) == 2) {
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
