#!/bin/bash

mkdir "mpg123"
cd "mpg123"

$package_dir/mpg123/configure \
	--host=$triple \
	--prefix=$install_dir \
	CC=$toolchain_dir/bin/clang \
	CFLAGS=-DHAVE_MMAP=1 \
	NM=$toolchain_dir/bin/${toolchain_prefix}nm \
	AR=$toolchain_dir/bin/${toolchain_prefix}ar \
	RANLIB=$toolchain_dir/bin/${toolchain_prefix}ranlib \
	--disable-modules

make

make install

package "lib/libmpg123.so"
