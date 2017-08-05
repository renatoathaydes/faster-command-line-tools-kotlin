# Faster command-line tools with Kotlin

This is a repository with the code used in my blog post.

## Running this benchmark

### Requirements

* Java 8
* Git
* download the [input file](https://storage.googleapis.com/books/ngrams/books/googlebooks-eng-all-1gram-20120701-0.gz),
  decompress it and save under `ngrams.tsv`.

### Running the benchmarks

To run the benchmarks:

1. clone this repo

```
git clone https://github.com/renatoathaydes/faster-command-line-tools-kotlin.git
cd faster-command-line-tools-kotlin
```

2. checkout a branch or commit you want to benchmark.

Commits:

* v1: b7c9e09d73093b8bf52d22db3965c7a71117fd18
* v2: 89e3f9b23cb6a512588833700475a8c836d46234
* v3: 24547d9d8ebf7b8ce605b1b0c90f21f5cd8f91d4
* v4: 315d8910342cc54a6d2c6dbb70a00ad61d3dbc37
* v5: d8bc248a104cad09333adb9f6b1c0986683fca0c
* v6: e9e0d60e8ec293e32961d71e1caa77af3adacbf3
* v7: f1983271d6b3ad5c2516e1cdf41648e446ab1d7c
* bonus version with MappedByteBuffer: 0b305bbe64f7586badb9c9efdef296fe9c4df38c 

> Notice that the versions in the commit messages got messed up. Sorry for that.

For example:

```
git checkout b7c9e09d73093b8bf52d22db3965c7a71117fd18
```

> Notice that `master` contains v7.

3. Build with gradlew

```
gradlew clean jar
```

4. Run the Kotlin tool using the `java` command:

```
java -cp "build/libs/*" Max_column_sum_by_keyKt ngrams.tsv 1 2
```
