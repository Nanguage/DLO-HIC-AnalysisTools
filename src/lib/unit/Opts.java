package lib.unit;

import java.io.File;

public class Opts {
    /**
     * 文件类型枚举类
     */
    public enum FileFormat {
        ErrorFormat, EmptyFile, BedpePointFormat, BedpeRegionFormat, TwoDMatrixFormat, SpareMatrixFormat, Phred33, Phred64
    }

    /**
     * 参数设置枚举类
     */
    public enum Options {
        Single, PairEnd, R1, R2, ShortReads, LongReads
    }

    public static final int Single = 1;
    public static final int PairEnd = 2;
//    public static final int Phred33 = 33;
//    public static final int Phred64 = 64;
    public static final int R1 = 1;
    public static final int R2 = 2;
    public static final int ShortReads = 1;
    public static final int LongReads = 2;
    public static final int ErrorFormat = -1;
//    public static final int EmptyFile = 0;
//    public static final int BedpePointFormat = 1;
//    public static final int BedpeRegionFormat = 2;
//    public static final int TwoDMatrixFormat = 2;
//    public static final int SpareMatrixFormat = 3;
    public static final int MaxBinNum = 50000;
    public static final String PreDir = "PreProcess";
    public static final String SeDir = "SeProcess";
    public static final String BedpeDir = "BedpeProcess";
    public static final String MatrixDir = "MakeMatrix";
    public static final String EnzyFragDir = "EnzymeFragment";
    public static final String IndexDir = "Index";
    public static final String TransDir = "TransDetection";
    public static final String[] Step = new String[]{"LinkerFilter", "DivideLinker", "SeProcess", "Bed2BedPe", "BedPe2Inter", "MakeMatrix", "TranslocationDetect"};
    public static final File JarFile = new File(Opts.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    public static final CustomFile CommandOutFile = new CustomFile("./command.log");
    public static final CustomFile ConfigFile = new CustomFile(JarFile.getParent() + "/default.conf");
    public static final CustomFile AdvConfigFile = new CustomFile(JarFile.getParent() + "/default_adv.conf");
    public static final File PlotScriptFile = new File(JarFile.getParent() + "/script/PlotHeatmap.py");
    public static final Float Version = 1.0F;
    public static final String Author = "Snowflakes";
    public static final String Email = "john-jh@foxmail.com";
    public static String Bwa = "bwa";
    public static String Bowtie = "bowtie2";
    public static int DeBugLevel = 0;//Debug等级，0表示保留主要文件，1表示保留中间文件，3表示保留所有文件
    public static final long MaxMemory = Runtime.getRuntime().maxMemory();//java能获取的最大内存
}



