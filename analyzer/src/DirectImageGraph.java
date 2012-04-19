import java.util.*;
import java.io.*;

public class DirectImageGraph implements Processor
{
	private Hashtable<String, Integer> edges;
	private Hashtable<String, String> category;
	private HashSet<String> ignored;
	private TreeMap<Integer, MappedImage> images;
	private HashSet<String> run_images;

	public DirectImageGraph (String category_file, String ignored_file)
	{
		edges = new Hashtable<String, Integer>();
		images = new TreeMap<Integer, MappedImage>();
		run_images = new HashSet<String>();

		if (category_file != null) {
			try {
				category = new Hashtable<String, String>();
				BufferedReader in = new BufferedReader(new FileReader(category_file));
				while (true) {
					String line = in.readLine();
					if (line == null)
						break;
					StringTokenizer st = new StringTokenizer(line, "\t");
					String path = st.nextToken();
					path = path.replaceAll(".*\\\\", "").replaceAll("\\....$", "").toLowerCase();
					category.put(path, st.nextToken());
				}
				in.close();
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}

		if (ignored_file != null) {
			try {
				ignored = new HashSet<String>();
				BufferedReader in = new BufferedReader(new FileReader(ignored_file));
				while (true) {
					String line = in.readLine();
					if (line == null)
						break;
					ignored.add(line);
				}
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
	}

	private MappedImage getImage (int addr)
	{
		Map.Entry<Integer,MappedImage> entry = images.floorEntry(addr);
		if (entry == null || entry.getValue().size < addr - entry.getValue().addr)
			return null;
		else
			return entry.getValue();
	}

	public void process_call (int tid, int target, int retaddr, int esp)
	{
		MappedImage callerimg = getImage(retaddr);
		MappedImage calleeimg = getImage(target);
		if (callerimg == null || calleeimg == null || callerimg == calleeimg)
			return;
		if (ignored != null && (ignored.contains(callerimg.name) || ignored.contains(calleeimg.name)))
			return;

		String caller = callerimg.name;
		run_images.add(caller);
		if (category != null && category.containsKey(caller))
			caller = category.get(caller);
		String callee = calleeimg.name;
		run_images.add(callee);
		if (category != null && category.containsKey(callee))
			callee = category.get(callee);

		if (caller.equals(callee))
			return;

		String edge = caller + "\t" + callee;
		if (edges.containsKey(edge))
			edges.put(edge, edges.get(edge) + 1);
		else
			edges.put(edge, 1);
	}

	public void process_return (int tid, int retaddr, int esp) {}
	public void process_sysenter (int tid, int standard, int sysnum) {}
	public void process_sysexit (int tid, int standard, int retval) {}
	public void process_ctx (int tid, int reason) {}
	public void process_thstart (int tid) {}
	public void process_thterm (int tid) {}

	public void process_imload (int tid, int addr, int size, int entry, boolean ismain, String name)
	{
		MappedImage image = new MappedImage(name, addr, size, entry, ismain);
		images.put(addr, image);
	}

	public void process_imunload (int tid, int addr, int size, String name) {}
	public void process_symbol (int tid, int addr, String name) {}
	public void process_memory (int tid, boolean iswrite, int insaddr, int size, int addr, int value) {}

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

		// remove dual
		HashSet<String> dual = new HashSet<String>();
		for (String edge : edges.keySet()) {
			StringTokenizer st = new StringTokenizer(edge, "\t");
			String caller = st.nextToken();
			String callee = st.nextToken();
			if (edges.containsKey(callee + "\t" + caller) && caller.compareTo(callee) < 0)
				dual.add(caller + "\t" + callee);
		}
		for (String edge : dual) {
			StringTokenizer st = new StringTokenizer(edge, "\t");
			String caller = st.nextToken();
			String callee = st.nextToken();
			edges.put(edge, - edges.get(edge) - edges.get(callee + "\t" + caller));
			edges.remove(callee + "\t" + caller);
		}

		// node labels
		if (category != null) {
			Hashtable<String, TreeSet<String>> catset = new Hashtable<String, TreeSet<String>>();
			for (MappedImage image : images.values()) {
				String cat = category.get(image.name);
				if (cat != null) {
					TreeSet<String> set = catset.get(cat);
					if (set == null) {
						set = new TreeSet<String>();
						catset.put(cat, set);
					}
					set.add(image.name);
				}
			}
			for (String cat : catset.keySet()) {
				StringBuffer label = new StringBuffer().append("[").append(cat).append("]");
				for (String image : catset.get(cat))
					if (run_images.contains(image))
						label.append("\\n").append(image);
				for (String image : catset.get(cat))
					if (!run_images.contains(image))
						label.append("\\n*").append(image).append('*');
				out.println("\"" + cat + "\" [label=\"" + label + "\"];");
			}
		}

		for (Map.Entry<String,Integer> entry : edges.entrySet()) {
			StringTokenizer st = new StringTokenizer(entry.getKey(), "\t");
			String caller = st.nextToken();
			String callee = st.nextToken();
			if (entry.getValue() < 0)
				out.println("\"" + caller + "\" -> \"" + callee + "\" [arrowhead=none,style=bold,label=\"" + (-entry.getValue()) + "\"];");
			else
				out.println("\"" + caller + "\" -> \"" + callee + "\" [label=\"" + entry.getValue() + "\"];");
		}

		out.println("}");
		out.close();
	}

	public boolean isEmpty ()
	{
		return edges.isEmpty();
	}
}
