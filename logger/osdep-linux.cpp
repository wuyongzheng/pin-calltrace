#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <pin.H>
#include <unordered_set>
#include <gelf.h>
#include <libdwarf/dwarf.h>
#include <libdwarf/libdwarf.h>
#include "osdep.h"

/* return the offset of DW_OP_fbreg based param
 * for other form of loc, return -1 */
static int param_offset (Dwarf_Debug dbg, Dwarf_Die die)
{
	Dwarf_Error error;
	int result, retval;

	Dwarf_Attribute attrloc;
	result = dwarf_attr(die, DW_AT_location, &attrloc, &error);
	assert(result != DW_DLV_ERROR);
	if (result != DW_DLV_OK)
		return -1;

	Dwarf_Half form;
	assert(dwarf_whatform(attrloc, &form, &error) == DW_DLV_OK);

	Dwarf_Signed lcnt;
	Dwarf_Locdesc *llbuf;
	if (form == DW_FORM_exprloc) {
		Dwarf_Unsigned exprlen;
		Dwarf_Ptr blockptr;
		assert(dwarf_formexprloc(attrloc, &exprlen, &blockptr, &error) == DW_DLV_OK);
		assert(dwarf_loclist_from_expr(dbg, blockptr, exprlen, &llbuf, &lcnt, &error) == DW_DLV_OK);
		assert(lcnt == 1);
	} else {
		assert(dwarf_loclist(attrloc, &llbuf, &lcnt, &error) == DW_DLV_OK);
		assert(lcnt == 1);
	}

	if (llbuf->ld_cents == 1 && llbuf->ld_s[0].lr_atom == DW_OP_fbreg)
		retval = llbuf->ld_s[0].lr_number;
	else
		retval = -1;

	dwarf_dealloc(dbg, llbuf->ld_s, DW_DLA_LOC_BLOCK);
	dwarf_dealloc(dbg, llbuf, DW_DLA_LOCDESC);
	dwarf_dealloc(dbg, attrloc, DW_DLA_ATTR);

	return retval;
}

static void process_subprogram (Dwarf_Debug dbg, Dwarf_Die die, ADDRINT loadoff, std::unordered_set<ADDRINT> &added, osdep_process_symbol proc, void *priv)
{
	Dwarf_Error error;
	int result;

	Dwarf_Addr lowpc = 0, highpc = 0;
	if (dwarf_lowpc(die, &lowpc, &error) != DW_DLV_OK ||
			lowpc == 0 ||
			dwarf_highpc(die, &highpc, &error) != DW_DLV_OK ||
			highpc == 0)
		return;
	if (added.find((ADDRINT)lowpc) != added.end())
		return;
	added.insert((ADDRINT)lowpc);

	char *name = NULL;
	result = dwarf_diename(die, &name, &error);
	assert(result != DW_DLV_ERROR);
	if (result == DW_DLV_NO_ENTRY) // we ignore anonymous function
		return;
	assert(result == DW_DLV_OK);
	assert(name != NULL);
	if (name[0] == '\0') {
		dwarf_dealloc(dbg, name, DW_DLA_STRING);
		return;
	}

	Dwarf_Die child;
	int max_offset = -1;
	if (dwarf_child(die, &child, &error) == DW_DLV_OK) {
		while (1) {
			Dwarf_Half tag;
			assert(dwarf_tag(die, &tag, &error) == DW_DLV_OK);
			if (tag == DW_TAG_formal_parameter) {
				int offset = param_offset(dbg, child);
				if (offset > max_offset)
					max_offset = offset;
			}

			Dwarf_Die next;
			int result = dwarf_siblingof(dbg, child, &next, &error);
			dwarf_dealloc(dbg, child, DW_DLA_DIE);
			if (result == DW_DLV_NO_ENTRY)
				break;
			assert(result == DW_DLV_OK);
			child = next;
		}
	}

	//printf("emmit %s %p+%p=%p\n", name, (void*)lowpc, (void*)loadoff, (void *)((unsigned long)lowpc + (unsigned long)loadoff));
	proc(priv, name, lowpc + loadoff);

	dwarf_dealloc(dbg, name, DW_DLA_STRING);
}

