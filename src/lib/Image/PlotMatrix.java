package lib.Image;

import lib.tool.Tools;
import lib.unit.Opts;

import java.io.File;
import java.io.IOException;

public class PlotMatrix {
    private File MatrixFile;
    private File OutFile;
    private File BinSizeFile;
    private int Resolution;
    private String ComLine;

    public PlotMatrix(File MatrixFile, File OutFile, int Resolution) {
        this.MatrixFile = MatrixFile;
        this.OutFile = OutFile;
        this.Resolution = Resolution;
    }

    public int Run(File BinSizeFile) throws IOException, InterruptedException {
        this.BinSizeFile = BinSizeFile;
        ComLine = "python " + Opts.PlotScriptFile + " -m A -i " + MatrixFile + " -o " + OutFile + " -r " + Resolution + " -c " + BinSizeFile + " -q 98";
        return Tools.ExecuteCommandStr(ComLine, null, null);
    }

    public int Run(String[] Region) throws IOException, InterruptedException {
        ComLine = "python " + Opts.PlotScriptFile + " -t localGenome -m A -i " + MatrixFile + " -o " + OutFile + " -r " + Resolution + " -p " + String.join(":", Region) + " -q 95";
        return Tools.ExecuteCommandStr(ComLine, null, null);
    }

    public String getComLine() {
        return ComLine;
    }
}
