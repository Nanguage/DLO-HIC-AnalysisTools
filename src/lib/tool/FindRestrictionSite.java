package lib.tool;

import lib.unit.Chromosome;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class FindRestrictionSite {
    private File FastFile;
    private File OutPath;
    private String Restriction;
    private String Prefix;
    private File ChrSizeFile;
    private Chromosome[] Chromosomes;
    private File[] ChrFragmentFile;

    public FindRestrictionSite(File FastFile, File OutPath, String Restriction, String Prefix) {
        this.FastFile = FastFile;
        this.OutPath = OutPath;
        this.Restriction = Restriction;
        this.Prefix = Prefix;
        this.ChrSizeFile = new File(OutPath + "/" + Prefix + ".ChrSize");
        if (!OutPath.isDirectory() && !OutPath.mkdir()) {
            System.err.println(new Date() + "\tERROR! Can't Create " + OutPath);
            System.exit(1);
        }
    }

    public ArrayList<Chromosome> Run() throws IOException {
        BufferedReader fastfile = new BufferedReader(new FileReader(FastFile));
        BufferedWriter chrwrite;
        ArrayList<File> OutFiles = new ArrayList<>();
        ArrayList<String> list = new ArrayList<>();
        ArrayList<Chromosome> ChrSize = new ArrayList<>();
        StringBuilder Seq = new StringBuilder();
        String line;
        String Chr = "";
        int Site = Restriction.indexOf("^");
        Restriction = Restriction.replace("^", "");
        int ResLength = Restriction.length();
        //找到第一个以 ">" 开头的行
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                Chr = line.split("\\s+")[0].replace(">", "");
                break;
            }
        }
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                int Count = 0;
                int len = Seq.length();
                ChrSize.add(new Chromosome(Chr, len));
                list.add(Chr + "\t" + len);
                File OutFile = new File(OutPath + "/" + Prefix + "." + Chr + ".bed");
                OutFiles.add(OutFile);
                chrwrite = new BufferedWriter(new FileWriter(OutFile));
                chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
                for (int i = 0; i <= len - ResLength; i++) {
                    if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                        Count++;
                        chrwrite.write(Count + "\t+\t" + Chr + "\t" + (i + Site) + "\n");
                    }
                }
                chrwrite.write((++Count) + "\t+\t" + Chr + "\t" + len + "\n");
                chrwrite.close();
                Seq.setLength(0);
                Chr = line.split("\\s+")[0].replace(">", "");
            } else {
                Seq.append(line);
            }
        }
        //========================================打印最后一条染色体=========================================
        int Count = 0;
        int len = Seq.length();
        ChrSize.add(new Chromosome(Chr, len));
        list.add(Chr + "\t" + len);
        File OutFile = new File(OutPath + "/" + Prefix + "." + Chr + ".bed");
        OutFiles.add(OutFile);
        chrwrite = new BufferedWriter(new FileWriter(OutFile));
        chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
        ChrSize.add(new Chromosome(Chr, len));
        for (int i = 0; i <= len - ResLength; i++) {
            if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                Count++;
                chrwrite.write(Count + "\t+\t" + Chr + "\t" + String.valueOf(i + Site) + "\n");
            }
        }
        chrwrite.write((++Count) + "\t+\t" + Chr + "\t" + len + "\n");
        chrwrite.close();
        Seq.setLength(0);
        ChrFragmentFile = OutFiles.toArray(new File[0]);
        Tools.PrintList(list, ChrSizeFile);//打印染色体大小信息
        return ChrSize;
    }

    public File[] getChrFragmentFile() {
        return ChrFragmentFile;
    }

    public File getChrSizeFile() {
        return ChrSizeFile;
    }
}
