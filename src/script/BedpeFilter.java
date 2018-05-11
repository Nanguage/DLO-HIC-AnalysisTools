package script;

import lib.File.FileTool;
import lib.tool.BedPeFilter;
import lib.tool.Tools;
import lib.unit.ChrRegion;
import lib.unit.InterAction;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.ArrayList;

public class BedpeFilter {
    public static void main(String[] args) throws ParseException, IOException {
        Options Argumemt = new Options();
        Argumemt.addOption("i", true, "bedpefile");
        Argumemt.addOption("f", true, "filter list");
        Argumemt.addOption("o", true, "out file");
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp " + FileTool.GetJarFile() + " " + FileTool.GetClassPath(BedpeFilter.class) + " [option]", Argumemt);
            System.exit(1);
        }
        CommandLine Comline = new DefaultParser().parse(Argumemt, args);
        String InFile = Comline.getOptionValue("i");
        String FilterFile = Comline.getOptionValue("f");
        String OutFile = Comline.getOptionValue("o");
//        ArrayList<InterAction> List = FileTool.ReadInterActionFile(InFile, -1, -1, -1, -1);
        ArrayList<InterAction> FilterList = FileTool.ReadInterActionFile(FilterFile, -1, 6, -1, -1);
        BedPeFilter Filter = new BedPeFilter(FilterList);
        BufferedReader in = new BufferedReader(new FileReader(InFile));
        BufferedWriter out = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        if (Tools.BedpeDetect(InFile) == 1) {
            while ((line = in.readLine()) != null) {
                str = line.split("\\s+");
                if (Filter.Run(new InterAction(new ChrRegion(new String[]{str[0], str[1], str[1]}), new ChrRegion(new String[]{str[2], str[3], str[3]})))) {
                    out.write(line + "\n");
                }
            }
        } else if (Tools.BedpeDetect(InFile) == 2) {
            while ((line = in.readLine()) != null) {
                str = line.split("\\s+");
                if (Filter.Run(new InterAction(new ChrRegion(new String[]{str[0], str[1], str[2]}), new ChrRegion(new String[]{str[3], str[4], str[5]})))) {
                    out.write(line + "\n");
                }
            }
        } else {
            System.err.println("Error format");
            System.exit(1);
        }
        out.close();
    }
}
