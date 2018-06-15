package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import lib.File.*;
import lib.unit.CustomFile;

public class BedpeProcess {
    private final String OptBedpeFile = "BedpeFile";
    private final String OptChromosome = "Chromosome";
    private final String OptEnzyFilePrefix = "EnzyFilePrefix";
    private final String OptOutPath = "OutPath";
    private final String OptPrefix = "Prefix";
    public final String OptThreads = "Thread";
    //======================================================
    private CustomFile BedpeFile;
    private String EnzyFilePrefix;
    public int Thread;
    private String[] Chromosome;
    private String[] LigationType;
    private CustomFile FinalBedpeFile;
    private CustomFile ValidBedpeFile;
    private CustomFile SelfLigationFile;
    private CustomFile ReLigationFile;
    private CustomFile SameBedpeFile;
    private CustomFile DiffBedpeFile;
    private CustomFile[] SameBedpeChrFile;
    private CustomFile[] DiffBedpeChrFile;
    private CustomFile[] ChrFragLocation;
    private CustomFile EnzyFragment;
    private CustomFile[][] LigationChrFile;
    private String EnzyDir;
    private String LigationDir;
    //============================================================
    private Hashtable<String, String> ParameterList = new Hashtable<>();
    private String[] RequiredParameter = new String[]{OptBedpeFile, OptChromosome, OptEnzyFilePrefix};
    private String[] OptionalParameter = new String[]{OptOutPath, OptPrefix, OptThreads};

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("Usage:  java -cp DLO-HIC-AnalysisTools.jar bin.BedpeProcess <ConfigFile>");
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

    public BedpeProcess(File outpath, String outprefix, String[] chr, String enzyfileprefix, File bedpefile) {
        ParameterInit();
        ParameterList.put(OptOutPath, outpath.getPath());
        ParameterList.put(OptPrefix, outprefix);
        Chromosome = chr;
        ParameterList.put(OptEnzyFilePrefix, enzyfileprefix);
        ParameterList.put(OptBedpeFile, bedpefile.getPath());
        Init();
    }

