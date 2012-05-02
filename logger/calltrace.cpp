#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <pin.H>
#include "tracebuffer.h"
#include "eventstruct.h"

int pid;
FILE *logfp;

void before_return (THREADID tid, ADDRINT esp)
{
	struct event_return event;
	event.comm.type = ET_RETURN;
	event.comm.tid = tid;
	event.retadd = *(unsigned int *)esp;
	event.esp = esp;
	tb_write((event_common *)&event, sizeof(event));
}

void before_call (THREADID tid, ADDRINT esp, ADDRINT nextins, ADDRINT target)
{
	struct event_call event;
	event.comm.type = ET_CALL;
	event.comm.tid = tid;
	event.target = target;
	event.retadd = nextins;
	event.esp = esp - sizeof(ADDRINT);
	tb_write((event_common *)&event, sizeof(event));
}

void trace (TRACE trace, void *v)
{
	for (BBL bbl = TRACE_BblHead(trace); BBL_Valid(bbl); bbl = BBL_Next(bbl)) {
		INS tail = BBL_InsTail(bbl);
		if (INS_IsRet(tail)) {
			INS_InsertCall(tail, IPOINT_BEFORE, AFUNPTR(before_return),
					IARG_THREAD_ID, IARG_REG_VALUE, REG_ESP, IARG_END);
		} else if (INS_IsCall(tail)) {
			INS_InsertCall(tail, IPOINT_BEFORE, AFUNPTR(before_call),
					IARG_THREAD_ID, IARG_REG_VALUE, REG_ESP, IARG_ADDRINT, INS_NextAddress(tail), IARG_BRANCH_TARGET_ADDR, IARG_END);
		}
	}
}

void on_load (THREADID tid, ADDRINT insaddr, ADDRINT addr, ADDRINT size)
{
	struct event_memory event;
	event.comm.type = ET_MEMLOAD;
	event.comm.tid = tid;
	event.insaddr = insaddr;
	event.addr = addr;
	event.size = size;
	switch (size) {
		case 1: event.value = *(unsigned char *)addr; break;
		case 2: event.value = *(unsigned short *)addr; break;
		case 4: event.value = *(unsigned long *)addr; break;
		default: event.value = 0;
	}
	tb_write((event_common *)&event, sizeof(event));
}

void on_store (THREADID tid, ADDRINT insaddr, ADDRINT addr, ADDRINT size)
{
	struct event_memory event;
	event.comm.type = ET_MEMSTORE;
	event.comm.tid = tid;
	event.insaddr = insaddr;
	event.addr = addr;
	event.size = size;
	event.value = 0;
	tb_write((event_common *)&event, sizeof(event));
}

void instruction (INS ins, void *v)
{
	if (INS_IsMemoryRead(ins)) {
		INS_InsertPredicatedCall(ins, IPOINT_BEFORE, AFUNPTR(on_load),
				IARG_THREAD_ID, IARG_INST_PTR,
				IARG_MEMORYREAD_EA, IARG_MEMORYREAD_SIZE,
				IARG_END);
	}
	if (INS_HasMemoryRead2(ins)) {
		INS_InsertPredicatedCall(ins, IPOINT_BEFORE, AFUNPTR(on_load),
				IARG_THREAD_ID, IARG_INST_PTR,
				IARG_MEMORYREAD2_EA, IARG_MEMORYREAD_SIZE,
				IARG_END);
	}
	if (INS_IsMemoryWrite(ins)) { //TODO get value. see SimpleExamples/pinatrace.cpp
		INS_InsertPredicatedCall(ins, IPOINT_BEFORE, AFUNPTR(on_store),
				IARG_THREAD_ID, IARG_INST_PTR,
				IARG_MEMORYWRITE_EA, IARG_MEMORYWRITE_SIZE,
				IARG_END);
	}
}

void thread_start (THREADID tid, CONTEXT *ctxt, INT32 flags, void *v)
{
	struct event_thstart event;

	tb_thread_create(tid);

	event.comm.type = ET_THSTART;
	event.comm.tid = tid;
	tb_write((event_common *)&event, sizeof(event));

	fprintf(logfp, "thread-start %d\n", tid);
}

void thread_fini (THREADID tid, const CONTEXT *ctxt, INT32 code, void *v)
{
	struct event_thterm event;
	event.comm.type = ET_THTERM;
	event.comm.tid = tid;
	tb_write((event_common *)&event, sizeof(event));

	tb_thread_delete(tid);

	fprintf(logfp, "thread-term %d\n", tid);
}

void sys_enter (THREADID tid, CONTEXT *ctxt, SYSCALL_STANDARD std, void *v)
{
	struct event_sysenter event;
	event.comm.type = ET_SYSENTER;
	event.comm.tid = tid;
	event.standard = std;
	event.sysnum = PIN_GetSyscallNumber(ctxt, std);
	tb_write((event_common *)&event, sizeof(event));
}

void sys_exit (THREADID tid, CONTEXT *ctxt, SYSCALL_STANDARD std, void *v)
{
	struct event_sysexit event;
	event.comm.type = ET_SYSEXIT;
	event.comm.tid = tid;
	event.standard = std;
	event.retval = PIN_GetSyscallReturn(ctxt, std);
	tb_write((event_common *)&event, sizeof(event));
}

