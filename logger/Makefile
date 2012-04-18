PIN_HOME = D:\pin-2.8-msvc10

CFLAGS = /c /MT /EHs- /EHa- /wd4530 /W3
CFLAGS = $(CFLAGS) /DTARGET_WINDOWS /DBIGARRAY_MULTIPLIER=1 /DUSING_XED /D_CRT_SECURE_NO_DEPRECATE /D_SECURE_SCL=0
CFLAGS = $(CFLAGS) /nologo /Gy /O2 /DTARGET_IA32 /DHOST_IA32
CFLAGS = $(CFLAGS) /I$(PIN_HOME)\source\include /I$(PIN_HOME)\source\include\gen /I$(PIN_HOME)\source\tools\InstLib /I$(PIN_HOME)\extras\xed2-ia32\include

LDFLAGS = /DLL /EXPORT:main /NODEFAULTLIB /NOLOGO /INCREMENTAL:NO /OPT:REF
LDFLAGS = $(LDFLAGS) /MACHINE:x86 /ENTRY:Ptrace_DllMainCRTStartup@12 /BASE:0x55000000
LDFLAGS = $(LDFLAGS) /LIBPATH:$(PIN_HOME)\ia32\lib /LIBPATH:$(PIN_HOME)\ia32\lib-ext /LIBPATH:$(PIN_HOME)\extras\xed2-ia32\lib
LIBS = pin.lib libxed.lib libcpmt.lib libcmt.lib pinvm.lib kernel32.lib ntdll-32.lib zlib.lib

calltrace.dll: calltrace.obj tracebuffer.obj
	link $(LDFLAGS) /OUT:calltrace.dll calltrace.obj tracebuffer.obj $(LIBS)

calltrace.obj: calltrace.cpp
	cl $(CFLAGS) calltrace.cpp

tracebuffer.obj: tracebuffer.cpp
	cl $(CFLAGS) tracebuffer.cpp