static void process_die (Dwarf_Debug dbg, Dwarf_Die die, ADDRINT loadoff, std::unordered_set<ADDRINT> &added, osdep_process_symbol proc, void *priv)
{
	Dwarf_Error error;

	Dwarf_Half tag = 0;
	assert(dwarf_tag(die, &tag, &error) == DW_DLV_OK);
	if (tag == DW_TAG_subprogram)
		process_subprogram(dbg, die, loadoff, added, proc, priv);

	Dwarf_Die child;
	if (dwarf_child(die, &child, &error) == DW_DLV_OK) {
		while (1) {
			process_die(dbg, child, loadoff, added, proc, priv);

			Dwarf_Die next;
			int result = dwarf_siblingof(dbg, child, &next, &error);
			dwarf_dealloc(dbg, child, DW_DLA_DIE);
			if (result == DW_DLV_NO_ENTRY)
				break;
			assert(result == DW_DLV_OK);
			child = next;
		}
	}
}

static void iterate_dwarf_symbols (Dwarf_Debug dbg, ADDRINT loadoff, std::unordered_set<ADDRINT> &added, osdep_process_symbol proc, void *priv)
{
	while (1) {
		Dwarf_Error error;
		Dwarf_Unsigned cu_header_length;
		Dwarf_Half version_stamp;
		Dwarf_Unsigned abbrev_offset;
		Dwarf_Half address_size;
		Dwarf_Unsigned next_cu_header;

		int result = dwarf_next_cu_header(
				dbg, &cu_header_length, &version_stamp,
				&abbrev_offset, &address_size,
				&next_cu_header, &error);
		if (result == DW_DLV_NO_ENTRY)
			break;
		assert(result == DW_DLV_OK);

		Dwarf_Die cu_die = NULL;
		assert(dwarf_siblingof(dbg, NULL, &cu_die, &error) == DW_DLV_OK);
		process_die(dbg, cu_die, loadoff, added, proc, priv);
		dwarf_dealloc(dbg, cu_die, DW_DLA_DIE);
	}
}

