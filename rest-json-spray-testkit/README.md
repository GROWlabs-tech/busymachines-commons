# busymachines-commons-rest-json-spray-testkit

_*DO NOT DEPEND ON BOTH THIS MODULE AND `rest-json-testkit`. They share the same packages, and type names. It will end badly, chose one or the other. THIS MODULE WILL RECEIVE WAY LESS ATTENTION THAN THE OTHERS, AND HAS A HIGH CHANCE OF BEING DROPPED.*_

## artifacts

Current version is `0.2.0-RC5`. SBT module id:
`"com.busymachines" %% "busymachines-commons-rest-json-spray-testkit" % "0.2.0-RC5" % test`

## usage
It's literally the same as with [`rest-json-testkit`](../rest-json-testkit/README.md), you just need to have the corresponding JSON serializers/deserializers in scope for your tests, and that's it.
