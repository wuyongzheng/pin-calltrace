#ifndef EVENTSTRUCT_H
#define EVENTSTRUCT_H

struct event_common {
	short tid;
	short type;
};

#define ET_CALL 1
struct event_call {
	struct event_common comm;
	unsigned long target;
	unsigned long retadd;
	unsigned long esp;
};

#define ET_RETURN 2
struct event_return {
	struct event_common comm;
	unsigned long retadd;
	unsigned long esp;
};

#define ET_SYSENTER 3
struct event_sysenter {
	struct event_common comm;
	unsigned long standard;
	unsigned long sysnum;
};

#define ET_SYSEXIT 4
struct event_sysexit {
	struct event_common comm;
	unsigned long standard;
	unsigned long retval;
};

#define ET_CTX 5
struct event_ctx {
	struct event_common comm;
	unsigned long reason;
};

#define ET_THSTART 6
struct event_thstart {
	struct event_common comm;
};

#define ET_THTERM 7
struct event_thterm {
	struct event_common comm;
};

#define ET_IMLOAD 8
struct event_imload {
	struct event_common comm;
	int struct_size; // including comm
	unsigned long addr;
	unsigned long size;
	unsigned long entry; // absolute address
	int ismain;
	char name[1]; // null term
};

#define ET_IMUNLOAD 9
struct event_imunload {
	struct event_common comm;
	int struct_size; // including comm
	unsigned long addr;
	unsigned long size;
	char name[1]; // null term
};

#define ET_SYMBOL 10
struct event_symbol {
	struct event_common comm;
	int struct_size; // including comm
	unsigned long addr; // absolute address
	char name[1]; // null term
};

#define ET_MEMLOAD 11
#define ET_MEMSTORE 12
struct event_memory {
	struct event_common comm;
	unsigned long insaddr; // absolute address
	int size;
	unsigned long addr;
	unsigned long value;
};

#endif
