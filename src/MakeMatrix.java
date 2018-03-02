import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;


public class MakeMatrix {
    private String OptOutPath = "OutPath";
    private String OptPrefix = "Prefix";
    private String OptInterBedpeFile = "InterBedpeFile";
    private String OptChromosome = "Chromosome";
    public String OptThread = "Thread";
    private String OptResolution = "Resolution";
    private String OptChrSzieFile = "ChrSzieFile";
    private String OptChromosomeSize = "ChrSize";
    //===============================================================
    private String OutPath;//输出路径
    private String Prefix;//输出前缀
    private String InterBedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    public int Thread;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private String ChrSzieFile;
    //==============================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptInterBedpeFile, OptResolution};
    private String[] OptionalParameter = new String[]{OptChromosome, OptOutPath, OptPrefix, OptThread, OptChrSzieFile};


    MakeMatrix(String outpath, String outprefix, String validpairs, String[] chrosomose, int[] chrsize, int resolution) throws IOException {
        ParameterInit();
        ParameterList.put(OptOutPath, outpath);
        ParameterList.put(OptPrefix, outprefix);
        ParameterList.put(OptInterBedpeFile, validpairs);
        String temp = "";
        if (chrosomose.length > 0) {
            temp = chrosomose[0];
            for (int i = 1; i < chrosomose.length; i++) {
                temp = temp + " " + chrosomose[i];
            }
        }
        ParameterList.put(OptChromosome, temp);
        temp = "";
        if (chrsize.length > 0) {
            temp = String.valueOf(chrsize[0]);
            for (int i = 1; i < chrsize.length; i++) {
                temp = temp + " " + chrsize[i];
            }
        }
        ParameterList.put(OptChromosomeSize, temp);
        ParameterList.put(OptResolution, String.valueOf(resolution));
        Init();
    }

    MakeMatrix(String ConfigFile) throws IOException {
        ParameterInit();
        GetOption(ConfigFile);
        Init();
    }

    public void Run() throws IOException {
        Routine P = new Routine();
        P.Threads = Thread;
        int[][] Matrix = P.CreatInterActionMatrix(InterBedpeFile, Chromosome, ChromosomeSize, Resolution, InterMatrixPrefix);
        double[][] NormalizeMatrix = P.MatrixNormalize(Matrix);
        CommonMethod.PrintMatrix(NormalizeMatrix, NormalizeMatrixPrefix + ".2d.matrix", NormalizeMatrixPrefix + ".spare.matrix");
        String[] ChrInterBedpeFile = P.SeparateInterBedpe(InterBedpeFile, Chromosome, OutPath + "/" + Prefix, "");
        for (int i = 0; i < Chromosome.length; i++) {
            String ChrInterMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".inter";
            String ChrNormalizeMatrixPrefix = OutPath + "/" + Prefix + "." + Chromosome[i] + ".normalize";
            Matrix = P.CreatInterActionMatrix(ChrInterBedpeFile[i], new String[]{Chromosome[i]}, new int[]{ChromosomeSize[i]}, Resolution / 10, ChrInterMatrixPrefix);
            NormalizeMatrix = P.MatrixNormalize(Matrix);
            CommonMethod.PrintMatrix(NormalizeMatrix, ChrNormalizeMatrixPrefix + ".2d.matrix", ChrNormalizeMatrixPrefix + ".spare.matrix");
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp DLO-HIC-AnalysisTools.jar MakeMatrix <Config.txt>");
            System.exit(0);
        }
        MakeMatrix mm = new MakeMatrix(args[0]);
        mm.ShowParameter();
        mm.Run();
    }

    private void GetOption(String optionfile) throws IOException {
        BufferedReader option = new BufferedReader(new FileReader(optionfile));
        String line;
        String[] str;
        while ((line = option.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s*=\\s*|\\s+");
            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
                ParameterList.put(str[0], str[1]);
            }
        }
        option.close();
    }

    public void ParameterInit() {
        for (String opt : RequiredParameter) {
            ParameterList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ParameterList.put(opt, "");
        }
        ParameterList.put(OptOutPath, "./");
        ParameterList.put(OptPrefix, "Matrix");
        ParameterList.put(OptThread, "1");
    }

    public boolean SetParameter(String Key, String Value) {
        if (ParameterList.containsKey(Key)) {
            ParameterList.put(Key, Value);
            return true;
        } else {
            return false;
        }
    }

    public void ShowParameter() {
        for (String opt : RequiredParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
        System.out.println("===============================================================================");
        for (String opt : OptionalParameter) {
            System.out.println(opt + ":\t" + ParameterList.get(opt));
        }
    }

    private void Init() throws IOException {
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
        //=======================================================
        OutPath = ParameterList.get(OptOutPath);
        Prefix = ParameterList.get(OptPrefix);
        InterBedpeFile = ParameterList.get(OptInterBedpeFile);
        ChrSzieFile = ParameterList.get(OptChrSzieFile);
        Chromosome = ParameterList.get(OptChromosome).split("\\s+");
        ChromosomeSize = new int[ParameterList.get(OptChromosomeSize).split("\\s+").length];
        for (int i = 0; i < ParameterList.get(OptChromosomeSize).split("\\s+").length; i++) {
            ChromosomeSize[i] = Integer.parseInt(ParameterList.get(OptChromosomeSize).split("\\s+")[i]);
        }
        Resolution = Integer.parseInt(ParameterList.get(OptResolution));
        Thread = Integer.parseInt(ParameterList.get(OptThread));
        //=======================================================
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdirs()) {
                System.err.println("Can't Creat " + OutPath);
                System.exit(0);
            }
        }
        if (ChrSzieFile.equals("") && (Chromosome.length == 0 || ChromosomeSize.length == 0)) {
            System.err.println("Error ! No Chromosome or ChromosomeSize");
            System.exit(0);
        } else if (Chromosome.length == 0 || ChromosomeSize.length == 0) {
            if (!new File(ChrSzieFile).isFile()) {
                System.err.println("Wrong ChrSzieFile " + ChrSzieFile + "is not a file");
                System.exit(0);
            }
            ExtractChrSize();
        }
        InterMatrixPrefix = OutPath + "/" + Prefix + ".inter";
        NormalizeMatrixPrefix = OutPath + "/" + Prefix + ".normalize";
    }


    public void ExtractChrSize() throws IOException {
        BufferedReader chrsize = new BufferedReader(new FileReader(ChrSzieFile));
        ArrayList<String[]> list = new ArrayList<>();
        String line;
        String[] str;
        while ((line = chrsize.readLine()) != null) {
            str = line.split("\\s+");
            list.add(str);
        }
        Chromosome = new String[list.size()];
        ChromosomeSize = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Chromosome[i] = list.get(i)[0];
            ChromosomeSize[i] = Integer.parseInt(list.get(i)[1]);
        }
    }

}
