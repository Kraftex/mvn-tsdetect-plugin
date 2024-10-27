# Test Smell Detector plugin for Maven
This repository is a custom maven plugin for [TestSmellDetector](https://github.com/TestSmells/TestSmellDetector) repository.
It's based on the JAR generated and that's the only part to configure to make the plugin works.

## Configuration for the plugin
The plugin needs only the `jar` tag configure to be executed.
That tag holds the path to the JAR download from [TestSmellDetector](https://github.com/TestSmells/TestSmellDetector) repository.

### Configuration available
| Configuration tag | Property tag | Default value | Information |
| ----------------- | ------------ | ------------- | ----------- |
| **jar** | tsdetect.jar |  | Path to JAR from [TestSmellDetector](https://github.com/TestSmells/TestSmellDetector) |
| **java** | tsdetect.java | java | Java executable, default value must works if you had configured your path. |
| **threshold** | tsdetect.threshold | 0 | Threshold on how many test smells can have the project |
| **verbose** | tsdetect.verbose | false | Print more information about what the plugin does |

### Phases
This plugin works by default on `test` phase.

### Goals
This plugin only define one goal `tsdetect`.

### Examples
**Minimal configuration**
```pom.xml
<plugin>
  <groupId>es.upm.alumnos.profundizacion</groupId>
  <artifactId>mvn-tsdetect-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <configuration>
    <jar>TestSmellDetector.jar</jar>
  </configuration>
  <executions>
    <execution>
    <goals>
      <goal>tsdetect</goal>
    </goals>
    </execution>
  </executions>
</plugin>
```
**Using Maven properties**
```pom.xml
<properties>
  <tsdetect.verbose>true</tsdetect.verbose>
</properties>
...
<plugin>
  <groupId>es.upm.alumnos.profundizacion</groupId>
  <artifactId>mvn-tsdetect-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <configuration>
    <jar>TestSmellDetector.jar</jar>
  </configuration>
  <executions>
    <execution>
    <goals>
      <goal>tsdetect</goal>
    </goals>
    </execution>
  </executions>
</plugin>
```

## Using in a Maven project
You can use this plugin through [Jitpack](https://jitpack.io/).
You need to include on your `pom.xml` the following things:

- **Jitpack repository**
```pom.xml
  <repositories>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
  ```

- **Add the repository**
```pom.xml
  <dependency>
    <groupId>com.github.Kraftex</groupId>
    <artifactId>mvn-tsdetect-plugin</artifactId>
    <version>TAG</version>
  </dependency>
```

## How works?
All the magic is from the JAR and the [TestSmellDetector](https://github.com/TestSmells/TestSmellDetector) project itself.
There's an automatism, the JAR needs a CSV as input to know what files need to be scaned for posibles test smells.
By the standard paths of source and test directories, the plugin searches for java files and make a relation if the file test is like `FileTest.java`, `TestFile.java` or `FileTestSuite.java` and source file is `File.java`.
