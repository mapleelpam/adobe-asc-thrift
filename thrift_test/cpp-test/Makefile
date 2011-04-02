

THRIFT_COMPILER := thrift

CFLAGS := -I/usr/local/include/thrift/ -I./gen-cpp/
CPPFLAGS := ${CFLAGS}
LIBS   := /usr/local/lib/libthrift.a


generated-objs = gen-cpp/AstDumper.o gen-cpp/SyntaxTree_constants.o gen-cpp/SyntaxTree_types.o


all:client server client-2

client-2: client-2.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

client: client.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

server: server.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

clean:
	rm -f client server *.o gen-cpp/*.o

generate-thrift:
	${THRIFT_COMPILER} -r --gen cpp SyntaxTree.thrift
