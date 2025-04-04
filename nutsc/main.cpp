/**
 * ====================================================================
 * Nuts : Network Updatable Things Service
 * (universal package manager)
 * <br>
 * is a new Open Source Package Manager to help install packages and libraries
 * for runtime execution. Nuts is the ultimate companion for maven (and other
 * build managers) as it helps installing all package dependencies at runtime.
 * Nuts is not tied to java and is a good choice to share shell scripts and
 * other 'things' . Its based on an extensible architecture to help supporting a
 * large range of sub managers / repositories.
 * <br>
 * <p>
 * Copyright [2020] [thevpc]
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE Version 3 (the "License");
 * you may  not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://www.gnu.org/licenses/lgpl-3.0.en.html
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 * <br>
 * ====================================================================
 */

/**
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 * @author Pete Kirkham (Win32 port)
 */

#ifdef WIN32
	#include <direct.h>
	#include <winsock2.h>
	#include <ws2tcpip.h>
#else
	#include <arpa/inet.h>
	#include <netdb.h>
	#include <netinet/in.h>
	#include <sys/socket.h>
	#include <sys/un.h>
	#include <sys/time.h>
	#include <sys/types.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>

#define NAILGUN_VERSION "1.0.0"

#define BUFSIZE (2048)

#ifdef WIN32
	HANDLE NG_STDIN_FILENO;
	HANDLE NG_STDOUT_FILENO;
	HANDLE NG_STDERR_FILENO;
	#define FILE_SEPARATOR '\\'
	#define MSG_WAITALL 0
#else
	#define NG_STDIN_FILENO STDIN_FILENO
	#define NG_STDOUT_FILENO STDOUT_FILENO
	#define NG_STDERR_FILENO STDERR_FILENO
	#define FILE_SEPARATOR '/'
	typedef int HANDLE;
	typedef unsigned int SOCKET;
#endif

#ifndef MSG_NOSIGNAL
  #define SEND_FLAGS 0
#else
  #define SEND_FLAGS MSG_NOSIGNAL
#endif

#ifndef MIN
#define MIN(a,b) ((a<b)?(a):(b))
#endif

#ifdef WIN32
	#define NAILGUN_FILESEPARATOR "NAILGUN_FILESEPARATOR=\\"
	#define NAILGUN_PATHSEPARATOR "NAILGUN_PATHSEPARATOR=;"
#else
	#define NAILGUN_FILESEPARATOR "NAILGUN_FILESEPARATOR=/"
	#define NAILGUN_PATHSEPARATOR "NAILGUN_PATHSEPARATOR=:"
#endif

#define NAILGUN_CLIENT_NAME_EXE "ng.exe"

#define NAILGUN_PORT_DEFAULT "2113"
#define NAILGUN_CLIENT_NAME "ng"
#define CHUNK_HEADER_LEN (5)

#define NAILGUN_SOCKET_FAILED (231)
#define NAILGUN_CONNECT_FAILED (230)
#define NAILGUN_UNEXPECTED_CHUNKTYPE (229)
#define NAILGUN_EXCEPTION_ON_SERVER (228)
#define NAILGUN_CONNECTION_BROKEN (227)
#define NAILGUN_BAD_ARGUMENTS (226)

#define CHUNKTYPE_STDIN '0'
#define CHUNKTYPE_STDOUT '1'
#define CHUNKTYPE_STDERR '2'
#define CHUNKTYPE_STDIN_EOF '.'
#define CHUNKTYPE_ARG 'A'
#define CHUNKTYPE_LONGARG 'L'
#define CHUNKTYPE_ENV 'E'
#define CHUNKTYPE_DIR 'D'
#define CHUNKTYPE_CMD 'C'
#define CHUNKTYPE_EXIT 'X'
#define CHUNKTYPE_SENDINPUT 'S'
#define CHUNKTYPE_HEARTBEAT 'H'

#define HEARTBEAT_TIMEOUT_MILLIS 500

#define NAILGUN_TTY_FORMAT "NAILGUN_TTY_%d=%d"

/*
   the following is required to compile for hp-ux
   originally posted at http://jira.codehaus.org/browse/JRUBY-2346
*/
#ifndef MSG_WAITALL
#define MSG_WAITALL 0x40 /* wait for full request or error */
#endif

