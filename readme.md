
### bintray-maven-plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin)
[![Bintray Download](https://api.bintray.com/packages/random-maven/maven/bintray-maven-plugin/images/download.svg) ](https://bintray.com/random-maven/maven/bintray-maven-plugin/_latestVersion)

Similar to [devexperts/bintray-maven-plugin](https://github.com/Devexperts/bintray-maven-plugin)

Features:
* auto create of bintray package
* auto cleanup of bintray versions
* preserve selected versions by regex

Maven goals:
* [bintary:deploy](https://random-maven.github.io/bintray-maven-plugin/deploy-mojo.html)

Usage example:
```
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-deploy-plugin</artifactId>
                        <configuration>
                            <skip>true</skip>
                        </configuration>
                    </plugin>
                    <plugin>
                        <groupId>com.carrotgarden.maven</groupId>
                        <artifactId>bintray-maven-plugin</artifactId>
                        <configuration>
                            <skip>false</skip>
                            <subject>${bintray.subject}</subject>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>deploy</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
```
