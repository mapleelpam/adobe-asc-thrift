#!/usr/bin/env python
#
# ***** BEGIN LICENSE BLOCK ***** 
# Version: MPL 1.1/GPL 2.0/LGPL 2.1 
#
# The contents of this file are subject to the Mozilla Public License Version 1.1 (the 
# "License"); you may not use this file except in compliance with the License. You may obtain 
# a copy of the License at http://www.mozilla.org/MPL/ 
#
# Software distributed under the License is distributed on an "AS IS" basis, WITHOUT 
# WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
# language governing rights and limitations under the License. 
#
# created by Adobe AS3 Team
#
# Alternatively, the contents of this file may be used under the terms of either the GNU 
# General Public License Version 2 or later (the "GPL"), or the GNU Lesser General Public 
# License Version 2.1 or later (the "LGPL"), in which case the provisions of the GPL or the 
# LGPL are applicable instead of those above. If you wish to allow use of your version of this 
# file only under the terms of either the GPL or the LGPL, and not to allow others to use your 
# version of this file under the terms of the MPL, indicate your decision by deleting provisions 
# above and replace them with the notice and other provisions required by the GPL or the 
# LGPL. If you do not delete the provisions above, a recipient may use your version of this file 
# under the terms of any one of the MPL, the GPL or the LGPL. 
# 
# ***** END LICENSE BLOCK ***** */

import os, sys, getopt, datetime, pipes, glob, itertools, tempfile, string, re, platform
from os.path import *
from os import getcwd,environ,walk
from datetime import datetime
from glob import glob
from sys import argv, exit
from getopt import getopt
from itertools import count
from subprocess import Popen, PIPE,STDOUT
from time import time

# add parent dir to python module search path
sys.path.append('..')

try:
    from util.runtestBase import RuntestBase
    # runtestUtils must be imported after "from os.path import *" as walk is overridden
    from util.runtestUtils import *
except ImportError:
    print "Import error.  Please make sure that the test/acceptance/util directory has been deleted."
    print "   (directory has been moved to test/util)."

