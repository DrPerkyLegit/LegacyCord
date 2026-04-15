cmake_minimum_required(VERSION 4.2)
project(LegacyCord)

set(CMAKE_CXX_STANDARD 20)

add_executable(LegacyCord src/LegacyCord.cpp
        src/Networking/NetworkManager.cpp
)

target_include_directories(LegacyCord PUBLIC includes)

if (WIN32)
    target_link_libraries(LegacyCord ws2_32)
endif()