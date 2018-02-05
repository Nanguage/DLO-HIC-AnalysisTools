import java.io.*;
import java.util.ArrayList;


public class MakeMatrix {
    private String OutPath = "./";//输出路径
    private String OutPrefix = "out";//输出前缀
    private String InterBedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private String ChrSzieFile;
    public int Threads = 1;


    MakeMatrix(String outpath, String outprefix, String validpairs, String[] chrosomose, int[] chrsize, int resolution) throws IOException {
        OutPath = outpath;
        OutPrefix = outprefix;
        InterBedpeFile = validpairs;
        Chromosome = chrosomose;
        ChromosomeSize = chrsize;
        Resolution = resolution;
        Init();
    }

    MakeMatrix(String optionfile) throws IOException {
        GetOption(optionfile);
    }

    public void Run() throws IOException {
        Routine P = new Routine();
        P.Threads = Threads;
        int[][] Matrix = P.CreatInterActionMatrix(InterBedpeFile, Chromosome, ChromosomeSize, Resolution, InterMatrixPrefix);
        double[][] NormalizeMatrix = P.MatrixNormalize(Matrix);
        CommonMethod.PrintMatrix(NormalizeMatrix, NormalizeMatrixPrefix + ".2d.matrix", NormalizeMatrixPrefix + ".spare.matrix");
        String[] ChrInterBedpeFile = P.SeparateInterBedpe(InterBedpeFile, Chromosome, OutPath + "/" + OutPrefix, "");
        for (int i = 0; i < Chromosome.length; i++) {
            String ChrInterMatrixPrefix = OutPath + "/" + OutPrefix + "." + Chromosome[i] + ".inter";
            String ChrNormalizeMatrixPrefix = OutPath + "/" + OutPrefix + "." + Chromosome[i] + ".normalize";
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
            switch (str[0]) {
                case "InterBedpeFile":
                    InterBedpeFile = str[1];
                    break;
                case "OutPath":
                    OutPath = str[1];
                    break;
                case "OutPrefix":
                    OutPrefix = str[1];
                    break;
                case "ChrSizeFile":
                    ChrSzieFile = str[1];
                    break;
                case "Chromosome":
                    Chromosome = new String[str.length - 1];
                    System.arraycopy(str, 1, Chromosome, 0, str.length - 1);
                    break;
                case "Thread":
                    Threads = Integer.parseInt(str[1]);
                    break;
                case "Resolution":
                    Resolution = Integer.parseInt(str[1]);
                    break;
            }
        }
        option.close();
        Init();
    }

    private void Init() throws IOException {
        if (!new File(OutPath).isDirectory()) {
            if (!new File(OutPath).mkdirs()) {
                System.err.println("Can't Creat " + OutPath);
                System.exit(0);
            }
        }
        if (InterBedpeFile == null) {
            System.err.println("Error ! No InterBedpeFile");
            System.exit(0);
        } else if (!new File(InterBedpeFile).isFile()) {
            System.err.println("Wrong InterBedpeFile " + InterBedpeFile + "is not a file");
            System.exit(0);
        }
        if (Resolution == 0) {
            System.err.println("Error ! No Resolution");
            System.exit(0);
        }
        if (ChrSzieFile == null && (Chromosome == null || ChromosomeSize == null)) {
            System.err.println("Error ! No Chromosome or ChromosomeSize");
            System.exit(0);
        } else if (Chromosome == null || ChromosomeSize == null) {
            if (!new File(ChrSzieFile).isFile()) {
                System.err.println("Wrong ChrSzieFile " + ChrSzieFile + "is not a file");
                System.exit(0);
            }
            ExtractChrSize();
        }
        InterMatrixPrefix = OutPath + "/" + OutPrefix + ".inter";
        NormalizeMatrixPrefix = OutPath + "/" + OutPrefix + ".normalize";
    }

    public void ShowParameter() {
        System.out.println("InterBedpeFile:\t" + InterBedpeFile);
        System.out.println("OutPath:\t" + OutPath);
        System.out.println("OutPrefix:\t" + OutPrefix);
        if (ChrSzieFile != null) {
            System.out.println("ChrSzieFile:\t" + ChrSzieFile);
        }
        System.out.print("Chromosome:");
        for (String Chr : Chromosome) {
            System.out.print("\t" + Chr);
        }
        System.out.println();
        System.out.print("ChrSize:");
        for (int ChrSize : ChromosomeSize) {
            System.out.print("\t" + ChrSize);
        }
        System.out.println();
        System.out.println("Resolution:\t" + Resolution);
        System.out.println("Thread:\t" + Threads);
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
