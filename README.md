# ligbugs

A Clojurescript app to simulate ad-hoc firefly synchronization. Based on [Firefly Synchronization in Ad Hoc Networks](https://mobile.aau.at/publications/tyrrell-2006-minema-firefly.pdf).

Check out a demo [here](http://nherzing.github.io/ligbugs/)

## Usage

`lein cljsbuild once` and then pop open index-dev.html in a browser of your choosing.

index-dev.html will display a grid of flashing fireflies. Each firefly can only see the fireflies that are adjacent to it (above, below, left of, right of). Fireflies change when they flash based on what they see their neighbors doing. Eventually, everbody syncs up and all flash at once.

The code could easily be reused to make all sorts of configurations of fireflies. Check out the `bug` function.

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
