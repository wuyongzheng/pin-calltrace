import java.util.*;
import java.io.*;
import java.util.regex.*;

public class ImageGraph extends AbstractCallGraph
{
	private Hashtable<String, String> category;
	private Pattern ignore_pattern;
	private Pattern hit_pattern;
	private boolean hitpre;
	private boolean hitpost;
	private boolean noapc;
	private boolean nodllinit;
	private Hashtable<String, Integer> edges;
	private HashSet<String> graph_images;
	private HashSet<String> run_images;

	public ImageGraph (String category_file, String ignore_reg, String hit_reg,
			boolean hitpre, boolean hitpost, boolean noapc, boolean nodllinit)
	{
		edges = new Hashtable<String, Integer>();
		graph_images = new HashSet<String>();
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
					if (!st.hasMoreTokens())
						continue;
					path = path.replaceAll(".*\\\\", "").replaceAll("\\....$", "").toLowerCase();
					category.put(path, st.nextToken());
				}
				in.close();
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}

		if (hit_pattern != null && hitpre == false && hitpost == false)
			throw new IllegalArgumentException("hitpre and hitpost can't be noth false.");

		ignore_pattern = ignore_reg == null ? null : Pattern.compile(ignore_reg);
		hit_pattern = hit_reg == null ? null : Pattern.compile(hit_reg);
		this.hitpre = hitpre;
		this.hitpost = hitpost;
		this.noapc = noapc;
		this.nodllinit = nodllinit;
	}

	private boolean show_image (MappedImage image)
	{
		if (image == null)
			return false;
		if (ignore_pattern != null && ignore_pattern.matcher(image.name).matches())
			return false;
		return true;
	}

	private void add_edge (String caller, String callee)
	{
		graph_images.add(caller);
		if (category != null && category.containsKey(caller))
			caller = category.get(caller);

		graph_images.add(callee);
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

	public void addCall (CallContext context, Call newcall)
	{
		if (noapc && context.type == CallContext.APC)
			return;
		if (!show_image(newcall.image))
			return;
		if (nodllinit) { // are we in a dllinit routine?
			if ((!newcall.image.ismain) && newcall.addr == newcall.image.entry)
				return;
			for (int i = context.stack.size() - 1; i >= 0; i --) {
				Call thecall = context.stack.get(i);
				if (thecall.image != null && (!thecall.image.ismain) && thecall.addr == thecall.image.entry)
					return;
			}
		}

		run_images.add(newcall.image.name);

		if (hit_pattern == null) {
			for (int i = context.stack.size() - 1; i >= 0; i --) {
				if (show_image(context.stack.get(i).image)) {
					add_edge(context.stack.get(i).image.name, newcall.image.name);
					break;
				}
			}
		} else { // hit_pattern != null
			if (hitpre && hit_pattern.matcher(newcall.image.name).matches()) {
				MappedImage callee = newcall.image;
				for (int i = context.stack.size() - 1; i >= 0; i --) {
					MappedImage caller = context.stack.get(i).image;
					if (show_image(caller)) {
						add_edge(caller.name, callee.name);
						if (hit_pattern.matcher(caller.name).matches()) // no need to search beyond, because they are added already
							break;
						callee = caller;
					}
				}
			}
			if (hitpost) {
				boolean hit = false;
				for (int i = context.stack.size() - 1; i >= 0; i --) {
					if (context.stack.get(i).image != null && hit_pattern.matcher(context.stack.get(i).image.name).matches()) {
						hit = true;
						break;
					}
				}
				if (hit) {
					for (int i = context.stack.size() - 1; i >= 0; i --) {
						if (show_image(context.stack.get(i).image)) {
							add_edge(context.stack.get(i).image.name, newcall.image.name);
							break;
						}
					}
				}
			}
		}
	}

	public void addSysCall (CallContext context, String name)
	{
	}

	public void writeDOT (String filename, Collection<MappedImage> loaded_images)
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
			for (MappedImage imagex : loaded_images) {
				String image = imagex.name;
				String cat = category.get(image);
				if (cat != null) {
					TreeSet<String> set = catset.get(cat);
					if (set == null) {
						set = new TreeSet<String>();
						catset.put(cat, set);
					}
					set.add(image);
				}
			}
			for (String cat : catset.keySet()) {
				StringBuffer label = new StringBuffer().append("[").append(cat).append("]");
				for (String image : catset.get(cat))
					if (graph_images.contains(image))
						label.append("\\n").append(image);
				for (String image : catset.get(cat))
					if (!graph_images.contains(image) && run_images.contains(image))
						label.append("\\n#").append(image).append("#");
				for (String image : catset.get(cat))
					if (!graph_images.contains(image) && !run_images.contains(image))
						label.append("\\n*").append(image).append("*");
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
}
