Connect
========

Basic Setup
------------

1. Install the [Simple Build Tool, version 0.11](https://github.com/harrah/xsbt/wiki/Setup)

2. The project builds and runs against the oauth2 branch of [Unfiltered](https://github.com/unfiltered/unfiltered).
Currently this is not available from a repo, so you need to build and install it locally first.
   1. `git clone git://github.com/unfiltered/unfiltered.git`
   2. `cd u nfiltered`
   3. `git checkout oauth2`
   4. Edit project/build.scala and change scalaVersion to "2.9.1"
   5. Clear the Ivy cache: `rm -Rf ~/.ivy2/cache`
   6. `sbt` (to start the sbt command shell)
   7. `set publishArtifact in (util, packageDoc) := false` (because of bug/issue with scaladoc)
   8. `compile` and then `publish-local` [1]

3. `cd connect`
4. `sbt`
5. From the sbt command line, run `compile` or `test`

Client and Server
------------------

These can be run from within `sbt`. Alternatively, from the command line:

1. `sbt "project server" run`
2. `sbt "project client" run`

The test server authentication accepts any username and password, but the only valid user (defined in TestUsers) has the
id "john", so the openid authentication should fail for other user names.

[1]: Alternatively, you can run multiple sbt commands from the standard command line, e.g. `sbt ";compile ;publish-local"`
