#!/bin/bash
# set -e

##START OF CONFIG

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

toolchain_prefixes=(
	"arm-linux-androideabi-"
	"arm-linux-androideabi-"
	"aarch64-linux-android-"
	"i686-linux-android-"
	"x86_64-linux-android-"
	"mipsel-linux-android-"
	"mips64el-linux-android-"
)

##END OF CONFIG

get_abi_index() {
	local i
	for((i = 0; i < ${#abis[@]}; i++)); do
		if [ "$1" == "${abis[i]}" ]; then
			echo "$i"
			break
		fi
	done
}

getopt --test > /dev/null
if [[ $? -ne 4 ]]; then
    echo "You have the wrong getopt, getoptta here."
    exit 1
fi

SHORT_OPT=t:
LONG_OPT=target:

# -temporarily store output to be able to check for errors
# -activate advanced mode getopt quoting e.g. via “--options”
# -pass arguments only via   -- "$@"   to separate them correctly
PARSED=$(getopt --options $SHORT --longoptions $LONG --name "$0" -- "$@")
if [[ $? -ne 0 ]]; then
    exit 2
fi
eval set -- "$PARSED"

targets=()
while true; do
    case "$1" in
        -t|--target)
			targets+=("$2")
            shift 2
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "Programming error"
            exit 3
            ;;
    esac
done

stages=()
if [ ${#targets[@]} -eq 0 ]; then
    echo "Compiling everything"
	for((i = 0; i < ${#abis[@]}; i++)); do
		stages+=($i)
	done
else
	for target in ${targets[@]}; do
		i=$(get_abi_index "$target")
		if [ "$i" == "" ]; then
			echo "Target \"$target\" not found"
			exit 1
		fi
		stages+=("$i")
	done
	echo "Compiling selectively"
fi

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

package() {
	file="$1"
	mkdir -p "$jniLibs/$abi/"
	cp -f "$install_dir/$file" "$jniLibs/$abi/"
}

export -f package

#I want to build for everything, so lets do that
steps=${#stages[@]}
for((i = 0; i < steps; i++)); do
	step=$((i+1))
	index=${stages[i]}

	export abi=${abis[index]}
	export triple=${triples[index]}
	export toolchain=${toolchains[index]}
	export toolchain_dir="$base/toolchain/$toolchain"
	export toolchain_prefix=${toolchain_prefixes[index]}

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