/* the socket connected to the nailgun server */
int nailgunsocket = 0;

/* buffer used for receiving and writing nail output chunks */
char buf[BUFSIZE];

/* track whether server is ready to receive */
#ifdef WIN32
    HANDLE readyToSend = 0;
    HANDLE sending = 0;
#else
    int readyToSend = 0;
    struct timeval sendtime;
#endif

/**
 * Clean up the application.
 */
void cleanUpAndExit (int exitCode) {


  #ifdef WIN32
    CancelIo(STDIN_FILENO);
    WSACleanup();
    if (nailgunsocket) {
      closesocket(nailgunsocket);
    }
  #else
    close(nailgunsocket);
  #endif

  exit(exitCode);
}

#ifdef WIN32
/**
 * Handles an error.
 * Shows the message for the latest error then exits.
 */
void handleError () {
  LPVOID lpMsgBuf;
  int error = GetLastError();

  FormatMessage(
    FORMAT_MESSAGE_ALLOCATE_BUFFER |
    FORMAT_MESSAGE_FROM_SYSTEM |
    FORMAT_MESSAGE_IGNORE_INSERTS,
    NULL,
    error,
    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), /* Default language */
    (LPTSTR) &lpMsgBuf,
    0,
    NULL);

  /* Display the string. */
  MessageBox( NULL, (LPCTSTR)lpMsgBuf, "Error", MB_OK | MB_ICONERROR );

  /* Free the buffer. */
  LocalFree( lpMsgBuf );

  cleanUpAndExit(error);
}
#endif

/**
 * Exits the client if the nailgun server ungracefully shut down the connection.
 */
void handleSocketClose() {
  cleanUpAndExit(NAILGUN_CONNECTION_BROKEN);
}

/**
 * Writes everything in the specified buffer to the specified
 * socket handle.
 *
 * @param s the socket descriptor
 * @param buf the buffer containing the data to send
 * @param len the number of bytes to send.  Also used to
 *            return the number of bytes sent.
 * @return total bytes written or 0 if failure
 */
int sendAll(SOCKET s, char *buf, int len) {
  int total = 0;
  int bytesleft = len;
  int n = 0;

  while(total < len) {
    n = send(s, buf+total, bytesleft, SEND_FLAGS);

    if (n == -1) {
      break;
    }

    total += n;
    bytesleft -= n;
  }

  return n==-1 ? 0:total;
}

/**
 * Sends a chunk noting the specified payload size and chunk type.
 * Waits for sending mutex on Win32.
 *
 * @param size the payload size
 * @param chunkType the chunk type identifier
 */
void sendChunk(unsigned int size, char chunkType, char* buf) {
  /* buffer used for reading and writing chunk headers */
  char header[CHUNK_HEADER_LEN];
  int bytesSent;

  header[0] = (size >> 24) & 0xff;
  header[1] = (size >> 16) & 0xff;
  header[2] = (size >> 8) & 0xff;
  header[3] = size & 0xff;
  header[4] = chunkType;

#ifdef WIN32
  if (WaitForSingleObject(sending, INFINITE) != WAIT_OBJECT_0) {
    handleError();
  }
#else
  gettimeofday(&sendtime, NULL);
#endif

  bytesSent = sendAll(nailgunsocket, header, CHUNK_HEADER_LEN);
  if (bytesSent != 0 && size > 0) {
    bytesSent = sendAll(nailgunsocket, buf, size);
  } else if (bytesSent == 0 && (chunkType != CHUNKTYPE_HEARTBEAT || !(errno == EPIPE || errno == ECONNRESET))) {
    perror("send");
    handleSocketClose();
  }

#ifdef WIN32
  ReleaseMutex(sending);
#endif
}

/**
 * Sends the contents of the specified file as a long argument (--nailgun-filearg)
 * This is sent as one or more chunks of type CHUNK_LONGARG.  The end of the argument
 * is indicated by an empty chunk.
 *
 * @param filename the name of the file to send.
 * @return nonzero on failure
 */
int sendFileArg(char *filename) {
  int i, f;

  if ((f = open(filename, O_RDONLY)) < 0) {
    perror("--nailgun-filearg");
    return 1;
  }

  i = read(f, buf, BUFSIZE);
  while (i > 0) {
    sendChunk(i, CHUNKTYPE_LONGARG, buf);
    i = read(f, buf, BUFSIZE);
  }
  if (i < 0) {
    perror("--nailgun-filearg");
    return 1;
  }
  sendChunk(0, CHUNKTYPE_LONGARG, buf);

  close(f);
  return 0;
}

