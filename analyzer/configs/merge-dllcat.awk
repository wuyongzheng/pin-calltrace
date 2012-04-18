BEGIN {
	FS = "\t";
}

{
	if (!(arr[$1]))
		arr[$1] = $2;
}

END {
	for (i in arr) {
		if (arr[i] == "")
			print i;
		else
			print i "\t" arr[i];
	}
}