    public void Run() throws IOException, InterruptedException {

//        Routine step = new Routine();
//        step.Threads = Thread;
        BedpeToSameAndDiff(BedpeFile, SameBedpeFile, DiffBedpeFile);
        Thread[] Process = new Thread[Chromosome.length];
        for (int i = 0; i < Chromosome.length; i++) {
            int finalI = i;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SeparateChromosome(SameBedpeFile, 1, Chromosome[finalI], SameBedpeChrFile[finalI]);
                        SeparateChromosome(DiffBedpeFile, 1, Chromosome[finalI], DiffBedpeChrFile[finalI]);
                        FragmentLocation(SameBedpeChrFile[finalI], new File(EnzyFilePrefix + "." + Chromosome[finalI] + ".txt"), ChrFragLocation[finalI]);
                        SeparateLigationType(ChrFragLocation[finalI], LigationChrFile[finalI][0], LigationChrFile[finalI][1], LigationChrFile[finalI][2]);
//                        FileTool.Append(DiffBedpeChrFile[finalI], LigationChrFile[finalI][2]);
                        LigationChrFile[finalI][2].Append(DiffBedpeChrFile[finalI]);
                        LigationChrFile[finalI][2].SortFile(new int[]{2, 3, 5, 6}, "n", "", new File(LigationChrFile[finalI][2] + ".sort"));
                        RemoveRepeat(new File(LigationChrFile[finalI][2] + ".sort"), new int[]{2, 3, 5, 6, 10, 11}, new File(LigationChrFile[finalI][2] + ".clean.sort"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        for (int j = 0; j < Chromosome.length; j++) {
            Process[j].join();
        }
        if (ValidBedpeFile.exists()) {
            ValidBedpeFile.delete();
        }
        if (SelfLigationFile.exists()) {
            SelfLigationFile.delete();
        }
        if (ReLigationFile.exists()) {
            ReLigationFile.delete();
        }
        if (EnzyFragment.exists()) {
            EnzyFragment.delete();
        }
        if (FinalBedpeFile.exists()) {
            FinalBedpeFile.delete();
        }
        for (int j = 0; j < Chromosome.length; j++) {
            FinalBedpeFile.Append(new File(LigationChrFile[j][2] + ".clean.sort"));
            SelfLigationFile.Append(LigationChrFile[j][0]);
            ReLigationFile.Append(LigationChrFile[j][1]);
            ValidBedpeFile.Append(LigationChrFile[j][2]);
            EnzyFragment.Append(ChrFragLocation[j]);
            //删除中间文件
            for (int i = 0; i < LigationType.length; i++) {
                LigationChrFile[j][i].delete();
                new File(LigationChrFile[j][i] + ".sort").delete();
                new File(LigationChrFile[j][i] + ".clean.sort").delete();
            }
            ChrFragLocation[j].delete();
            SameBedpeChrFile[j].delete();
            DiffBedpeChrFile[j].delete();
        }
    }

    public CustomFile getFinalBedpeFile() {
        return FinalBedpeFile;
    }

    public CustomFile getSelfLigationFile() {
        return SelfLigationFile;
    }

    public CustomFile getReLigationFile() {
        return ReLigationFile;
    }

    public CustomFile getValidBedpeFile() {
        return ValidBedpeFile;
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
        BedpeFile = new CustomFile(ParameterList.get(OptBedpeFile));
        EnzyFilePrefix = ParameterList.get(OptEnzyFilePrefix);
        Thread = Integer.parseInt(ParameterList.get(OptThreads));
        //===========================================================
        LigationType = new String[]{"self", "religation", "valid"};
        if (EnzyDir == null) {
            EnzyDir = "FragmentLocation";
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
        SameBedpeFile = new CustomFile(OutPath + "/" + Prefix + ".same.bedpe");
        DiffBedpeFile = new CustomFile(SameBedpeFile.getPath().replace(".same.bedpe", ".diff.bedpe"));
        SelfLigationFile = new CustomFile(OutPath + "/" + LigationDir + "/" + Prefix + "." + LigationType[0] + ".bedpe");
        ReLigationFile = new CustomFile(OutPath + "/" + LigationDir + "/" + Prefix + "." + LigationType[1] + ".bedpe");
        ValidBedpeFile = new CustomFile(OutPath + "/" + LigationDir + "/" + Prefix + "." + "valid.bedpe");
        FinalBedpeFile = new CustomFile(OutPath + "/" + Prefix + "." + "final.bedpe");
        EnzyFragment = new CustomFile(OutPath + "/" + EnzyDir + "/" + Prefix + ".enzy.bedpe");
        LigationChrFile = new CustomFile[Chromosome.length][LigationType.length];
        SameBedpeChrFile = new CustomFile[Chromosome.length];
        DiffBedpeChrFile = new CustomFile[Chromosome.length];
        ChrFragLocation = new CustomFile[Chromosome.length];
        for (int j = 0; j < Chromosome.length; j++) {
            SameBedpeChrFile[j] = new CustomFile(SameBedpeFile.getPath().replace(".bedpe", "." + Chromosome[j] + ".bedpe"));
            DiffBedpeChrFile[j] = new CustomFile(DiffBedpeFile.getPath().replace(".bedpe", "." + Chromosome[j] + ".bedpe"));
            ChrFragLocation[j] = new CustomFile(OutPath + "/" + EnzyDir + "/" + Prefix + "." + Chromosome[j] + ".enzy.bedpe");
            for (int k = 0; k < LigationType.length; k++) {
                LigationChrFile[j][k] = new CustomFile(OutPath + "/" + LigationDir + "/" + Prefix + "." + Chromosome[j] + "." + LigationType[k]);
            }
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

    public void SeparateChromosome(File InFile, int Row, String Chromosome, File OutFile) throws IOException {
        System.out.println(new Date() + "\tStart to SeparateChromosome\t" + Chromosome + "\t" + InFile.getName());
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        Thread[] Process = new Thread[Thread];
        for (int i = 0; i < Thread; i++) {
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                    try {
                        while ((line = infile.readLine()) != null) {
                            if (line.split("\\s+")[Row - 1].equals(Chromosome)) {
                                synchronized (Process) {
                                    outfile.write(line + "\n");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                }
            });
            Process[i].start();
        }
        for (int i = 0; i < Thread; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        outfile.close();
        System.out.println(new Date() + "\tEnd to SeparateChromosome\t" + Chromosome + "\t" + InFile.getName());
    }//OK

    private void FragmentLocation(File BedpeFile, File EnySiteFile, File OutFile) throws IOException {
        ArrayList<Integer> EnySiteList = new ArrayList<>();
        BufferedReader EnySiteRead = new BufferedReader(new FileReader(EnySiteFile));
        BufferedReader SeqRead = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter OutWrite = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        System.out.println(new Date() + "\tBegin to find eny site\t" + BedpeFile.getName());
        //---------------------------------------------------
        while ((line = EnySiteRead.readLine()) != null) {
            str = line.split("\\s+");
            EnySiteList.add(Integer.parseInt(str[str.length - 1]));
        }
        EnySiteRead.close();
        //---------------------多线程-------------------------
        Thread[] Process = new Thread[Thread];
        for (int i = 0; i < Thread; i++) {
            Process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + BedpeFile + "\t" + Thread.currentThread().getName() + " start");
                        while ((line = SeqRead.readLine()) != null) {
                            str = line.split("\\s+");
                            int[] position = {(Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2, (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2};
                            //PositionList.add(position);
                            int[] index = new int[position.length];
                            //-----------------------二分法查找-----------------
                            for (int j = 0; j < position.length; j++) {
                                int start = 0;
                                int end = EnySiteList.size();
                                int middle = (start + end) / 2;
                                while (start < end) {
                                    middle = (start + end) / 2;
                                    if (position[j] < EnySiteList.get(middle)) {
                                        end = middle;
                                    } else if (position[j] >= EnySiteList.get(middle)) {
                                        start = middle + 1;
                                    }
                                }
                                if (position[j] < EnySiteList.get(middle)) {
                                    index[j] = middle;
                                } else {
                                    index[j] = middle + 1;
                                }
                            }
                            //-----------------------------------------------------------------
                            synchronized (Process) {
                                OutWrite.write(line + "\t" + String.valueOf(index[0]) + "\t" + String.valueOf(index[1]) + "\n");
                            }
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (
                            IOException e)

                    {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //----------------------------------------------------
        SeqRead.close();
        OutWrite.close();
        System.out.println(new

                Date() + "\tEnd to find eny site\t" + BedpeFile.getName());
    }//OK

    public void SeparateLigationType(File InFile, File SelfFile, File ReligFile, File ValidFile) throws IOException {
        Thread[] process = new Thread[Thread];
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter selffile = new BufferedWriter(new FileWriter(SelfFile));
        BufferedWriter religfile = new BufferedWriter(new FileWriter(ReligFile));
        BufferedWriter valifile = new BufferedWriter(new FileWriter(ValidFile));
        String[] OutLock = new String[]{"sel", "rel", "val"};
        System.out.println(new Date() + "\tBegin to seperate ligation\t" + InFile.getName());
        for (int i = 0; i < Thread; i++) {
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " begin");
                        while ((line = infile.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[str.length - 2].equals(str[str.length - 1])) {
                                synchronized (OutLock[0]) {
                                    selffile.write(line + "\n");
                                }
                            } else if ((Integer.parseInt(str[str.length - 1]) - Integer.parseInt(str[str.length - 2]) == 1) && (Integer.parseInt(str[4]) < Integer.parseInt(str[2]))) {
                                synchronized (OutLock[1]) {
                                    religfile.write(line + "\n");
                                }
                            } else {
                                synchronized (OutLock[2]) {
                                    valifile.write(line + "\n");
                                }
                            }
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
        }
        for (int i = 0; i < Thread; i++) {
            try {
                process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        selffile.close();
        religfile.close();
        valifile.close();
        System.out.println(new Date() + "\tEnd seperate ligation\t" + InFile.getName());
    }//OK

    private void RemoveRepeat(File InFile, int[] Row, File OutFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        String temp1 = "";
        StringBuilder temp2 = new StringBuilder();
        System.out.println(new Date() + "\tStart to remove repeat\t" + InFile.getName());
        while ((line = infile.readLine()) != null) {
            str = line.split("\\s+");
            for (int i = 0; i < Row.length; i++) {
                temp2.append(str[Row[i] - 1]);
            }
            if (!temp1.equals(temp2.toString())) {
                outfile.write(line + "\n");
                temp1 = temp2.toString();
            }
            temp2.setLength(0);
        }
        infile.close();
        outfile.close();
        System.out.println(new Date() + "\tEnd to remove repeat\t" + InFile.getName());
    }//OK

    private void BedpeToSameAndDiff(File BedpeFile, File SameBedpeFile, File DiffBedpeFile) throws IOException, InterruptedException {
        BufferedReader BedpeRead = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter SameBedpeWrite = new BufferedWriter(new FileWriter(SameBedpeFile));
        BufferedWriter DiffBedpeWrite = new BufferedWriter(new FileWriter(DiffBedpeFile));
        Thread[] process = new Thread[Thread];
        System.out.println(new Date() + "\tBegin to Seperate bedpe file\t" + BedpeFile.getName());
        for (int i = 0; i < Thread; i++) {
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " strat");
                        while ((line = BedpeRead.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[0].equals(str[3])) {
                                //---------------------------取相同染色体上的交互-----------------------
                                if (Integer.parseInt(str[1]) < Integer.parseInt(str[4])) {
                                    synchronized (SameBedpeWrite) {
                                        SameBedpeWrite.write(line + "\n");
                                    }
                                } else {
                                    synchronized (SameBedpeWrite) {
                                        SameBedpeWrite.write(str[3] + "\t" + str[4] + "\t" + str[5] + "\t" + str[0] + "\t" + str[1] + "\t" + str[2] + "\t" + str[6] + "\t" + str[8] + "\t" + str[7] + "\t" + str[10] + "\t" + str[9] + "\n");
                                    }
                                }
                            } else {
                                //--------------------------取不同染色体上的交互----------------------
                                if (str[0].length() < str[3].length() || str[0].compareTo(str[3]) < 0) {
                                    synchronized (DiffBedpeWrite) {
                                        DiffBedpeWrite.write(line + "\n");
                                    }
                                } else {
                                    synchronized (DiffBedpeWrite) {
                                        DiffBedpeWrite.write(str[3] + "\t" + str[4] + "\t" + str[5] + "\t" + str[0] + "\t" + str[1] + "\t" + str[2] + "\t" + str[6] + "\t" + str[8] + "\t" + str[7] + "\t" + str[10] + "\t" + str[9] + "\n");
                                    }
                                }
                            }
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
        }
        for (int i = 0; i < Thread; i++) {
            process[i].join();
        }
        BedpeRead.close();
        SameBedpeWrite.close();
        DiffBedpeWrite.close();
    }//OK
}
