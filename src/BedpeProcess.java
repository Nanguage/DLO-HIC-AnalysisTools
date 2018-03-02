
import java.io.*;
import java.util.Hashtable;

public class BedpeProcess {
    private final String OptBedpeFile = "BedpeFile";
    private final String OptChromosome = "Chromosome";
    private final String OptEnzyFilePrefix = "EnzyFilePrefix";
    private final String OptOutPath = "OutPath";
    private final String OptPrefix = "Prefix";
    public final String OptThreads = "Thread";
    //======================================================
    private String BedpeFile;
    private String EnzyFilePrefix;
    public int Thread;
    private String[] Chromosome;
    private String[] LigationType;
    private String FinalBedpeFile;
    private String ValidBedpeFile;
    private String SelfLigationFile;
    private String ReLigationFile;
    private String SameBedpeFile;
    private String DiffBedpeFile;
    private String[] SameBedpeChrFile;
    private String[] DiffBedpeChrFile;
    private String[] EnzyChrFragment;
    private String EnzyFragment;
    private String[][] LigationChrFile;
    private String EnzyDir;
    private String LigationDir;
    //============================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptBedpeFile, OptChromosome, OptEnzyFilePrefix};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptThreads};

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage:  java -cp DLO-HIC-AnalysisTools.jar BedpeProcess <ConfigFile>");
            System.exit(0);
        }
        BedpeProcess bedpe = new BedpeProcess(args[0]);
        bedpe.ShowParameter();
        bedpe.Run();
    }

    //================================================================
    BedpeProcess(String OptFile) throws IOException {
        ParameterInit();
        GetOption(OptFile);
        Init();
        ShowParameter();
    }

    BedpeProcess(String outpath, String outprefix, String[] chr, String enzyfileprefix, String bedpefile) {
        ParameterInit();
        ParameterList.put(OptOutPath, outpath);
        ParameterList.put(OptPrefix, outprefix);
        Chromosome = chr;
        ParameterList.put(OptEnzyFilePrefix, enzyfileprefix);
        ParameterList.put(OptBedpeFile, bedpefile);
        Init();
    }

    public void Run() throws IOException {

        Routine step = new Routine();
        step.Threads = Thread;
        step.BedpeToSameAndDiff(BedpeFile, SameBedpeFile, DiffBedpeFile);
        Thread[] Process = new Thread[Chromosome.length];
        for (int i = 0; i < Chromosome.length; i++) {
            int finalI = i;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        step.SeparateChromosome(SameBedpeFile, 1, Chromosome[finalI], SameBedpeChrFile[finalI]);
                        step.SeparateChromosome(DiffBedpeFile, 1, Chromosome[finalI], DiffBedpeChrFile[finalI]);
                        step.WhichEnzymeFragment(SameBedpeChrFile[finalI], EnzyFilePrefix + "." + Chromosome[finalI] + ".txt", EnzyChrFragment[finalI]);
                        step.SeparateLigationType(EnzyChrFragment[finalI], LigationChrFile[finalI][0], LigationChrFile[finalI][1], LigationChrFile[finalI][2]);
                        CommonMethod.Append(DiffBedpeChrFile[finalI], LigationChrFile[finalI][2]);
                        CommonMethod.SortFile(LigationChrFile[finalI][2], new int[]{2, 3, 5, 6}, "n", "", LigationChrFile[finalI][2] + ".sort", Thread);
                        step.RemoveRepeat(LigationChrFile[finalI][2] + ".sort", new int[]{2, 3, 5, 6}, LigationChrFile[finalI][2] + ".clean.sort");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        for (int j = 0; j < Chromosome.length; j++) {
            try {
                Process[j].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int j = 0; j < Chromosome.length; j++) {
            CommonMethod.Append(LigationChrFile[j][2] + ".clean.sort", FinalBedpeFile);
            CommonMethod.Append(LigationChrFile[j][0], SelfLigationFile);
            CommonMethod.Append(LigationChrFile[j][1], ReLigationFile);
            CommonMethod.Append(LigationChrFile[j][2], ValidBedpeFile);
            CommonMethod.Append(EnzyChrFragment[j], EnzyFragment);
            //删除中间文件
            for (int i = 0; i < LigationType.length; i++) {
                new File(LigationChrFile[j][i]).delete();
                new File(LigationChrFile[j][i] + ".sort").delete();
                new File(LigationChrFile[j][i] + ".clean.sort").delete();
            }
            new File(EnzyChrFragment[j]).delete();
            new File(SameBedpeChrFile[j]).delete();
            new File(DiffBedpeChrFile[j]).delete();
        }
    }

    public String getFinalBedpeFile() {
        return FinalBedpeFile;
    }

    private void Init() {
        try {
            if (Chromosome.length > 0) {
                String ValChr = Chromosome[0];
                for (int i = 1; i < Chromosome.length; i++) {
                    ValChr = ValChr + " " + Chromosome[i];
                }
                ParameterList.put(OptChromosome, ValChr);
            }
        } catch (NullPointerException ignored) {

        }
        for (String opt : RequiredParameter) {
            if (ParameterList.get(opt).equals("")) {
                System.err.println("Error ! No " + opt);
                System.exit(0);
            }
        }
        //=============================================================
        String OutPath = ParameterList.get(OptOutPath);
        String Prefix = ParameterList.get(OptPrefix);
        BedpeFile = ParameterList.get(OptBedpeFile);
        EnzyFilePrefix = ParameterList.get(OptEnzyFilePrefix);
        Thread = Integer.parseInt(ParameterList.get(OptThreads));
        //===========================================================
        LigationType = new String[]{"self", "religation", "valid"};
        if (EnzyDir == null) {
            EnzyDir = "WhichEnzymeFragment";
        }
        if (LigationDir == null) {
            LigationDir = "Ligation";
        }
        if (!new File(OutPath).isDirectory()) {
            new File(OutPath).mkdirs();
        }
        if (!new File(OutPath + "/" + LigationDir).isDirectory()) {
            new File(OutPath + "/" + LigationDir).mkdir();
        }
        if (!new File(OutPath + "/" + EnzyDir).isDirectory()) {
            new File(OutPath + "/" + EnzyDir).mkdir();
        }
        //===========================================================================
        SameBedpeFile = OutPath + "/" + Prefix + ".same.bedpe";
        DiffBedpeFile = SameBedpeFile.replace(".same.bedpe", ".diff.bedpe");
        SelfLigationFile = OutPath + "/" + LigationDir + "/" + Prefix + "." + LigationType[0] + ".bedpe";
        ReLigationFile = OutPath + "/" + LigationDir + "/" + Prefix + "." + LigationType[1] + ".bedpe";
        ValidBedpeFile = OutPath + "/" + LigationDir + "/" + Prefix + "." + "valid.bedpe";
        FinalBedpeFile = OutPath + "/" + Prefix + "." + "final.bedpe";
        EnzyFragment = OutPath + "/" + EnzyDir + "/" + Prefix + ".enzy.bedpe";
        LigationChrFile = new String[Chromosome.length][LigationType.length];
        SameBedpeChrFile = new String[Chromosome.length];
        DiffBedpeChrFile = new String[Chromosome.length];
        EnzyChrFragment = new String[Chromosome.length];
        for (int j = 0; j < Chromosome.length; j++) {
            SameBedpeChrFile[j] = SameBedpeFile.replace(".bedpe", "." + Chromosome[j] + ".bedpe");
            DiffBedpeChrFile[j] = DiffBedpeFile.replace(".bedpe", "." + Chromosome[j] + ".bedpe");
            EnzyChrFragment[j] = OutPath + "/" + EnzyDir + "/" + Prefix + "." + Chromosome[j] + ".enzy.bedpe";
            for (int k = 0; k < LigationType.length; k++) {
                LigationChrFile[j][k] = OutPath + "/" + LigationDir + "/" + Prefix + "." + Chromosome[j] + "." + LigationType[k];
            }
        }
        if (new File(ValidBedpeFile).exists()) {
            new File(ValidBedpeFile).delete();
        }
        if (new File(SelfLigationFile).exists()) {
            new File(SelfLigationFile).delete();
        }
        if (new File(ReLigationFile).exists()) {
            new File(ReLigationFile).delete();
        }
        if (new File(EnzyFragment).exists()) {
            new File(EnzyFragment).delete();
        }
        if (new File(FinalBedpeFile).exists()) {
            new File(FinalBedpeFile).delete();
        }
    }

    private void GetOption(String ConfigFile) throws IOException {
        BufferedReader file = new BufferedReader(new FileReader(ConfigFile));
        String line;
        String[] str;
        while ((line = file.readLine()) != null) {
            line = line.trim();
            if (line.equals("")) {
                continue;
            }
            str = line.split("\\s*=\\s*");
            if (ParameterList.containsKey(str[0]) && str.length >= 2) {
                ParameterList.put(str[0], str[1]);
            }
        }
        file.close();
    }

    public void ParameterInit() {
        for (String opt : RequiredParameter) {
            ParameterList.put(opt, "");
        }
        for (String opt : OptionalParameter) {
            ParameterList.put(opt, "");
        }
        ParameterList.put(OptOutPath, "./");
        ParameterList.put(OptPrefix, "out");
        ParameterList.put(OptThreads, "1");
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
}
