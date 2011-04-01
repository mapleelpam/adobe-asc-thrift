

THRIFT_COMPILER := /home/maple/iiii/bin/thrift

CFLAGS := -I/home/maple/iiii/include/thrift/ -I./gen-cpp/
CPPFLAGS := ${CFLAGS}
LIBS   := /home/maple/iiii/lib/libthrift.a


generated-objs = gen-cpp/AstDumper.o gen-cpp/SyntaxTree_constants.o gen-cpp/SyntaxTree_types.o


all:client server

client: client.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

server: server.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

clean:
	rm -f client server *.o gen-cpp/*.o

generate-thrift:
	${THRIFT_COMPILER} -r --gen cpp SyntaxTree.thrift
