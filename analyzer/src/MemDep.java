import java.util.*;
import java.io.*;

public class MemDep implements Processor
{
	private static final boolean debug = false;
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

	private AbstractCallGraph callgraph;
	private ArrayList<Thread> threads;
	public TreeMap<Integer, MappedImage> images; //FIXME
	private TreeMap<Integer, String> symbols;
	private TreeMap<Integer, MemUnit> mem;

	public MemDep (AbstractCallGraph callgraph)
	{
		this.callgraph = callgraph;
		threads = new ArrayList<Thread>();
		images = new TreeMap<Integer, MappedImage>();
		symbols = new TreeMap<Integer, String>();
		mem = new TreeMap<Integer, MemUnit>();
	}

	private Call new_call (int addr, int esp, int retaddr, long tick)
	{
		Map.Entry<Integer,MappedImage> entry = images.floorEntry(addr);
		if (entry == null || entry.getValue().size < addr - entry.getValue().addr)
			return new Call(addr, esp, retaddr, null, "unknown.#" + Integer.toHexString(addr) + "." + tick);
			//return new Call(addr, esp, retaddr, null, "unknown.#" + Integer.toHexString(addr));
		MappedImage image = entry.getValue();

		String sym = symbols.get(addr);
		return new Call(addr, esp, retaddr, image,
				image.name + "." + (sym == null ? "#" + Integer.toHexString(addr - image.addr) : sym) + "." + tick);
				//image.name + "." + (sym == null ? "#" + Integer.toHexString(addr - image.addr) : sym));
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

	public void process_call (int tid, int target, int retaddr, int esp)
	{
		if (debug)
			System.err.printf("cal: %d %08x %08x %08x\n", tid, target, retaddr, esp);

		Thread thread = threads.get(tid);
		CallContext context = thread.contexts.peek();

		/* remove earlier calls if necessary */
		while (unsigned_int_le(context.stack.peek().esp, esp)) {
			Call unmatched = context.stack.pop(); //TODO warn removed call
			System.err.printf("Warning: call %x,%x,%x or %s don't have return\n",
					unmatched.addr, unmatched.esp, unmatched.retaddr,
					addr_to_name(unmatched.addr));
		}

		/* add to call graph */
		Call newcall = new_call(target, esp, retaddr, thread.tick ++);
		callgraph.addCall(context, newcall);

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
				System.err.printf("Warning: call %x,%x,%x or %s don't have return\n",
						unmatched.addr, unmatched.esp, unmatched.retaddr,
						addr_to_name(unmatched.addr));
			} else { /* unmatched return */
				System.err.printf("Warning: return don't have call\n");
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
		else
			callname = "syscall." + standard + "." + sysnum;
		callgraph.addSysCall(context, callname);

		if (standard == SYSCALL_STANDARD_WINDOWS_INT || sysnum == 0x014) {
			// 0x014 means NtCallbackReturn
			context = thread.contexts.pop();
			if (context.type != CallContext.CALLBACK)
				throw new RuntimeException("NtCallbackReturn doesn't match CALLBACK context. standard=" +
						standard + ",sysnum=" + sysnum + ",context.type=" + context.type +
						",tid=" + tid);
		} else if (sysnum == 0x020) {
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
		} else if (sysnum == 0x103) {
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

	public void process_memory (int tid, boolean iswrite, int insaddr, int size, int addr, int value)
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
			mem.put(addr, new MemUnit((short)size, call));
		} else {
			Map.Entry<Integer, MemUnit> next = mem.ceilingEntry(addr);
			if (next != null && next.getKey() - addr < size && call != next.getValue().writer)
				System.out.printf("%s\t%s\t%x\t%d\n", call.name, next.getValue().writer.name, addr, size);
			Map.Entry<Integer, MemUnit> prev = mem.floorEntry(addr - 1);
			if (prev != null && addr - prev.getKey() < prev.getValue().size && call != next.getValue().writer)
				System.out.printf("%s\t%s\t%x\t%d\n", call.name, prev.getValue().writer.name, addr, size);
		}
	}
}
