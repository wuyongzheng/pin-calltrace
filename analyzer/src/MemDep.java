import java.util.*;
import java.io.*;

public class MemDep extends Processor
{
	private static final boolean debug = false;
	private static final boolean ignore_callret = true;
	private static final boolean warn_miscall = false;
	private static final String [] winxp_syscall1 = {
		"NtAcceptConnectPort","NtAccessCheck","NtAccessCheckAndAuditAlarm","NtAccessCheckByType",
		"NtAccessCheckByTypeAndAuditAlarm","NtAccessCheckByTypeResultList","NtAccessCheckByTypeResultListAndAuditAlarm","NtAccessCheckByTypeResultListAndAuditAlarmByHandle",
		"NtAddAtom","NtAddBootEntry","NtAdjustGroupsToken","NtAdjustPrivilegesToken",
		"NtAlertResumeThread","NtAlertThread","NtAllocateLocallyUniqueId","NtAllocateUserPhysicalPages",
		"NtAllocateUuids","NtAllocateVirtualMemory","NtAreMappedFilesTheSame","NtAssignProcessToJobObject",
		"NtCallbackReturn","NtCancelDeviceWakeupRequest","NtCancelIoFile","NtCancelTimer",
		"NtClearEvent","NtClose","NtCloseObjectAuditAlarm","NtCompactKeys",
		"NtCompareTokens","NtCompleteConnectPort","NtCompressKey","NtConnectPort",
		"NtContinue","NtCreateDebugObject","NtCreateDirectoryObject","NtCreateEvent",
		"NtCreateEventPair","NtCreateFile","NtCreateIoCompletion","NtCreateJobObject",
		"NtCreateJobSet","NtCreateKey","NtCreateMailslotFile","NtCreateMutant",
		"NtCreateNamedPipeFile","NtCreatePagingFile","NtCreatePort","NtCreateProcess",
		"NtCreateProcessEx","NtCreateProfile","NtCreateSection","NtCreateSemaphore",
		"NtCreateSymbolicLinkObject","NtCreateThread","NtCreateTimer","NtCreateToken",
		"NtCreateWaitablePort","NtDebugActiveProcess","NtDebugContinue","NtDelayExecution",
		"NtDeleteAtom","NtDeleteBootEntry","NtDeleteFile","NtDeleteKey",
		"NtDeleteObjectAuditAlarm","NtDeleteValueKey","NtDeviceIoControlFile","NtDisplayString",
		"NtDuplicateObject","NtDuplicateToken","NtEnumerateBootEntries","NtEnumerateKey",
		"NtEnumerateSystemEnvironmentValuesEx","NtEnumerateValueKey","NtExtendSection","NtFilterToken",
		"NtFindAtom","NtFlushBuffersFile","NtFlushInstructionCache","NtFlushKey",
		"NtFlushVirtualMemory","NtFlushWriteBuffer","NtFreeUserPhysicalPages","NtFreeVirtualMemory",
		"NtFsControlFile","NtGetContextThread","NtGetDevicePowerState","NtGetPlugPlayEvent",
		"NtGetWriteWatch","NtImpersonateAnonymousToken","NtImpersonateClientOfPort","NtImpersonateThread",
		"NtInitializeRegistry","NtInitiatePowerAction","NtIsProcessInJob","NtIsSystemResumeAutomatic",
		"NtListenPort","NtLoadDriver","NtLoadKey","NtLoadKey2",
		"NtLockFile","NtLockProductActivationKeys","NtLockRegistryKey","NtLockVirtualMemory",
		"NtMakePermanentObject","NtMakeTemporaryObject","NtMapUserPhysicalPages","NtMapUserPhysicalPagesScatter",
		"NtMapViewOfSection","NtModifyBootEntry","NtNotifyChangeDirectoryFile","NtNotifyChangeKey",
		"NtNotifyChangeMultipleKeys","NtOpenDirectoryObject","NtOpenEvent","NtOpenEventPair",
		"NtOpenFile","NtOpenIoCompletion","NtOpenJobObject","NtOpenKey",
		"NtOpenMutant","NtOpenObjectAuditAlarm","NtOpenProcess","NtOpenProcessToken",
		"NtOpenProcessTokenEx","NtOpenSection","NtOpenSemaphore","NtOpenSymbolicLinkObject",
		"NtOpenThread","NtOpenThreadToken","NtOpenThreadTokenEx","NtOpenTimer",
		"NtPlugPlayControl","NtPowerInformation","NtPrivilegeCheck","NtPrivilegeObjectAuditAlarm",
		"NtPrivilegedServiceAuditAlarm","NtProtectVirtualMemory","NtPulseEvent","NtQueryAttributesFile",
		"NtQueryBootEntryOrder","NtQueryBootOptions","NtQueryDebugFilterState","NtQueryDefaultLocale",
		"NtQueryDefaultUILanguage","NtQueryDirectoryFile","NtQueryDirectoryObject","NtQueryEaFile",
		"NtQueryEvent","NtQueryFullAttributesFile","NtQueryInformationAtom","NtQueryInformationFile",
		"NtQueryInformationJobObject","NtQueryInformationPort","NtQueryInformationProcess","NtQueryInformationThread",
		"NtQueryInformationToken","NtQueryInstallUILanguage","NtQueryIntervalProfile","NtQueryIoCompletion",
		"NtQueryKey","NtQueryMultipleValueKey","NtQueryMutant","NtQueryObject",
		"NtQueryOpenSubKeys","NtQueryPerformanceCounter","NtQueryQuotaInformationFile","NtQuerySection",
		"NtQuerySecurityObject","NtQuerySemaphore","NtQuerySymbolicLinkObject","NtQuerySystemEnvironmentValue",
		"NtQuerySystemEnvironmentValueEx","NtQuerySystemInformation","NtQuerySystemTime","NtQueryTimer",
		"NtQueryTimerResolution","NtQueryValueKey","NtQueryVirtualMemory","NtQueryVolumeInformationFile",
		"NtQueueApcThread","NtRaiseException","NtRaiseHardError","NtReadFile",
		"NtReadFileScatter","NtReadRequestData","NtReadVirtualMemory","NtRegisterThreadTerminatePort",
		"NtReleaseMutant","NtReleaseSemaphore","NtRemoveIoCompletion","NtRemoveProcessDebug",
		"NtRenameKey","NtReplaceKey","NtReplyPort","NtReplyWaitReceivePort",
		"NtReplyWaitReceivePortEx","NtReplyWaitReplyPort","NtRequestDeviceWakeup","NtRequestPort",
		"NtRequestWaitReplyPort","NtRequestWakeupLatency","NtResetEvent","NtResetWriteWatch",
		"NtRestoreKey","NtResumeProcess","NtResumeThread","NtSaveKey",
		"NtSaveKeyEx","NtSaveMergedKeys","NtSecureConnectPort","NtSetBootEntryOrder",
		"NtSetBootOptions","NtSetContextThread","NtSetDebugFilterState","NtSetDefaultHardErrorPort",
		"NtSetDefaultLocale","NtSetDefaultUILanguage","NtSetEaFile","NtSetEvent",
		"NtSetEventBoostPriority","NtSetHighEventPair","NtSetHighWaitLowEventPair","NtSetInformationDebugObject",
		"NtSetInformationFile","NtSetInformationJobObject","NtSetInformationKey","NtSetInformationObject",
		"NtSetInformationProcess","NtSetInformationThread","NtSetInformationToken","NtSetIntervalProfile",
		"NtSetIoCompletion","NtSetLdtEntries","NtSetLowEventPair","NtSetLowWaitHighEventPair",
		"NtSetQuotaInformationFile","NtSetSecurityObject","NtSetSystemEnvironmentValue","NtSetSystemEnvironmentValueEx",
		"NtSetSystemInformation","NtSetSystemPowerState","NtSetSystemTime","NtSetThreadExecutionState",
		"NtSetTimer","NtSetTimerResolution","NtSetUuidSeed","NtSetValueKey",
		"NtSetVolumeInformationFile","NtShutdownSystem","NtSignalAndWaitForSingleObject","NtStartProfile",
		"NtStopProfile","NtSuspendProcess","NtSuspendThread","NtSystemDebugControl",
		"NtTerminateJobObject","NtTerminateProcess","NtTerminateThread","NtTestAlert",
		"NtTraceEvent","NtTranslateFilePath","NtUnloadDriver","NtUnloadKey",
		"NtUnloadKeyEx","NtUnlockFile","NtUnlockVirtualMemory","NtUnmapViewOfSection",
		"NtVdmControl","NtWaitForDebugEvent","NtWaitForMultipleObjects","NtWaitForSingleObject",
		"NtWaitHighEventPair","NtWaitLowEventPair","NtWriteFile","NtWriteFileGather",
		"NtWriteRequestData","NtWriteVirtualMemory","NtYieldExecution","NtCreateKeyedEvent",
		"NtOpenKeyedEvent","NtReleaseKeyedEvent","NtWaitForKeyedEvent","NtQueryPortInformationProcess"
	};
	private static final String [] linux32_syscall = {
		"syscall_0", "exit", "fork", "read",
		"write", "open", "close", "waitpid",
		"creat", "link", "unlink", "execve",
		"chdir", "time", "mknod", "chmod",
		"lchown", "break", "oldstat", "lseek",
		"getpid", "mount", "umount", "setuid",
		"getuid", "stime", "ptrace", "alarm",
		"oldfstat", "pause", "utime", "stty",
		"gtty", "access", "nice", "ftime",
		"sync", "kill", "rename", "mkdir",
		"rmdir", "dup", "pipe", "times",
		"prof", "brk", "setgid", "getgid",
		"signal", "geteuid", "getegid", "acct",
		"umount2", "lock", "ioctl", "fcntl",
		"mpx", "setpgid", "ulimit", "oldolduname",
		"umask", "chroot", "ustat", "dup2",
		"getppid", "getpgrp", "setsid", "sigaction",
		"sgetmask", "ssetmask", "setreuid", "setregid",
		"sigsuspend", "sigpending", "sethostname", "setrlimit",
		"getrlimit", "getrusage", "gettimeofday", "settimeofday",
		"getgroups", "setgroups", "select", "symlink",
		"oldlstat", "readlink", "uselib", "swapon",
		"reboot", "readdir", "mmap", "munmap",
		"truncate", "ftruncate", "fchmod", "fchown",
		"getpriority", "setpriority", "profil", "statfs",
		"fstatfs", "ioperm", "socketcall", "syslog",
		"setitimer", "getitimer", "stat", "lstat",
		"fstat", "olduname", "iopl", "vhangup",
		"idle", "vm86old", "wait4", "swapoff",
		"sysinfo", "ipc", "fsync", "sigreturn",
		"clone", "setdomainname", "uname", "modify_ldt",
		"adjtimex", "mprotect", "sigprocmask", "create_module",
		"init_module", "delete_module", "get_kernel_syms", "quotactl",
		"getpgid", "fchdir", "bdflush", "sysfs",
		"personality", "afs_syscall", "setfsuid", "setfsgid",
		"_llseek", "getdents", "_newselect", "flock",
		"msync", "readv", "writev", "getsid",
		"fdatasync", "_sysctl", "mlock", "munlock",
		"mlockall", "munlockall", "sched_setparam", "sched_getparam",
		"sched_setscheduler", "sched_getscheduler", "sched_yield", "sched_get_priority_max",
		"sched_get_priority_min", "sched_rr_get_interval", "nanosleep", "mremap",
		"setresuid", "getresuid", "vm86", "query_module",
		"poll", "nfsservctl", "setresgid", "getresgid",
		"prctl", "rt_sigreturn", "rt_sigaction", "rt_sigprocmask",
		"rt_sigpending", "rt_sigtimedwait", "rt_sigqueueinfo", "rt_sigsuspend",
		"pread64", "pwrite64", "chown", "getcwd",
		"capget", "capset", "sigaltstack", "sendfile",
		"getpmsg", "putpmsg", "vfork", "ugetrlimit",
		"mmap2", "truncate64", "ftruncate64", "stat64",
		"lstat64", "fstat64", "lchown32", "getuid32",
		"getgid32", "geteuid32", "getegid32", "setreuid32",
		"setregid32", "getgroups32", "setgroups32", "fchown32",
		"setresuid32", "getresuid32", "setresgid32", "getresgid32",
		"chown32", "setuid32", "setgid32", "setfsuid32",
		"setfsgid32", "pivot_root", "mincore", "madvise",
		"getdents64", "fcntl64", "syscall_222", "syscall_223",
		"gettid", "readahead", "setxattr", "lsetxattr",
		"fsetxattr", "getxattr", "lgetxattr", "fgetxattr",
		"listxattr", "llistxattr", "flistxattr", "removexattr",
		"lremovexattr", "fremovexattr", "tkill", "sendfile64",
		"futex", "sched_setaffinity", "sched_getaffinity", "set_thread_area",
		"get_thread_area", "io_setup", "io_destroy", "io_getevents",
		"io_submit", "io_cancel", "fadvise64", "syscall_251",
		"exit_group", "lookup_dcookie", "epoll_create", "epoll_ctl",
		"epoll_wait", "remap_file_pages", "set_tid_address", "timer_create",
		"timer_settime", "timer_gettime", "timer_getoverrun", "timer_delete",
		"clock_settime", "clock_gettime", "clock_getres", "clock_nanosleep",
		"statfs64", "fstatfs64", "tgkill", "utimes",
		"fadvise64_64", "vserver", "mbind", "get_mempolicy",
		"set_mempolicy", "mq_open", "mq_unlink", "mq_timedsend",
		"mq_timedreceive", "mq_notify", "mq_getsetattr", "kexec_load",
		"waitid", "syscall_285", "add_key", "request_key",
		"keyctl", "ioprio_set", "ioprio_get", "inotify_init",
		"inotify_add_watch", "inotify_rm_watch", "migrate_pages", "openat",
		"mkdirat", "mknodat", "fchownat", "futimesat",
		"fstatat64", "unlinkat", "renameat", "linkat",
		"symlinkat", "readlinkat", "fchmodat", "faccessat",
		"pselect6", "ppoll", "unshare", "set_robust_list",
		"get_robust_list", "splice", "sync_file_range", "tee",
		"vmsplice", "move_pages", "getcpu", "epoll_pwait",
		"utimensat", "signalfd", "timerfd_create", "eventfd",
		"fallocate", "timerfd_settime", "timerfd_gettime", "signalfd4",
		"eventfd2", "epoll_create1", "dup3", "pipe2",
		"inotify_init1", "preadv", "pwritev", "rt_tgsigqueueinfo",
		"perf_event_open", "recvmmsg", "fanotify_init", "fanotify_mark",
		"prlimit64", "name_to_handle_at", "open_by_handle_at", "clock_adjtime",
		"syncfs", "sendmmsg", "setns", "process_vm_readv",
		"process_vm_writev"
	};

