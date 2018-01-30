import java.io.*;
import java.util.ArrayList;


public class MakeMatrix {
    private String OutPath;//输出路径
    private String OutPrefix;//输出前缀
    private String InterBedpeFile;
    private String[] Chromosome;
    private int[] ChromosomeSize;
    private int Resolution;
    private String InterMatrixPrefix;
    private String NormalizeMatrixPrefix;
    private String ChrSzieFile;
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
                case "InterBedpeFile":
                    InterBedpeFile = str[2];
                    System.out.println("InterBedpeFile:\t" + InterBedpeFile);
                    break;
                case "OutPath":
                    OutPath = str[2];
                    System.out.println("OutPath:\t" + OutPath);
                    break;
                case "OutPrefix":
                    OutPrefix = str[2];
                    System.out.println("OutPrefix:\t" + OutPrefix);
                    break;
                case "ChrSizeFile":
                    ChrSzieFile = str[2];
                    System.out.println("ChrSzieFile:\t" + ChrSzieFile);
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
        if (ChrSzieFile != null) {
            BufferedReader chrsize = new BufferedReader(new FileReader(ChrSzieFile));
            ArrayList<String[]> list = new ArrayList<>();
            while ((line = chrsize.readLine()) != null) {
                str = line.split("\\s+");
                list.add(str);
            }
            Chromosome = new String[list.size()];
            ChromosomeSize = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Chromosome[i] = list.get(i)[0];
                ChromosomeSize[i] = Integer.parseInt(list.get(i)[1]);
                System.out.println("Chr:\t" + Chromosome[i]);
                System.out.println("Size:\t" + ChromosomeSize[i]);
            }
        } else {
            if (Chromosome == null) {
                System.out.println("Error ! No Chromosome");
                System.exit(0);
            }
            if (ChromosomeSize == null) {
                System.out.println("Error ! No ChromosomeSize");
                System.exit(0);
            }
            System.out.print("Chromosome:");
            for (int i = 0; i < Chromosome.length; i++) {
                Chromosome[i] = ChromosomePrefix + Chromosome[i];
                System.out.print("\t" + Chromosome[i]);
            }
            System.out.print("\n");
        }
        if (Resolution == 0) {
            System.out.println("Error ! No Resolution");
            System.exit(0);
        }
        if (InterBedpeFile == null) {
            System.out.println("Error ! No InterBedpeFile");
            System.exit(0);
        }

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
