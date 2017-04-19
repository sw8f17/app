cmake_minimum_required(VERSION 3.4.1)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

MESSAGE("CMAKE_SYSROOT: " ${ANDROID_SYSROOT})
MESSAGE("CMAKE_C_COMPILER: " ${CMAKE_C_COMPILER})

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp )

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


link_directories(/home/delusional/Documents/P8/app/app/.externalNativeBuild/cmake/debug/armeabi/project_mpg123-prefix/lib)

find_library( # Sets the name of the path variable.
              mpg123-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              mpg123
              PATHS /home/delusional/Documents/P8/app/app/.externalNativeBuild/cmake/debug/armeabi/project_mpg123-prefix/lib
              )

MESSAGE(STATUS "Lib LOCATION: " ${CMAKE_LIBRARY_PATH})

include_directories(
    ${mpg123-lib}/../include)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       libmpg123
                       ${log-lib})

