#!/usr/bin/perl

####################################################################################
# genindex.pl:
# Reads the source directory and creates an index.html with the link
# Author: Ram Viswanadha
#
####################################################################################
use File::Find;
use File::Basename;
use IO::File;
use Cwd;
use File::Copy;
use Getopt::Long;
use File::Path;
use File::Copy;

GetOptions(
           "--sourcedir=s" => \$sourceDir,
           "--destdir=s" => \$destDir,
           "--href=s" => \$href
          );
           

usage() unless defined $sourceDir;
usage() unless defined $destDir;
usage() unless defined $href;


# create list of html files
my @list;
if (@ARGV) {
    @list = @ARGV;
    foreach (@list) { $_ .= ".html" unless (/\.html$/i); }
} else {
    opendir(DIR,$sourceDir);
    @list = grep{/\.html$/} readdir(DIR);
    closedir(DIR);
}
$outfile = $destDir."/index.html";
$outFH = IO::File->new($outfile,"w")
            or die  "could not open the file $outfile for writing: $! \n";
print $outFH "<html>\n";
print $outFH "\t<head>Index of all comparison charts</head>\n";
print $outFH "\t<body bgcolor=\"#FFFFFF\">\n";
print $outFH "\t\t<ul>\n";
# now convert
foreach $item (@list){
    next if($item eq "." || $item eq ".." || $item eq "index.html");
    ($s1, $s2) = split(/\./,$item);
    #grab the english translation
    $inFH = IO::File->new($item,"r")
            or die  "could not open the file $outfile for reading: $! \n";
    $fullName="";
    while(defined ($line=<$inFH>)){
          if($line =~ /.*?(\(.*\).*Cover Page.*)/){
            $line =~ /.*?(\(.*\))/;
            $fullName =  $1;
            if($fullName eq ""){
                print $line."\n";
            }
            break;
          }
    }

    print $outFH "\t\t\t<li><a href=\"$href/$item\">$s1</a> $fullName</li>\n";
    close($inFH);
}

print $outFH "\t\t</ul>\n";
print $outFH "\t</body>\n";
print $outFH "</html>\n";
close($outFH);

#-----------------------------------------------------------------------
sub usage {
    print << "END";
Usage:
txt2ldml.pl
Options:
        --sourcedir=<directory>
        --destdir=<directory>
        --href=<The URL to be prepended>

END
  exit(0);
}
