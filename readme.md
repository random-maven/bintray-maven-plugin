
### Bintray maven plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin)
[![Bintray Download](https://api.bintray.com/packages/random-maven/maven/bintray-maven-plugin/images/download.svg) ](https://bintray.com/random-maven/maven/bintray-maven-plugin/_latestVersion)
[![Travis Status](https://travis-ci.org/random-maven/bintray-maven-plugin.svg?branch=master)](https://travis-ci.org/random-maven/bintray-maven-plugin/builds)

Similar plugins
* [devexperts/bintray-maven-plugin](https://github.com/Devexperts/bintray-maven-plugin)

Plugin features
* proxy authentication
* bintray target package removal
* create bintray target package on-demand
* automatic cleanup of old bintray target versions
* preservation of selected versions from cleanup by regex
* upload folder content, such as eclipse p2 repository, to fixed path

Maven goals
* [bintary:deploy](https://random-maven.github.io/bintray-maven-plugin/deploy-mojo.html)
* [bintary:upload](https://random-maven.github.io/bintray-maven-plugin/upload-mojo.html)

### Usage examples

#### `bintary:deploy` - deploy maven artifacts:

```
mvn clean deploy -P distro-bintray
```

```xml
        <profile>
            <id>distro-bintray</id>
            <build>
                <plugins>

                    <!-- Disable default deployer. -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-deploy-plugin</artifactId>
                        <configuration>
                            <skip>true</skip>
                        </configuration>
                    </plugin>

                    <!-- Enable alternative deployer. -->
                    <plugin>
                        <groupId>com.carrotgarden.maven</groupId>
                        <artifactId>bintray-maven-plugin</artifactId>
                        <configuration>
                            <skip>false</skip>

                            <!-- Bintray oranization name. -->
                            <subject>random-maven</subject>

                            <!-- Bintray target repository. -->
                            <repository>maven</repository>

                            <!-- Bintray credentials in settings.xml. -->
                            <serverId>distro-bintary</serverId>

                        </configuration>
                        <executions>
                            <!-- Activate "bintary:deploy" during "deploy" -->
                            <execution>
                                <goals>
                                    <goal>deploy</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

#### `bintary:upload` - upload eclipse p2 repository

```
mvn clean deploy -P upload-bintray
```

```xml
        <profile>
            <id>upload-bintray</id>
            <build>
                <plugins>

                    <!-- Disable default deployer. -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-deploy-plugin</artifactId>
                        <configuration>
                            <skip>true</skip>
                        </configuration>
                    </plugin>

                    <!-- Enable alternative deployer. -->
                    <plugin>
                        <groupId>com.carrotgarden.maven</groupId>
                        <artifactId>bintray-maven-plugin</artifactId>
                        <configuration>
                            <skip>false</skip>

                            <!-- Bintray oranization name. -->
                            <subject>random-eclipse</subject>

                            <!-- Bintray target repository. -->
                            <repository>eclipse</repository>

                            <!-- Nominal permanent bintray identity. -->
                            <!-- Actual remote content will mirror local dir. -->
                            <bintrayPackage>tracker</bintrayPackage>
                            <bintrayVersion>release</bintrayVersion>

                            <!-- Local folder content to sync to the remote repo. -->
                            <sourceFolder>${project.build.directory}/repository</sourceFolder>
                            <!-- Remote folder for local content upload, relative path. -->
                            <targetFolder>repository</targetFolder>

                            <!-- Bintray credentials in settings.xml. -->
                            <serverId>distro-bintary</serverId>

                        </configuration>
                        <executions>
                            <!-- Activate "bintary:upload" during "deploy" -->
                            <execution>
                                <goals>
                                    <goal>upload</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

### Build yourself

```
cd /tmp
git clone git@github.com:random-maven/bintray-maven-plugin.git
cd bintray-maven-plugin
./mvnw.sh clean install -B -P skip-test
```
