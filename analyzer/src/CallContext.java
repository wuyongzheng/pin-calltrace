import java.util.*;

public class CallContext
{
	public static final int NORMAL    = 1;
	public static final int APC       = 2;
	public static final int EXCEPTION = 3;
	public static final int CALLBACK  = 4;

	int type;
	Stack<Call> stack;
	private Hashtable<String,Object> props;

	public CallContext (int type)
	{
		this.type = type;
		stack = new Stack<Call>();
		stack.push(new Call(0, -1, 0, null, "start")); // dummy bottom call
		props = new Hashtable<String,Object>();
	}

	public Object getProperty (String key)
	{
		return props.get(key);
	}

	public void setProperty (String key, Object obj)
	{
		props.put(key, obj);
	}

	public void removeProperty (String key)
	{
		props.remove(key);
	}
}
