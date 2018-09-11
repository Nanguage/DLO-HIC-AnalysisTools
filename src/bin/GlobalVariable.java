package bin;

import lib.unit.Chromosome;

import java.io.File;

public class GlobalVariable {
    private enum OutDir {
        Pre("PreProcess"), Se("SeProcess"), Bedpe("BedpeProcess"), CM("CreateMatrix"), Trans("TransLocation");
        private String Name;

        private OutDir(String s) {
            this.Name = s;
        }

        @Override
        public String toString() {
            return this.Name;
        }
    }


    public File OutPath;
    public String Prefix;
    public File GenomeFile;
    public File IndexPrefix;
    public Chromosome[] Chrs;
    public String[] HalfLinker;
    public String[] LinkerSeq;
    public int MinUniqueScore;
    public File R1FastqFile, R2FastqFile;
    public File R1SamFile, R2SamFile;
    public File R1BedFile, R2BedFile;
    public File RawBedpeFile, RawSameBedpeFile, RawDiffBedpeFile, CleanSameBedpeFile, CleanDiffBedpeFile, CleanBedpeFile;
    public File SelfLigation, RelLigation, ValidLigation;
    public File[] ChrCleanSameBedpeFile;
    public int ThreadNum = 1;
    public int DebugLevel = 1;

    public void OutFileInit() {
        if (HalfLinker != null) {
            LinkerSeq = new String[HalfLinker.length * HalfLinker.length];
            for (int i = 0; i < HalfLinker.length; i++) {
                for (int j = 0; j < HalfLinker.length; j++) {
                    LinkerSeq[i * HalfLinker.length + j] = HalfLinker[i] + new StringBuffer(HalfLinker[j]).reverse();
                }
            }
        }

    }
}
