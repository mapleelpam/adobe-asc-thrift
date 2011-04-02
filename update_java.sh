thrift -r --gen java -o ./ SyntaxTree.thrift
rm -rf ./src/java/tw
mv gen-java/tw src/java/
