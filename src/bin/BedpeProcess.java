package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import lib.unit.Chromosome;
import lib.unit.CustomFile;
import lib.unit.Opts;

public class BedpeProcess {
    private final String OptBedpeFile = "BedpeFile";
    private final String OptChromosome = "Chromosomes";
    private final String OptEnzyFilePrefix = "EnzyFilePrefix";
    private final String OptOutPath = "OutPath";
    private final String OptPrefix = "Prefix";
    public final String OptThreads = "Threads";
    //======================================================
    private File OutPath;
    private File FinalDir;
    private File MiddleDir;
    private File LigationDir;
    private String Prefix;
    private CustomFile BedpeFile;
    private File[] EnzyFile;
    public int Threads;
    private Chromosome[] Chromosomes;
    //    private String[] LigationType;
    private CustomFile SameFile;//染色体内的交互文件
    private CustomFile DiffFile;//染色体间的交互文件
    private CustomFile FragmentLocationFile;
    private CustomFile[] ChrFragLocationFile;//每条染色体的交互定位
    /**
     * 每一行表示一条染色体，每一列表示一种连接类型例如：
     * Sel     Rel     Valid
     * Chr1  -       -       -
     * Chr2  -       -       -
     * Chr3  -       -       -
     * Chr4  -       -       -
     */
    private CustomFile[][] ChrLigationFile;
    private CustomFile SelfLigationFile;//总的自连接文件=SUM(ChrLigationFile[:][0])
    private CustomFile ReLigationFile;//总的再连接文件=SUM(ChrLigationFile[:][1])
    private CustomFile ValidFile;//总的有效数据文件=SUM(ChrLigationFile[:][2])
    private CustomFile[] ChrSameFile;//每条染色体内的交互
    private CustomFile[] ChrSameNoDumpFile;//每条染色体内的交互（去duplication）=ChrLigationFile[:][2]去duplication
    //    private CustomFile[] ChrDiffFile;
    private CustomFile SameNoDumpFile;//最终的染色体内的交互文件=SUM(ChrSameNoDumpFile[:])
    private CustomFile DiffNoDumpFile;//最终的染色体间的交互文件=DiffFile去duplication
    //    private File EnzyDir;
    private CustomFile FinalFile;//最终文件=SameNoDumpFile+DiffNoDumpFile
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

    public BedpeProcess(File OutPath, String Prefix, Chromosome[] Chrs, File[] EnzyFile, CustomFile BedpeFile) {
        this.OutPath = OutPath;
        this.Prefix = Prefix;
        this.Chromosomes = Chrs;
        this.BedpeFile = BedpeFile;
        this.EnzyFile = EnzyFile;
//        ParameterInit();
//        ParameterList.put(OptOutPath, OutPath.getPath());
//        ParameterList.put(OptPrefix, outprefix);
//        Chromosomes = Chrs;
//        ParameterList.put(OptEnzyFilePrefix, enzyfileprefix);
//        ParameterList.put(OptBedpeFile, bedpefile.getPath());
        Init();
    }

