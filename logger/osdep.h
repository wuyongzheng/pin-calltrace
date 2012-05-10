#ifndef OSDEP_H
#define OSDEP_H

#include <pin.H>

#if defined(WIN32) || defined(_WIN32)

#include <windows.h>

#define PATH_SEPARATOR '\\'
#define osdep_file HANDLE
#define osdep_createappend(filename) CreateFile(filename, FILE_APPEND_DATA, FILE_SHARE_READ, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL)
#define osdep_invalidhandle INVALID_HANDLE_VALUE


#else // ifdef WIN32

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define PATH_SEPARATOR '/'
#define osdep_file int
#define osdep_createappend(filenamr) open((filename), O_WRONLY | O_APPEND | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)
#define osdep_invalidhandle -1
#define osdep_createfailed(handle) ((handle) == -1)
#define osdep_write(handle,p,s) write((handle), (p), (s))
#define osdep_close(handle) close(handle)

#endif // ifdef WIN32

typedef void (*osdep_process_symbol) (char *name, ADDRINT addr);
void osdep_iterate_symbols (IMG img, osdep_process_symbol proc);

#endif // OSDEP_H
