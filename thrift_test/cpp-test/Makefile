

CFLAGS := `pkg-config --cflags thrift` -I./gen-cpp/
CPPFLAGS := ${CFLAGS}
LIBS   := `pkg-config --libs thrift`


generated-objs = gen-cpp/AstDumper.o gen-cpp/SyntaxTree_constants.o gen-cpp/SyntaxTree_types.o


all:client server

client: client.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

server: server.o ${generated-objs}
	${CXX} -o $@ $^ ${LIBS}

clean:
	rm -f client server *.o gen-cpp/*.o

generate-thrift:
	thrift -r --gen cpp SyntaxTree.thrift