    public void Run() throws IOException, InterruptedException {
        //将bedpe分成染色体内的交互和染色体间的交互
        BedpeToSameAndDiff(BedpeFile, SameFile, DiffFile);
        ChrSameFile = SameFile.SeparateBedpe(Chromosomes, OutPath + "/" + Prefix, Threads);
        //=====================================染色体内的交互处理=========================================
        Thread[] Process = new Thread[Chromosomes.length];
        for (int i = 0; i < Chromosomes.length; i++) {
            int finalI = i;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //定位交互发生在哪个酶切片段
                        FragmentLocation(ChrSameFile[finalI], EnzyFile[finalI], ChrFragLocationFile[finalI]);
                        //区分不同的连接类型（自连接，再连接，有效数据）
                        SeparateLigationType(ChrFragLocationFile[finalI], ChrLigationFile[finalI][0], ChrLigationFile[finalI][1], ChrLigationFile[finalI][2]);
//                        FileTool.Append(ChrDiffFile[finalI], ChrLigationFile[finalI][2]);
//                        ChrLigationFile[finalI][2].Append(ChrDiffFile[finalI]);
                        File SortChrLigationFile = new File(ChrLigationFile[finalI][2] + ".sort");
                        //按交互位置排序
                        ChrLigationFile[finalI][2].SortFile(new int[]{2, 3, 5, 6}, "n", "", SortChrLigationFile);
                        //去除duplication
                        RemoveRepeat(SortChrLigationFile, new int[]{2, 3, 4, 5, 6, 10, 11}, ChrSameNoDumpFile[finalI]);
                        if (Opts.DeBugLevel < 1) {
                            SortChrLigationFile.delete();
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        for (int j = 0; j < Chromosomes.length; j++) {
            Process[j].join();
        }
        //==========================================染色体间的交互处理==================================================
        File SortDiffFile = new File(DiffFile + ".sort");
        DiffFile.SortFile(new int[]{2, 3, 5, 6}, "n", "", SortDiffFile);//排序
        RemoveRepeat(SortDiffFile, new int[]{2, 3, 4, 5, 6, 10, 11}, DiffNoDumpFile);//去duplication
        //================================================================
        File[] NeedRemove = new File[]{SameNoDumpFile, SelfLigationFile, ReLigationFile, ValidFile, FragmentLocationFile};
        for (File f : NeedRemove) {
            if (f.exists() && !f.delete()) {
                System.err.println(new Date() + "\tWarning! Can't delete " + f.getName());
            }
        }
        for (int j = 0; j < Chromosomes.length; j++) {
            SameNoDumpFile.Append(ChrSameNoDumpFile[j]);//合并染色体内的交互（去除duplication）
            SelfLigationFile.Append(ChrLigationFile[j][0]);//合并自连接
            ReLigationFile.Append(ChrLigationFile[j][1]);//合并再连接
            ValidFile.Append(ChrLigationFile[j][2]);//合并有效数据（未去duplication）
            FragmentLocationFile.Append(ChrFragLocationFile[j]);//合并定位的交互片段
            //删除中间文件
            if (Opts.DeBugLevel < 1) {
                for (int i = 0; i < 3; i++) {
                    ChrLigationFile[j][i].delete();//删除（自连接，再连接，有效数据）
                }
//                ChrSameFile[j].delete();
                ChrFragLocationFile[j].delete();//删除每条染色体的交互片段定位的文件（只保留包含全部染色体的一个文件）
//                ChrSameNoDumpFile[j].delete();
            }

        }
        FinalFile.Merge(new File[]{SameNoDumpFile, DiffNoDumpFile});
    }

    public CustomFile getFinalFile() {
        return FinalFile;
    }


    private void Init() {
//        try {
//            if (Chromosomes.length > 0) {
//                String ValChr = Chromosomes[0];
//                for (int i = 1; i < Chromosomes.length; i++) {
//                    ValChr = ValChr + " " + Chromosomes[i];
//                }
//                ParameterList.put(OptChromosome, ValChr);
//            }
//        } catch (NullPointerException ignored) {
//
//        }
//        for (String opt : RequiredParameter) {
//            if (ParameterList.get(opt).equals("")) {
//                System.err.println("Error ! No " + opt);
//                System.exit(0);
//            }
//        }
        //=============================================================
//        String OutPath = ParameterList.get(OptOutPath);
//        String Prefix = ParameterList.get(OptPrefix);
//        BedpeFile = new CustomFile(ParameterList.get(OptBedpeFile));
//        EnzyFilePrefix = ParameterList.get(OptEnzyFilePrefix);
//        Threads = Integer.parseInt(ParameterList.get(OptThreads));
        //===========================================================
//        if (EnzyDir == null) {
//            EnzyDir = "FragmentLocation";
//        }
//        if (LigationDir == null) {
//            LigationDir = "Ligation";
//        }
        File LigationDir = new File(OutPath + "/Ligation");
        File MiddleDir = new File(OutPath + "/Temp");//存放中间文件的目录
        File FinalDir = new File(OutPath + "/Clean");//存放最终结果的目录
        File[] CheckDir = new File[]{OutPath, LigationDir, MiddleDir, FinalDir};
        for (File f : CheckDir) {
            synchronized (BedpeProcess.class) {
                if (!f.isDirectory() && !f.mkdir()) {
                    System.err.println(new Date() + "\tERROR! Can't Create " + f);
                    System.exit(1);
                }
            }
        }
        //===========================================================================
        SameFile = new CustomFile(OutPath + "/" + Prefix + ".same.bedpe");
        DiffFile = new CustomFile(OutPath + "/" + Prefix + ".diff.bedpe");
        SelfLigationFile = new CustomFile(LigationDir + "/" + Prefix + ".self.bedpe");
        ReLigationFile = new CustomFile(LigationDir + "/" + Prefix + ".rel.bedpe");
        ValidFile = new CustomFile(LigationDir + "/" + Prefix + ".valid.bedpe");
        FragmentLocationFile = new CustomFile(LigationDir + "/" + Prefix + ".enzy.bedpe");
        ChrSameFile = new CustomFile[Chromosomes.length];
        ChrFragLocationFile = new CustomFile[Chromosomes.length];
        ChrLigationFile = new CustomFile[Chromosomes.length][3];
        ChrSameNoDumpFile = new CustomFile[Chromosomes.length];
//        ChrDiffFile = new CustomFile[Chromosomes.length];
        for (int j = 0; j < Chromosomes.length; j++) {
//            ChrSameFile[j] = new CustomFile(SameFile.getPath().replace(".bedpe", "." + Chromosomes[j] + ".bedpe"));
//            ChrDiffFile[j] = new CustomFile(DiffFile.getPath().replace(".bedpe", "." + Chromosomes[j] + ".bedpe"));
            ChrFragLocationFile[j] = new CustomFile(MiddleDir + "/" + Prefix + "." + Chromosomes[j].Name + ".enzy.bedpe");
            ChrLigationFile[j][0] = new CustomFile(MiddleDir + "/" + Prefix + "." + Chromosomes[j].Name + ".self.bedpe");
            ChrLigationFile[j][1] = new CustomFile(MiddleDir + "/" + Prefix + "." + Chromosomes[j].Name + ".rel.bedpe");
            ChrLigationFile[j][2] = new CustomFile(MiddleDir + "/" + Prefix + "." + Chromosomes[j].Name + ".valid.bedpe");
            ChrSameNoDumpFile[j] = new CustomFile(FinalDir + "/" + Prefix + "." + Chromosomes[j].Name + ".same.clean.bedpe");
        }
        SameNoDumpFile = new CustomFile(FinalDir + "/" + Prefix + ".same.clean.bedpe");
        DiffNoDumpFile = new CustomFile(FinalDir + "/" + Prefix + ".diff.clean.bedpe");
        FinalFile = new CustomFile(FinalDir + "/" + Prefix + "." + "final.bedpe");
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

    private void FragmentLocation(File BedpeFile, File EnySiteFile, File OutFile) throws IOException, InterruptedException {
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
//        Threads[] Process = new Threads[Threads];
//        for (int i = 0; i < Process.length; i++) {
//            Process[i] = new Threads(new Runnable() {
//
//                @Override
//                public void run() {
//                    try {
//                        String line;
//                        String[] str;
        while ((line = SeqRead.readLine()) != null) {
            str = line.split("\\s+");
            int[] position = {(Integer.parseInt(str[1]) + Integer.parseInt(str[2])) / 2, (Integer.parseInt(str[4]) + Integer.parseInt(str[5])) / 2};
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
//                            synchronized (Process) {
            OutWrite.write(line + "\t" + String.valueOf(index[0]) + "\t" + String.valueOf(index[1]) + "\n");
//                            }
        }
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            Process[i].start();
//            Process[i].join();
//        }
        //----------------------------------------------------
        SeqRead.close();
        OutWrite.close();
        System.out.println(new Date() + "\tEnd to find eny site\t" + BedpeFile.getName());
    }//OK

    private void SeparateLigationType(File InFile, File SelfFile, File ReligFile, File ValidFile) throws IOException {
//        Threads[] process = new Threads[Threads];
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter selffile = new BufferedWriter(new FileWriter(SelfFile));
        BufferedWriter religfile = new BufferedWriter(new FileWriter(ReligFile));
        BufferedWriter valifile = new BufferedWriter(new FileWriter(ValidFile));
//        String[] OutLock = new String[]{"sel", "rel", "val"};
        System.out.println(new Date() + "\tBegin to seperate ligation\t" + InFile.getName());
//        for (int i = 0; i < Threads; i++) {
//            process[i] = new Threads(new Runnable() {
//                @Override
//                public void run() {
        String line;
        String[] str;
//                    try {
        while ((line = infile.readLine()) != null) {
            str = line.split("\\s+");
            if (str[str.length - 2].equals(str[str.length - 1])) {
//                                synchronized (selffile) {
                selffile.write(line + "\n");
//                                }
            } else if ((Integer.parseInt(str[str.length - 1]) - Integer.parseInt(str[str.length - 2]) == 1) && (Integer.parseInt(str[4]) < Integer.parseInt(str[2]))) {
//                                synchronized (religfile) {
                religfile.write(line + "\n");
//                                }
            } else {
//                                synchronized (valifile) {
                valifile.write(line + "\n");
//                                }
            }
        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            process[i].start();
//        }
//        for (int i = 0; i < Threads; i++) {
//            try {
//                process[i].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
        infile.close();
        selffile.close();
        religfile.close();
        valifile.close();
        System.out.println(new Date() + "\tEnd separate ligation\t" + InFile.getName());
    }//OK

    private void RemoveRepeat(File InFile, int[] Row, File OutFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] firststr;
        String[] secondstr;
        try {
            firststr = infile.readLine().split("\\s+");
        } catch (NullPointerException e) {
            infile.close();
            outfile.close();
            return;
        }
        System.out.println(new Date() + "\tStart to remove repeat\t" + InFile.getName());
        while ((line = infile.readLine()) != null) {
            boolean flag = true;
            secondstr = line.split("\\s+");
            for (int i = 0; i < Row.length; i++) {
                if (!firststr[Row[i] - 1].equals(secondstr[Row[i] - 1])) {
                    flag = false;
                    break;
                }
            }
            if (!flag) {
                outfile.write(line + "\n");
            }
            firststr = secondstr;
        }
        infile.close();
        outfile.close();
        System.out.println(new Date() + "\tEnd to remove repeat\t" + InFile.getName());
    }//OK

    private void BedpeToSameAndDiff(File BedpeFile, File SameBedpeFile, File DiffBedpeFile) throws IOException, InterruptedException {
        BufferedReader BedpeRead = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter SameBedpeWrite = new BufferedWriter(new FileWriter(SameBedpeFile));
        BufferedWriter DiffBedpeWrite = new BufferedWriter(new FileWriter(DiffBedpeFile));
        Thread[] process = new Thread[Threads * 3];
        System.out.println(new Date() + "\tBegin to Seperate bedpe file\t" + BedpeFile.getName());
        for (int i = 0; i < Threads; i++) {
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
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
                                if (str[0].compareTo(str[3]) < 0) {
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
//                        System.out.println(new Date() + "\t" + Threads.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
        }
        for (int i = 0; i < Threads; i++) {
            process[i].join();
        }
        BedpeRead.close();
        SameBedpeWrite.close();
        DiffBedpeWrite.close();
    }//OK

    public CustomFile[] getChrSameNoDumpFile() {
        return ChrSameNoDumpFile;
    }

    public CustomFile getSameNoDumpFile() {
        return SameNoDumpFile;
    }

    public CustomFile getDiffNoDumpFile() {
        return DiffNoDumpFile;
    }

    public CustomFile getDiffFile() {
        return DiffFile;
    }
    public CustomFile getSelfLigationFile() {
        return SelfLigationFile;
    }

    public CustomFile getReLigationFile() {
        return ReLigationFile;
    }

    public CustomFile getValidFile() {
        return ValidFile;
    }
}