	public static boolean unsigned_int_lt (int a, int b)
	{
		return ((long)a + 0x100000000l) % 0x100000000l < ((long)b + 0x100000000l) % 0x100000000l;
	}

	public static boolean unsigned_int_le (int a, int b)
	{
		return ((long)a + 0x100000000l) % 0x100000000l <= ((long)b + 0x100000000l) % 0x100000000l;
	}

	public class Thread
	{
		Stack<CallContext> contexts;
		int testalert;
		long tick;

		public Thread ()
		{
			contexts = new Stack<CallContext>();
			contexts.push(new CallContext(CallContext.NORMAL));
		}
	}

	public static class MemUnit
	{
		public short size;
		public Call writer;
		public MemUnit (short size, Call writer)
		{
			this.size = size;
			this.writer = writer;
		}
	}

	private ArrayList<Thread> threads;
	public TreeMap<Integer, MappedImage> images; //FIXME
	private TreeMap<Integer, String> symbols;
	private TreeMap<Integer, MemUnit> mem;
	private PrintWriter outdep;
	private PrintWriter outcall;

	public MemDep ()
	{
		threads = new ArrayList<Thread>();
		images = new TreeMap<Integer, MappedImage>();
		symbols = new TreeMap<Integer, String>();
		mem = new TreeMap<Integer, MemUnit>();
		try {
			outdep = new PrintWriter("outdep.txt");
			outcall = new PrintWriter("outcall.txt");
		} catch (FileNotFoundException x) {
			throw new RuntimeException(x);
		}
	}

