
### bintray-maven-plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin)
[![Bintray Download](https://api.bintray.com/packages/random-maven/maven/bintray-maven-plugin/images/download.svg) ](https://bintray.com/random-maven/maven/bintray-maven-plugin/_latestVersion)

Similar plugins
* [devexperts/bintray-maven-plugin](https://github.com/Devexperts/bintray-maven-plugin)

Plugin features
* bintray target package removal
* create bintray target package on-demand
* automatic cleanup of old bintray target versions
* preservation of selected versions from cleanup by regex

Maven goals
* [bintary:deploy](https://random-maven.github.io/bintray-maven-plugin/deploy-mojo.html)

Usage example
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
                            <mavenRepo>maven</mavenRepo>
                            <!-- Bintray credentials in settings.xml. -->
                            <serverId>distro-bintary</serverId>
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
        </profile>
```
