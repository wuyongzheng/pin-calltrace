public class MappedImage
{
	String path;
	String name;
	int addr;
	int size;
	int entry;
	boolean ismain;

	public MappedImage (String path, int addr, int size, int entry, boolean ismain)
	{
		this.path = path;
		this.addr = addr;
		this.size = size;
		this.entry = entry;
		this.ismain = ismain;
		name = path.replaceAll(".*\\\\", "").replaceAll("\\....$", "").toLowerCase();
	}
}