/**
 * Sends a null-terminated string with the specified chunk type.
 *
 * @param chunkType the chunk type identifier
 * @param text the null-terminated string to send
 */
void sendText(char chunkType, char *text) {
  int len = text ? strlen(text) : 0;
  sendChunk(len, chunkType, text);
}

/**
 * Receives len bytes from the nailgun socket and copies them to the specified file descriptor.
 * Used to route data to stdout or stderr on the client.
 *
 * @param destFD the destination file descriptor (stdout or stderr)
 * @param len the number of bytes to copy
 */
void recvToFD(HANDLE destFD, char *buf, unsigned long len) {
  unsigned long bytesRead = 0;
  int bytesCopied;

  while (bytesRead < len) {
    unsigned long bytesRemaining = len - bytesRead;
    int bytesToRead = (BUFSIZE < bytesRemaining) ? BUFSIZE : bytesRemaining;
    int thisPass = 0;

    thisPass = recv(nailgunsocket, buf, bytesToRead, MSG_WAITALL);
    if (thisPass == 0 || thisPass == -1) {
      perror("recv");
      handleSocketClose();
    }
    bytesRead += thisPass;

    bytesCopied = 0;

    while(bytesCopied < thisPass) {
      #ifdef WIN32
        DWORD thisWrite =  0;

        WriteFile(destFD, buf + bytesCopied, thisPass - bytesCopied,
          &thisWrite, NULL);

        if (thisWrite < 0) {
          break;
        }

        bytesCopied += thisWrite;
      #else
        int bytesWritten = write(destFD, buf + bytesCopied, thisPass - bytesCopied);
        if (bytesWritten == -1) {
          perror("write");
          handleSocketClose();
        }
        bytesCopied += bytesWritten;
      #endif
    }
  }
}

unsigned long recvToBuffer(unsigned long len) {
  unsigned long bytesRead = 0;
  while(bytesRead < len) {
    int thisPass = recv(nailgunsocket, buf + bytesRead, len - bytesRead, MSG_WAITALL);
    if (thisPass == 0 || thisPass == -1) {
        perror("recv");
        handleSocketClose();
    }
    bytesRead += thisPass;
  }
  return bytesRead;
}

/**
 * Processes an exit chunk from the server.  This is just a string
 * containing the exit code in decimal format.  It should fit well
 * within our buffer, so assume that it does.
 *
 * @param len the current length of the buffer containing the exit code.
 */
void processExit(char *buf, unsigned long len) {
  int exitcode;
  int bytesToRead = (BUFSIZE - 1 < len) ? BUFSIZE - 1 : len;
  int bytesRead = recvToBuffer(bytesToRead);

  if (bytesRead < 0) {
    handleSocketClose();
  }

  buf[bytesRead] = 0;

  exitcode = atoi(buf);

  cleanUpAndExit(exitcode);
}


/**
 * Sends len bytes from buf to the nailgun server in a stdin chunk.
 *
 * @param buf the bytes to send
 * @param len the number of bytes to send
 */
void sendStdin(char *buf, unsigned int len) {
#ifndef WIN32
  readyToSend = 0;
#endif
  sendChunk(len, CHUNKTYPE_STDIN, buf);
}

/**
 * Sends a stdin-eof chunk to the nailgun server
 */
void processEof() {
  sendChunk(0, CHUNKTYPE_STDIN_EOF, buf);
}

/**
 * Sends a heartbeat chunk to let the server know the client is still alive.
 */
void sendHeartbeat() {
  sendChunk(0, CHUNKTYPE_HEARTBEAT, buf);
}

#ifdef WIN32

HANDLE createEvent(BOOL manualReset) {
  return CreateEvent(NULL, /* default security */
		     manualReset,
		     FALSE, /* initial state unsignalled */
		     NULL /* unnamed event */);
}

DWORD WINAPI sendHeartbeats(LPVOID lpParameter) {
	/* this could be made more efficient by only sending heartbeats when stdin chunks aren't being sent */
	for (;;) {
		Sleep(HEARTBEAT_TIMEOUT_MILLIS);
		sendHeartbeat();
	}
}

