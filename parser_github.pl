#author 4A69616E67
use Getopt::Long;
use Switch;
$usage_str="
usage:perl $0 [option] <infile>
--prefix|-p    out prefix
";
my($infile,$prefix,@project_list,@owner_list,@star_list,@language_list,@contributors_list);
GetOptions("prefix|p=s"=>\$prefix);
if(!$ARGV[0])
{
    print $usage_str;
    exit;
}
$infile=$ARGV[0];
open(IN,$infile);
open(EDGE,">$prefix.edge.csv");
open(NODE,">$prefix.node.csv");
print EDGE "source\ttarget\tweight\n";
print NODE "id\tlabel\tstar_count\tlanguage\n";
%hash=();
%node=();
print "read data ......\n";
while($line=<IN>)
{
    chomp $line;
    @str=split(/\s*:\s*/,$line);
    switch($str[0])
    {
        case "Project"
        {
            push(@project_list,$str[1]);
            next;
        }
        case "Owner"
        {
            push(@owner_list,$str[1]);
            next;
        }
        case "Star"
        {
            push(@star_list,$str[1]);
            next;
        }
        case "Language"
        {
            push(@language_list,$str[1]);
            next;
        }
        case "Contributors"
        {
            push(@contributors_list,$str[1]);
            next;
        }
    }
}
#----------------------------------------------------
print "print node file\n";
for($i=0;$i<scalar(@project_list);$i++)
{
    print NODE $project_list[$i]."\t".$project_list[$i]."\t".$star_list[$i]."\t".$language_list[$i]."\n";
    @str=split(/\s*,\s*/,$contributors_list[$i]);
    foreach $j (@str)
    {
        push(@{$hash{$project_list[$i]}},$j);
    }
}
close(NODE);
#---------------------------------------------------
print "print edge file\n";
@name_list=keys(%hash);
for($i=0;$i<scalar(@name_list)-1;$i++)
{
    %namehash=();
    foreach $number (@{$hash{$name_list[$i]}})
    {
        $namehash{$number}=1;
    }
    for($j=$i+1;$j<scalar(@name_list);$j++)
    {
        $count=0;
        foreach $number (@{$hash{$name_list[$j]}})
        {
            if(exists($namehash{$number}))
            {
                $count++;
            }
        }
        if($count>0)
        {
            print EDGE $name_list[$i]."\t".$name_list[$j]."\t".$count."\n";
        }
    }
}
close(EDGE);