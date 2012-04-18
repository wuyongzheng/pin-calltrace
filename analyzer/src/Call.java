import java.util.*;

public class Call
{
	int addr;
	int esp;
	int retaddr;
	MappedImage image;
	String name;

	public Call (int addr, int esp, int retaddr, MappedImage image, String name)
	{
		this.addr = addr;
		this.esp = esp;
		this.retaddr = retaddr;
		this.image = image;
		this.name = name;
	}
}