static void iterate_elf_symbols (Elf *elf, ADDRINT loadoff, std::unordered_set<ADDRINT> &added, osdep_process_symbol proc, void *priv)
{
	size_t shstrndx;
	{
		// assert(elf_getshdrstrndx(elf , &shstrndx) == 0);
		// let's reimplement elf_getshdrstrndx for backward compatibility
		GElf_Ehdr ehdr;
		assert(gelf_getehdr(elf, &ehdr) == &ehdr);
		shstrndx = ehdr.e_shstrndx;
		assert(shstrndx != SHN_XINDEX);
	}

	/* there's no get_section_by_name, so we have to scan. */
	Elf_Scn *section = NULL, *relplt = NULL, *symtab = NULL;
	while ((section = elf_nextscn(elf, section)) != NULL) {
		GElf_Shdr shdr;
		assert(gelf_getshdr(section , &shdr) == &shdr);
		char *name = elf_strptr(elf, shstrndx, shdr.sh_name);
		if (strcmp(name, ".rel.plt") == 0)
			relplt = section;
		else if (strcmp(name, ".symtab") == 0)
			symtab = section;
	}

	if (symtab != NULL) {
		GElf_Shdr shdr;
		gelf_getshdr(symtab , &shdr);
		Elf_Data *data = elf_getdata(symtab, NULL); assert(data != NULL);

		GElf_Sym sym;
		for (int i = 0; gelf_getsym(data, i, &sym) == &sym; i ++) {
			if (sym.st_value == 0 || GELF_ST_TYPE(sym.st_info) != STT_FUNC)
				continue;
			char *symname = elf_strptr(elf, shdr.sh_link, (size_t)sym.st_name);
			if (added.find((ADDRINT)sym.st_value) == added.end()) { // such a funny way of expressing set.exists()
				proc(priv, symname, (ADDRINT)sym.st_value + loadoff);
				added.insert(sym.st_value);
			}
			/*printf("%04x %02x %02x %04x %08x %s\n",
					sym.st_shndx, sym.st_other, sym.st_info,
					(unsigned int)sym.st_size, (unsigned int)sym.st_value, symname);*/
		}
	}

	if (relplt != NULL) {
		GElf_Shdr shdr;
		assert(gelf_getshdr(relplt , &shdr) == &shdr);

		ADDRINT pltaddr;
		{
			Elf_Scn *plt = elf_getscn(elf, shdr.sh_info); assert(plt != NULL);
			GElf_Shdr shdr1;
			assert(gelf_getshdr(plt , &shdr1) == &shdr1);
			pltaddr = shdr1.sh_addr; assert(pltaddr != 0);
		}

		Elf_Data *dsymdata;
		size_t symstrtab;
		{
			assert(shdr.sh_link != 0);
			Elf_Scn *dsym = elf_getscn(elf, shdr.sh_link); assert(dsym != NULL);
			dsymdata = elf_getdata(dsym, NULL); assert(dsymdata != NULL);
			GElf_Shdr shdr1;
			assert(gelf_getshdr(dsym , &shdr1) == &shdr1);
			symstrtab = shdr1.sh_link;
		}

		Elf_Data *data = elf_getdata(relplt, NULL); assert(data != NULL);

		GElf_Rel rel;
		for (int i = 0; gelf_getrel(data, i, &rel) == &rel; i ++) {
			int index = GELF_R_SYM(rel.r_info); /* seems it's never 0. */
			GElf_Sym sym;
			assert(gelf_getsym(dsymdata, index, &sym) == &sym);
			char *symname = elf_strptr(elf, symstrtab, (size_t)sym.st_name);
			//printf("emmit %p %s@plt\n", (char *)pltaddr + (i+1) * 16 + loadoff, symname);
			char buffer[256];
			assert(strlen(symname) + 4 < sizeof(buffer));
			snprintf(buffer, sizeof(buffer), "%s@plt", symname);
			if (added.find((ADDRINT)(pltaddr + (i+1) * 16)) == added.end()) {
				proc(priv, buffer, pltaddr + (i+1) * 16 + loadoff); // seems both 32 and 64 ELF have 16 plt call stub.
				added.insert(pltaddr + (i+1) * 16);
			}
		}
	}
}

void osdep_iterate_symbols (IMG img, osdep_process_symbol proc, void *priv)
{
	ADDRINT loadoff = IMG_LoadOffset(img);
	assert(elf_version(EV_CURRENT) != EV_NONE);
	std::unordered_set<ADDRINT> added;

	char path[256];
	assert(IMG_Name(img).length() > 0 && IMG_Name(img)[0] == '/');
	snprintf(path, sizeof(path), "/usr/lib/debug%s.debug", IMG_Name(img).c_str());
	int fd = open(path, O_RDONLY);
	if (fd != -1) {
		Elf *elf = elf_begin(fd, ELF_C_READ, NULL);
		assert(elf != NULL);
		assert(elf_kind(elf) == ELF_K_ELF);
		GElf_Ehdr ehdr;
		assert(gelf_getehdr(elf, &ehdr) == &ehdr);

		Dwarf_Debug dbg = 0;
		Dwarf_Error error;
		assert(dwarf_elf_init(elf, DW_DLC_READ, NULL, NULL, &dbg, &error) == DW_DLV_OK);
		iterate_dwarf_symbols(dbg, loadoff + IMG_Entry(img) - ehdr.e_entry, added, proc, priv);
		assert(dwarf_finish(dbg, &error) == DW_DLV_OK);
		elf_end(elf);
		close(fd);
	}

	Elf *elf = elf_memory((char *)IMG_StartAddress(img), IMG_SizeMapped(img));
	assert(elf != NULL);
	assert(elf_kind(elf) == ELF_K_ELF);
	{
		Dwarf_Debug dbg = 0;
		Dwarf_Error error;
		if (dwarf_elf_init(elf, DW_DLC_READ, NULL, NULL, &dbg, &error) == DW_DLV_OK) {
			iterate_dwarf_symbols(dbg, loadoff, added, proc, priv);
			assert(dwarf_finish(dbg, &error) == DW_DLV_OK);
		}
	}
	iterate_elf_symbols(elf, loadoff, added, proc, priv);
	elf_end(elf);
}
