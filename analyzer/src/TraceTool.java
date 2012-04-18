import java.io.*;
import java.util.*;

public class TraceTool
{
	private static void print_help ()
	{
		System.out.println("java TraceTool trace_file tool_name tool_arg ...");
		System.out.println("Tools:");
		System.out.println("java TraceTool trace_file cg");
		System.out.println("java TraceTool trace_file hit [-nopre] [-nopos] [-e] fun1 ...");
		System.out.println("java TraceTool trace_file dimage [-cat catfile] [-ignore ignfile]");
		System.out.println("java TraceTool trace_file image [-cat catfile] [-ignore ignore_expression] [-hit hit_expression] [-nopre] [-nopost] [-noapc] [-nodllinit]");
		System.out.println("java TraceTool trace_file print");
	}

	public static void main (String [] args) throws Exception
	{
		if (args.length < 2) {
			print_help();
			return;
		}

		String trace = args[0];
		String tool = args[1];
		int toolarg = 2;

		if (tool.equals("cg")) {
			BasicCallGraph callgraph = new BasicCallGraph(false);
			Parser.parse(trace, new CGProcessor(callgraph));
			callgraph.writeDOT(trace.replaceAll("\\.[^.]*$", ".dot"));
		} else if (tool.equals("hit")) {
			HashSet<String> funcs = new HashSet<String>();
			boolean regexp = false;
			boolean showpre = true, showpost = true;
			for (int i = toolarg; i < args.length; i ++) {
				if (args[i].equals("-e"))
					regexp = true;
				else if (args[i].equals("-nopre"))
					showpre = false;
				else if (args[i].equals("-nopos"))
					showpost = false;
				else
					funcs.add(args[i]);
			}
			if (funcs.isEmpty()) {
				System.out.println("no functions specified");
				return;
			}
			HittingCallGraph callgraph = new HittingCallGraph(showpre, showpost, regexp, funcs);
			Parser.parse(trace, new CGProcessor(callgraph));
			callgraph.writeDOT(trace.replaceAll("\\.[^.]*$", ".dot"));
		} else if (tool.equals("dimage")) {
			String category = null;
			String ignored = null;
			for (int i = toolarg; i < args.length; i ++) {
				if (args[i].equals("-cat"))
					category = args[++ i];
				else if (args[i].equals("-ignore"))
					ignored = args[++ i];
				else
					throw new RuntimeException("Unknown option " + args[i]);
			}
			DirectImageGraph imagegraph = new DirectImageGraph(category, ignored);
			Parser.parse(trace, imagegraph);
			imagegraph.writeDOT(trace.replaceAll("\\.[^.]*$", ".dot"));
		} else if (tool.equals("image")) {
			String category_file = null;
			String ignore_reg = null;
			String hit_reg = null;
			boolean hitpre = true;
			boolean hitpost = true;
			boolean noapc = false;
			boolean nodllinit = false;
			for (int i = toolarg; i < args.length; i ++) {
				if (args[i].equals("-cat"))
					category_file = args[++ i];
				else if (args[i].equals("-ignore"))
					ignore_reg = args[++ i];
				else if (args[i].equals("-hit"))
					hit_reg = args[++ i];
				else if (args[i].equals("-nopre"))
					hitpre = false;
				else if (args[i].equals("-nopost"))
					hitpost = false;
				else if (args[i].equals("-noapc"))
					noapc = true;
				else if (args[i].equals("-nodllinit"))
					nodllinit = true;
				else
					throw new RuntimeException("Unknown option " + args[i]);
			}
			ImageGraph imagegraph = new ImageGraph(category_file, ignore_reg, hit_reg, hitpre, hitpost, noapc, nodllinit);
			CGProcessor cgp = new CGProcessor(imagegraph);
			Parser.parse(trace, cgp);
			imagegraph.writeDOT(trace.replaceAll("\\.[^.]*$", ".dot"), cgp.images.values());
		} else if (tool.equals("print")) {
			Parser.parse(trace, new PrintProcessor());
		} else if (tool.equals("dumpcall")) {
			Parser.parse(trace, new CGProcessor(new DumpCall()));
		} else {
			print_help();
			return;
		}
	}
}
