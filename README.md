[![TravisCI](https://img.shields.io/travis/Vorror/anitomyJ/master.svg?style=flat-square)](https://travis-ci.org/Vorror/anitomyJ)
[![Maven Central](https://img.shields.io/maven-central/v/com.dgtlrepublic/anitomyJ.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22anitomyJ%22)
[![Javadocs](http://javadoc.io/badge/com.dgtlrepublic/anitomyJ.svg?style=flat-square)](http://javadoc.io/doc/com.dgtlrepublic/anitomyJ)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg?style=flat-square)](https://opensource.org/licenses/MPL-2.0)

# AnitomyForJava

>*AnitomyForJava* is a Java port of [Anitomy](https://github.com/erengy/anitomy), a library for parsing anime video filenames. It's accurate, fast, and simple to use. All credit goes to [@erengy](https://github.com/erengy). I simply ported it.

## Examples

The following filename...

    [BM&T] Toradora! (2008) - 07v2 - Pool Opening [720p Hi10p ] [BD] [8F59F2BA].mkv

...would be resolved into these elements:

- Release group: *BM&T*
- Anime title: *Toradora!*
- Anime year: *2008*
- Episode number: *07*
- Source: *BD*
- Release version: *2*
- Episode title: *Pool Opening*
- Video resolution: *720p*
- Video term: *Hi10p*
- Audio term: *FLAC*
- File checksum: *8F59F2BA*

Here's an example code snippet...

```java
import com.dgtlrepublic.anitomyj.AnitomyJ;
import com.dgtlrepublic.anitomyj.Element;

public class Main {
    public static void main(String[] args) {
        List<Element> elements = AnitomyJ.parse(
                "[BM&T] Toradora! (2008) - 07v2 - Pool Opening [720p Hi10p ] [BD] [8F59F2BA].mkv");
        System.out.println(elements.stream()
                                   .map(e -> e.getCategory().name() + "=" + e.getValue())
                                   .collect(Collectors.joining("\n")));
    }
}

```

...which will output:

```
kElementFileExtension=mkv
kElementFileName=[BM&T] Toradora! (2008) - 07v2 - Pool Opening [720p Hi10p ] [BD] [8F59F2BA]
kElementVideoResolution=720p
kElementAnimeType=Opening
kElementVideoTerm=Hi10p
kElementSource=BD
kElementFileChecksum=8F59F2BA
kElementAnimeYear=2008
kElementEpisodeNumber=07
kElementReleaseVersion=2
kElementAnimeTitle=Toradora!
kElementReleaseGroup=BM&T
```
## Installation
### Requirements
Java 8+
### Maven Release
Add the following to your pom.xml:
```
<dependency>
    <groupId>com.dgtlrepublic</groupId>
    <artifactId>anitomyJ</artifactId>
</dependency>
```

### Binary Release
Binary releases can be found [here](https://github.com/Vorror/anitomyJ/releases/latest). Apache Commons(v3.4+) and Apache Commons Collections(v3.2+) are required. For convenience `anitomyJ-<version>-with-dependencies.jar` is provided, but installing the dependencies manually is recommended.

## Issues & Pull Requests

For the most part, AnitomyJ aims to be an exact Java replica of the original Anitomy. To make porting upstream changes easier most of the logic + file structure remain similar to their c++ counterparts. So, for the time being, I won't be accepting pull requests/issues that change the core parsing logic. I suggest opening an issue with the original Anitomy project and when it's fixed I'll merge it downstream.

That being said if the output of AnitomyJ and Anitomy differ in *any way* please open an issue.

## FAQ

- Why didn't you use JNI/JNA over porting over the library?

    That was an option, but since anitomy is a small library I decided just to just to rewrite it in native Java. The average SLOC is about 140 lines per java file, so it didn't take much time.


## License

*AnitomyForJava* is licensed under [Mozilla Public License 2.0](https://www.mozilla.org/en-US/MPL/2.0/FAQ/).