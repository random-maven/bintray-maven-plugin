
### Bintray Maven Plugin

A Bintray plugin to deploy Maven artifacts and upload Eclipse repositories. 

[![Project License][licence_icon]][licence_link]
[![Travis Status][travis_icon]][travis_link]
[![Lines of Code][tokei_basic_icon]][tokei_basic_link]

|         | Production Release | Development Release |
|---------|--------------------|---------------------|
| <h5>Install</h5> | [![Central][central_icon]][central_link] | [![Bintray][bintray_icon]][bintray_link] | 

Similar plugins
* [devexperts/bintray-maven-plugin](https://github.com/Devexperts/bintray-maven-plugin)

Plugin features
* proxy authentication
* bintray target package removal
* create bintray target package on-demand
* automatic cleanup of old bintray target versions
* preservation of selected versions from cleanup by regex
* upload folder content, such as eclipse p2 repository, to fixed path
* automatic cleanup of previous eclipse p2 repository resources by regex

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

```bash
cd /tmp
git clone git@github.com:random-maven/bintray-maven-plugin.git
cd bintray-maven-plugin
./mvnw.sh clean install -B -P skip-test
```

[licence_icon]: https://img.shields.io/github/license/random-maven/bintray-maven-plugin.svg?label=License
[licence_link]: http://www.apache.org/licenses/

[travis_icon]: https://travis-ci.org/random-maven/bintray-maven-plugin.svg
[travis_link]: https://travis-ci.org/random-maven/bintray-maven-plugin/builds

[tokei_basic_icon]: https://tokei.rs/b1/github/random-maven/bintray-maven-plugin
[tokei_basic_link]: https://github.com/random-maven/bintray-maven-plugin 

[central_icon]: https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin/badge.svg?style=plastic
[central_link]: https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/bintray-maven-plugin

[bintray_icon]: https://api.bintray.com/packages/random-maven/maven/bintray-maven-plugin/images/download.svg
[bintray_link]: https://bintray.com/random-maven/maven/bintray-maven-plugin/_latestVersion
