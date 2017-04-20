#!/bin/bash

mkdir "mpg123"
cd "mpg123"

$package_dir/mpg123/configure --host=$triple --build=i686-apple-darwin --prefix=$install_dir CC=$toolchain_dir/bin/clang CFLAGS=-DHAVE_MMAP=1 NM=$toolchain_dir/bin/arm-linux-androideabi-nm AR=$toolchain_dir/bin/arm-linux-androideabi-ar RANLIB=$toolchain_dir/bin/arm-linux-androideabi-ranlib --disable-modules

make

make install

package "lib/libmpg123.so"
