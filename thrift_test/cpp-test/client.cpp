
#include <iostream>
#include "AstDumper.h"  // As an example

#include <transport/TSocket.h>
#include <transport/TBufferTransports.h>
#include <transport/TSimpleFileTransport.h>
#include <transport/TFileTransport.h>
#include <protocol/TBinaryProtocol.h>

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;

int main(int argc, char **argv) {
#if 0
  boost::shared_ptr<TSocket> socket(new TSocket("localhost", 9090));
  boost::shared_ptr<TTransport> transport(new TBufferedTransport(socket));
#else
  boost::shared_ptr<TTransport> transport(new TSimpleFileTransport("1.pn",false,true));
  //boost::shared_ptr<TTransport> transport(new TFileTransport("1.pn",false));
#endif
  boost::shared_ptr<TProtocol> protocol(new TBinaryProtocol(transport));

    try {
        ast::dumper::AstDumperClient client( protocol);
        transport->open();
        client.ping();
        transport->close();
    }
    catch (TTransportException ex )
    {
        std::cout << ex.what()<<std::endl;
        return 1;
    }

  return 0;
}