void context_switch (THREADID tid, CONTEXT_CHANGE_REASON reason, const CONTEXT *from, CONTEXT *to, INT32 info, VOID *v)
{
	struct event_ctx event;
	event.comm.type = ET_CTX;
	event.comm.tid = tid;
	event.reason = reason;
	tb_write((event_common *)&event, sizeof(event));
}

void fini (INT32 code, void *v)
{
	tb_close();
	fprintf(logfp, "prog-term %d\n", code);
	fclose(logfp);
}

void img_load (IMG img, void *v)
{
	SYM sym;
	struct event_imload *imevent;
	struct event_symbol *sbevent;
	char buffer[512];
	size_t length;

	imevent = (struct event_imload *)buffer;
	length = IMG_Name(img).length();
	if (length > sizeof(buffer) - sizeof(struct event_imload))
		length = sizeof(buffer) - sizeof(struct event_imload);
	imevent->comm.type = ET_IMLOAD;
	imevent->comm.tid = PIN_ThreadId();
	imevent->struct_size = (int)((char *)imevent->name - (char *)imevent) + length + 1;
	imevent->addr = IMG_LowAddress(img);
	imevent->size = IMG_HighAddress(img) - IMG_LowAddress(img);
	imevent->entry = IMG_Entry(img);
	imevent->ismain = IMG_IsMainExecutable(img);
	memcpy(imevent->name, IMG_Name(img).c_str(), length);
	imevent->name[length] = '\0';
	tb_write((event_common *)imevent, (size_t)imevent->struct_size);

	sbevent = (struct event_symbol *)buffer;
	sbevent->comm.type = ET_SYMBOL;
	sbevent->comm.tid = PIN_ThreadId();
	for (sym = IMG_RegsymHead(img); SYM_Valid(sym); sym = SYM_Next(sym)) {
		length = SYM_Name(sym).length();
		if (length > sizeof(buffer) - sizeof(struct event_symbol))
			length = sizeof(buffer) - sizeof(struct event_symbol);
		sbevent->struct_size = (int)((char *)sbevent->name - (char *)sbevent) + length + 1;
		sbevent->addr = SYM_Address(sym);
		memcpy(sbevent->name, SYM_Name(sym).c_str(), length);
		sbevent->name[length] = '\0';
		tb_write((event_common *)sbevent, (size_t)sbevent->struct_size);
	}
	tb_flush(PIN_ThreadId());

	fprintf(logfp, "img+ %08x+%08x %s\n", IMG_StartAddress(img), IMG_SizeMapped(img), IMG_Name(img).c_str());

//	for (sym = IMG_RegsymHead(img); SYM_Valid(sym); sym = SYM_Next(sym)) {
//		fprintf(logfp, "sym %08x %d %s\n", SYM_Value(sym), SYM_Dynamic(sym), SYM_Name(sym).c_str());
//	}
	for (SEC sec = IMG_SecHead(img); SEC_Valid(sec); sec = SEC_Next(sec)) {
		for (RTN rtn = SEC_RtnHead(sec); RTN_Valid(rtn); rtn = RTN_Next(rtn)) {
			sym = RTN_Sym(rtn);
			if (SYM_Valid(sym))
				fprintf(logfp, "sym %08x %d %s\n", SYM_Value(sym), SYM_Dynamic(sym), SYM_Name(sym).c_str());
		}
	}
}

void img_unload (IMG img, void *v)
{
	char buff[256];
	struct event_imunload *event = (struct event_imunload *)buff;
	int name_len;

	event->comm.type = ET_IMUNLOAD;
	event->comm.tid = PIN_ThreadId();
	event->addr = IMG_StartAddress(img);
	event->size = IMG_SizeMapped(img);
	name_len = IMG_Name(img).length();
	if (name_len > 240)
		name_len = 240;
	event->struct_size = (int)((char *)event->name - (char *)event) + name_len + 1;
	strncpy(event->name, IMG_Name(img).c_str(), name_len);
	event->name[name_len] = '\0';
	tb_write((event_common *)event, (size_t)event->struct_size);

	fprintf(logfp, "img- %08x+%08x %s\n", IMG_StartAddress(img), IMG_SizeMapped(img), IMG_Name(img).c_str());
}

int main (int argc, char *argv[])
{
	char buff[20];

	if(PIN_Init(argc,argv)) {
		printf("command line error\n");
		return 1;
	}

	pid = PIN_GetPid();

	sprintf(buff, "ct%d.log", pid);
	logfp = fopen(buff, "w");
	if (logfp == NULL) {
		printf("cannot open '%s' for writing\n", buff);
		return 1;
	}

	sprintf(buff, "ct%d.trace", pid);
	if (tb_create(buff)) {
		fprintf(logfp, "tb_create() failed\n");
		fflush(logfp);
		return 1;
	}

	PIN_AddFiniFunction(fini, 0);
	PIN_AddThreadStartFunction(thread_start, 0);
	PIN_AddThreadFiniFunction(thread_fini, 0);
	IMG_AddInstrumentFunction(img_load, NULL);
	IMG_AddUnloadFunction(img_unload, NULL);
	TRACE_AddInstrumentFunction(trace, NULL);
	INS_AddInstrumentFunction(instruction, NULL);
	PIN_AddSyscallEntryFunction(sys_enter, NULL);
	PIN_AddSyscallExitFunction(sys_exit, NULL);
	PIN_AddContextChangeFunction(context_switch, NULL);

	PIN_InitSymbols();

	PIN_StartProgram(); // Never returns
	return 0;
}