/**
 * Thread main for reading from stdin and sending
 */
DWORD WINAPI processStdin (LPVOID lpParameter) {
  /* buffer used for reading and sending stdin chunks */
  char wbuf[BUFSIZE];

  /* number of bytes read */
  DWORD numberOfBytes;

  for (;;) {

    /* wait for ready to send */
    if(WaitForSingleObject(readyToSend, INFINITE) != WAIT_OBJECT_0) {
      handleError();
    }

	/* read data from stdin */
	if (! ReadFile(NG_STDIN_FILENO, wbuf, BUFSIZE, &numberOfBytes, NULL)) {
		if (numberOfBytes != 0) {
			handleError();
		}
	}

    /* send data to server */
    if (numberOfBytes > 0) {
      sendStdin(wbuf, numberOfBytes);
    } else {
      processEof();
      break;
    }

  }
  return 0;
}
#else
/**
 * Reads from stdin and transmits it to the nailgun server in a stdin chunk.
 * Sends a stdin-eof chunk if necessary.
 *
 * @return zero if eof has been reached.
 */
int processStdin() {
	int bytesread = read(STDIN_FILENO, buf, BUFSIZE);
	if (bytesread > 0) {
		sendStdin(buf, bytesread);
	} else if (bytesread == 0) {
		processEof();
	}
	return(bytesread);
}
#endif

#ifdef WIN32
/**
 * Initialise Windows sockets
 */
void initSockets () {
  WSADATA win_socket_data;     /* required to initialise winsock */

  WSAStartup(2, &win_socket_data);

  /* create flow control event and mutex */
  readyToSend = createEvent(FALSE);
  sending = CreateMutex(NULL, FALSE, NULL);
}
#endif

#ifdef WIN32
/**
 * Initialise the asynchronous io.
 */
void initIo () {
  /* create non-blocking console io */
  AllocConsole();

  NG_STDIN_FILENO = GetStdHandle(STD_INPUT_HANDLE);
  NG_STDOUT_FILENO = GetStdHandle(STD_OUTPUT_HANDLE);
  NG_STDERR_FILENO = GetStdHandle(STD_ERROR_HANDLE);
}
#endif

#ifdef WIN32
/**
 * Initialise the asynchronous io.
 */
void winStartInput () {
  SECURITY_ATTRIBUTES securityAttributes;
  DWORD threadId = 0;

  securityAttributes.bInheritHandle = TRUE;
  securityAttributes.lpSecurityDescriptor = NULL;
  securityAttributes.nLength = 0;

  if (!CreateThread(&securityAttributes, 0, &processStdin, NULL, 0, &threadId)) {
    handleError();
  }

  if (!CreateThread(&securityAttributes, 0, &sendHeartbeats, NULL, 0, &threadId)) {
    handleError();
  }
}
#endif

/**
 * Processes data from the nailgun server.
 */
void processnailgunstream() {

  /*for (;;) {*/
    unsigned long len;
    char chunkType;

    recvToBuffer(CHUNK_HEADER_LEN);

    len = ((buf[0] << 24) & 0xff000000)
      | ((buf[1] << 16) & 0x00ff0000)
      | ((buf[2] << 8) & 0x0000ff00)
      | ((buf[3]) & 0x000000ff);

    chunkType = buf[4];

    switch(chunkType) {
      case CHUNKTYPE_STDOUT: recvToFD(NG_STDOUT_FILENO, buf, len);
            break;
      case CHUNKTYPE_STDERR: recvToFD(NG_STDERR_FILENO, buf, len);
            break;
      case CHUNKTYPE_EXIT:   processExit(buf, len);
            break;
      case CHUNKTYPE_SENDINPUT:
#ifdef WIN32
            SetEvent(readyToSend);
#else
            readyToSend = 1;
#endif
            break;
      default:  fprintf(stderr, "Unexpected chunk type %d ('%c')\n", chunkType, chunkType);
          cleanUpAndExit(NAILGUN_UNEXPECTED_CHUNKTYPE);
    }
  /*}*/
}

/**
 * Returns the time interval between start and end in milliseconds.
 * @param end the end time
 * @param start the start time
 */
