# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(TOOLCHAIN_DIR "${CMAKE_CURRENT_LIST_DIR}/../toolchain/${ANDROID_ARCH_NAME}")

list(APPEND CMAKE_ARGS "-DANDROID_TOOLCHAIN_NAME=${ANDROID_TOOLCHAIN_NAME}")
list(APPEND CMAKE_ARGS "-DANDROID_ABI=${ANDROID_ABI}")
list(APPEND CMAKE_ARGS "-DANDROID_NDK=${ANDROID_NDK}")
list(APPEND CMAKE_ARGS "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
list(APPEND CMAKE_ARGS "-DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}")
list(APPEND CMAKE_ARGS "-DANDROID_STANDALONE_TOOLCHAIN=${TOOLCHAIN_DIR}")
list(APPEND CMAKE_ARGS "-DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}")

MESSAGE( STATUS "CMAKE_ARGS: " ${CMAKE_ARGS})
MESSAGE( STATUS "CMAKE_SYSROOT: " ${CMAKE_SYSROOT})
MESSAGE( STATUS "CMAKE_PREFIX_PATH: " ${CMAKE_SYSTEM_LIBRARY_PATH})

include(ExternalProject)

set(MY_CC "${TOOLCHAIN_DIR}/bin/clang")

set(C_EXTRA_FLAGS "-v -DHAVE_MMAP=1")
set(LINKER_EXTRA_FLAGS "")
set(CONFIGURE_EXTRA_FLAGS "")

MESSAGE( STATUS "TOOLCHAIN_DIR: " ${TOOLCHAIN_DIR} )
MESSAGE( STATUS "ANDROID_ABI: " ${ANDROID_ABI} )

string(REGEX REPLACE -Werror=[a-z0-9A-Z-]* "" CMAKE_SAFE_C_FLAGS ${CMAKE_C_FLAGS})
#string(REPLACE " -mips32" "" CMAKE_SAFE_C_FLAGS ${CMAKE_SAFE_C_FLAGS})
#string(REPLACE " -march=armv7-a" "" CMAKE_SAFE_C_FLAGS ${CMAKE_SAFE_C_FLAGS})
#string(REPLACE " -march=armv5te" "" CMAKE_SAFE_C_FLAGS ${CMAKE_SAFE_C_FLAGS})
#string(REPLACE " -mfloat-abi=soft" "" CMAKE_SAFE_C_FLAGS ${CMAKE_SAFE_C_FLAGS})

MESSAGE( STATUS "CMAKE_SAFE_C_FLAGS: " ${CMAKE_SAFE_C_FLAGS} )

if(ANDROID_ABI STREQUAL "x86")
    string(CONCAT C_EXTRA_FLAGS ${C_EXTRA_FLAGS} " -m32")
    string(CONCAT LINKER_EXTRA_FLAGS ${LINKER_EXTRA_FLAGS} " -m32")
endif()
if(ANDROID_ABI STREQUAL "x86_64")
    string(CONCAT C_EXTRA_FLAGS ${C_EXTRA_FLAGS} " -m64")
    string(CONCAT LINKER_EXTRA_FLAGS ${LINKER_EXTRA_FLAGS} " -m64")
endif()
if(ANDROID_ABI STREQUAL "arm64-v8a")
    string(CONCAT CONFIGURE_EXTRA_FLAGS ${CONFIGURE_EXTRA_FLAGS} "--with-cpu=generic_fpu")
endif()
if(ANDROID_ABI STREQUAL "armeabi-v7a")
    string(CONCAT CONFIGURE_EXTRA_FLAGS ${CONFIGURE_EXTRA_FLAGS} "--with-cpu=generic_fpu")
endif()
if(ANDROID_ABI STREQUAL "armeabi")
    string(CONCAT CONFIGURE_EXTRA_FLAGS ${CONFIGURE_EXTRA_FLAGS} "--with-cpu=generic_fpu")
endif()
MESSAGE( STATUS "CONFIGURE_EXTRA_FLAGS: " ${CONFIGURE_EXTRA_FLAGS} )
MESSAGE( STATUS "C_EXTRA_FLAGS: " ${C_EXTRA_FLAGS} )
MESSAGE( STATUS "LINKER_EXTRA_FLAGS: " ${LINKER_EXTRA_FLAGS} )

ExternalProject_Add(project_mpg123
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp-dep/mpg123
    CONFIGURE_COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp-dep/mpg123/configure --host=${ANDROID_LLVM_TRIPLE} --build=i686-pc-linux-gnu --prefix=<INSTALL_DIR> ${CONFIGURE_EXTRA_FLAGS} "CXX=${CMAKE_CXX_COMPILER}" "CXXFLAGS=${CMAKE_CXX_FLAGS} ${C_EXTRA_FLAGS}" "CC=${MY_CC}" "CFLAGS=${CMAKE_SAFE_C_FLAGS} ${C_EXTRA_FLAGS}" "LDFLAGS=${CMAKE_STATIC_LINKER_FLAGS} ${LINKER_EXTRA_FLAGS}" --disable-modules # LD=${CMAKE_C_LINK_EXECUTABLE}
    LOG_BUILD 1
    LOG_CONFIGURE 1
    LOG_INSTALL 1
    BUILD_COMMAND ${MAKE})
ExternalProject_Get_Property(project_mpg123 install_dir)
add_library(mpg123 SHARED IMPORTED)
set_property(TARGET mpg123 PROPERTY IMPORTED_LOCATION ${install_dir}/lib/libmpg123.so)


ExternalProject_Get_Property(project_mpg123 install_dir)
#set(install_dir "/home/delusional/Documents/P8/app/app/src/main/cpp-dep/mpg123-build/testtest/")

#include_directories(${install_dir}/include)

MESSAGE( STATUS "CMAKE_CXX_COMPILER: " ${CMAKE_CXX_COMPILER} )
MESSAGE( STATUS "CMAKE_SYSTEM_PROCESSOR: " ${CMAKE_SYSTEM_PROCESSOR} )
MESSAGE( STATUS "CMAKE_CXX_FLAGS: " ${CMAKE_CXX_FLAGS} )
MESSAGE( STATUS "CMAKE_C_COMPILER: " ${CMAKE_C_COMPILER} )
MESSAGE( STATUS "CMAKE_C_FLAGS: " ${CMAKE_C_FLAGS} )
MESSAGE( STATUS "CMAKE_SHARED_LINKER_FLAGS: " ${CMAKE_SHARED_LINKER_FLAGS} )
MESSAGE( STATUS "CMAKE_C_LINK_EXECUTABLE: " ${CMAKE_C_LINK_EXECUTABLE} )

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

MESSAGE("CMAKE_SYSROOT: " ${CMAKE_SYSROOT})

ExternalProject_Add(project_native
    SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp
    DEPENDS project_mpg123
    CMAKE_ARGS "${CMAKE_ARGS};-DCMAKE_LIBRARY_PATH=${install_dir}/lib;-DCMAKE_INSTALL_PREFIX=<INSTALL_DIR>")

