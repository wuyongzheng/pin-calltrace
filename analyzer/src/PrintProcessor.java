public class PrintProcessor extends Processor
{
	public void process_call (int tid, int target, int retadd, int esp)
	{
		System.out.printf("cal: %d %08x %08x %08x\n", tid, target, retadd, esp);
	}

	public void process_return (int tid, int retadd, int esp)
	{
		System.out.printf("ret: %d %08x %08x\n", tid, retadd, esp);
	}

	public void process_sysenter (int tid, int standard, int sysnum)
	{
		System.out.printf("syi: %d %d %x\n", tid, standard, sysnum);
	}

	public void process_sysexit (int tid, int standard, int retval)
	{
		System.out.printf("syo: %d %d %x\n", tid, standard, retval);
	}

	public void process_ctx (int tid, int reason)
	{
		System.out.printf("ctx: %d %d\n", tid, reason);
	}

	public void process_thstart (int tid)
	{
		System.out.printf("ths: %d\n", tid);
	}

	public void process_thterm (int tid)
	{
		System.out.printf("tht: %d\n", tid);
	}

	public void process_imload (int tid, int addr, int size, int entry, boolean ismain, String name)
	{
		System.out.printf("iml: %d %08x %08x %08x %d \"%s\"\n", tid, addr, size, entry, ismain ? 1 : 0, name);
	}

	public void process_imunload (int tid, int addr, int size, String name)
	{
		System.out.printf("imu: %d %08x %08x \"%s\"\n", tid, addr, size, name);
	}

	public void process_symbol (int tid, int addr, String name)
	{
		System.out.printf("sym: %d %08x \"%s\"\n", tid, addr, name);
	}

	public void process_memory (int tid, boolean iswrite, int insaddr, int opcode, int size, int addr, int value)
	{
		System.out.printf("%s: %d %08x %08x %08x %08x %08x\n",
				iswrite ? "sto" : "lod", tid, insaddr, opcode, size, addr, value);
	}
}
