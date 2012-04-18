#include <stdio.h>
#include <stdlib.h>

int func (void)
{
	return 0;
}

int execute (void *f)
{
	return ((int (*)(void))f)();
}

int access (void *ptr)
{
	return *(int *)ptr;
}

int divide (int a, int b)
{
	return a/b;
}

void help (void)
{
	printf("usage: exception [catch|nocatch] [good|fault] [div|read|execute]\n");
	exit(1);
}

int main (int argc, char *argv[])
{
	if (argc != 4)
		help();

	if (argv[1][0] == 'c') {
		__try {
			if (argv[2][0] == 'g') {
				if (argv[3][0] == 'd')
					divide(4,2);
				else if (argv[3][0] == 'r')
					access(&argc);
				else
					execute(func);
			} else {
				if (argv[3][0] == 'd')
					divide(2,0);
				else if (argv[3][0] == 'r')
					access(NULL);
				else
					execute(argv);
			}
		} __except (1) {
			printf("thrown\n");
		}
	} else {
			if (argv[2][0] == 'g') {
				if (argv[3][0] == 'd')
					divide(4,2);
				else if (argv[3][0] == 'r')
					access(&argc);
				else
					execute(func);
			} else {
				if (argv[3][0] == 'd')
					divide(2,0);
				else if (argv[3][0] == 'r')
					access(NULL);
				else
					execute(argv);
			}
	}

	return 0;
}
