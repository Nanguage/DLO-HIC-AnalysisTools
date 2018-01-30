
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;


public class Routine {

    public int Threads;

    Routine() {
        Threads = 1;
    }


    /**
     * <p>比对，调用bwa</p>
     *
     * @param IndexFile 索引文件
     * @param FastqFile fastq文件
     * @param SamFile   Sam文件
     * @param Threads   比对的线程数
     * @param MisMatch  最大错配个数
     * @throws IOException
     */
    public void Align(String IndexFile, String FastqFile, String SamFile, int Threads, int MisMatch) throws IOException {
        //比对
        int ExitValue;//命令行退出值
        System.out.println(new Date() + "\tBegin to align\t" + FastqFile);
        String CommandStr = "bwa aln -t " + Threads + " -n " + MisMatch + " -f " + FastqFile + ".sai " + IndexFile + " " + FastqFile;
        ExitValue = CommonMethod.CommandStrExe(CommandStr);//执行命令行
        if (ExitValue != 0) {
            //异常退出
            System.err.println(new Date() + "\tError in bwa aln");
            System.exit(0);
        }
        System.out.println(new Date() + "\tsai to sam\t" + FastqFile);
        CommandStr = "bwa samse -f " + SamFile + " " + IndexFile + " " + FastqFile + ".sai " + FastqFile;
        ExitValue = CommonMethod.CommandStrExe(CommandStr);//执行命令行
        if (ExitValue != 0) {
            //异常退出
            System.err.println(new Date() + "\tError in bwa samse");
            System.exit(0);
        }
        System.out.println(new Date() + "\tDelete " + FastqFile + ".sai ");
        new File(FastqFile + ".sai").delete();//删除sai文件
        System.out.println(new Date() + "\tEnd align\t" + FastqFile);

    }//OK

