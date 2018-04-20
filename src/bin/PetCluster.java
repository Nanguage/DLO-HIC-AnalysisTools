package bin;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;

import lib.tool.Tools;
import org.apache.commons.cli.*;

public class PetCluster {
    private ArrayList<int[]> Region;
    private ArrayList<int[]> Cluster = new ArrayList<>();
    private Hashtable<Integer, Integer> CountStat = new Hashtable<>();

    PetCluster(ArrayList<int[]> r) {
        Region = r;
    }


    public static void main(String args[]) throws IOException, ParseException {
        Options Arguement = new Options();
        Arguement.addOption(Option.builder("f").argName("file").hasArg().required().desc("[required] bedpe file").build());
        Arguement.addOption(Option.builder("l").argName("int").hasArg().desc("extend length (default 0, should set when interaction site is a point)").build());
        Arguement.addOption(Option.builder("p").longOpt("pre").argName("string").hasArg().desc("out prefix (include path)").build());
        if (args.length == 0) {
            new HelpFormatter().printHelp("java -cp DLO-HIC-AnalysisTools.jar bin.PetCluster <-f file> [option]", Arguement);
            System.exit(1);
        }
        CommandLine comline = new DefaultParser().parse(Arguement, args);
        String infile = comline.getOptionValue("f");
        String outprefix = comline.hasOption("p") ? comline.getOptionValue("p") : infile;
        int Length = comline.hasOption("l") ? Integer.parseInt(comline.getOptionValue("l")) : 0;
        String line;
        BufferedReader in = new BufferedReader(new FileReader(infile));
        Hashtable<String, ArrayList<int[]>> ChrMatrix = new Hashtable<>();
        if (Tools.BedpeDetect(infile) == 1) {
            if (Length == 0) {
                System.err.println("Error! extend length is 0");
                System.exit(1);
            }
            while ((line = in.readLine()) != null) {
                String[] str = line.split("\\s+");
                String chr1 = str[0];
                String chr2 = str[2];
                String key = chr1 + "-" + chr2;
                int count = 1;
                if (!ChrMatrix.containsKey(key)) {
                    ChrMatrix.put(key, new ArrayList<>());
                }
                if (str.length >= 5) {
                    try {
                        count = Integer.parseInt(str[4]);
                    } catch (NumberFormatException e) {
                        count = 1;
                    }
                }
                ChrMatrix.get(key).add(new int[]{Integer.parseInt(str[1]) - Length, Integer.parseInt(str[1]) + Length, Integer.parseInt(str[3]) - Length, Integer.parseInt(str[3]) + Length, count});
            }
        } else if (Tools.BedpeDetect(infile) == 2) {
            while ((line = in.readLine()) != null) {
                String[] str = line.split("\\s+");
                String chr1 = str[0];
                String chr2 = str[3];
                String key = chr1 + "-" + chr2;
                int count = 1;
                if (!ChrMatrix.containsKey(key)) {
                    ChrMatrix.put(key, new ArrayList<>());
                }
                if (str.length >= 7) {
                    try {
                        count = Integer.parseInt(str[6]);
                    } catch (NumberFormatException e) {
                        count = 1;
                    }
                }
                ChrMatrix.get(key).add(new int[]{Integer.parseInt(str[1]) - Length, Integer.parseInt(str[2]) + Length, Integer.parseInt(str[4]) - Length, Integer.parseInt(str[5]) + Length, count});
            }
        } else {
            System.err.println("Error format !");
            System.exit(1);
        }
        in.close();
        BufferedWriter out = new BufferedWriter(new FileWriter(outprefix + ".cluster"));
        Hashtable<Integer, Integer> countstat = new Hashtable<>();
        for (String chrinter : ChrMatrix.keySet()) {
            String chr1 = chrinter.split("-")[0];
            String chr2 = chrinter.split("-")[1];
            PetCluster pet = new PetCluster(ChrMatrix.get(chrinter));
            pet.FindCluster();
            ArrayList<int[]> clustr = pet.getCluster();
            for (int[] aClustr : clustr) {
                out.write(chr1 + "\t" + aClustr[0] + "\t" + aClustr[1] + "\t" + chr2 + "\t" + aClustr[2] + "\t" + aClustr[3] + "\t" + aClustr[4] + "\n");
            }
            Hashtable<Integer, Integer> tempstat = pet.getCountStat();
            for (Integer count : tempstat.keySet()) {
                countstat.put(count, tempstat.get(count) + countstat.getOrDefault(count, 0));
            }
        }
        out.close();
        out = new BufferedWriter(new FileWriter(outprefix + ".count"));
        ArrayList<Integer> keylist = new ArrayList<>(countstat.keySet());
        Collections.sort(keylist);
        for (int k : keylist) {
            out.write(k + "\t" + countstat.get(k) + "\n");
        }
        out.close();
    }

    public void FindCluster() {
        int OldSize = 0;
        int NewSize = Region.size();
        int Flag = -1;
        while (OldSize != NewSize) {
            OldSize = NewSize;
            for (int i = 0; i < Region.size(); i++) {
                if (Region.get(i)[0] == Flag) {
                    continue;
                }
                int j = i + 1;
                while (j < Region.size()) {
                    if (Region.get(j)[0] != Flag) {
                        if (Region.get(i)[1] < Region.get(j)[0]) {
                            break;
                        }
                        int RMax = Math.max(Region.get(i)[3], Region.get(j)[3]);
                        int RMin = Math.min(Region.get(i)[2], Region.get(j)[2]);
                        int LMax = Math.max(Region.get(i)[1], Region.get(j)[1]);
                        int LMin = Region.get(i)[0];
                        if ((RMax - RMin) <= (Region.get(i)[3] + Region.get(j)[3] - Region.get(i)[2] - Region.get(j)[2])) {
                            Region.set(i, new int[]{LMin, LMax, RMin, RMax, Region.get(i)[4] + Region.get(j)[4]});
                            Region.set(j, new int[]{Flag});
                            NewSize--;
                        }
                    }
                    j++;
                }
            }
        }
        for (int[] item : Region) {
            if (item[0] != Flag) {
                Cluster.add(item);
                CountStat.put(item[4], CountStat.getOrDefault(item[4], 0) + 1);
            }
        }
    }

    public ArrayList<int[]> getCluster() {
        return Cluster;
    }

    public Hashtable<Integer, Integer> getCountStat() {
        return CountStat;
    }
}
