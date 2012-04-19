import java.io.*;

public class Parser
{
	public static class Stream
	{
		public java.util.zip.Inflater inf = new java.util.zip.Inflater();
		public byte [] buffer = new byte [8192];
		public int consumer = 0;
		public int producer = 0;
	}

	private InputStream in;
	private Processor proc;
	private Stream [] streams = new Stream [8192];

	private static int bytes_int16 (byte [] b, int offset)
	{
		return (b[offset] & 0xff) + ((b[offset + 1] & 0xff) << 8);
	}

	private static int bytes_int32 (byte [] b, int offset)
	{
		return (b[offset] & 0xff) + ((b[offset + 1] & 0xff) << 8) + ((b[offset + 2] & 0xff) << 16) + ((b[offset + 3] & 0xff) << 24);
	}

	// string may be null terminated
	private String bytes_string (byte [] b, int offset, int length)
	{
		if (length == 0)
			return "";
		return (b[offset + length - 1] == 0) ? new String(b, offset, length - 1) : new String(b, offset, length);
	}

	private void consume_stream (int tid, Stream stream)
	{
		assert(0 <= stream.consumer);
		assert(stream.consumer <= stream.producer);

outer:
		while (true) {
			if (stream.producer - stream.consumer < 2)
				break outer;
			int type = bytes_int16(stream.buffer, stream.consumer);
			int curptr = stream.consumer + 2;
			switch (type) {
				case 1: {
					if (stream.producer - curptr < 12)
						break outer;
					int target = bytes_int32(stream.buffer, curptr); curptr += 4;
					int retadd = bytes_int32(stream.buffer, curptr); curptr += 4;
					int esp = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_call(tid, target, retadd, esp);
					break;
				}
				case 2: {
					if (stream.producer - curptr < 8)
						break outer;
					int retadd = bytes_int32(stream.buffer, curptr); curptr += 4;
					int esp = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_return(tid, retadd, esp);
					break;
				}
				case 3: {
					if (stream.producer - curptr < 8)
						break outer;
					int standard = bytes_int32(stream.buffer, curptr); curptr += 4;
					int sysnum = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_sysenter(tid, standard, sysnum);
					break;
				}
				case 4: {
					if (stream.producer - curptr < 8)
						break outer;
					int standard = bytes_int32(stream.buffer, curptr); curptr += 4;
					int retval = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_sysexit(tid, standard, retval);
					break;
				}
				case 5: {
					if (stream.producer - curptr < 4)
						break outer;
					int reason = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_ctx(tid, reason);
					break;
				}
				case 6: {
					proc.process_thstart(tid);
					break;
				}
				case 7: {
					proc.process_thterm(tid);
					break;
				}
				case 8: {
					if (stream.producer - curptr < 20)
						break outer;
					int struct_size = bytes_int32(stream.buffer, curptr); curptr += 4;
					if (stream.producer - curptr < struct_size - 8)
						break outer;
					int addr = bytes_int32(stream.buffer, curptr); curptr += 4;
					int size = bytes_int32(stream.buffer, curptr); curptr += 4;
					int entry = bytes_int32(stream.buffer, curptr); curptr += 4;
					boolean ismain = bytes_int32(stream.buffer, curptr) != 0; curptr += 4;
					String name = bytes_string(stream.buffer, curptr, struct_size - 24); curptr += struct_size - 24;
					proc.process_imload(tid, addr, size, entry, ismain, name);
					break;
				}
				case 9: {
					if (stream.producer - curptr < 12)
						break outer;
					int struct_size = bytes_int32(stream.buffer, curptr); curptr += 4;
					if (stream.producer - curptr < struct_size - 8)
						break outer;
					int addr = bytes_int32(stream.buffer, curptr); curptr += 4;
					int size = bytes_int32(stream.buffer, curptr); curptr += 4;
					String name = bytes_string(stream.buffer, curptr, struct_size - 16); curptr += struct_size - 16;
					proc.process_imunload(tid, addr, size, name);
					break;
				}
				case 10: {
					if (stream.producer - curptr < 8)
						break outer;
					int struct_size = bytes_int32(stream.buffer, curptr); curptr += 4;
					if (stream.producer - curptr < struct_size - 8)
						break outer;
					int addr = bytes_int32(stream.buffer, curptr); curptr += 4;
					String name = bytes_string(stream.buffer, curptr, struct_size - 12); curptr += struct_size - 12;
					proc.process_symbol(tid, addr, name);
					break;
				}
				case 11:
				case 12: {
					if (stream.producer - curptr < 16)
						break outer;
					int insaddr = bytes_int32(stream.buffer, curptr); curptr += 4;
					int size = bytes_int32(stream.buffer, curptr); curptr += 4;
					int addr = bytes_int32(stream.buffer, curptr); curptr += 4;
					int value = bytes_int32(stream.buffer, curptr); curptr += 4;
					proc.process_memory(tid, type == 12, insaddr, size, addr, value);
					break;
				}
				default: {
					throw new RuntimeException("unknown event type " + type);
				}
			}
			stream.consumer = curptr;
		}

		assert(0 <= stream.consumer);
		assert(stream.consumer <= stream.producer);

		if (stream.consumer != 0) {
			if (stream.consumer != stream.producer) {
				for (int i = stream.consumer; i < stream.producer; i ++)
					stream.buffer[i - stream.consumer] = stream.buffer[i];
				stream.producer -= stream.consumer;
				stream.consumer = 0;
			} else
				stream.consumer = stream.producer = 0;
		}
	}

	private void parse_chunk (int tid, byte [] chunk, int chunk_length) throws IOException
	{
		Stream stream = streams[tid];
		if (stream == null) {
			stream = new Stream();
			streams[tid] = stream;
		}

		stream.inf.setInput(chunk, 0, chunk_length);
		while (true) {
			int status = 1; // 1:again; 2:needs input; 3:finished
			if (stream.producer < stream.buffer.length) {
				int len = 0;
				try {
					len = stream.inf.inflate(stream.buffer, stream.producer, stream.buffer.length - stream.producer);
				} catch (java.util.zip.DataFormatException x) {
					throw new IOException(x);
				}
				if (len == 0) {
					if (stream.inf.needsInput())
						status = 2;
					else if (stream.inf.finished())
						status = 3;
					else
						throw new RuntimeException("unknown Inflater status");
				}
				stream.producer += len;
			}

			consume_stream(tid, stream);

			if (status == 2)
				break;
			if (status == 3) {
				if (stream.producer != stream.consumer)
					throw new RuntimeException("some bytes left when stream is finished");
				streams[tid] = null;
				break;
			}
		}
	}

	private void parse () throws IOException
	{
		byte [] buffer = new byte [8192];

		while (true) {
			int len = in.read(buffer, 0, 4);
			if (len <= 0)
				break;
			if (len != 4)
				throw new EOFException("parse");
			len = (buffer[0] & 0xff) + (buffer[1] & 0xff) * 256;
			int tid = (buffer[2] & 0xff) + (buffer[3] & 0xff) * 256;

			if (buffer.length < len)
				buffer = new byte [len];
			if (in.read(buffer, 0, len) != len)
				throw new EOFException("parse");
			parse_chunk(tid, buffer, len);
		}
	}

	public static void parse (String filename, Processor proc) throws IOException
	{
		InputStream input = new FileInputStream(filename);
		Parser p = new Parser(input, proc);
		p.parse();
	}

	private Parser (InputStream in, Processor proc)
	{
		this.in = in;
		this.proc = proc;
	}
}
