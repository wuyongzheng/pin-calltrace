#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <pin.H>
#include <gelf.h>
#include "osdep.h"

void osdep_iterate_symbols (IMG img, osdep_process_symbol proc)
{
/*	fprintf(stderr, "load %s off=%08x low=%08x high=%08x start=%08x size=%08x\n",
			IMG_Name(img).c_str(),
			IMG_LoadOffset(img), IMG_LowAddress(img), IMG_HighAddress(img),
			IMG_StartAddress(img), IMG_SizeMapped(img));*/
	ADDRINT loadoff = IMG_LoadOffset(img);

	assert(elf_version(EV_CURRENT) != EV_NONE);
	Elf *elf = elf_memory((char *)IMG_StartAddress(img), IMG_SizeMapped(img));
	assert(elf != NULL);
	assert(elf_kind(elf) == ELF_K_ELF);
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
			if (sym.st_value == 0 || GELF_ST_BIND(sym.st_info) != STT_FUNC)
				continue;
			char *symname = elf_strptr(elf, shdr.sh_link, (size_t)sym.st_name);
			proc(symname, (ADDRINT)sym.st_value);
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
			int index = GELF_R_SYM(rel.r_info); /* seems it's never 0. it's also index into .plt code */
			GElf_Sym sym;
			assert(gelf_getsym(dsymdata, index, &sym) == &sym);
			char *symname = elf_strptr(elf, symstrtab, (size_t)sym.st_name);
			//printf("emmit %p %s@plt\n", (char *)pltaddr + index * 16 + loadoff, symname);
			char buffer[256];
			assert(strlen(symname) + 4 < sizeof(buffer));
			snprintf(buffer, sizeof(buffer), "%s@plt", symname);
			proc(buffer, pltaddr + index * 16 + loadoff); // seems both 32 and 64 ELF have 16 plt call stub.
		}
	}

	elf_end(elf);
}
