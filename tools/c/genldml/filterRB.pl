#!/usr/bin/perl
# Copyright (c) 2001-2003 International Business Machines
# Corporation and others. All Rights Reserved.

####################################################################################
# filterRB.pl:
# This tool filters the ICU resource bundle files and creates output trees
#
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

my $copyright = <<EOS;
// ***************************************************************************
// *
// *   Copyright (C) 2003, International Business Machines
// *   Corporation and others.  All Rights Reserved.
// *
// ***************************************************************************
// THIS IS A MACHINE-GENERATED FILE

EOS

#run the program
main();

#---------------------------------------------------------------------
# The main program

sub main(){
  GetOptions(
           "--srcdir=s"        => \$sourceDir,
           "--destdir=s"       => \$destDir,
           "--src-filename=s"  => \$srcFileName,
           "--package-name=s"  => \$packageName,
           "--resource-name=s" => \$resourceName
           );
  usage() unless defined $sourceDir;
  usage() unless defined $destDir;
  usage() unless defined $srcFileName;
  usage() unless defined $packageName;
  usage() unless defined $resourceName;

  $infile = $sourceDir."/".$srcFileName;
  $inFH = IO::File->new($infile,"r")
            or die  "could not open the file $infile for reading: $! \n";
  ($localeName,$temp) = split(/\./,$srcFileName);
  $separatorChar = "-";
  $outfile = $destDir."/".$packageName.$separatorChar.$srcFileName;

  unlink($outfile);
  $outFH = IO::File->new($outfile,"a")
            or die  "could not open the file $outfile for writing: $! \n";

  while (defined ($line = <$inFH>)){
       if($line =~  /$resourceName/){
           writeResource($inFH, $outFH, $localeName,$line);
       }
  }
  close($inFH);
  close($outFH);
  if( (-s $outfile) == 0 ){
      unlink($outfile);
  }
}

#-----------------------------------------------------------------------
sub writeResource{
    local($inFH, $outFH, $localeName, $topLine) = @_;
    $level = 0;
    if ($topLine =~ /\}/ || $topLine =~ /^\/\//) {
        return;
    }
    print $outFH $copyright;
    print $outFH  $localeName." {\n";
    print $outFH $topLine;
    if ($topLine =~ /\{/) {$level++;}
    while( defined ($line = <$inFH>)){
           if ($line =~ /\{/) {$level++;}
           if ($line =~ /\}/) {$level--;}
           if($level==0){
               print $outFH "    }\n";
               print $outFH "}\n";
               return;
           }
           print $outFH $line;
    }

}
#-----------------------------------------------------------------------
sub usage {
    print << "END";
Usage:
filterRB.pl
Options:
        --srcdir=<directory>
        --destdir=<directory>
        --src-filename=<name of RFC file>
        --package-name=<name of package>
        --resource-name=<name of the resource>


e.g.: filterRB.pl --srcdir=. --destdir=./output --src-filename=root.txt --package-name=Collation --resource-name=CollationElements

filterRB.pl filters the resource bundle file and creates a seperate file for resource tree structuring.

END
  exit(0);
}

