#include <windows.h>
#include <stdio.h>
#include <intrin.h>
#include "zlib.h"
#include "tracebuffer.h"

#define TB_BUFSIZE 8192
struct tb_stream {
	z_stream zs;
	unsigned char outbuf[TB_BUFSIZE]; // first 2 bytes is length; second 2 bytes is thread id
};

#define NUM_THREAD 8192
static struct tb_stream *thread_table[NUM_THREAD];
static HANDLE tracefp;
extern FILE *logfp;

int tb_create (const char *filename)
{
	tracefp = CreateFile(filename, FILE_APPEND_DATA, FILE_SHARE_READ, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
	if (tracefp == INVALID_HANDLE_VALUE) {
		fprintf(logfp, "CreateFile() failed\n");
		fflush(logfp);
		return -1;
	}

	ZeroMemory(thread_table, NUM_THREAD*sizeof(void *));

	return 0;
}

void tb_close (void)
{
	int i;

	for (i = 0; i < NUM_THREAD; i ++) {
		if (thread_table[i] != NULL)
			tb_thread_delete(i);
	}

	CloseHandle(tracefp);
	tracefp = NULL;
}

void tb_thread_create (int tid)
{
	struct tb_stream *stream;

	if (thread_table[tid] != NULL)
		return;

	stream = (struct tb_stream *)malloc(sizeof(struct tb_stream));
	ZeroMemory(&stream->zs, sizeof(z_stream));

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
			DWORD written;
			*(unsigned short *)stream->outbuf = TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out;
			WriteFile(tracefp, stream->outbuf, TB_BUFSIZE - stream->zs.avail_out, &written, NULL);
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

void tb_write (struct event_common *event, int length)
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
			DWORD written;
			*(unsigned short *)stream->outbuf = TB_BUFSIZE - sizeof(short)*2;
			WriteFile(tracefp, stream->outbuf, TB_BUFSIZE, &written, NULL);
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
		int len;

		deflate(&stream->zs, Z_SYNC_FLUSH);
		len = TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out;
		if (len > 0) {
			DWORD written;
			*(unsigned short *)stream->outbuf = len;
			WriteFile(tracefp, stream->outbuf, TB_BUFSIZE - stream->zs.avail_out, &written, NULL);
			fprintf(logfp, "write %d bytes\n", TB_BUFSIZE - sizeof(short)*2 - stream->zs.avail_out);
			stream->zs.next_out = stream->outbuf + sizeof(short)*2;
			stream->zs.avail_out = TB_BUFSIZE - sizeof(short)*2;
		}

		if (len < TB_BUFSIZE - sizeof(short)*2)
			break;
	}
}
