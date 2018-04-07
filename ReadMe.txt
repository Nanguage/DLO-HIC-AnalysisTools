==================DLO-HiC Analysis Tools====================

Usage: java -jar DLO-HIC-AnalysisTools.jar <ConfigFile>
Usage: java -cp DLO-HIC-AnalysisTools.jar <SubClass> <ConfigFile>

==============================================ConfigFile: Such as follow================================================

//------------------------------required parameters----------------------------
FastqFile = DLO-test.fastq
Chromosome = chr1 chr2 chr3 chr4 chr5 chr6 chr7 chr8 chr9 chr10 chr11 chr12 chr13 chr14 chr15 chr16 chr17 chr18 chr19 chr20 chr21 chr22 chrX chrY
LinkersType = AA
Restriction = T^TAA
LinkerFile = DLO-linker_TP.txt
Index = Hg19
AlignMinQuality = 20
GenomeFile = Hg19.clean.fna
//------------------------------optional parameters---------------------------
Prefix = DLO-HiC
OutPath = /home/hjiang/HiC-test/
AdapterFile = adapter.txt
Phred = 33
UseLinker = AA
MatchScore = 1
MisMatchScore = -2
IndelScore = -2
MaxMisMatchLength = 3
AlignThread = 10
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
Index               String      Index prefix of reference genome
AlignMinQuality     Int         Min quality allowed in alignment
GenomeFile          String      Reference genome file
Prefix              String      prefix of output    (default    "out")
OutPath             String      Path of output  (default    "./")
AdapterFile         String      File include adapter sequence    (default    "")
Phred               Int         Quality format in fastq file    (default    "33")
UseLinker           String      linker's type used  (default    =LinkersType)
MatchScore          Int         Match score in linker filter    (default    "1")
MisMatchScore       Int         MisMatch Score in linker filter (default    "-2")
IndelScore          Int         Indel Score in linker filter    (default    "-2")
MaxMisMatchLength   Int         Max misMatch length in linker filter    (default    "3")
AlignThread         Int         Thread in alignment (default    "8")
Resolution          Int         Bin size when creat interaction matrix  (default    "1000000"byte)
AlignMisMatch       Int         MisMatch number in alignment    (default    "0")
MinReadsLength      Int         Min reads length when extract interaction reads (default    "16")
MaxReadsLength      Int         Max reads length when extract interaction reads (default    "20")
Thread              Int         Number of thread    (default    "4")
Step                String      assign  where start and end (default    "-")

//Step include "LinkerFilter" "ClusterLinker" "bin.SeProcess" "Bed2BedPe" "BedPeProcess" "BedPe2Inter" "bin.MakeMatrix"
//If we want to run from "Bed2BedPe" to "bin.MakeMatrix", we can set "Bed2BedPe - bin.MakeMatrix"
//If we only want to run from "bin.SeProcess" to end, we can set "bin.SeProcess -"
//If we want to run all, we can set "-"

=====================================Sub Class=============================================

bin.PreProcess:     usage:      java -cp DLO-HIC-AnalysisTools.jar bin.PreProcess <Config.txt>
                include:    linker filter
bin.SeProcess:      usage:      java -cp DLO-HIC-AnalysisTools.jar bin.SeProcess <Config.txt>
                include:    alignment, sam filter, sam to bed, sort bed file
BedPeProcess:   usage:      java -cp DLO-HIC-AnalysisTools.jar bin.BedpeProcess <Config.txt>
                include:    extract interaction in same and diff chromosome, separate chromosome, find enzyme fragment, separate ligation type, sort file
bin.MakeMatrix:     usage:      java -cp DLO-HIC-AnalysisTools.jar bin.MakeMatrix <Config.txt>
                include:    creat interaction matrix, creat interaction matrix for every chromosome, matrix normalize