int intervalMillis(struct timeval end, struct timeval start) {

  return ((end.tv_sec - start.tv_sec) * 1000) +
    ((end.tv_usec - start.tv_usec) /1000);
}

/**
 * Trims any path info from the beginning of argv[0] to determine
 * the name used to launch the client.
 *
 * @param s argv[0]
 */
char *shortClientName(char *s) {
  char *result = strrchr(s, FILE_SEPARATOR);
  return ((result == NULL) ? s : result + 1);
}

/**
 * Returns true if the specified string is the name of the nailgun
 * client.  The comparison is made case-insensitively for windows.
 *
 * @param s the program name to check
 */
int isNailgunClientName(char *s) {
  /* VMS can't get the command name from argv[0] when defined
     as a foreign command, as the symbol is expanded. Aliases
     are supported by defining a foreign command that includes
     the class alias argument, e.g.
         ng=="$path:[to]ng.exe"
         alias==ng+" ""alias"""
  */
  #ifdef __VMS
  return (1);
  #else
  #ifdef WIN32
  return (!strcasecmp(s, NAILGUN_CLIENT_NAME) ||
           !strcasecmp(s, NAILGUN_CLIENT_NAME_EXE));
  #else
  return(!(strcmp(s, NAILGUN_CLIENT_NAME)));
  #endif
  #endif
}

/**
 * Displays usage info and bails
 */
void usage(int exitcode) {
  fprintf(stderr, "NailGun v%s\n\n", NAILGUN_VERSION);
  fprintf(stderr, "Usage: ng class [--nailgun-options] [args]\n");
  fprintf(stderr, "          (to execute a class)\n");
  fprintf(stderr, "   or: ng alias [--nailgun-options] [args]\n");
  fprintf(stderr, "          (to execute an aliased class)\n");
  fprintf(stderr, "   or: alias [--nailgun-options] [args]\n");
  fprintf(stderr, "          (to execute an aliased class, where \"alias\"\n");
  fprintf(stderr, "           is both the alias for the class and a symbolic\n");
  fprintf(stderr, "           link to the ng client)\n\n");

  fprintf(stderr, "where options include:\n");
  fprintf(stderr, "   --nailgun-D<name>=<value>   set/override a client environment variable\n");
  fprintf(stderr, "   --nailgun-version           print product version and exit\n");
  fprintf(stderr, "   --nailgun-showversion       print product version and continue\n");
  fprintf(stderr, "   --nailgun-server            to specify the address of the nailgun server\n");
  fprintf(stderr, "                               (default is NAILGUN_SERVER environment variable\n");
  fprintf(stderr, "                               if set, otherwise localhost)\n");
  fprintf(stderr, "   --nailgun-port              to specify the port of the nailgun server\n");
  fprintf(stderr, "                               (default is NAILGUN_PORT environment variable\n");
  fprintf(stderr, "                               if set, otherwise 2113)\n");
  fprintf(stderr, "   --nailgun-filearg FILE      places the entire contents of FILE into the\n");
  fprintf(stderr, "                               next argument, which is interpreted as a string\n");
  fprintf(stderr, "                               using the server's default character set.  May be\n");
  fprintf(stderr, "                               specified more than once.\n");
  fprintf(stderr, "   --nailgun-help              print this message and exit\n");

  cleanUpAndExit(exitcode);
}

