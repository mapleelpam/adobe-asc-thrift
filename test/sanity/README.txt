This describes running the ASC sanity tests.  These are a series of quick tests designed to ensure that ASC is generally working.

1.  Edit build.properties to point to the correct avmplus executable.  The executables are located in ../../bin/<platform>.  
    You should change avm.name in build.properties to be the avmplus you wish to use relative to the bin director (windows/avmplus_s.exe, mac/avmplus_s, etc)

2.  type 'ant'

3.  check the contents of diff.out to see if you have introduced any differences.  Anything appearing in diff.out is considered a failed test.
