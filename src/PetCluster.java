
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;

public class PetCluster {
    private ArrayList<int[]> Region;
    // private ArrayList<int[]> Region2;
    private ArrayList<int[]> Cluster = new ArrayList<>();

    PetCluster(ArrayList<int[]> r) {
        Region = r;
        // Region2 = r2;
    }


    public static void main(String args[]) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java PetCluster <file> [<length>]");
            System.exit(0);
        }
        String infile = args[0];
        String outfile = "";
        String line;
        int Length = 0;
        try {
            Length = Integer.parseInt(args[1]);
            outfile = args[2];
        } catch (IndexOutOfBoundsException e) {
            if (Length == 0) {
                Length = 500;
            }
            if (outfile.equals("")) {
                outfile = infile + ".cluster";
            }
        }
        BufferedReader in = new BufferedReader(new FileReader(infile));
        // ArrayList<int[]> r1 = new ArrayList<>();
        // ArrayList<int[]> r2 = new ArrayList<>();
        Hashtable<String, ArrayList<int[]>> ChrMatrix = new Hashtable<>();
        while ((line = in.readLine()) != null) {
            String[] str = line.split("\\s+");
            String chr1 = str[0];
            String chr2 = str[2];
            String key = chr1 + "-" + chr2;
            int count = 1;
            if (!ChrMatrix.containsKey(key)) {
                ChrMatrix.put(key, new ArrayList<int[]>());
            }
            if (str.length >= 5) {
                try {
                    count = Integer.parseInt(str[4]);
                } catch (NumberFormatException e) {
                    count = 1;
                }
            }
            ChrMatrix.get(key).add(new int[]{Integer.parseInt(str[1]) - Length, Integer.parseInt(str[1]) + Length, Integer.parseInt(str[3]) - Length, Integer.parseInt(str[3]) + Length, count});
//            System.out.println(str[0] + "\t" + r1.get(r1.size() - 1)[0] + "\t" + r1.get(r1.size() - 1)[1] + "\t" + str[2] + "\t" + r2.get(r2.size() - 1)[0] + "\t" + r2.get(r2.size() - 1)[1]);
        }
        in.close();
        BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
        for (String key : ChrMatrix.keySet()) {
            String chr1 = key.split("-")[0];
            String chr2 = key.split("-")[1];
            PetCluster pet = new PetCluster(ChrMatrix.get(key));
            pet.Run();
            ArrayList<int[]> clustr = pet.getCluster();
            for (int[] aClustr : clustr) {
                out.write(chr1 + "\t" + aClustr[0] + "\t" + aClustr[1] + "\t" + chr2 + "\t" + aClustr[2] + "\t" + aClustr[3] + "\t" + aClustr[4] + "\n");
            }
        }
        out.close();
    }

    public void Run() {
        ArrayList<int[]> temp = Region;
        boolean Flag = true;
//        while (Flag) {
//            Flag = FindCluster(temp);
//            temp = Cluster;
//        }
        FindCluster(temp);
    }

    public void FindCluster(ArrayList<int[]> r) {
//        ArrayList<Long> MaxNum = new ArrayList<>();
        ArrayList<Integer> OldUpDate = new ArrayList<>();
        for (int i = r.size() - 1; i >= 0; i--) {
            OldUpDate.add(i);
        }
        ArrayList<Integer> NewUpDate = new ArrayList<>();
        int Flag = -1;
//        MaxNum.add(r.get(0)[1]);
        while (OldUpDate.size() > 0) {
            for (int i = 0; i < OldUpDate.size(); i++) {
                int UpdateIndex = -1;
                int index = OldUpDate.get(i);
                if (r.get(index)[0] == Flag) {
                    continue;
                }
                int j;
                j = index + 1;
                while (j < r.size()) {
                    if (r.get(j)[0] != Flag) {
                        if (r.get(index)[1] < r.get(j)[0]) {
                            break;
                        }
                        int RMax = Math.max(r.get(index)[3], r.get(j)[3]);
                        int RMin = Math.min(r.get(index)[2], r.get(j)[2]);
                        int LMax = Math.max(r.get(index)[1], r.get(j)[1]);
                        int LMin = r.get(index)[0];
                        if ((RMax - RMin) <= (r.get(index)[3] + r.get(j)[3] - r.get(index)[2] - r.get(j)[2])) {
                            r.set(index, new int[]{LMin, LMax, RMin, RMax, r.get(index)[4] + r.get(j)[4]});
                            r.set(j, new int[]{Flag});
                            UpdateIndex = index;
                        }
                    }
                    j++;
                }
                j = index - 1;
                while (j >= 0) {
                    if (r.get(j)[0] != Flag) {
                        if (r.get(j)[1] < r.get(index)[0]) {
                            break;
                        }
                        int RMax = Math.max(r.get(index)[3], r.get(j)[3]);
                        int RMin = Math.min(r.get(index)[2], r.get(j)[2]);
                        int LMax = Math.max(r.get(index)[1], r.get(j)[1]);
                        int LMin = r.get(j)[0];
                        if ((RMax - RMin) <= (r.get(index)[3] + r.get(j)[3] - r.get(index)[2] - r.get(j)[2])) {
                            r.set(j, new int[]{LMin, LMax, RMin, RMax, r.get(index)[4] + r.get(j)[4]});
                            r.set(index, new int[]{Flag});
                            UpdateIndex = j;
                            break;
                        }
                    }
                    j--;
                }
                if (UpdateIndex != -1) {
                    NewUpDate.add(UpdateIndex);
                }
            }
            OldUpDate.clear();
            OldUpDate.addAll(NewUpDate);
            NewUpDate.clear();
        }
        for (int[] item : r) {
            if (item[0] != Flag) {
                Cluster.add(item);
            }
        }
    }


    public ArrayList<int[]> getCluster() {
        return Cluster;
    }

}
