# Synthesis of Distributed Environments with Petri Games

Synthesis algorithms for petri games with one system and an arbitrary number of environment players.
Contains the packages: synthesisDistrEnv. Depends on the repos: libs, framework, synthesizer.

We use BDDs to build a two-player graph game and extract a winning, deadlock-avoiding graph game strategy.
This step supports bounded petri games.

From this graph game strategy we can build a winning, deadlock-avoiding petri game strategy.
Because these tree like structures are often infinite, we create a finite representation of it.
When a graph game state is reachable by different edges,
it's corresponding cut in the petri game strategy will be marked after taking any of those edges.
This procedure has not been proven correct.
This step supports safe petri games.

Integration:
------------
This modules can be used as separate library
and is integrated in the the [webinterface](https://github.com/adamtool/webinterface)

Related Publications:
---------------------
The theoretical background for finding a petri game strategy for a petri game with one system and a bounded number of environment players:
- _Bernd Finkbeiner, Paul Gölz:_
  [Synthesis in Distributed Environments](https://doi.org/10.4230/LIPIcs.FSTTCS.2017.28).

------------------------------------

How To Build
------------
A __Makefile__ is located in the main folder.
First, pull a local copy of the dependencies with
```
make pull_dependencies
```
then build the whole framework with all the dependencies with
```
make
```
To build a single dependencies separately, use, e.g,
```
make synthesiser
```
To delete the build files and clean-up
```
make clean
```
To also delete the files generated by the test and all temporary files use
```
make clean-all
```
Some of the algorithms depend on external libraries or tools. To locate them properly create a file in the main folder
```
touch ADAM.properties
```
and add the absolute paths of the necessary libraries or tools:
```
libraryFolder=<path2Repo>/dependencies/libs/
dot=dot
time=/usr/bin/time
```
You may leave some of the properties open if you don't use the corresponding libraries/tools.

Tests
-----
Make sure your `testoutputfolder` (configured in `build.properties`) exists
and run all tests by just typing
```
$ ant test
```