class AscRuntest(RuntestBase):
    fullRun = False
    regexOutput = False
    writeResultProperties = True
    

    def __init__(self):
        # Set threads to # of available cpus/cores
        self.threads = detectCPUs()
        RuntestBase.__init__(self)

    def setEnvironVars(self):
        RuntestBase.setEnvironVars(self)
        
    def usage(self, c):
        RuntestBase.usage(self, c)
        print '    --ext           set the testfile extension (defaults to .as)'
        print '    --threads       number of threads to run (default=# of cpu/cores), set to 1 to have tests finish sequentially'
        print '    --full          do a full coverage pass (all switches and options exercised)'
        # print ' -i --intermediate  create and compare intermediate code/AVM assembly/parse tree'
        print ' -r --regex         output err.actual files that are regex corrected'
        
        exit(c)
    
    def setOptions(self):
        RuntestBase.setOptions(self)
        self.options += 'r'
        self.longOptions.extend(['ext=','full','regex','threads='])
    
    def parseOptions(self):
        opts = RuntestBase.parseOptions(self)
        for o, v in opts:
            if o in ('--ext',):
                self.sourceExt = v
            elif o in ('--threads',):
                self.threads=int(v)
            elif o in ('-r','--regex',):
                self.regexOutput = True
            elif o in ('--full',):
                self.fullRun = True

                
    def run(self):
        self.setEnvironVars()
        self.loadPropertiesFile()
        self.setOptions()
        self.parseOptions()
        self.setTimestamp()
        self.checkPath()
        if not self.config:
            self.determineConfig()
        if self.htmlOutput and not self.rebuildtests:
            self.createOutputFile()
        self.tests = self.getTestsList(self.args)
        # Load the root testconfig file
        self.settings, self.includes = self.parseTestConfig('.')
        self.preProcessTests()
        self.runTests(self.tests)
        self.cleanup()
    
    def run_avm(self, abc, settings={}):
        output=[]
        avmargs = ''
        if not settings:
            settings = self.getLocalSettings(splitext(abc)[0])
        if settings.has_key('.*') and settings['.*'].has_key('avm.args'):
            avmargs = settings['.*']['avm.args']
        cmd="%s %s %s" % (self.avm,avmargs,abc)
        f,err,exit = self.run_pipe(cmd)
        for line in err+f:
            line = line.strip()
            if line:
                if not (line[0:3] == 'at ' and line[-1] == ')'):  #don't record error location, so we strip it out
                    output.append(line)
        return output
    
    def runTestPrep(self, testAndNum):
        
        ast = testAndNum[0]
        testnum = testAndNum[1]
        outputCalls = [] #queue all output calls so that output is written in a block
        extraVmArgs = ''
        abcargs = ''
        
        dir = split(ast)[0]
        root,ext = splitext(ast)
        testName = root + '.abc'
            
        includes = self.includes #list
        
        settings = self.getLocalSettings(root)
        
        #TODO: possibly handle includes by building test list?  This works for now...
        if includes and not list_match(includes,root):
            return
        
        # skip entire test if specified
        if settings.has_key('.*') and settings['.*'].has_key('skip'):
            outputCalls.append((self.js_print,('  skipping... reason: %s' % settings['.*']['skip'],)))
            self.allskips += 1
            outputCalls.insert(0,(self.js_print,('%d running %s' % (testnum, ast), '<b>', '</b><br/>')));
            return outputCalls
        
        # delete abc
        if isfile(testName):
            os.unlink(testName)
            
        actual = self.compile_test(ast, settings=settings)
        if isfile("%s.abc" % root):
            run_output = self.run_avm("%s.abc" % root, settings=settings)
            for line in run_output:
                actual.append(line)
        for line in actual:
            outputCalls.append((self.verbose_print,(line,)))
            
        expected=[]
        
        if self.vmtype == 'releasedebugger':
            if isfile('%s.err_s' % root):
                expected = self.loadExpected(root,'err_s')
            elif isfile('%s.err_sd' % root):
                expected = self.loadExpected(root,'err_sd')      
        elif self.vmtype == 'debugdebugger':
            if isfile('%s.err_sd' % root):
                expected = self.loadExpected(root,'err_sd')
            elif isfile('%s.err_d' % root):
                expected = self.loadExpected(root,'err_d')
        elif self.vmtype == 'debug':
            if isfile('%s.err_d' % root):
                expected = self.loadExpected(root,'err_d')
            elif isfile('%s.err_sd' % root):
                expected = self.loadExpected(root,'err_sd')
                
        if not expected:
            if isfile("%s.err" % root):
                expected = self.loadExpected(root,'err')
            elif isfile("%s.out" % root):
                expected = self.loadExpected(root,'out')
            else:
                expected=["%s.abc, [\d]+ bytes written" % splitext(split(ast)[1])[0]]
        
        if len(expected) != len(actual):
            if settings.has_key('.*') and settings['.*'].has_key('expectedfailure'):
                outputCalls.append((self.fail,(testName, 'expected failure: ' + line.strip() + ' reason: '+settings['.*']['expectedfailure'], self.expfailmsgs)))
                self.allexpfails += 1
            else:
                failmsg = "  FAILED: number of lines actual vs expected does not match\n"
                failmsg += "  expected:[\n"
                for line in expected:
                    failmsg += "    %s\n" % line
                failmsg += "  ]\n  actual  :[\n"
                for line in actual:
                    failmsg += "    %s\n" % line
                failmsg += "  ]"
                outputCalls.append((self.fail,(testName, failmsg, self.failmsgs)))
                self.allfails += 1
                self.writeErrActualFile(root,actual)
        else:
            expected = self.fixExpected(expected)
            result=True
            for i in range(len(expected)):
                try:
                    if not re.match(expected[i],actual[i]):
                        outputCalls.append((self.fail,(testName, "  FAILED matching line %d\n  expected:[%s]\n  actual  :[%s]" % (i+1,expected[i],actual[i]), self.failmsgs)))
                        result = False
                except:
                  raise
            if result: # test passed
                if settings.has_key('.*') and settings['.*'].has_key('expectedfailure'):
                    outputCalls.append((self.fail,(testName, 'unexpected pass - reason: '+settings['.*']['expectedfailure'], self.unpassmsgs)))
                    self.allunpass += 1
                else:
                  self.allpasses += 1
            else:   # test failed
                if settings.has_key('.*') and settings['.*'].has_key('expectedfailure'):
                    outputCalls.append((self.fail,(testName, 'expected failure - reason: '+settings['.*']['expectedfailure'], self.expfailmsgs)))
                    self.allexpfails += 1
                else:
                    self.allfails += 1
                self.writeErrActualFile(root, actual)
        
        # insert the testname before all other messages
        outputCalls.insert(0,(self.js_print,('%s running %s %s %s' % (testnum, ast, extraVmArgs, abcargs), '<b>', '</b><br/>')));
        return outputCalls
    
    def compile_test(self, as_file, extraArgs=[], settings={}):
        asc, builtinabc, shellabc, ascargs = self.asc, self.builtinabc, self.shellabc, self.ascargs

        if not isfile(asc):
            exit('ERROR: cannot build %s, ASC environment variable or --asc must be set to asc.jar' % as_file)
           
        (dir, file) = split(as_file)
    
        if not isfile(builtinabc):
            exit('ERROR: builtin.abc (formerly global.abc) %s does not exist, BUILTINABC environment variable or --builtinabc must be set to builtin.abc' % builtinabc)
    
            
        (testdir, ext) = splitext(as_file)
        
        if not settings:
            settings = self.getLocalSettings(testdir)
        
        if settings.has_key('.*') and settings['.*'].has_key('asc.override'):
            cmd = self.substTestconfigFilenames(settings['.*']['asc.override'])
        else:
            if asc.endswith('.jar'):
                cmd = '"%s" -jar %s' % (self.java,asc)
            else:
                cmd = asc
            
            for arg in extraArgs:
                cmd += ' %s' % arg
            
            if settings.has_key('.*') and settings['.*'].has_key('asc.args'):
                configAscargs = settings['.*']['asc.args']
                if not self.useShell:   # if we're running windows python
                    configAscargs = configAscargs.replace('\(','(')
                    configAscargs = configAscargs.replace('\)',')')
                # override the standard asc args and use settings from testconfig
                cmd += ' '+self.substTestconfigFilenames(configAscargs)
            else:    
                cmd += ' -import %s' % builtinabc
                # include files from directory with same name as test
                deps = glob(join(testdir,'*'+self.sourceExt))
                deps.sort()
                for util in deps + glob(join(dir,'*Util'+self.sourceExt)):
                    cmd += ' -in %s' % string.replace(util, '$', '\$')
            
            arglist = parseArgStringToList(ascargs)
            for arg in arglist:
                cmd += ' %s' % arg
            
        try:
            if self.fullRun: # compile test w/ multiple command line options
                ignore = self.run_pipe("%s %s %s" % (cmd,"-f" ,as_file))
                ignore = self.run_pipe("%s %s %s" % (cmd,"-i" ,as_file))
                ignore = self.run_pipe("%s %s %s" % (cmd,"-m" ,as_file))
                ignore = self.run_pipe("%s %s %s" % (cmd,"-p" ,as_file))
                ignore = self.run_pipe("%s %s %s" % (cmd,"-d" ,as_file))
            
            (f,err,exitcode) = self.run_pipe('%s %s' % (cmd,as_file))
            output = []
            for line in err+f:
                line = line.strip()
                self.verbose_print(line)
                if line:
                    output.append(line)
                
            # if we are using asc.override, may need to clean up .cpp and .h files generated from api-versioning tests
            if settings.has_key('.*') and settings['.*'].has_key('asc.override'):
                fileroot = splitext(as_file)[0]
                if exists(fileroot+'.h'):
                    os.remove(fileroot+'.h')
                if exists(fileroot+'.cpp'):
                    os.remove(fileroot+'.cpp')
                    
            return output
        except:
            raise
    
    
    #### Util Functions ####
    def fixExpected(self, expected):
        for i in range(len(expected)):
            expected[i] = expected[i].strip()
            if expected[i].startswith("[Compiler]"):
                expected[i] = r"\[Compiler\]" + expected[i][10:]
            if expected[i].startswith("[Coach]"):
                expected[i] = "\[Coach\]" + expected[i][7:]
            if expected[i].endswith(".^"):
                expected[i] = expected[i][:len(expected[i])-1] + "\^"
            if re.match(".*, Ln \d+, Col \d+:",expected[i]):
                expected[i] = ".*" + expected[i]
            if re.match(".*\(\).*",expected[i]):
                p=re.compile("\(\)")
                expected[i] = p.sub("\(\)",expected[i])
        return expected
    
    def loadExpected(self, root, ext):
        fileobject=open("%s.%s" % (root, ext))
        expected = []
        for line in fileobject:
            line = line.strip()
            if line:
                expected.append(line)
        return expected
    
    def regexReplace(self, match):
        # regex replace function for writeErrActualFile
        matchDict = match.groupdict()
        for i in matchDict:
            if matchDict['regexchar']:
                return r'\%s' % matchDict['regexchar']
            elif matchDict['byteswritten']:
                return r'\d+ bytes written'
            elif matchDict['errorNumber']:
                return '%s.*$' % matchDict['errorNumber']
            else: #testdir
                return r'.*'
        
    
    def substTestconfigFilenames(self, str):
        # Substitute the actual file locations for the filenames in testconfig.txt
        if self.playerglobalabc:
            str = re.sub(' playerglobal\.abc', ' ' + self.playerglobalabc, str)
        str = re.sub(' global\.abc', ' ' + self.builtinabc, str)
        str = re.sub(' builtin\.abc', ' ' + self.builtinabc, str)
        str = re.sub(' shell_toplevel\.abc', ' ' + self.shellabc, str)
        str = re.sub(' avmshell', ' ' + self.avm, str)
        str = re.sub(' asc\.jar', ' ' + self.asc, str)
        return str

    def writeErrActualFile(self, root, actual):
        # output a regex ready err.actual file
        fdopen=open(root+'.err.actual','w')
        testDir = dirname(root)
        if self.regexOutput:
            subPattern = r'((?P<testdir>%s.)|(?P<byteswritten>\d+ bytes written)|(?P<errorNumber>^.*Error: Error #[0-9]{4})(.*$)|(?P<regexchar>[\^\$\*\+\?\{\}\[\]\(\)\\]))' % testDir
            for line in actual:
                line = re.sub(subPattern, self.regexReplace,line)
                fdopen.write(line+"\n")
        else:
            for line in actual:
                fdopen.write(line+"\n")
        fdopen.close()
  
runtest = AscRuntest()