int main(int argc, char *argv[], char *env[]) {
  int i;
  struct sockaddr *server_addr;
  socklen_t server_addr_len;
  struct sockaddr_in server_addr_in;
  #ifndef WIN32
    struct sockaddr_un server_addr_un;
  #endif
  char *nailgun_server;        /* server as specified by user */
  char *nailgun_port;          /* port as specified by user */
  char *cwd;
  u_short port;                /* port */
  struct hostent *hostinfo;
  char *cmd;
  int firstArgIndex;           /* the first argument _to pass to the server_ */
  char isattybuf[] = NAILGUN_TTY_FORMAT;

  #ifndef WIN32
    fd_set readfds;
    int eof = 0;
    struct timeval readtimeout;
    struct timeval currenttime;
    memset(&sendtime, '\0', sizeof(sendtime));
  #endif

  #ifdef WIN32
  initSockets();
  #endif

  /* start with environment variable.  default to localhost if not defined. */
  nailgun_server = getenv("NAILGUN_SERVER");
  if (nailgun_server == NULL) {
    nailgun_server = "127.0.0.1";
  }

  /* start with environment variable.  default to normal nailgun port if not defined */
  nailgun_port = getenv("NAILGUN_PORT");
  if (nailgun_port == NULL) {
    nailgun_port = NAILGUN_PORT_DEFAULT;
  }

  /* look at the command used to launch this program.  if it was "ng", then the actual
     command to issue to the server must be specified as another argument.  if it
     wasn't ng, assume that the desired command name was symlinked to ng in the user's
     filesystem, and use the symlink name (without path info) as the command for the server. */
  cmd = shortClientName(argv[0]);

  if (isNailgunClientName(cmd)) {
    cmd = NULL;
  }

  /* if executing just the ng client with no arguments or -h|--help, then
     display usage and exit.  Don't handle -h|--help if a command other than
     ng or ng.exe was used, since the appropriate nail should then handle
     --help. */
  if (cmd == NULL &&
        (argc == 1 ||
	  (argc == 2 && strcmp("--help", argv[1]) == 0) ||
	  (argc == 2 && strcmp("-h", argv[1]) == 0))) usage(0);

  firstArgIndex = 1;

  /* quite possibly the lamest commandline parsing ever.
     look for the two args we care about (--nailgun-server and
     --nailgun-port) and NULL them and their parameters after
     reading them if found.  later, when we send args to the
     server, skip the null args. */
  for (i = 1; i < argc; ++i) {
    if (!strcmp("--nailgun-server", argv[i])) {
      if (i == argc - 1) usage(NAILGUN_BAD_ARGUMENTS);
      nailgun_server = argv[i + 1];
      argv[i] = argv[i + 1] = NULL;
      ++i;
    } else if(!strcmp("--nailgun-port", argv[i])) {
      if (i == argc - 1) usage(NAILGUN_BAD_ARGUMENTS);
      nailgun_port = argv[i + 1];
      argv[i] = argv[i + 1]= NULL;
      ++i;
    } else if (!strcmp("--nailgun-filearg", argv[i])) {
      /* just verify usage here.  do the rest when sending args. */
      if (i == argc - 1) usage (NAILGUN_BAD_ARGUMENTS);
    } else if (!strcmp("--nailgun-version", argv[i])) {
      printf("NailGun client version %s\n", NAILGUN_VERSION);
      cleanUpAndExit(0);
    } else if (!strcmp("--nailgun-showversion", argv[i])) {
      printf("NailGun client version %s\n", NAILGUN_VERSION);
      argv[i] = NULL;
    } else if (!strcmp("--nailgun-help", argv[i])) {
      usage(0);
    } else if (cmd == NULL) {
      cmd = argv[i];
      firstArgIndex = i + 1;
    }
  }

  /* if there's no command, we should only display usage info
     if the version number was not displayed. */
  if (cmd == NULL) {
    usage(NAILGUN_BAD_ARGUMENTS);
  }

  #ifndef WIN32
    if (strncmp(nailgun_server, "local:", 6) == 0) {
      const char *socket_path = nailgun_server + 6;
      size_t socket_path_len = strlen(socket_path);
      if (socket_path_len > sizeof(server_addr_un.sun_path) - 1) {
        fprintf(stderr, "Socket path [%s] too long (%ld)\n", socket_path, (long) socket_path_len);
        cleanUpAndExit(NAILGUN_SOCKET_FAILED);
      }
      if ((nailgunsocket = socket(PF_LOCAL, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        cleanUpAndExit(NAILGUN_SOCKET_FAILED);
      }

      server_addr_un.sun_family = AF_LOCAL;
      strncpy(server_addr_un.sun_path, socket_path, socket_path_len);
      server_addr_un.sun_path[socket_path_len] = '\0';

      #ifdef BSD
        server_addr_un.sun_len = offsetof(struct sockaddr_un, sun_path) + socket_path_len;
      #endif
      server_addr = (struct sockaddr *)&server_addr_un;
      server_addr_len = sizeof(server_addr_un);
    } else {
  #endif

      /* jump through a series of connection hoops */
      hostinfo = gethostbyname(nailgun_server);

      if (hostinfo == NULL) {
        fprintf(stderr, "Unknown host: %s\n", nailgun_server);
        cleanUpAndExit(NAILGUN_CONNECT_FAILED);
      }

      port = atoi(nailgun_port);

      if ((nailgunsocket = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        perror("socket");
        cleanUpAndExit(NAILGUN_SOCKET_FAILED);
      }

      server_addr_in.sin_family = AF_INET;
      server_addr_in.sin_port = htons(port);
      server_addr_in.sin_addr = *(struct in_addr *) hostinfo->h_addr;

      memset(&(server_addr_in.sin_zero), '\0', 8);
      server_addr = (struct sockaddr *)&server_addr_in;
      server_addr_len = sizeof(server_addr_in);

  #ifndef WIN32
    }
  #endif

#ifndef MSG_NOSIGNAL
#ifdef SO_NOSIGPIPE
  int option_value = 1;
  if (setsockopt(nailgunsocket, SOL_SOCKET, SO_NOSIGPIPE, &option_value, sizeof(option_value)) < 0) {
    perror("setsockopt");
  }
#endif
#endif

  if (connect(nailgunsocket, server_addr, server_addr_len) == -1) {
    //should run a new nuts instance here!
    //TODO
      
    perror("connect");
    cleanUpAndExit(NAILGUN_CONNECT_FAILED);
  }

  /* ok, now we're connected.  first send all of the command line
     arguments for the server, if any.  remember that we may have
     marked some arguments NULL if we read them to specify the
     nailgun server and/or port */
  for(i = firstArgIndex; i < argc; ++i) {
    if (argv[i] != NULL) {
      if (!strcmp("--nailgun-filearg", argv[i])) {
        int sendResult = sendFileArg(argv[++i]);
        if (sendResult != 0) {
          perror("send");
          handleSocketClose();
        }
      } else sendText(CHUNKTYPE_ARG, argv[i]);
    }
  }

  /* now send environment */
  sendText(CHUNKTYPE_ENV, NAILGUN_FILESEPARATOR);
  sendText(CHUNKTYPE_ENV, NAILGUN_PATHSEPARATOR);
#ifndef WIN32
  /* notify isatty for standard pipes */
  for(i = 0; i < 3; i++) {
    sprintf(isattybuf, NAILGUN_TTY_FORMAT, i, isatty(i));
    sendText(CHUNKTYPE_ENV, isattybuf);
  }
#endif
  /* forward the client process environment */
  for(i = 0; env[i]; ++i) {
    sendText(CHUNKTYPE_ENV, env[i]);
  }

  /* now send the working directory */
  cwd = getcwd(NULL, 0);
  sendText(CHUNKTYPE_DIR, cwd);
  free(cwd);

  /* and finally send the command.  this marks the point at which
     streams are linked between client and server. */
  sendText(CHUNKTYPE_CMD, cmd);


  /* initialise the std-* handles and the thread to send stdin to the server */
  #ifdef WIN32
  initIo();
  winStartInput();
  #endif

  /* stream forwarding loop */
  while(1) {
    #ifndef WIN32
      FD_ZERO(&readfds);

      /* don't select on stdin if we've already reached its end */
      if (readyToSend && !eof) {
	FD_SET(NG_STDIN_FILENO, &readfds);
      }

      FD_SET(nailgunsocket, &readfds);

      memset(&readtimeout, '\0', sizeof(readtimeout));
      readtimeout.tv_usec = HEARTBEAT_TIMEOUT_MILLIS * 1000;
      if(select (nailgunsocket + 1, &readfds, NULL, NULL, &readtimeout) == -1) {
	  perror("select");
      }

      if (FD_ISSET(nailgunsocket, &readfds)) {
    #endif
	processnailgunstream();
    #ifndef WIN32
      } else if (FD_ISSET(NG_STDIN_FILENO, &readfds)) {
        int result = processStdin();
        if (result == -1) {
          perror("read");
          handleSocketClose();
        } else if (result == 0) {
          FD_CLR(NG_STDIN_FILENO, &readfds);
          eof = 1;
        }
      }
      gettimeofday(&currenttime, NULL);
      if (intervalMillis(currenttime, sendtime) > HEARTBEAT_TIMEOUT_MILLIS) {
	  sendHeartbeat();
      }
    #endif
  }

  /* normal termination is triggered by the server, and so occurs in processExit(), above */
}