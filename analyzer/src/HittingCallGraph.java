import java.util.*;
import java.io.*;
import java.util.regex.*;

public class HittingCallGraph extends AbstractCallGraph
{
	private HashSet<String> pre_edges;
	private Hashtable<String, Integer> post_edges;
	private boolean showpre, showpost;
	private boolean regexp;
	private Set<String> funcs;
	private ArrayList<Pattern> func_regs;

	public HittingCallGraph (boolean showpre, boolean showpost, boolean regexp, Set<String> funcs)
	{
		if ((!showpre) && !showpost)
			throw new IllegalArgumentException("showpre and showpost can't be noth null.");

		pre_edges = new HashSet<String>();
		post_edges = new Hashtable<String, Integer>();
		this.showpre = showpre;
		this.showpost = showpost;
		this.regexp = regexp;
		this.funcs = funcs;

		if (regexp) {
			func_regs = new ArrayList<Pattern>();
			for (String func : funcs)
				func_regs.add(Pattern.compile(func));
		}
	}

	private boolean hit (String name)
	{
		if (regexp) {
			for (Pattern p : func_regs)
				if (p.matcher(name).matches())
					return true;
			return false;
		} else {
			return funcs.contains(name);
		}
	}

	private void addAllCall (CallContext context, String name)
	{
		if (showpre && hit(name)) {
			pre_edges.add("\"" + context.stack.get(context.stack.size()-1).name + "\" -> \"" + name + "\"");
			for (int i = context.stack.size() - 2; i >= 0; i --)
				pre_edges.add("\"" + context.stack.get(i).name + "\" -> \"" + context.stack.get(i+1).name + "\"");
		}

		if (showpost) {
			for (int i = 0; i < context.stack.size(); i ++) {
				if (hit(context.stack.get(i).name)) {
					String edge = "\"" + context.stack.get(context.stack.size()-1).name + "\" -> \"" + name + "\"";
					if (post_edges.containsKey(edge))
						post_edges.put(edge, post_edges.get(edge) + 1);
					else
						post_edges.put(edge, 1);
					break;
				}
			}
		}
	}

	public void addCall (CallContext context, Call newcall)
	{
		addAllCall(context, newcall.name);
	}

	public void addSysCall (CallContext context, String name)
	{
		addAllCall(context, name);
	}

	public void writeDOT (String filename)
	{
		PrintWriter out = null;
		try {
			out = new PrintWriter(filename);
		} catch (IOException x) {
			throw new RuntimeException(x);
		}

		out.println("digraph G {");
		out.println("size=\"7.27,10.69\";");
		out.println("rankdir=\"LR\";");

		for (String edge : pre_edges){
			out.println(edge + ";");
		}

		for (Map.Entry<String,Integer> entry : post_edges.entrySet()) {
			if (entry.getValue() == 1)
				out.println(entry.getKey() + ";");
			else
				out.println(entry.getKey() + "[label=\"" + entry.getValue() + "\"];");
		}

		out.println("}");
		out.close();
	}
}
