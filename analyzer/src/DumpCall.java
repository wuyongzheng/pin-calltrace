import java.util.*;
import java.io.*;

public class DumpCall extends AbstractCallGraph
{
	public void addCall (CallContext context, Call newcall)
	{
		System.out.println(newcall.name);
	}

	public void addSysCall (CallContext context, String name)
	{
		System.out.println(name);
	}
}
