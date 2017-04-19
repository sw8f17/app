#!/bin/bash

mkdir "mpg123"
cd "mpg123"

$package_dir/mpg123/configure --host=$triple --build=i686-pc-linux-gnu --prefix=$install_dir CC=$toolchain_dir/bin/clang CFLAGS=-DHAVE_MMAP=1 --disable-modules

make

make install

package "lib/libmpg123.so"
