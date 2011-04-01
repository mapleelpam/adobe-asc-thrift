thrift -r --gen java -o ./ SyntaxTree.thrift
rm -rf ./src/java/ast
mv gen-java/ast src/java/
