#/usr/bin/perl

####################################################################################
# txt2ldml.pl:
# This tool invokes genldml and genrb to produce res files from xml files
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
           "--lib=s" => \$envVar,
           "--sourcedir=s" => \$sourceDir,
           "--destdir=s" => \$destDir,
           "--icuroot=s" => \$icuRoot,
           "--genldml=s" => \$genldml,
           "--genrb=s" => \$genrb,
	       "--ldml-only" => \$ldmlOnly
          );
           

usage() unless defined $sourceDir;
usage() unless defined $destDir;
usage() unless defined $icuRoot;
usage() unless defined $genldml;
usage() unless defined $genrb;

# create a temp directory and copy all the txt files there
my $tempDir = $destDir."/temp";
mkpath($tempDir);
my $prefix;
my $tempPackage="LDMLTestPackage";
# set up environment 
if($$^O =~ /win/){
    $prefix ="";
    cmd("set PATH=%PATH%;$icuRoot/bin;");
}else{
    $prefix ="$ldVar=$ICU_ROOT/source/common:$ICU_ROOT/source/i18n:$ICU_ROOT/source/tools/toolutil:$ICU_ROOT/source/data/out:$ICU_ROOT/source/data: "
}

# create list of xml files
my @list;
if (@ARGV) {
    @list = @ARGV;
    foreach (@list) { $_ .= ".txt" unless (/\.txt$/i); }
} else {
    opendir(DIR,$sourceDir);
    @list = grep{/\.txt$/} readdir(DIR);
    closedir(DIR);
}

# now convert
foreach $item (@list){
    next if($item eq "." || $item eq "..");

    resify($item) unless defined $ldmlOnly;

    $txt = $item;;
    $txt =~ s/\.txt$//i;
    ldmlify($txt);
}

# run the xmlify converter
sub ldmlify{
    my $infile = shift;
    my $genldmlExec = $genldml."/genldml";
    cmd("$prefix $genldmlExec --sourcedir $tempDir --destdir $destDir --package $tempPackage $infile");
}

# run genrb 
sub resify{
    my $infile = shift;
    my $genrbExec = $genrb."/genrb";
    cmd("$prefix $genrbExec --sourcedir $sourceDir --package-name $tempPackage --destdir $tempDir --encoding UTF8 $infile");
}

#-----------------------------------------------------------------------
# Execute a command
# Param: Command
# Param: Display line, or '' to display command
sub cmd {
    my $cmd = shift;
    my $prompt = shift;
    $prompt = "Command: $cmd.." unless ($prompt);
    print $prompt."\n";
    system($cmd);
    my $exit_value  = $? >> 8;
    #my $signal_num  = $? & 127;
    #my $dumped_core = $? & 128;
    if ($exit_value == 0) {
        print "ok\n";
    } else {
        ++$errCount;
        print "ERROR ($exit_value)\n";
        exit(1);
    }
}

#-----------------------------------------------------------------------
sub usage {
    print << "END";
Usage:
txt2ldml.pl 
Options:
        --lib=<environment variable for lib path> 
        --sourcedir=<directory> 
        --destdir=<directory>
        --icuroot=<path to ICU's root directory> 
        --genldml=<path to genldml executatble>
        --genrb=<path to genrb executatble>

txt2ldml creates *.xml file from *.txt files by invoking the respective tools
Optionally, one or more locales may be specified on the command line.
If this is done, only those locales will be processed.  If no locales
are listed, all locales are processed.

END
  exit(0);
}
