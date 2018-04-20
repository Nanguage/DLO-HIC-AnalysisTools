package lib.File;

import java.io.*;
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
}
