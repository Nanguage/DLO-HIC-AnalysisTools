import java.io.IOException;

import lib.tool.Statistic;

public class RangeCount {
    public static void main(String[] args) throws IOException {
        if (args.length <= 1) {
            System.out.println("Usage: java -cp DLO-HIC-AnalysisTools.jar RangeCount <infile> <min:max>");
            System.exit(0);
        }
        String File = args[0];
        float[] range = new float[2];
        try {
            range[0] = Float.parseFloat(args[1].split(":")[0]);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            range[0] = Float.NEGATIVE_INFINITY;
        }
        try {
            range[1] = Float.parseFloat(args[1].split(":")[1]);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            range[1] = Float.POSITIVE_INFINITY;
        }
        int Threads;
        try {
            Threads = Integer.parseInt(args[2]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            Threads = 1;
        }
        try {
            System.out.println("Rabge between " + range[0] + " to " + range[1] + " :\t" + Statistic.RangeCount(File, range[0], range[1], Threads));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}