	private Call new_call (int addr, int esp, int retaddr, long tick)
	{
		Map.Entry<Integer,MappedImage> entry = images.floorEntry(addr);
		if (entry == null || entry.getValue().size < addr - entry.getValue().addr)
			//return new Call(addr, esp, retaddr, null, "unknown.#" + Integer.toHexString(addr) + "." + tick);
			return new Call(addr, esp, retaddr, null, "unknown.#" + Integer.toHexString(addr));
		MappedImage image = entry.getValue();

		String sym = symbols.get(addr);
		return new Call(addr, esp, retaddr, image,
				//image.name + "." + (sym == null ? "#" + Integer.toHexString(addr - image.addr) : sym) + "." + tick);
				image.name + "." + (sym == null ? "#" + Integer.toHexString(addr - image.addr) : sym));
	}

	private String addr_to_name (int addr)
	{
		MappedImage img = images.floorEntry(addr).getValue();
		if (img == null || img.size < addr - img.addr)
			return "unknown.#" + Integer.toHexString(addr);

		String sym = symbols.get(addr);
		if (sym == null) {
			return img.name + ".#" + Integer.toHexString(addr - img.addr);
		} else {
			return img.name + "." + sym;
		}
	}

	public void start ()
	{
	}

	public void end ()
	{
		outdep.close();
		outdep = null;
		outcall.close();
		outcall = null;
	}

