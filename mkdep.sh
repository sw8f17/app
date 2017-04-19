#!/bin/bash
set -e

function clean_up {
	exit
}
trap clean_up SIGHUP SIGINT SIGTERM

this=$(readlink -f "$0")
base=$(dirname "$this")

#Let's start from a known directory
cd "$base"

dep_dir="$base/app/src/main/cpp-dep/"
build_dir="$dep_dir/build/"
outer_install_dir="$dep_dir/install/"

export jniLibs=$(readlink -f "$dep_dir/../jniLibs")

export package_dir=$(readlink -f "$dep_dir")

#Remove the old builds
rm -r "$build_dir"
rm -r "$outer_install_dir"

#Let's make somewhere we can compile it
mkdir "$build_dir"

#We need somewhere to install something
mkdir "$outer_install_dir"

cd "$build_dir"

#Now we are ready to compile
#Remember to create a new directory for every package, and install into the install dir

abis=(
	"armeabi"
	"armeabi-v7a"
	"arm64-v8a"
	"x86"
	"x86_64"
	"mips"
	"mips64"
)

triples=(
	"arm-none-linux-androideabi"
	"armv7-none-linux-androideabi"
	"aarch64-none-linux-androideabi"
	"x86-none-linux-androideabi"
	"x86_64-none-linux-androideabi"
	"mips-none-linux-androideabi"
	"mips64-none-linux-androideabi"
)

toolchains=(
	"arm"
	"arm"
	"arm64"
	"x86"
	"x86_64"
	"mips"
	"mips64"
)

package() {
	file="$1"
	mkdir -p "$jniLibs/$abi/"
	cp -f "$install_dir/$file" "$jniLibs/$abi/"
}

export -f package

#I want to build for everything, so lets do that
steps=${#abis[@]}
for((i = 0; i < steps; i++)); do
	step=$((i+1))

	export abi=${abis[i]}
	export triple=${triples[i]}
	export toolchain=${toolchains[i]}
	export toolchain_dir="$base/toolchain/$toolchain"

	status_line="[$step/$steps] Building for $abi"
	echo -e '\033]2;'$status_line'\007'
	echo "$status_line"
	echo "Triple: $triple"
	echo "Toolchain: $toolchain"

	abi_build_dir="$build_dir/$abi"
	mkdir "$abi_build_dir"
	cd "$abi_build_dir"

	export install_dir="$outer_install_dir/$abi/"
	mkdir "$install_dir"

	while read package; do
		echo ""
		echo "--> $(basename "$package")"
		bash -ex $package
	done < <(find "$base/depbuild.d" -mindepth 1 -type f)
done

#Include dirs are usually shareable, Lets just copy the last one to a shared location
mkdir "$outer_install_dir/shared/"
cp -rf "$install_dir/include/" "$outer_install_dir/shared/include/"
