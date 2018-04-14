package script;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import lib.tool.Tools;
import lib.unit.IntegerArrays;

public class PowerLaw {
    private String BedpeFile;
    private int StepLength;
    private String OutFile;
    Options Arguement = new Options();


    PowerLaw(String bedpe, int length) {
        BedpeFile = bedpe;
        StepLength = length;
    }

    PowerLaw(String[] args) {
        ArguementInit();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(Arguement, args);
            if (line.hasOption("h")) {
                new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.PowerLaw [option]", Arguement);
                System.exit(0);
            }
            BedpeFile = line.getOptionValue("f");
            StepLength = Integer.parseInt(line.getOptionValue("l"));
            OutFile = line.getOptionValue("o");
        } catch (ParseException exp) {
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar script.PowerLaw [option]", Arguement);
            System.exit(0);
        }
    }

    public static void main(String[] args) throws IOException {
        new PowerLaw(args).Creat();
    }

    public ArrayList<int[]> Creat() throws IOException {
        ArrayList<int[]> List = new ArrayList<>();
        List.add(new int[]{0, StepLength, 0});
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        String line;
        String[] str;
        int distant;
        if (Tools.BedpeDetect(BedpeFile) == 1) {
            while ((line = infile.readLine()) != null) {
                str = line.split("\\s+");
                distant = Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3]));
                int i = 0;
                while (i < List.size()) {
                    if (distant > List.get(i)[1]) {
                        i++;
                    } else {
                        List.get(i)[2]++;
                        break;
                    }
                }
                if (i == List.size()) {
                    List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                    while (List.get(i)[1] < distant) {
                        i++;
                        List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                    }
                    List.get(i)[2]++;
                }
            }
        } else if (Tools.BedpeDetect(BedpeFile) == 2) {
            while ((line = infile.readLine()) != null) {
                str = line.split("\\s+");
                distant = Math.abs(Integer.parseInt(str[5]) + Integer.parseInt(str[4]) - Integer.parseInt(str[2]) - Integer.parseInt(str[1])) / 2;
                int i = 0;
                while (i < List.size()) {
                    if (distant > List.get(i)[1]) {
                        i++;
                    } else {
                        List.get(i)[2]++;
                        break;
                    }
                }
                if (i == List.size()) {
                    List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                    while (List.get(i)[1] < distant) {
                        i++;
                        List.add(new int[]{List.get(i - 1)[1] + 1, List.get(i - 1)[1] + StepLength, 0});
                    }
                    List.get(i)[2]++;
                }
            }
        } else {
            System.err.println("Error format!");
            System.exit(0);
        }
        if (OutFile != null) {
            ArrayList<String> templist = new ArrayList<>();
            for (int[] anlist : List) {
                templist.add(String.join("\t", IntegerArrays.toString(anlist)));
            }
            Tools.PrintList(templist, OutFile);
        }
        return List;
    }

    private void ArguementInit() {
        Arguement.addOption(Option.builder("f").argName("file").hasArg().required().desc("[required] bedpe file").build());
        Arguement.addOption(Option.builder("l").longOpt("step").argName("int").hasArg().required().desc("[required] step length").build());
        Arguement.addOption(Option.builder("o").longOpt("out").argName("file").hasArg().desc("out file").build());
        Arguement.addOption("h", "help", false, "print help message");
    }
}
