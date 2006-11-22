/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

//
// $Rev$ $Date$
//

class MavenBuilder
    extends CliSupport
{
    def repodir = new File(basedir, "repository")
    
    def javaHome = System.getenv("JAVA_HOME")
    
    int timeout
    
    def MavenBuilder() {
        // Enable emacs mode to disable [task] prefix on output
        def p = ant.getAntProject()
        p.getBuildListeners()[0].setEmacsMode(true)
    }
    
    def setJavaVersion(ver) {
        def tmp = ver.replace(".", "_")
        def dir = System.getenv("JAVA_HOME_${tmp}")
        if (dir == null) {
            throw new Exception("Unable to use Java ${ver}; missing JAVA_HOME_${tmp}")
        }
        
        println("Using JAVA_HOME: ${dir}")
        
        this.javaHome = dir
    }
    
    def main(args) {
        def iter = args.toList().iterator()
        args = []
        def pom
        
        while (iter.hasNext()) {
            def arg = iter.next()
            
            switch (arg) {
                case [ '-j', '--java' ]:
                    setJavaVersion(iter.next())
                    break
                
                case '--timeout':
                    timeout = Integer.parseInt(iter.next())
                    break
                
                // HACK: Groovy's use of commons-cli eats up an '--' so need to use '---' to skip
                case '---':
                    while (iter.hasNext()) {
                        args.add(iter.next())
                    }
                    break
                
                //
                // TODO: Add bits to pick off -P* so we can always insert a profile to activate
                //
                
                case ~"-.*":
                    args.add(arg)
                    break
                
                default:
                    if (pom != null) {
                        throw new Exception("Unexpected argument: ${arg}")
                    }
                    pom = new File(arg)
                    if (!pom.isAbsolute()) {
                        pom = new File(basedir, arg)
                    }
                    break
            }
        }
        
        if (pom == null) {
            throw new Exception("Missing pom")
        }
        
        maven(pom, args)
    }
    
    def maven(pom, args) {
        assert pom != null
        assert args != null
        
        if (javaHome == null) {
            throw new Exception("Please define JAVA_HOME; or use --java <ver>")
        }
        
        ant.exec(executable: "mvn", dir: basedir, failonerror: true) {
            // Get a reference to the current node so we can conditionally set attributes
            def node = current.wrapper
            
            // Maybe set timeout
            if (timeout > 0) {
                println("Timeout after: ${timeout} seconds");
                def millis = timeout * 1000
                node.setAttribute('timeout', "${millis}")
            }
            
            arg(value: "-Dmaven.repo.local=${repodir}")
            arg(value: '--batch-mode')
            arg(value: '--errors')
            
            arg(value: '--file')
            arg(file: "${pom}")
            
            args.each {
                arg(value: "${it}")
            }
            
            env(key: "JAVA_HOME", file: javaHome)
        }
    }
}

new MavenBuilder().main(args)