import java.util.*;
import java.io.*;

public abstract class AbstractCallGraph
{
	public abstract void addCall (CallContext context, Call newcall);
	public abstract void addSysCall (CallContext context, String name);
}
