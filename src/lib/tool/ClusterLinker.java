package lib.tool;

import java.io.*;
import java.util.Date;

public class ClusterLinker {
    public ClusterLinker(String PastFastq, String[] LinkerFastq, int MinReadsLength, int MaxReadsLength, int MinLinkerFilterQuality, String Restriction, String AddQuality, String Type,int thread) throws IOException {
        BufferedReader fastq_read = new BufferedReader(new FileReader(PastFastq));
        BufferedWriter[] fastq_write = new BufferedWriter[LinkerFastq.length];
        for (int i = 0; i < LinkerFastq.length; i++) {
            fastq_write[i] = new BufferedWriter(new FileWriter(LinkerFastq[i]));
        }
        String add = AddQuality;
        String MatchRestriction;
        String AddSeq;
        String TempSeq = Restriction.replace("^", "");
        int restrictionSite = Restriction.indexOf("^");
        if (Type.equals("R1")) {
            if (restrictionSite < TempSeq.length() - restrictionSite) {
                restrictionSite = TempSeq.length() - restrictionSite;
            }
            MatchRestriction = TempSeq.substring(0, restrictionSite);
            try {
                AddSeq = TempSeq.substring(restrictionSite);
            } catch (IndexOutOfBoundsException e) {
                AddSeq = "";
            }
        } else if (Type.equals("R2")) {
            if (restrictionSite > TempSeq.length() - restrictionSite) {
                restrictionSite = TempSeq.length() - restrictionSite;
            }
            MatchRestriction = TempSeq.substring(restrictionSite);
            try {
                AddSeq = TempSeq.substring(0, restrictionSite);
            } catch (IndexOutOfBoundsException e) {
                AddSeq = "";
            }
        } else {
            MatchRestriction = "";
            AddSeq = "";
            System.err.println("Wrong Type " + Type);
            System.exit(0);
        }
        for (int i = 1; i < AddSeq.length(); i++) {
            AddQuality = AddQuality + add;
        }
        if (Type.equals("R1")) {
            Thread[] process = new Thread[thread];
            System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
            //多线程读取
            for (int i = 0; i < thread; i++) {
                String finalAddSeq = AddSeq;
                String finalAddQuality = AddQuality;
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
                                        if (AppendBase(str[0], MatchRestriction, Type)) {
                                            int len = 0;
                                            if (str[0].length() >= MaxReadsLength) {
                                                len = str[0].length();
                                            } else {
                                                len = MaxReadsLength;
                                            }
                                            synchronized (fastq_write[j]) {
                                                fastq_write[j].write(str[6] + "\n");
                                                fastq_write[j].write(str[0].substring(len - MaxReadsLength, str[0].length()) + finalAddSeq + "\n");
                                                fastq_write[j].write(str[8] + "\n");
                                                fastq_write[j].write(str[9].substring(len - MaxReadsLength, str[0].length()) + finalAddQuality + "\n");
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
            for (int i = 0; i < thread; i++) {
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
        } else if (Type.equals("R2")) {
            Thread[] process = new Thread[thread];
            System.out.println(new Date() + "\tBegin to cluster linker\t" + Type);
            //多线程读取
            for (int i = 0; i < thread; i++) {
                String finalAddSeq = AddSeq;
                String finalAddQuality1 = AddQuality;
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
                                        if (AppendBase(str[3], MatchRestriction, Type)) {
                                            int len;
                                            if (str[3].length() >= MaxReadsLength) {
                                                len = MaxReadsLength;
                                            } else {
                                                len = str[3].length();
                                            }
                                            synchronized (process) {
                                                fastq_write[j].write(str[6] + "\n");
                                                fastq_write[j].write(finalAddSeq + str[3].substring(0, len) + "\n");
                                                fastq_write[j].write(str[8] + "\n");
                                                fastq_write[j].write(finalAddQuality1 + str[9].substring(Integer.parseInt(str[2]) + 1, Integer.parseInt(str[2]) + 1 + len) + "\n");
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
            for (int i = 0; i < thread; i++) {
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
    private Boolean AppendBase(String Sequence, String Restriction, String Type) {
        if (Type.equals("R1")) {
            return Sequence.substring(Sequence.length() - Restriction.length(), Sequence.length()).equals(Restriction);
        } else if (Type.equals("R2")) {
            return Sequence.substring(0, Restriction.length()).equals(Restriction);
        } else {
            System.err.println(new Date() + "\tError parameter in  append one base\t" + Type);
            System.exit(0);
            return false;
        }
    }//OK
}
