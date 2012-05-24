#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <pin.H>
#include <unordered_set>
#include <gelf.h>
#include "osdep.h"

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
		iterate_elf_symbols(elf, loadoff + IMG_Entry(img) - ehdr.e_entry, added, proc, priv);
		elf_end(elf);
		close(fd);
	}

	Elf *elf = elf_memory((char *)IMG_StartAddress(img), IMG_SizeMapped(img));
	assert(elf != NULL);
	assert(elf_kind(elf) == ELF_K_ELF);
	iterate_elf_symbols(elf, loadoff, added, proc, priv);
	elf_end(elf);
}
