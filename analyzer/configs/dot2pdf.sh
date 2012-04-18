#!/bin/bash

if [ $# -ne 1 ] ; then
	echo usage dot2pdf.sh abc.dot
	exit 1
fi

f=`dirname $1`/`basename $1 .dot`

dot -Tps -o $f.ps $1
ps2pdf $f.ps $f.1.pdf
pdfcrop --margins 10 $f.1.pdf $f.pdf
rm $f.ps $f.1.pdf
