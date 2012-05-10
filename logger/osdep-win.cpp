#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <pin.H>
#include "osdep.h"

void osdep_iterate_symbols (IMG img, osdep_process_symbol proc)
{
	ADDRINT imaddr = IMG_StartAddress(img);
	for (sym = IMG_RegsymHead(img); SYM_Valid(sym); sym = SYM_Next(sym)) {
		proc(SYM_Name(sym).c_str(), imaddr + SYM_Value(sym));
	}
}
