# diffdot.awk more.dot base.dot

BEGIN {
}

{
	if (!filename)
		filename = FILENAME;
	else if (filename != FILENAME) {
		filename = FILENAME;
		second = 1;
	}
}

/ -> / {
	edge = gensub("\" \\[.*", "\"", "g");
	src = gensub(" -> .*", "", "g", edge);
	dst = gensub(".* -> ", "", "g", edge);
	if (/style=bold/)
		bidir = 1;
	else
		bidir = 0;

	if (!second) {
		edge1[src " -> " dst] = 1;
		if (bidir)
			edge1[dst " -> " src] = 1;
	} else {
		edge2[src " -> " dst] = 1;
		if (bidir)
			edge2[dst " -> " src] = 1;
	}

	next;
}

/" \[label="/ {
	node = gensub("\" \\[.*", "", "g");
	node = gensub("^\"", "", "g", node);
	if (!second) {
		nodes1[node] = $0;
	} else {
		nodes2[node] = $0;
	}
}

END {
	print "digraph G {";
	print "size=\"7.27,10.69\";";
	print "rankdir=\"LR\";";

	for (edge in edge1) {
		if (!(edge in edge2)) {
			src = gensub(" -> .*", "", "g", edge);
			src = gensub("\"", "", "g", src);
			dst = gensub(".* -> ", "", "g", edge);
			dst = gensub("\"", "", "g", dst);
			usednode[src] = 1;
			usednode[dst] = 1;

			if ((("\"" dst "\" -> \"" src "\"") in edge1) &&
					!(("\"" dst "\" -> \"" src "\"") in edge2)) {
				if (src < dst)
					print edge " [arrowhead=none,style=bold];";
			} else {
				print edge ";";
			}
		}
	}

	for (node in nodes2) {
		str = nodes2[node];
		str = gensub(/"\];/, "", "g", str);
		str = gensub(/.*\]\\n/, "", "g", str);
		split(str, arr, /\\n/);
		for (i = 1; i in arr; i ++)
			dllsinbase[arr[i]] = 1;
	}
	for (node in usednode) {
		if (!(node in nodes1))
			continue;

		str = nodes1[node];
		printf "%s]", gensub(/\]\\n.*/, "", "g", str);

		str = gensub(/"\];/, "", "g", str);
		str = gensub(/.*\]\\n/, "", "g", str);
		split(str, arr, /\\n/);
		for (i = 1; i in arr; i ++) {
			if (arr[i] in dllsinbase)
				printf "\\n%s", arr[i];
			else
				printf "\\n+%s+", arr[i];
		}
		print "\"];"
	}

	print "}";
}
