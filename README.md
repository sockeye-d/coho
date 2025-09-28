# <img src="https://raw.githubusercontent.com/sockeye-d/coho/refs/heads/main/coho.svg" style="height: 1em; width: 1em"> coho

Coho is a static website generator.
The entire thing is built around the Kotlin scripting engine using a custom Kotlin DSL as a build script.
This makes it significantly more flexible than existing solutions, since you basically get to build your own
mini-framework on top of a coho.

Here's a rough list of features it has:

* Kotlin DSL-based buildscript
* Flexible Kotlin-powered templated HTML files (like PHP)
* Live-reloading web server
* Build-time syntax highlighting
* [XML Kotlin DSL](https://fishies.dev/posts/new-xml-dsl) (that's right, *nested* DSLs)

For more information on why I made it, you can read my [webpage on it](https://fishies.dev/projects/coho), or
my [blog post](https://fishies.dev/posts/coho).

## Getting started

First, make sure you've got a Java Runtime Environment installed.
I use Java 21, and you *might* get compatibility issues with lower versions because I haven't tested it.

Next, go to the
[Build gradle artifact](https://github.com/sockeye-d/coho/actions/workflows/gradle-artifact.yml)
page, click the latest CI deployment, scroll to the bottom, and download the coho.jar artifact (you need to be
signed in to do this — this is a GitHub requirement, not mine).

Once you have the artifact downloaded, extract it and copy the `cli-all.jar` file somewhere.

And then that's it!

> You might want to add a script like this somewhere on your PATH:
> ```bash
> java -jar /path/to/cli-all.jar "$@"
> ```

> You can run `coho --print-shell-completions shell`, where `shell` is `nu` or `zsh`, and pipe that into wherever
> your shell completions are stored.

## Creating your first project

In an empty directory (or non-empty), run

```bash
coho --create serve
```

That'll create a template `main.coho.kts` file, an `index.md` file with some documentation, a Markdown template, and a
stylesheet. From there, you can edit the files.

`coho serve` will serve a live-updating web server.
If you just want to build the website, run `coho`.

> If your main coho script is not called `main.coho.kts`, you can do
> ```bash
> coho custom.coho.kts
> ```

### Debugging websites

You can run coho with extra debug information:

```bash
coho --verbose
```

For debugging build performance, you can use the `--show-execution-times` flag:

```bash
coho --show-execution-times
```

It'll print the output tree of the website along with how much time each element took to generate.

## Example websites

[My website](https://fishies.dev) is built using coho. You can view the entire source for it at
[sockeye-d/sockeye-d.github.io](https://github.com/sockeye-d/sockeye-d.github.io).

## Building coho

If you want to build coho from source, make sure you've got an entire JDK (not just a JRE), then run

```bash
./gradlew shadowJar
```

The built JAR file will be in `cli/build/libs/cli-all.jar`.
You can run the `coho` script to run the freshly built JAR file.

## Extra stuff

Here's a random [ksyntaxhighlighter6](https://invent.kde.org/frameworks/syntax-highlighting/) definition for Kate 
for the templated HTML files:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<language name="Kotlin HTML" version="20" kateversion="5.79" section="Markup" extensions="*.html" mimetype="text/html"
  author="fishnpotatoes (me@fishies.dev)" license="LGPL" priority="11">

  <highlighting>
    <contexts>

      <context name="Start" attribute="Normal Text" lineEndContext="#stay">
        <StringDetect attribute="Preprocessor" context="Kotlin Inner" String="&lt;?kt" />
        <IncludeRules context="FindText##HTML" includeAttrib="true" />
        <IncludeRules context="FindHTML##HTML" includeAttrib="true" />
      </context>

      <context name="Kotlin Inner" attribute="Preprocessor" lineEndContext="#stay">
        <Detect2Chars attribute="Preprocessor" char="?" char1="&gt;" context="#pop"/>
        <IncludeRules context="Normal##Kotlin" includeAttrib="true" />
      </context>

    </contexts>

    <itemDatas>
      <itemData name="Normal Text" defStyleNum="dsNormal" />
      <itemData name="Preprocessor" defStyleNum="dsPreprocessor" bold="1" spellChecking="false" />
    </itemDatas>

  </highlighting>

  <general>
    <comments>
      <comment name="multiLine" start="&lt;!--" end="--&gt;" region="comment" />
    </comments>
  </general>

</language>
<!-- kate: replace-tabs on; tab-width 2; indent-width 2; -->
```