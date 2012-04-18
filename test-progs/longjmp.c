#include <setjmp.h>

jmp_buf buf;

void func2 (void)
{
	longjmp(buf, 1);
}

void func1 (void)
{
	func2();
}

int main (void)
{
	if (!setjmp(buf)) {
		func1();
	}

	return 0;
}
