
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class EditConfig {
    private ArrayList<String[]> List;
    private Hashtable<String, String> HashList;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage:  java " + Thread.currentThread().getStackTrace()[1].getClassName() + " <Configure file> <Parameter> [Value1 [Value2] ...]");
            System.exit(0);
        }
        String ConfigFile = args[0];
        String Parameter = args[1];
        EditConfig edit = new EditConfig();
        if (Parameter.equals("All") || Parameter.equals("Pre") || Parameter.equals("Se") || Parameter.equals("Bedpe") || Parameter.equals("Matrix")) {
            edit.Creat(ConfigFile, Parameter);
        } else {
            String[] Value;
            if (args.length == 2) {
                Value = new String[]{""};
            } else {
                Value = new String[args.length - 2];
                System.arraycopy(args, 2, Value, 0, Value.length);
            }
            edit.Edit(Parameter, Value);
            edit.PrintConfig(ConfigFile);
        }
    }

    public void Creat(String ConfigFile, String Type) throws IOException {
        String Config;
        if (Type.equals("All")) {
            Main m = new Main();
            m.OptionListInit();
            HashList = m.getParameterList();
            if (new File(ConfigFile).isFile()) {
                String line;
                String[] str;
                BufferedReader configfile = new BufferedReader(new FileReader(ConfigFile));
                while ((line = configfile.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("")) {
                        continue;
                    }
                    str = line.split("\\s*=\\s*");
                    if (HashList.containsKey(str[0]) && str.length >= 2) {
                        HashList.put(str[0], str[1]);
                    }
                }
            }
            System.out.println("//------------------------------Required parameters----------------------------");
            for (String s : m.getRequiredParameter()) {
                System.out.println(s + " = " + HashList.get(s));
            }
            System.out.println("//------------------------------optional parameters----------------------------");
            for (String s : m.getOptionalParameter()) {
                System.out.println(s + " = " + HashList.get(s));
            }
        } else if (Type.equals("Pre")) {
            PreProcess pre = new PreProcess();
            pre.OptionListInit();
            HashList = pre.getOptionList();
            if (new File(ConfigFile).isFile()) {
                String line;
                String[] str;
                BufferedReader configfile = new BufferedReader(new FileReader(ConfigFile));
                while ((line = configfile.readLine()) != null) {
                    line = line.trim();
                    if (line.equals("")) {
                        continue;
                    }
                    str = line.split("\\s*=\\s*");
                    if (HashList.containsKey(str[0]) && str.length >= 2) {
                        HashList.put(str[0], str[1]);
                    }
                }
            }
            System.out.println("//------------------------------Required parameters----------------------------");
            for (String s : pre.getRequiredParameter()) {
                System.out.println(s + " = " + HashList.get(s));
            }
            System.out.println("//------------------------------optional parameters----------------------------");
            for (String s : pre.getOptionalParameter()) {
                System.out.println(s + " = " + HashList.get(s));
            }

        } else if (Type.equals("Se")) {
            Config = "//------------------------------Required parameters----------------------------\n" +
                    "PastFile = ./DLO-HiC.linkerfilter.output.txt\n" +
                    "LinkersType = AA\n" +
                    "RestrictionSeq = T^TAA\n" +
                    "Index = Hg19\n" +
                    "Type = R1\n" +
                    "GenomeFile = Hg19.clean.fna\n" +
                    "//------------------------------Optional parameters---------------------------\n" +
                    "OutPath = ./SeProcess\n" +
                    "OutPrefix = test\n" +
                    "Phred = 33\n" +
                    "UseLinker = AA\n" +
                    "AlignThread = 10\n" +
                    "Thread = 4\n" +
                    "AlignMinQuality = 20\n" +
                    "AlignMisMatch = 0\n" +
                    "MinLinkerFilterQuality = 32\n" +
                    "MinReadsLength = 16\n" +
                    "MaxReadsLength = 20";
        } else if (Type.equals("Bedpe")) {
            Config = "//------------------------------Required parameters----------------------------\n" +
                    "BedpeFile = ./Hic.bedpe\n" +
                    "LinkerType = AA\n" +
                    "Chromosome = chr1 chr2 chr3 chr4 chr5 chr6 chr7 chr8 chr9 chr10 chr11 chr12 chr13 chr14 chr15 chr16 chr17 chr18 chr19 chr20 chr21 chr22 chrX chrY" +
                    "EnzyFilePrefix = " +
                    "//------------------------------Optional parameters---------------------------\n" +
                    "OutPath = ./BedpeProcess\n" +
                    "OutPrefix = test\n" +
                    "Thread = 4\n";
        } else {
            Config = "//------------------------------Required parameters----------------------------\n" +
                    "InterBedpeFile = ./Hic.bedpe\n" +
                    "Chromosome = chr1 chr2 chr3 chr4 chr5 chr6 chr7 chr8 chr9 chr10 chr11 chr12 chr13 chr14 chr15 chr16 chr17 chr18 chr19 chr20 chr21 chr22 chrX chrY" +
                    "Resolution = 1000000" +
                    "ChrSizeFile = Chromosome.chrsize.txt" +
                    "//------------------------------Optional parameters---------------------------\n" +
                    "OutPath = ./MakeMatrix\n" +
                    "OutPrefix = test\n" +
                    "Thread = 4\n";
        }
    }

    public void Edit(String Parameter, String[] Value) throws IOException {
        boolean Flag = false;
        String[] str = new String[Value.length + 1];
        str[0] = Parameter;
        System.arraycopy(Value, 0, str, 1, Value.length);
        for (int i = 0; i < List.size(); i++) {
            if (List.get(i)[0].equals(Parameter)) {
                if (Value.length == 0 || Value[0].equals("")) {
                    List.remove(i);
                } else {
                    List.set(i, str);
                }
                Flag = true;
                break;
            }
        }
        if (!Flag) {
            if (!Value[0].equals("")) {
                List.add(str);
            }
        }
    }

    EditConfig(String ConfigFile) throws IOException {
        Init(ConfigFile);
    }

    EditConfig() {
        List = new ArrayList<>();
        HashList = new Hashtable<>();
    }

    public void Init(String File) throws IOException {
        BufferedReader configfile = new BufferedReader(new FileReader(File));
        String line;
        String[] str;
        List = new ArrayList<>();
        while ((line = configfile.readLine()) != null) {
            if (line.equals("")) {
                List.add(new String[]{line});
                continue;
            }
            str = line.split("\\s*=\\s*|\\s+");
            List.add(str);
        }
    }

    public void PrintConfig(String ConfigFile) throws IOException {
        BufferedWriter configfile = new BufferedWriter(new FileWriter(ConfigFile));
        for (String[] ele : List) {
            if (ele[0].matches("^//.*")) {
                System.out.print(ele[0]);
            } else {
                System.out.print(ele[0] + " =");
            }
            for (int i = 1; i < ele.length; i++) {
                configfile.write(" " + ele[i]);
            }
            configfile.write("\n");
        }
        configfile.close();
    }

    public void PrintConfig() {
        for (String[] ele : List) {
            if (ele[0].matches("^//.*")) {
                System.out.print(ele[0]);
            } else {
                System.out.print(ele[0] + " =");
            }
            for (int i = 1; i < ele.length; i++) {
                System.out.print(" " + ele[i]);
            }
            System.out.print("\n");
        }
    }


    private String ConfigStr = "FastqFile = HIC.fastq\n" +
            "GenomeFile = Hg19.clean.fna\n" +
            "Chromosome = 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y\n" +
            "LinkersType = AA BB AB BA\n" +
            "RestrictionSeq = T^TAA\n" +
            "LinkerFile = linker.txt\n" +
            "MaxMisMatchLength = 3\n" +
            "Index = Hg19\n" +
            "AlignMisMatch = 0\n" +
            "MinReadsLength = 16\n" +
            "MaxReadsLength = 20\n" +
            "AlignMinQuality = 20\n" +
            "OutPrefix = DLO-HiC\n" +
            "OutPath = ./\n" +
            "AdapterFile = adapter.txt\n" +
            "Phred = 33\n" +
            "UseLinker = AA BB\n" +
            "ChromosomePrefix = chr\n" +
            "MatchScore = 1\n" +
            "MisMatchScore = -2\n" +
            "IndelScore = -2\n" +
            "AlignThread = 10\n" +
            "Resolution = 1000000\n" +
            "Thread = 4";
}