	public void process_call (int tid, int target, int retaddr, int esp)
	{
		if (debug)
			System.err.printf("cal: %d %08x %08x %08x\n", tid, target, retaddr, esp);

		Thread thread = threads.get(tid);
		CallContext context = thread.contexts.peek();

		/* remove earlier calls if necessary */
		while (unsigned_int_le(context.stack.peek().esp, esp)) {
			Call unmatched = context.stack.pop();
			if (warn_miscall)
				System.err.printf("Warning: call %x,%x,%x or %s doesn't have return\n",
						unmatched.addr, unmatched.esp, unmatched.retaddr,
						addr_to_name(unmatched.addr));
		}

		Call newcall = new_call(target, esp, retaddr, thread.tick ++);
		if (newcall.name.equals("ld-linux._dl_fixup"))
			return;
		outcall.printf("%s\t%s\n", context.stack.peek().name, newcall.name);

		/* append the new esp */
		context.stack.push(newcall);
	}

	public void process_return (int tid, int retaddr, int esp)
	{
		if (debug)
			System.err.printf("ret: %d %08x %08x\n", tid, retaddr, esp);

		Thread thread = threads.get(tid);
		CallContext context = thread.contexts.peek();

		while (true) {
			Call closing = context.stack.peek();
			if (closing.esp == esp) { /* normal call/return convention (warning no retaddr checking) */
				context.stack.pop();
				break;
			} else if (closing.retaddr == retaddr) { /* some special func like alloca, SEH_prolog, SEH_epilog */
				context.stack.pop();
				break;
			} else if (unsigned_int_lt(closing.esp, esp)) { /* unmatched call */
				Call unmatched = context.stack.pop();
				if (warn_miscall)
					System.err.printf("Warning: call %x,%x,%x or %s doesn't have return\n",
							unmatched.addr, unmatched.esp, unmatched.retaddr,
							addr_to_name(unmatched.addr));
			} else { /* unmatched return */
				if (warn_miscall)
					System.err.printf("Warning: return doesn't have call\n");
				break;
			}
		}
	}

