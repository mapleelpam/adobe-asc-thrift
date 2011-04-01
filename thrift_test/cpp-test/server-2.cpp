#include "Something.h"
#include <protocol/TBinaryProtocol.h>
#include <server/TSimpleServer.h>
#include <transport/TSimpleFileTransport.h>
#include <transport/TServerSocket.h>
#include <transport/TTransportUtils.h>

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;

using boost::shared_ptr;

using namespace Test;

class SomethingHandler : virtual public SomethingIf {
 public:
  SomethingHandler() {
   // Your initialization goes here
  }

  void ping() {
   // Your implementation goes here
    printf("ping\n");
  }
  void ping2( const int32_t a) {
   // Your implementation goes here
    printf("ping2 %d\n",a);
  }

};

int main(int argc, char **argv) {
  int port = 9090;
  shared_ptr<SomethingHandler> handler(new SomethingHandler());
  shared_ptr<TProcessor> processor(new SomethingProcessor(handler));
#if 0
  shared_ptr<TServerTransport> serverTransport(new TServerSocket(port));
#else
  shared_ptr<TServerTransport> serverTransport(new TSimpleFileTransport("1.pn",true,false));
#endif
  shared_ptr<TTransportFactory> transportFactory(new TBufferedTransportFactory());
  shared_ptr<TProtocolFactory> protocolFactory(new TBinaryProtocolFactory());

  TSimpleServer server(processor, serverTransport, transportFactory, protocolFactory);
  server.serve();
  return 0;
}
