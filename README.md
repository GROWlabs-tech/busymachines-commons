# busymachines-commons

Light-weight, modular, libraries for varying technology stacks, built _primarily_ on top of the [typelevel.scala](https://github.com/typelevel) ecosystem.

## Quickstart

Current version is `0.1.0-SNAPSHOT`.

Currently there is no CI that automatically publishes versions, so you'll have to clone the repo, and do a `+ publishLocal` from the `sbt` repl for each module(`core`). This will be fixed ASAP.

Then the available modules are:
* `"com.busymachines" %% "busymachines-commons-core" % "0.1.0-SNAPSHOT"`  

## Library Structure

The idea behind these sets of libraries is to help jumpstart backend RESTful api application servers with varying technology stacks. That's why you will have to pick and choose the modules suited for your specific stack.

Basically, as long as modules reside in the same repository they will be versioned with the same number, and released at the same time to avoid confusion. The moment we realize that a module has to take a life of its own, it will be moved to a separate module and versioned independently.

* [core](/core) `0.1.0-SNAPSHOT`

### Current version
The latest version is `N/A`. Will keep you up to date.

## Developer's Guide

This section will have to be expanded more once there are more projects living here.

### Contributing

Currently, if you want to contribute use the `fork+pull request` model, and Busy Machines team members will have a look at your PR asap. Currently, the active maintainers of this library are:
@lorandszakacs

## History

This used to be the resting place of the `busymachines-commons` library that we used internally for various projects, it reached version `0.6.5`, but it fell into disrepair. If you require that library, by any chance, then check-out the `zz_deprecated/release-0.6.5` branch, and good luck from there. That bit will never be maintained again.

## Alumni

People who have contributed in the past.
