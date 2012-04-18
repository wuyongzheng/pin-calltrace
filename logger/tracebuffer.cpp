#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "zlib.h"
#include "osdep.h"
#include "tracebuffer.h"

#define TB_BUFSIZE 8192
struct tb_stream {
	z_stream zs;
	unsigned char outbuf[TB_BUFSIZE]; // first 2 bytes is length; second 2 bytes is thread id
};

#define NUM_THREAD 8192
static struct tb_stream *thread_table[NUM_THREAD];
static osdep_file tracefp;
extern FILE *logfp;

int tb_create (const char *filename)
{
	tracefp = osdep_createappend(filename);
	if (osdep_createfailed(tracefp)) {
		fprintf(logfp, "create trace file failed\n");
		fflush(logfp);
		return -1;
	}

	memset(thread_table, 0, NUM_THREAD*sizeof(void *));

	return 0;
}

void tb_close (void)
{
	int i;

	for (i = 0; i < NUM_THREAD; i ++) {
		if (thread_table[i] != NULL)
			tb_thread_delete(i);
	}

	osdep_close(tracefp);
	tracefp = osdep_invalidhandle;
}

void tb_thread_create (int tid)
{
	struct tb_stream *stream;

	if (thread_table[tid] != NULL)
		return;

	stream = (struct tb_stream *)malloc(sizeof(struct tb_stream));
	memset(&stream->zs, 0, sizeof(z_stream));

	if (deflateInit(&stream->zs, Z_DEFAULT_COMPRESSION) != Z_OK) {
		fprintf(logfp, "Error: deflateInit failed\n");
		fflush(logfp);
		free(stream);
		return;
	}

	stream->zs.next_out = stream->outbuf + sizeof(short)*2;
	stream->zs.avail_out = TB_BUFSIZE - sizeof(short)*2;
	((unsigned short *)stream->outbuf)[1] = tid;

	thread_table[tid] = stream;
}

void tb_thread_delete (int tid)
{
	struct tb_stream *stream = thread_table[tid];

	stream->zs.avail_in = 0; /* should be zero already anyway */
	for (;;) {
		int z_err = deflate(&stream->zs, Z_FINISH);
		if (stream->zs.avail_out < TB_BUFSIZE - sizeof(short)*2) {
			*(unsigned short *)stream->outbuf = TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out;
			osdep_write(tracefp, stream->outbuf, TB_BUFSIZE - stream->zs.avail_out);
			fprintf(logfp, "write %d bytes\n", TB_BUFSIZE - stream->zs.avail_out);
			stream->zs.next_out = stream->outbuf + sizeof(short)*2;
			stream->zs.avail_out = TB_BUFSIZE - sizeof(short)*2;
		}
		if (z_err != Z_OK)
			break;
	}
	deflateEnd(&stream->zs);
	free(stream);
	thread_table[tid] = NULL;
}

void tb_write (struct event_common *event, size_t length)
{
	struct tb_stream *stream = thread_table[event->tid];
	if (stream == NULL) {
		tb_thread_create(event->tid);
		stream = thread_table[event->tid];
	}

	stream->zs.next_in = (Bytef *)&event->type;
	stream->zs.avail_in = length - sizeof(short);

	do {
		if (stream->zs.avail_out == 0) {
			*(unsigned short *)stream->outbuf = TB_BUFSIZE - sizeof(short)*2;
			osdep_write(tracefp, stream->outbuf, TB_BUFSIZE);
			fprintf(logfp, "write %d bytes\n", TB_BUFSIZE);
			stream->zs.next_out = stream->outbuf + sizeof(short)*2;
			stream->zs.avail_out = TB_BUFSIZE - sizeof(short)*2;
		}
		deflate(&stream->zs, Z_NO_FLUSH);
	} while (stream->zs.avail_in != 0);
}

void tb_flush (int tid)
{
	struct tb_stream *stream = thread_table[tid];

	stream->zs.avail_in = 0; /* should be zero already anyway */

	for (;;) {
		size_t len;

		deflate(&stream->zs, Z_SYNC_FLUSH);
		len = TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out;
		if (len > 0) {
			*(unsigned short *)stream->outbuf = len;
			osdep_write(tracefp, stream->outbuf, TB_BUFSIZE - stream->zs.avail_out);
			fprintf(logfp, "write %d bytes\n", TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out);
			stream->zs.next_out = stream->outbuf + sizeof(short)*2;
			stream->zs.avail_out = TB_BUFSIZE - sizeof(short)*2;
		}

		if (len < TB_BUFSIZE - sizeof(short)*2)
			break;
	}
}