	public void process_sysenter (int tid, int standard, int sysnum)
	{
		if (debug)
			System.err.printf("syi: %d %d %x\n", tid, standard, sysnum);

		Thread thread = threads.get(tid);
		CallContext context = thread.contexts.peek();

		/* add to call graph */
		String callname;
		if (standard == SYSCALL_STANDARD_IA32_WINDOWS_FAST && sysnum >= 0 && sysnum < winxp_syscall1.length)
			callname = "syscall." + winxp_syscall1[sysnum];
		else if (standard == SYSCALL_STANDARD_IA32_LINUX && sysnum >= 0 && sysnum < linux32_syscall.length)
			callname = "syscall." + linux32_syscall[sysnum];
		else
			callname = "syscall." + standard + "." + sysnum;

		if (standard == SYSCALL_STANDARD_WINDOWS_INT || (standard == SYSCALL_STANDARD_IA32_WINDOWS_FAST && sysnum == 0x014)) {
			// 0x014 means NtCallbackReturn
			context = thread.contexts.pop();
			if (context.type != CallContext.CALLBACK)
				throw new RuntimeException("NtCallbackReturn doesn't match CALLBACK context. standard=" +
						standard + ",sysnum=" + sysnum + ",context.type=" + context.type +
						",tid=" + tid);
		} else if (standard == SYSCALL_STANDARD_IA32_WINDOWS_FAST && sysnum == 0x020) {
			// 0x020 means NtContinue
			if (thread.testalert > 0)
				thread.testalert --;
			else {
				context = thread.contexts.pop();
				if (context.type != CallContext.EXCEPTION && context.type != CallContext.APC)
					throw new RuntimeException("NtContinue doesn't match EXCEPTION or APC context. standard=" +
							standard + ",sysnum=" + sysnum + ",context.type=" + context.type +
							",tid=" + tid);
			}
		} else if (standard == SYSCALL_STANDARD_IA32_WINDOWS_FAST && sysnum == 0x103) {
			thread.testalert ++;
		}
	}

