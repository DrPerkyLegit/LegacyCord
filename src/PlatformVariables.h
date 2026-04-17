//
// Created by PeakB on 4/16/2026.
//
#pragma once

#if defined(_WIN32)
#include <winsock2.h>
#include <ws2tcpip.h>

typedef SOCKET socket_t;

#else
#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <fcntl.h>
#include <cerrno>

typedef int socket_t;

#endif