#ifndef RINGBUFFER_H
#define RINGBUFFER_H

#include "eventstruct.h"

int tb_create (const char *filename);
void tb_close (void);
void tb_thread_create (int tid);
void tb_thread_delete (int tid);
void tb_write (struct event_common *event, int length);
void tb_flush (int tid);

#endif
