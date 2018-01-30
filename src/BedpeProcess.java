import java.io.*;

public class BedpeProcess {
    private String OutPath;
    private String OutPrefix;
    private String LinkerType;
    private String[] Chromosome;
    private String EnzyFilePrefix;
    private String BedpeFile;
    private String[] LigationType;
    //======================================================
    private String FinalBedpeFile;
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
    public int Threads = 1;

    //================================================================
    BedpeProcess(String outpath, String outprefix, String linkertype, String[] chr, String enzyfileprefix, String bedpefile) {
        OutPath = outpath;
        OutPrefix = outprefix;
        LinkerType = linkertype;
        Chromosome = chr;
        EnzyFilePrefix = enzyfileprefix;
        BedpeFile = bedpefile;
        Init();
    }

    public void Run() throws IOException {

        Routine step = new Routine();
        step.Threads = Threads;
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
                        CommonMethod.SortFile(LigationChrFile[finalI][2], new int[]{2, 3, 5, 6}, "n", "", LigationChrFile[finalI][2] + ".sort", Threads);
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
        LigationType = new String[]{"self", "religation", "valid"};
        OutPath = OutPath + "/" + LinkerType;
        FinalBedpeFile = OutPath + "/" + OutPrefix + "." + "final.bedpe";
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
        SameBedpeFile = OutPath + "/" + OutPrefix + "." + LinkerType + ".same.bedpe";
        DiffBedpeFile = SameBedpeFile.replace(".same.bedpe", ".diff.bedpe");
        SelfLigationFile = OutPath + "/" + LigationDir + "/" + OutPrefix + "." + LinkerType + "." + LigationType[0] + ".bedpe";
        ReLigationFile = OutPath + "/" + LigationDir + "/" + OutPrefix + "." + LinkerType + "." + LigationType[1] + ".bedpe";
        EnzyFragment = OutPath + "/" + EnzyDir + "/" + OutPrefix + "." + LinkerType + ".enzy.bedpe";
        LigationChrFile = new String[Chromosome.length][LigationType.length];
        SameBedpeChrFile = new String[Chromosome.length];
        DiffBedpeChrFile = new String[Chromosome.length];
        EnzyChrFragment = new String[Chromosome.length];
        for (int j = 0; j < Chromosome.length; j++) {
            SameBedpeChrFile[j] = SameBedpeFile.replace(".bedpe", "." + Chromosome[j] + ".bedpe");
            DiffBedpeChrFile[j] = DiffBedpeFile.replace(".bedpe", "." + Chromosome[j] + ".bedpe");
            EnzyChrFragment[j] = OutPath + "/" + EnzyDir + "/" + OutPrefix + "." + LinkerType + "." + Chromosome[j] + ".enzy.bedpe";
            for (int k = 0; k < LigationType.length; k++) {
                LigationChrFile[j][k] = OutPath + "/" + LigationDir + "/" + OutPrefix + "." + LinkerType + "." + Chromosome[j] + "." + LigationType[k];
            }
        }
        if (new File(FinalBedpeFile).exists()) {
            new File(FinalBedpeFile).delete();
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
    }
}
