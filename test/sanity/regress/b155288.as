var a = []
for(var i:int=0; i<10000; i++) a.push(i+"salt")
a.sort(Array.CASEINSENSITIVE)

// now allocate some non Strings
for(var i:int=0; i<10000; i++) new QName("asdf", i)

//touch each string
for(var i:int=0; i<10000; i++) a[i].toUpperCase()
