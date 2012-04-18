import java.util.*;
import java.io.*;

public class BasicCallGraph extends AbstractCallGraph
{
	private Hashtable<String, Integer> edges;
	private HashSet<String> nodes;
	private boolean hideUnknown;
	private HashSet<String> ignored_symbols;

	public BasicCallGraph (boolean hideUnknown)
	{
		edges = new Hashtable<String, Integer>();
		nodes = new HashSet<String>();
		this.hideUnknown = hideUnknown;
		ignored_symbols = new HashSet<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader("ignored-symbols.txt"));
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;
				ignored_symbols.add(line);
			}
			in.close();
		} catch (FileNotFoundException x) {
			System.err.println("Warning: ignored-symbols.txt not found.");
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	private boolean isCallHidden (Call call)
	{
		if (!hideUnknown)
			return false;

		if (call.addr == 0)
			return false;
		if (call.image != null && call.image.path.endsWith(".exe"))
			return false;
		if (ignored_symbols.contains(call.name))
			return true;
		return call.name.indexOf(".#") != -1;
	}

	public void addCall (CallContext context, Call newcall)
	{
		if (isCallHidden(newcall))
			return;
		int i = context.stack.size() - 1;
		while (isCallHidden(context.stack.get(i)))
			i --;
		String edge = "\"" + context.stack.get(i).name + "\" -> \"" + newcall.name + "\"";
		if (edges.containsKey(edge))
			edges.put(edge, edges.get(edge) + 1);
		else
			edges.put(edge, 1);
		nodes.add(context.stack.get(i).name);
		nodes.add(newcall.name);
	}

	public void addSysCall (CallContext context, String name)
	{
		int i = context.stack.size() - 1;
		while (isCallHidden(context.stack.get(i)))
			i --;
		String edge = "\"" + context.stack.get(i).name + "\" -> \"" + name + "\"";
		if (edges.containsKey(edge))
			edges.put(edge, edges.get(edge) + 1);
		else
			edges.put(edge, 1);
		nodes.add(context.stack.get(i).name);
		nodes.add(name);
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

		for (Map.Entry<String,Integer> entry : edges.entrySet()) {
			if (entry.getValue() == 1)
				out.println(entry.getKey() + ";");
			else
				out.println(entry.getKey() + "[label=\"" + entry.getValue() + "\"];");
		}

		for (String node : nodes) {
			if (node.startsWith("syscall."))
				out.println("\"" + node + "\" [color=red];");
		}

		out.println("}");
		out.close();
	}
}