	public void process_sysexit (int tid, int standard, int retval)
	{
		if (debug)
			System.err.printf("syo: %d %d %x\n", tid, standard, retval);
	}

	public void process_ctx (int tid, int reason)
	{
		if (debug)
			System.err.printf("ctx: %d %d\n", tid, reason);

		Thread thread = threads.get(tid);
		switch (reason) {
		case CONTEXT_CHANGE_REASON_APC:
			thread.contexts.push(new CallContext(CallContext.APC));
			break;
		case CONTEXT_CHANGE_REASON_EXCEPTION:
			thread.contexts.push(new CallContext(CallContext.EXCEPTION));
			break;
		case CONTEXT_CHANGE_REASON_CALLBACK:
			thread.contexts.push(new CallContext(CallContext.CALLBACK));
			break;
		default:
			throw new RuntimeException("unknown ctx reason " + reason);
		}
	}

	public void process_thstart (int tid)
	{
		if (tid < threads.size())
			threads.set(tid, new Thread());
		else {
			threads.ensureCapacity(tid + 1);
			for (int i = threads.size(); i <= tid; i ++)
				threads.add(i, null);
			threads.set(tid, new Thread());
		}
	}

	public void process_thterm (int tid)
	{
		Thread thread = threads.get(tid);

		if (thread.contexts.size() != 1)
			System.err.printf("Warning: thread %d terminates in %d context\n", tid, thread.contexts.peek().type);

		threads.set(tid, null);
	}

	public void process_imload (int tid, int addr, int size, int entry, boolean ismain, String name)
	{
		MappedImage image = new MappedImage(name, addr, size, entry, ismain);
		images.put(addr, image);
	}

	public void process_imunload (int tid, int addr, int size, String name)
	{
		//TODO remove it.
	}

	public void process_symbol (int tid, int addr, String name)
	{
		symbols.put(addr, name);
	}

	public void process_memory (int tid, boolean iswrite, int insaddr, int opcode, int size, int addr, int value)
	{
		Thread thread = threads.get(tid);
		CallContext context = thread.contexts.peek();
		Call call = context.stack.peek();
		if (call == null)
			return;

		if (iswrite) {
			Map.Entry<Integer, MemUnit> next = mem.ceilingEntry(addr);
			if (next != null && next.getKey() - addr < size) {
				 mem.remove(next.getKey());
			}
			Map.Entry<Integer, MemUnit> prev = mem.floorEntry(addr - 1);
			if (prev != null && addr - prev.getKey() < prev.getValue().size) {
				mem.remove(prev.getKey());
			}

			if (ignore_callret && (opcode == PinOpcodeClass32.XED_ICLASS_CALL_FAR.ordinal() || opcode == PinOpcodeClass32.XED_ICLASS_CALL_NEAR.ordinal()))
				return;

			mem.put(addr, new MemUnit((short)size, call));
		} else {
			if (ignore_callret && (opcode == PinOpcodeClass32.XED_ICLASS_RET_FAR.ordinal() || opcode == PinOpcodeClass32.XED_ICLASS_RET_NEAR.ordinal()))
				return;

			Map.Entry<Integer, MemUnit> next = mem.ceilingEntry(addr);
			if (next != null && next.getKey() - addr < size && call != next.getValue().writer)
				outdep.printf("%s\t%s\t%x\t%d\n", call.name, next.getValue().writer.name, addr, size);
			Map.Entry<Integer, MemUnit> prev = mem.floorEntry(addr - 1);
			if (prev != null && addr - prev.getKey() < prev.getValue().size && call != next.getValue().writer)
				outdep.printf("%s\t%s\t%x\t%d\n", call.name, prev.getValue().writer.name, addr, size);
		}
	}
}
