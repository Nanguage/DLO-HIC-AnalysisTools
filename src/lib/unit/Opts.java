package lib.unit;

import java.io.File;

public class Opts {
    public static final int Single = 1;
    public static final int PairEnd = 2;
    public static final int Phred33 = 33;
    public static final int Phred64 = 64;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int ErrorFormat = -1;
    public static final int BedpePointFormat = 1;
    public static final int BedpeRegionFormat = 2;
    public static final int MaxBinNum = 50000;
    public static final String PreDir = "PreProcess";
    public static final String SeDir = "SeProcess";
    public static final String BedpeDir = "BedpeProcess";
    public static final String MatrixDir = "MakeMatrix";
    public static final String EnzyFragDir = "EnzymeFragment";
    public static final String IndexDir = "Index";
    public static final String TransDir = "TransDetect";
    public static final String[] Step = new String[]{"LinkerFilter", "DivideLinker", "SeProcess", "Bed2BedPe", "BedPe2Inter", "MakeMatrix", "TranslocationDetect"};
    public static final File JarFile = new File(Opts.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    public static final File PlotScriptFile = new File(Opts.JarFile.getParent() + "/script/PlotHeatmap.py");

    public class Default {
        public static final int Resolution = 1000000;
        public static final int Thread = 1;
        public static final int MatchScore = 1;
        public static final int MisMatchScore = -1;
        public static final int IndelScore = -1;
        public static final String OutPath = "./";
        public static final String Prefix = "out";
        public static final int MaxReadsLen = 20;
        public static final int MinReadsLen = 16;
    }
}

