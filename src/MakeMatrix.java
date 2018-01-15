import java.io.*;


public class MakeMatrix {
    private String OutPath;//输出路径
    private String OutPrefix;//输出前缀
    private String InterBedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    public int Threads = 1;


    MakeMatrix(String outpath, String outprefix, String validpairs, String[] chrosomose, int[] chrsize, int resolution) {
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
        String[] ChrInterBedpeFile = P.SeperateInterBedpe(InterBedpeFile, Chromosome, OutPath + "/" + OutPrefix, "");
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
            System.out.println("Error ! No parameter");
            System.out.println("Usage: java MakeMatrix Config.txt");
            System.exit(0);
        }
        MakeMatrix mm = new MakeMatrix(args[0]);
        mm.Run();

    }

    private void GetOption(String optionfile) throws IOException {
        BufferedReader option = new BufferedReader(new FileReader(optionfile));
        String line;
        String[] str;
        String ChromosomePrefix = "";
        while ((line = option.readLine()) != null) {
            str = line.split("\\s+");
            switch (str[0]) {
                case "OutPrefix":
                    OutPrefix = str[2];
                    System.out.println("OutPrefix:\t" + OutPrefix);
                    break;
                case "OutPath":
                    OutPath = str[2];
                    System.out.println("OutPath:\t" + OutPath);
                    break;
                case "ChromosomePrefix":
                    ChromosomePrefix = str[2];
                    System.out.println("ChromosomePrefix:\t" + ChromosomePrefix);
                    break;
                case "Chromosome":
                    Chromosome = new String[str.length - 2];
                    System.arraycopy(str, 2, Chromosome, 0, str.length - 2);
                    break;
                case "Thread":
                    Threads = Integer.parseInt(str[2]);
                    System.out.println("Thread:\t" + Threads);
                    break;
                case "Resolution":
                    Resolution = Integer.parseInt(str[2]);
                    System.out.println("Resolution:\t" + Resolution);
                    break;
                case "InterBedpeFile":
                    InterBedpeFile = str[2];
                    System.out.println("InterBedpeFile:\t" + InterBedpeFile);
                    break;
                case "ChrSize":
                    System.out.print("ChrSize:");
                    ChromosomeSize = new int[str.length - 2];
                    for (int i = 2; i < str.length; i++) {
                        ChromosomeSize[i - 2] = Integer.parseInt(str[i]);
                        System.out.print("\t" + ChromosomeSize[i - 2]);
                    }
                    System.out.print("\n");
                    break;
            }
        }
        if (OutPath == null) {
            OutPath = "./";
        }
        if (OutPrefix == null) {
            OutPrefix = "Out";
        }
        if (Chromosome == null) {
            System.out.println("Error ! No Chromosome");
            System.exit(0);
        }
        if (Resolution == 0) {
            System.out.println("Error ! No Resolution");
            System.exit(0);
        }
        if (InterBedpeFile == null) {
            System.out.println("Error ! No InterBedpeFile");
            System.exit(0);
        }
        System.out.print("Chromosome:");
        for (int i = 0; i < Chromosome.length; i++) {
            Chromosome[i] = ChromosomePrefix + Chromosome[i];
            System.out.print("\t" + Chromosome[i]);
        }
        System.out.print("\n");
        Init();
    }

    private void Init() {
        InterMatrixPrefix = OutPath + "/" + OutPrefix + ".inter";
        NormalizeMatrixPrefix = OutPath + "/" + OutPrefix + ".normalize";
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdirs();
        }
    }

}
