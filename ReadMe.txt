==================DLO-HiC Analysis Tools====================

Usage: java -jar DLO-HIC-AnalysisTools.jar <ConfigFile>

==========================require==================================
mafft               need add in "PATH"
bwa                 need add in "PATH"
python              2.XX recommend 2.7
python module:
matplotlib          install by "pip install matplotlib"
opencv              install by "pip install opencv-python"
scipy               install by "pip install scipy"

recommend install "AnaConda" and then you can install all of above tools by "conda install XXXX" (exclude python)
//mafft and bwa can install by "conda install bwa/mafft" (if you install AnaConda before)

=====================ConfigFile: Such as follow====================

//------------------------------required parameters----------------------------
FastqFile = DLO-test.fastq
Chromosome = chr1 chr2 chr3 chr4 chr5 chr6 chr7 chr8 chr9 chr10 chr11 chr12 chr13 chr14 chr15 chr16 chr17 chr18 chr19 chr20 chr21 chr22 chrX chrY
LinkersType = AA
Restriction = T^TAA
LinkerFile = DLO-linker_TP.txt
AlignMinQuality = 20
GenomeFile = Hg19.clean.fna
//------------------------------optional parameters---------------------------
Prefix = DLO-HiC
Index = Hg19
OutPath = /home/hjiang/HiC-test/
AdapterFile = adapter.txt
UseLinker = AA
MatchScore = 1
MisMatchScore = -1
IndelScore = -1
ReadsType = Short
MaxMisMatchLength = 3
AlignThread = 2
Resolution = 1000000
AlignMisMatch = 0
MinReadsLength = 16
MaxReadsLength = 20
Thread = 4
Step = -

=================================================================================

FastqFile           String      Input File with Fastq Format
Chromosome          String      Chromosome name must same as Chromosome name in reference genome
LinkersType         String      The linker's type in raw data
Restriction         String      Sequence of restriction, enzyme cutting site expressed by "^"
LinkerFile          String      A file include linker sequence
AlignMinQuality     Int         Min quality allowed in alignment
GenomeFile          String      Reference genome file
//=================================================================================
Prefix              String      prefix of output    (default    "out")
Index               String      Index prefix of reference genome
OutPath             String      Path of output  (default    "./")
AdapterFile         String      File include adapter sequence    (default    "")
UseLinker           String      linker's type used  (default    =LinkersType)
MatchScore          Int         Match score in linker filter    (default    "1")
MisMatchScore       Int         MisMatch Score in linker filter (default    "-1")
IndelScore          Int         Indel Score in linker filter    (default    "-1")
ReadsType           String      Reads type include ["Short","Long"] (default    "Short")
MaxMisMatchLength   Int         Max misMatch length in linker filter    (default    "3")
AlignThread         Int         Thread in alignment (default    "2")
Resolution          Int         Bin size when creat interaction matrix  (default    "1000000" byte)
AlignMisMatch       Int         MisMatch number in alignment    (default    "0")
MinReadsLength      Int         Min reads length when extract interaction reads (default    "16")
MaxReadsLength      Int         Max reads length when extract interaction reads (default    "20")
Thread              Int         Number of threads    (default    "4")
Step                String      assign  where start and end (default    "-")

//if we set ReadsType "Short", we will align with "bwa aln",and if set "Long",we will align with "bwa mem"

//Step include "LinkerFilter" "DivideLinker" "SeProcess" "Bed2BedPe" "BedPeProcess" "BedPe2Inter" "MakeMatrix"
//If we want to run from "Bed2BedPe" to "MakeMatrix", we can set "Bed2BedPe - MakeMatrix"
//If we only want to run from "SeProcess" to end, we can set "SeProcess -"
//If we want to run all, we can set "-"
