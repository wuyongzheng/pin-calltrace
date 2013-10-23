public abstract class Processor
{
	public static final int CONTEXT_CHANGE_REASON_FATALSIGNAL = 0;
	public static final int CONTEXT_CHANGE_REASON_SIGNAL      = 1;
	public static final int CONTEXT_CHANGE_REASON_SIGRETURN   = 2;
	public static final int CONTEXT_CHANGE_REASON_APC         = 3;
	public static final int CONTEXT_CHANGE_REASON_EXCEPTION   = 4;
	public static final int CONTEXT_CHANGE_REASON_CALLBACK    = 5;
	public static final int SYSCALL_STANDARD_INVALID            = 0;
	public static final int SYSCALL_STANDARD_IA32_LINUX         = 1;
	public static final int SYSCALL_STANDARD_IA32E_LINUX        = 2;
	public static final int SYSCALL_STANDARD_IPF_LINUX          = 3;
	public static final int SYSCALL_STANDARD_IA32_MAC           = 4;
	public static final int SYSCALL_STANDARD_IA32E_BSD          = 5;
	public static final int SYSCALL_STANDARD_IA32_WINDOWS_FAST  = 6;
	public static final int SYSCALL_STANDARD_IA32E_WINDOWS_FAST = 7;
	public static final int SYSCALL_STANDARD_IA32_WINDOWS_ALT   = 8;
	public static final int SYSCALL_STANDARD_WOW64              = 9;
	public static final int SYSCALL_STANDARD_WINDOWS_INT        = 10;

	public void start () {}
	public void end () {}

	public void process_call (int tid, int target, int retadd, int esp) {}
	public void process_return (int tid, int retadd, int esp) {}
	public void process_sysenter (int tid, int standard, int sysnum) {}
	public void process_sysexit (int tid, int standard, int retval) {}
	public void process_ctx (int tid, int reason) {}
	public void process_thstart (int tid) {}
	public void process_thterm (int tid) {}
	public void process_imload (int tid, int addr, int size, int entry, boolean ismain, String name) {}
	public void process_imunload (int tid, int addr, int size, String name) {}
	public void process_symbol (int tid, int addr, String name) {}
	public void process_memory (int tid, boolean iswrite, int insaddr, int opcode, int size, int addr, int value) {}
}
