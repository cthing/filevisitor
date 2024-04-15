# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") filevisitor

[![CI](https://github.com/cthing/filevisitor/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/filevisitor/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.cthing/filevisitor/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.cthing/filevisitor)
[![javadoc](https://javadoc.io/badge2/org.cthing/filevisitor/javadoc.svg)](https://javadoc.io/doc/org.cthing/filevisitor)

A Java library providing glob pattern matching traversal of the file system. In addition to pattern matching,
Git ignore files can be honored.

## Usage
The library is available from [Maven Central](https://repo.maven.apache.org/maven2/org/cthing/filevisitor/) using
the following Maven dependency:
```xml
<dependency>
  <groupId>org.cthing</groupId>
  <artifactId>filevisitor</artifactId>
  <version>1.0.0</version>
</dependency>
```
or the following Gradle dependency:
```kotlin
implementation("org.cthing:filevisitor:1.0.0")
```

### File Tree Traversal
In a similar manner to [Files.walkFileTree](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html#walkFileTree(java.nio.file.Path,java.util.Set,int,java.nio.file.FileVisitor)),
the `MatchingTreeWalker` will traverse the file system starting at the specified directory. The walker will attempt
to match files and directories against the patterns specified (see [Pattern Matching Syntax](#pattern-matching-syntax)).
Specify an implementation of the `MatchHandler` interface to process the matched files and directories. The following
example looks for all Java files:

```java
final MatchHandler handler = new MyMatchHandler();
final MatchingTreeWalker walker = new MatchingTreeWalker(start, handler, "**/*.java");
walker.walk();
```

To use [Files.walkFileTree](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html#walkFileTree(java.nio.file.Path,java.util.Set,int,java.nio.file.FileVisitor))
directly, specify an instance of the `MatchingFileVisitor`.  The equivalent to the above example is:

```java
final MatchHandler handler = new MyMatchHandler();
final MatchingFileVisitor visitor = new MatchingFileVisitor(handler, "**/*.java");
Files.walkFileTree(start, visitor);
```

To obtain a list of all matched files and directories, use the `CollectingMatchHandler`.

```java
final CollectingMatchHandler handler = new CollectingMatchHandler();
new MatchingTreeWalker(start, handler, "**/*.java").walk();
final List<Path> matchedPaths = handler.getPaths();
```

To obtain a list of only matched files:

```java
final CollectingMatchHandler handler = new CollectingMatchHandler(false);
new MatchingTreeWalker(start, handler, "**/*.java").walk();
final List<Path> matchedPaths = handler.getPaths();
```

### Pattern Matching Syntax
Patterns to match files and directories use the [Git ignore](https://git-scm.com/docs/gitignore#_pattern_format)
glob syntax. Note that unlike Git ignore globs, which indicate the files and directories to exclude, in this
library, they indicate the files and directories to include. For example, specifying `**/*.java` includes
all Java files in any directory. To exclude files and directories, use the "!" negation prefix. Below is summary
of the glob syntax. See the [Git ignore](https://git-scm.com/docs/gitignore#_pattern_format) documentation for a
detailed explanation.

* Blank lines are ignored
* Lines starting with "#" are comments
* A "!" prefix negates the pattern
* A "\" escapes special characters (e.g. to use a "#" character, specify "\\#")
* A "/" is used as the directory separator regardless of the platform
* A "/" at the end of the pattern only matches directories
* Relative paths are resolved against the starting directory of the traversal
* A "*" matches anything except a slash
* A "?" matches any one character except a slash
* Range notation (e.g. "\[a-zA-Z]") can be used to match one character in that range
* A leading "\**/" (e.g. "\**/foo") matches in all directories
* A trailing "/\**" (e.g. "foo/**") matches everything inside
* A slash followed by two consecutive asterisks followed by a slash (e.g. "a/**/b") matches zero or more directories

### Git Ignore Files
By default, file tree traversal respects [Git ignore files](https://git-scm.com/docs/gitignore). If the start directory
is anywhere within a Git work tree, any Git ignore files will be used to exclude files from matching. Global Git
ignore files are also considered. Git ignore files have the same precedence as they do in Git. User specified match
patterns have higher precedence than Git ignore patterns.

To disable the use of Git ignore files, call the `MatchingTreeWalker.respectGitignore` or
`MatchingFileVisitor.respectGitignore` method with `false`.

### Hidden Files
By default, file tree traversal ignores hidden files and directories. To include hidden files and directories, call
the `MatchingTreeWalker.excludeHidden` or `MatchingFileVisitor.excludeHidden` method with `false`.

### Symbolic Links
By default, the `MatchingTreeWalker` does not follow symbolic links. To follow symbolic links, call the
`MatchingTreeWalker.followLinks` method with `true`.

### Walk Depth
By default, the `MatchingTreeWalker` performs a depth first traversal of the entire file tree under the starting
directory. To restrict the traversal to a specific depth, call the `MatchingTreeWalker.maxDepth` method. Specify,
`Integer.MAX_VALUE` for unlimited depth.

## Acknowledgements
The glob and Git ignore pattern parsing and matching in this library is based on the Rust code in the
[ripgrep](https://github.com/BurntSushi/ripgrep) project's
[globset](https://github.com/BurntSushi/ripgrep/tree/master/crates/globset) and
[ignore](https://github.com/BurntSushi/ripgrep/tree/master/crates/ignore) crates covered by the
[Unlicense](http://unlicense.org/) (i.e. public domain).

The Git config file parser is a heavily modified copy of the
[Config](https://eclipse.googlesource.com/jgit/jgit/+/refs/heads/master/org.eclipse.jgit/src/org/eclipse/jgit/lib/Config.java)
class from the [Eclipse JGit<sup>TM</sup> project](https://www.eclipse.org/jgit/) and is covered by the
[EDL](https://www.eclipse.org/org/documents/edl-v10.php) license.

## Building
The library is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the library:
```bash
./gradlew build
```
The Javadoc for the library can be generated by running:
```bash
./gradlew javadoc
```

## Releasing
This project is released on the [Maven Central repository](https://central.sonatype.com/artifact/org.cthing/filevisitor).
Perform the following steps to create a release.

- Commit all changes for the release
- In the `build.gradle.kts` file
    - Ensure that `baseVersion` is set to the version for the release. The project follows [semantic versioning](https://semver.org/).
    - Set `isSnapshot` to `false`
- Commit the changes
- Wait until CI builds the release candidate
- Run the command `mkrelease filevisitor <version>`
- In a browser go to the [Maven Central Repository Manager](https://s01.oss.sonatype.org/)
- Log in
- Use the `Staging Upload` to upload the generated artifact bundle `filevisitor-bundle-<version>.jar`
- Click on `Staging Repositories`
- Once it is enabled, press `Release` to release the artifacts to Maven Central
- Log out
- Wait for the new release to be available on Maven Central
- In a browser, go to the project on GitHub
- Generate a release with the tag `<version>`
- In the build.gradle.kts file
    - Increment the `baseVersion` patch number
    - Set `isSnapshot` to `true`
- Update the `CHANGELOG.md` with the changes in the release and prepare for next release changes
- Update the `Usage` section in the `README.md` with the latest artifact release version
- Commit these changes
