# snake

An implementation of Snake using Clojurescript and React (via rum).

The intention is to have a clean seperation of browser/dom aware code from the game logic.


## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2014 Royce Townsend

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

Heavily influence by http://github.com/logaan/snake
