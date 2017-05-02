#!/bin/bash

androidPath=$1

if [ -z "$androidPath" ]; then
	echo "Usage $0 [path to android SDK]"
	exit 1
fi

if [ ! -d "$androidPath" ] || [ ! -d "$androidPath/ndk-bundle" ]; then
	echo "That was either the wrong SDK directory, or you don't have the ndk installed"
	echo "I recommend that you either give me the right directory, or install the NDK"
	exit 1
fi

myPath=$(readlink -f "$0")
myDir=$(dirname "$myPath")

toolchainDir="$myDir/toolchain"

apiVersion=23

toolchain() {
	arch=$1
	api=$apiVersion
	echo "Making toolchain for $arch, api level $api"
	"$androidPath/ndk-bundle/build/tools/make_standalone_toolchain.py" --unified-headers --arch "$arch" --api "$api" --install-dir "$toolchainDir/$arch"
}


toolchain arm
toolchain arm64
toolchain mips
toolchain mips64
toolchain x86
toolchain x86_64