    /**
     * <p>Sam文件过滤</p>
     *
     * @param SameFile        Sam文件
     * @param FilterSameFile  过滤后的Sam文件
     * @param MinAlignQuality 最小比对质量
     * @throws IOException
     */
    public void SamFilter(String SameFile, String FilterSameFile, int MinAlignQuality) throws IOException {
        //sam文件过滤
        BufferedReader sam_read = new BufferedReader(new FileReader(SameFile));
        BufferedWriter sam_write = new BufferedWriter(new FileWriter(FilterSameFile));
        System.out.println(new Date() + "\tBegin to sam filter\t" + SameFile);
        Thread[] process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            process[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " begin");
                        while ((line = sam_read.readLine()) != null) {
                            str = line.split("\\s+");
                            if (str[0].matches("^@.+") || (Integer.parseInt(str[4]) >= MinAlignQuality)) {
                                synchronized (process) {
                                    sam_write.write(line + "\n");
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
            try {
                process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        sam_read.close();
        sam_write.close();
        System.out.println(new Date() + "\tEnd to sam filter\t" + SameFile);
    }//OK

    /**
     * <p>Sam文件转bed文件</p>
     *
     * @param SamFile Sam文件
     * @param BamFile Bam文件
     * @param BedFile Bed文件
     * @throws IOException
     */
    public void SamToBed(String SamFile, String BamFile, String BedFile) throws IOException {
        System.out.println(new Date() + "\tBegin\t" + SamFile + " to " + BedFile);
        String CommandStr = "samtools view -Sb -o " + BamFile + " " + SamFile;
        CommonMethod.CommandStrExe(CommandStr, SamFile + ".log");
        CommandStr = "bedtools bamtobed -i " + BamFile;
        CommonMethod.CommandStrExe(CommandStr, BedFile, BedFile + ".log");
        System.out.println(new Date() + "\tEnd\t" + SamFile + " to " + BedFile);
    }//OK

    public void SortBed(String BedFile, String SortBedFile) throws IOException {
        String line;
        String[] str;
        BufferedReader bed_read = new BufferedReader(new FileReader(BedFile));
        BufferedWriter bed_write = new BufferedWriter(new FileWriter(SortBedFile));
        ArrayList<String> bed_list = new ArrayList<>();
        SortListStr bed_sort_list = new SortListStr();
        System.out.println(new Date() + "\tBegin to sort bed file\t" + BedFile);
        while ((line = bed_read.readLine()) != null) {
            str = line.split("\\s+");
            bed_list.add(line);//构建打印表
            bed_sort_list.add(new String[]{str[3]});//构建排序表
        }
        bed_sort_list.QuickSort(0, bed_sort_list.size() - 1, "", (int) (Math.log(Threads) / Math.log(2)));//快速排序
        for (int i = 0; i < bed_sort_list.size(); i++) {
            bed_write.write(bed_list.get(Integer.parseInt(bed_sort_list.get(i)[0].toString())) + "\n");//打印
        }
        bed_read.close();
        bed_write.close();
        System.out.println(new Date() + "\tEnd to sort bed file\t" + BedFile);
    }//OK

    /**
     * <p>区分连接类型</p>
     *
     * @param InFile    输入文件
     * @param SelfFile
     * @param ReligFile
     * @param ValidFile
     * @throws IOException
     */
    public void SeparateLigationType(String InFile, String SelfFile, String ReligFile, String ValidFile) throws IOException {
        Thread[] process = new Thread[Threads];
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter selffile = new BufferedWriter(new FileWriter(SelfFile));
        BufferedWriter religfile = new BufferedWriter(new FileWriter(ReligFile));
        BufferedWriter valifile = new BufferedWriter(new FileWriter(ValidFile));
        System.out.println(new Date() + "\tBegin to seperate ligation\t" + InFile);
        for (int i = 0; i < Threads; i++) {
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
                                synchronized (process) {
                                    selffile.write(line + "\n");
                                }
                            } else if ((Integer.parseInt(str[str.length - 1]) - Integer.parseInt(str[str.length - 2]) == 1) && (Integer.parseInt(str[4]) < Integer.parseInt(str[2]))) {
                                synchronized (process) {
                                    religfile.write(line + "\n");
                                }
                            } else {
                                synchronized (process) {
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
        for (int i = 0; i < Threads; i++) {
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
        System.out.println(new Date() + "\tEnd seperate ligation\t" + InFile);
    }//OK

    /**
     * <p>区分不同类型的linker并写入到fastq文件</p>
     *
     * @param PastFastq              输入文件
     * @param LinkerFastq            不同类型的linker文件（输出为fastq格式）
     * @param MinReadsLength         最小reads长度
     * @param MaxReadsLength         最大reads长度
     * @param MinLinkerFilterQuality linker比对的最小质量
     * @param Restriction            需要匹配的酶切位点序列
     * @param AddSeq                 延长的序列
     * @param AddQuality             延长的质量
     * @param Type                   单端类型（1或2）
     * @throws IOException
     */
    public void ClusterLinker(String PastFastq, String[] LinkerFastq, int MinReadsLength, int MaxReadsLength, int MinLinkerFilterQuality, String Restriction, String AddSeq, String AddQuality, int Type) throws IOException {
        BufferedReader fastq_read = new BufferedReader(new FileReader(PastFastq));
        BufferedWriter[] fastq_write = new BufferedWriter[LinkerFastq.length];
        for (int i = 0; i < LinkerFastq.length; i++) {
            fastq_write[i] = new BufferedWriter(new FileWriter(LinkerFastq[i]));
        }
        if (Type == 1) {
            Thread[] process = new Thread[Threads];
            System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
            //多线程读取
            for (int i = 0; i < Threads; i++) {
                process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
//                            System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " begin");
                            while ((line = fastq_read.readLine()) != null) {
                                str = line.split("\\t+");
                                for (int j = 0; j < LinkerFastq.length; j++) {
                                    if (str[0].length() >= MinReadsLength && Integer.parseInt(str[5]) >= MinLinkerFilterQuality && Integer.parseInt(str[4]) == j) {
                                        if (AppendBase(str[0], Restriction, Type)) {
                                            int len = 0;
                                            if (str[0].length() >= MaxReadsLength) {
                                                len = str[0].length();
                                            } else {
                                                len = MaxReadsLength;
                                            }
                                            synchronized (process) {
                                                fastq_write[j].write(str[6] + "\n");
                                                fastq_write[j].write(str[0].substring(len - MaxReadsLength, str[0].length()) + AddSeq + "\n");
                                                fastq_write[j].write(str[8] + "\n");
                                                fastq_write[j].write(str[9].substring(len - MaxReadsLength, str[0].length()) + AddQuality + "\n");
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    }
                });
                process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                try {
                    process[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            fastq_read.close();
            for (int i = 0; i < LinkerFastq.length; i++) {
                fastq_write[i].close();
            }
            System.out.println(new Date() + "\tEnd to cluster linker\t" + Type);
        } else if (Type == 2) {
            Thread[] process = new Thread[Threads];
            System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
            //多线程读取
            for (int i = 0; i < Threads; i++) {
                process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
//                            System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " begin");
                            while ((line = fastq_read.readLine()) != null) {
                                str = line.split("\\t+");
                                for (int j = 0; j < LinkerFastq.length; j++) {
                                    if (str[3].length() >= MinReadsLength && Integer.parseInt(str[5]) >= MinLinkerFilterQuality && Integer.parseInt(str[4]) == j) {
                                        if (AppendBase(str[3], Restriction, Type)) {
                                            int len = 0;
                                            if (str[3].length() >= MaxReadsLength) {
                                                len = MaxReadsLength;
                                            } else {
                                                len = str[3].length();
                                            }
                                            synchronized (process) {
                                                fastq_write[j].write(str[6] + "\n");
                                                fastq_write[j].write(AddSeq + str[3].substring(0, len) + "\n");
                                                fastq_write[j].write(str[8] + "\n");
                                                fastq_write[j].write(AddQuality + str[9].substring(Integer.parseInt(str[2]) + 1, Integer.parseInt(str[2]) + 1 + len) + "\n");
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    }
                });
                process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                try {
                    process[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            fastq_read.close();
            for (int i = 0; i < LinkerFastq.length; i++) {
                fastq_write[i].close();
            }
            System.out.println(new Date() + "\tEnd to cluster linker\t" + Type);
        } else {
            System.err.println(new Date() + "\tError parameter in cluster linker\t" + Type);
            System.exit(0);
        }

    }//OK

    /**
     * <p>判断某条序列是否匹配酶切位点，并延长相应的碱基</p>
     *
     * @param Sequence    碱基序列
     * @param Restriction 要匹配的酶切位点序列
     * @param Type        单端类型（1或2）
     * @return 返回是否可以延长碱基
     */
    private Boolean AppendBase(String Sequence, String Restriction, int Type) {
        if (Type == 1) {
            if (Sequence.substring(Sequence.length() - Restriction.length(), Sequence.length()).equals(Restriction)) {
                return true;
            } else {
                return false;
            }
        } else if (Type == 2) {
            if (Sequence.substring(0, Restriction.length()).equals(Restriction)) {
                return true;
            } else {
                return false;
            }
        } else {
            System.err.println(new Date() + "\tError parameter in  append one base\t" + Type);
            System.exit(0);
            return false;
        }

    }//OK

    /**
     * <p>区分bedpe文件中染色体内的交互和染色体间的交互</p>
     *
     * @param BedpeFile     bedpe文件名
     * @param SameBedpeFile 染色体内的交互文件名
     * @param DiffBedpeFile 染色体间的交互文件名
     * @throws IOException
     */
    public void BedpeToSameAndDiff(String BedpeFile, String SameBedpeFile, String DiffBedpeFile) throws IOException {
        BufferedReader BedpeRead = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter SameBedpeWrite = new BufferedWriter(new FileWriter(SameBedpeFile));
        BufferedWriter DiffBedpeWrite = new BufferedWriter(new FileWriter(DiffBedpeFile));
        Thread[] process = new Thread[Threads];
        System.out.println(new Date() + "\tBegin to Seperate bedpe file\t" + BedpeFile);
        for (int i = 0; i < Threads; i++) {
            process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder line = new StringBuilder();
                    String[] str;
                    try {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " strat");
                        while (!line.append(BedpeRead.readLine()).toString().equals("null")) {
                            str = line.toString().split("\\s+");
                            //---------------------------取相同染色体上的交互-----------------------
                            if (str[0].equals(str[3])) {
                                if (Integer.parseInt(str[1]) < Integer.parseInt(str[4])) {
                                    synchronized (process) {
                                        SameBedpeWrite.write(line.toString() + "\n");
                                    }
                                } else {
                                    synchronized (process) {
                                        SameBedpeWrite.write(str[3] + "\t" + str[4] + "\t" + str[5] + "\t" + str[0] + "\t" + str[1] + "\t" + str[2] + "\t" + str[6] + "\t" + str[7] + "\t" + str[9] + "\t" + str[8] + "\n");
                                    }
                                }
                            }
                            //--------------------------取不同染色体上的交互----------------------
                            else {
                                try {
                                    if (Integer.parseInt(str[0].substring(3)) < Integer.parseInt(str[3].substring(3))) {
                                        synchronized (process) {
                                            DiffBedpeWrite.write(line.toString() + "\n");
                                        }
                                    } else {
                                        synchronized (process) {
                                            DiffBedpeWrite.write(str[3] + "\t" + str[4] + "\t" + str[5] + "\t" + str[0] + "\t" + str[1] + "\t" + str[2] + "\t" + str[6] + "\t" + str[7] + "\t" + str[9] + "\t" + str[8] + "\n");
                                        }
                                    }
                                } catch (NumberFormatException e) {//如果有XY染色体就会出现异常，捕捉到异常后就要用字符串来比较大小
                                    if (str[0].compareTo(str[3]) < 0) {
                                        synchronized (process) {
                                            DiffBedpeWrite.write(line.toString() + "\n");
                                        }
                                    } else {
                                        synchronized (process) {
                                            DiffBedpeWrite.write(str[3] + "\t" + str[4] + "\t" + str[5] + "\t" + str[0] + "\t" + str[1] + "\t" + str[2] + "\t" + str[6] + "\t" + str[7] + "\t" + str[9] + "\t" + str[8] + "\n");
                                        }
                                    }
                                }
                            }
                            line.setLength(0);
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            process[i].start();
        }
        for (int i = 0; i < Threads; i++) {
            try {
                process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        BedpeRead.close();
        SameBedpeWrite.close();
        DiffBedpeWrite.close();
    }//OK

    /**
     * <p>查找每个交互区域所属的酶切文件片段</p>
     *
     * @param BedpeFile   Bedpe文件名
     * @param EnySiteFile 酶切位点文件名
     * @param OutFile     输出文件名
     * @throws IOException
     */
    public void WhichEnzymeFragment(String BedpeFile, String EnySiteFile, String OutFile) throws IOException {
        ArrayList<Integer> EnySiteList = new ArrayList<>();
        BufferedReader EnySiteRead = new BufferedReader(new FileReader(EnySiteFile));
        BufferedReader SeqRead = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter OutWrite = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        System.out.println(new Date() + "\tBegin to find eny site\t" + BedpeFile);
        //---------------------------------------------------
        while ((line = EnySiteRead.readLine()) != null) {
            str = line.split("\\s+");
            EnySiteList.add(Integer.parseInt(str[str.length - 1]));
        }
        EnySiteRead.close();
        //---------------------多线程-------------------------
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
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
                                //修改了格式
//                                OutWrite.write(str[0] + "\t" + position[0] + "\t" + str[3] + "\t" + position[1]);
//                                for (int k = 6; k < str.length; k++) {
//                                    OutWrite.write("\t" + str[k]);
//                                }
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

                Date() + "\tEnd to find eny site\t" + BedpeFile);
    }//OK

    /**
     * 支持多线程，不用太多
     *
     * @param InFile     input file
     * @param Row        row number
     * @param Chromosome chromosome
     * @param OutFile    out file
     * @throws IOException throws IOException
     */
    public void SeparateChromosome(String InFile, int Row, String Chromosome, String OutFile) throws IOException {
        System.out.println(new Date() + "\tStart to SeparateChromosome\t" + Chromosome + "\t" + InFile);
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
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
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        outfile.close();
        System.out.println(new Date() + "\tEnd to SeparateChromosome\t" + Chromosome + "\t" + InFile);
    }//OK

    /**
     * Need to sort first
     *
     * @param InFile
     * @param Row
     * @param OutFile
     * @throws IOException
     */
    public void RemoveRepeat(String InFile, int[] Row, String OutFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(InFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        String temp1 = "";
        StringBuilder temp2 = new StringBuilder();
        System.out.println(new Date() + "\tStart to remove repeat\t" + InFile);
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
        System.out.println(new Date() + "\tEnd to remove repeat\t" + InFile);
    }//OK

    /**
     * <p>将bedpe文件做处理，将2，3和5，6列的区间换成中点</p>
     *
     * @param BedpeFile bedpe文件名
     * @param OutFile   输出文件名
     * @throws IOException
     */
    public void BedpeToInter(String BedpeFile, String OutFile) throws IOException {
        BufferedReader infile = new BufferedReader(new FileReader(BedpeFile));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String line;
        String[] str;
        System.out.println(new Date() + "\tBed to interaction start\t" + BedpeFile);
        while ((line = infile.readLine()) != null) {
            str = line.split("\\s+");
            outfile.write(str[0] + "\t" + (Integer.parseInt(str[2]) + Integer.parseInt(str[1])) / 2 + "\t");
            outfile.write(str[3] + "\t" + (Integer.parseInt(str[5]) + Integer.parseInt(str[4])) / 2);
            for (int i = 6; i < str.length; i++) {
                outfile.write("\t" + str[i]);
            }
            outfile.write("\n");
        }
        outfile.close();
        System.out.println(new Date() + "\tBed to interaction end\t" + BedpeFile);
    }//OK

    /**
     * <p>建立交互矩阵，输入为处理过的bedpe文件，交互区域为点，而不是区域</p>
     *
     * @param InterBedpeFile 处理过的bedoe文件（将bedpe文件的区间改成一个点）
     * @param Chromosome     要处理的染色体名字
     * @param ChrSize        染色体的大小，单位base，与前面的染色体名字对应
     * @param Resolution     分辨率
     * @param Prefix         输出前缀
     * @return 交互矩阵
     * @throws IOException
     */
    public int[][] CreatInterActionMatrix(String InterBedpeFile, String[] Chromosome, int[] ChrSize, int Resolution, String Prefix) throws IOException {
        System.out.println(new Date() + "\tBegin to creat interaction matrix " + InterBedpeFile);
        int[] ChrBinSize;
        int SumBin = 0;
        //计算bin的总数
        ChrBinSize = CommonMethod.CalculatorBinSize(ChrSize, Resolution);
        for (int i = 0; i < ChrBinSize.length; i++) {
            SumBin = SumBin + ChrBinSize[i];
        }
        if (SumBin > 50000) {
            System.err.println("Error ! too many bins, there are " + SumBin + " bins.");
            System.exit(0);
        }
        int[][] InterMatrix = new int[SumBin][SumBin];
        for (int i = 0; i < InterMatrix.length; i++) {
            Arrays.fill(InterMatrix[i], 0);//数组初始化为0
        }
        BufferedReader infile = new BufferedReader(new FileReader(InterBedpeFile));
        Thread[] Process = new Thread[Threads];
        //----------------------------------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            int finalSumBin = SumBin;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        String[] str;
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " start");
                        while ((line = infile.readLine()) != null) {
                            str = line.split("\\s+");
                            int hang = Integer.parseInt(str[1]) / Resolution;
                            int lie = Integer.parseInt(str[3]) / Resolution;
                            for (int j = 0; j < Chromosome.length; j++) {
                                if (str[0].equals(Chromosome[j])) {
                                    break;
                                }
                                hang = hang + ChrBinSize[j];
                            }
                            if (hang >= finalSumBin) {
                                continue;
                            }
                            for (int j = 0; j < Chromosome.length; j++) {
                                if (str[2].equals(Chromosome[j])) {
                                    break;
                                }
                                lie = lie + ChrBinSize[j];
                            }
                            if (lie >= finalSumBin) {
                                continue;
                            }
                            synchronized (Process) {
                                InterMatrix[hang][lie]++;
                                if (hang != lie) {
                                    InterMatrix[lie][hang]++;
                                }
                            }
                        }
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + " end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        //-------------------------------------------------
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        infile.close();
        //--------------------------------------------------------
        //打印矩阵
        CommonMethod.PrintMatrix(InterMatrix, Prefix + ".2d.matrix", Prefix + ".spare.matrix");
        System.out.println(new Date() + "\tEnd to creat interaction matrix");
        //--------------------------------------------------------------------
        int temp = 0;
        BufferedWriter outfile = new BufferedWriter(new FileWriter(Prefix + ".matrix.BinSize"));
        for (int i = 0; i < Chromosome.length; i++) {
            temp = temp + 1;
            outfile.write(Chromosome[i] + "\t" + temp + "\t");
            temp = temp + ChrBinSize[i] - 1;
            outfile.write(temp + "\n");
        }
        outfile.close();
        return InterMatrix;
    }//OK

    /**
     * <p>交互矩阵的标准化</p>
     *
     * @param Matrix 交互矩阵
     * @return 标准化后的交互矩阵
     */
    public double[][] MatrixNormalize(int[][] Matrix) {
        System.out.println(new Date() + "\tNormalize Matrix");
        double[][] NormalizeMatrix = new double[Matrix.length][Matrix.length];//定义标准化矩阵
        double[][] Distance = new double[3][Matrix.length];//定义距离数组
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = i; j < Matrix.length; j++) {
                Distance[0][j - i]++;//计算相同距离的交互点的个数
                Distance[1][j - i] += Matrix[i][j];//计算相同距离的交互点的总数
            }
        }
        for (int i = 0; i < Matrix.length; i++) {
            Distance[2][i] = Distance[1][i] / Distance[0][i];//计算平均交互数
        }
        for (int i = 0; i < Matrix.length; i++) {
            for (int j = 0; j < Matrix.length; j++) {
                if (Distance[2][Math.abs(i - j)] == 0) {
                    NormalizeMatrix[i][j] = 0;//如果某个距离平均交互数为0，则直接将标准化矩阵对应点设成0
                } else {
                    NormalizeMatrix[i][j] = Matrix[i][j] / Distance[2][Math.abs(i - j)];//用对应距离的交互值除以对应的平均交互值
                }
            }
        }
        System.out.println(new Date() + "\tNormalize Matrix end");
        return NormalizeMatrix;//返回标准化后的矩阵
    }

    /**
     * <p>将两个排序好的bed文件合并成bedpe文件</p>
     *
     * @param InFile1 左端bed文件
     * @param InFile2 右端bed文件
     * @param OutFile 输出文件
     * @param Row     比较的列数
     * @param Regex   拆分行用的正则表达式，默认为多个空格
     * @throws IOException
     */
    public void MergeBedToBedpe(String InFile1, String InFile2, String OutFile, int Row, String Regex) throws IOException {
        System.out.println(new Date() + "\tMerge " + InFile1 + " and " + InFile2 + " to " + OutFile + " start");
        BufferedReader infile1 = new BufferedReader(new FileReader(InFile1));
        BufferedReader infile2 = new BufferedReader(new FileReader(InFile2));
        BufferedWriter outfile = new BufferedWriter(new FileWriter(OutFile));
        String regex = Regex;
        if (Regex.isEmpty()) {
            regex = "\\s+";
        }
        String line1, line2;
        String[] str1, str2;
        line1 = infile1.readLine();
        line2 = infile2.readLine();
        str1 = line1.split(regex);
        str2 = line2.split(regex);
        boolean Flage = true;
        while (line1 != null && line2 != null) {
            if (Flage) {
                str1 = line1.split(regex);
            } else {
                str2 = line2.split(regex);
            }
            if (str1[Row - 1].compareTo(str2[Row - 1]) < 0) {
                line1 = infile1.readLine();
                Flage = true;
            } else if (str1[Row - 1].compareTo(str2[Row - 1]) > 0) {
                line2 = infile2.readLine();
                Flage = false;
            } else {
                outfile.write(str1[0] + "\t" + str1[1] + "\t" + str1[2] + "\t" + str2[0] + "\t" + str2[1] + "\t" + str2[2] + "\t" + str1[Row - 1] + "\t" + str1[str1.length - 2] + "\t" + str2[str2.length - 2] + "\t" + str1[str1.length - 1] + "\t" + str2[str2.length - 1] + "\n");
                line1 = infile1.readLine();
                line2 = infile2.readLine();
                try {
                    str1 = line1.split(regex);
                    str2 = line2.split(regex);
                } catch (NullPointerException ignored) {

                }
            }
        }
        infile1.close();
        infile2.close();
        outfile.close();
        System.out.println(new Date() + "\tMerge " + InFile1 + " and " + InFile2 + " to " + OutFile + " end");
    }

    /**
     * <p>将交互bedpe文件拆分成单个染色体内的交互文件，染色体间的交互会被分到Prefix+".diff.bedpe"文件</p>
     *
     * @param InterBedpeFile 交互bedpe文件，就是将bedpe文件的区间改成了点
     * @param Chromosome     染色体数组，包括想要区分的染色体，名字要与bedpe文件中的名字一致
     * @param Prefix         输出文件的前缀
     * @param Regex          拆分行所用的正则表达式，默认为多个空格
     * @return 拆分成每个染色体的交互bedpe的文件名
     * @throws IOException
     */
    public String[] SeparateInterBedpe(String InterBedpeFile, String[] Chromosome, String Prefix, String Regex) throws IOException {
        System.out.println(new Date() + "\tSeperate InterBedpe " + InterBedpeFile);
        BufferedReader interfile = new BufferedReader(new FileReader(InterBedpeFile));
        String[] SameChrFile = new String[Chromosome.length];
        BufferedWriter[] chrwrite = new BufferedWriter[Chromosome.length];
        String DiffFile = Prefix + ".diff.bedpe";
        BufferedWriter diffwrite = new BufferedWriter(new FileWriter(DiffFile));
        //------------------------------------------------------------
        Hashtable<String, Integer> ChrIndex = new Hashtable<>();
        for (int i = 0; i < Chromosome.length; i++) {
            ChrIndex.put(Chromosome[i], i);
            SameChrFile[i] = Prefix + "." + Chromosome[i] + ".same.bedpe";
            chrwrite[i] = new BufferedWriter(new FileWriter(SameChrFile[i]));
        }
        if (Regex.isEmpty()) {
            Regex = "\\s+";
        }
        //================================================================
        Thread[] Process = new Thread[Threads];
        for (int i = 0; i < Threads; i++) {
            String finalRegex = Regex;
            Process[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    String[] str;
                    try {
                        while ((line = interfile.readLine()) != null) {
                            str = line.split(finalRegex);
                            if (str[0].equals(str[2])) {
                                synchronized (Process) {
                                    chrwrite[ChrIndex.get(str[0])].write(line + "\n");
                                }
                            } else {
                                synchronized (Process) {
                                    diffwrite.write(line + "\n");
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Process[i].start();
        }
        for (int i = 0; i < Threads; i++) {
            try {
                Process[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < Chromosome.length; i++) {
            chrwrite[i].close();
        }
        diffwrite.close();
        System.out.println(new Date() + "\tEnd seperate InterBedpe " + InterBedpeFile);
        return SameChrFile;
    }

    public Hashtable<String, Integer> FindRestrictionSite(String FastFile, String Restriction, String Prefix) throws IOException {
        BufferedReader fastfile = new BufferedReader(new FileReader(FastFile));
        BufferedWriter chrwrite;
        Hashtable<String, Integer> ChrSize = new Hashtable<>();
        StringBuilder Seq = new StringBuilder();
        String line;
        String Chr = "";
        int Site = Restriction.indexOf("^");
        Restriction = Restriction.replace("^", "");
        int ResLength = Restriction.length();
        //找到第一个以 ">" 开头的行
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                Chr = line.split("\\s+")[0].replace(">", "");
                break;
            }
        }
        while ((line = fastfile.readLine()) != null) {
            if (line.matches("^>.+")) {
                int Count = 0;
                int len = Seq.length();
                chrwrite = new BufferedWriter(new FileWriter(Prefix + "." + Chr + ".txt"));
                chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
                ChrSize.put(Chr, len);
                for (int i = 0; i <= len - ResLength; i++) {
                    if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                        Count++;
                        chrwrite.write(Count + "\t+\t" + Chr + "\t" + String.valueOf(i + Site) + "\n");
                    }
                }
                chrwrite.write(++Count + "\t+\t" + Chr + "\t" + len + "\n");
                chrwrite.close();
                Seq.setLength(0);
                Chr = line.split("\\s+")[0].replace(">", "");
            } else {
                Seq.append(line);
            }
        }
        //========================================打印最后一条染色体=========================================
        int Count = 0;
        int len = Seq.length();
        chrwrite = new BufferedWriter(new FileWriter(Prefix + "." + Chr + ".txt"));
        chrwrite.write(Count + "\t+\t" + Chr + "\t0\n");
        ChrSize.put(Chr, len);
        for (int i = 0; i <= len - ResLength; i++) {
            if (Seq.substring(i, i + ResLength).equals(Restriction)) {
                Count++;
                chrwrite.write(Count + "\t+\t" + Chr + "\t" + String.valueOf(i + Site) + "\n");
            }
        }
        chrwrite.write(++Count + "\t+\t" + Chr + "\t" + len + "\n");
        chrwrite.close();
        Seq.setLength(0);
        return ChrSize;
    }

}
