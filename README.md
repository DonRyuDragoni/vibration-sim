# Mass-Spring-Damper Simulator

In vibration theory, this is the first studied system, for many reasons. This
system is simple enough for didactic demonstration in classes and complex enough
to show every basic concept needed for advanced studies.

So, to help students visualize this basic concepts and to teach me a little more
Clojure, I coded this project in my free time. I also thought it would be cool
to include it in my course completion assignment to complement some basic
explanations.

# The Project

Using Clojure, a little bit of [play-clj][play-clj], and a lot of free time and
will of learning a new programming language, I put together a simple animation
to demonstrate the response of the system for diferent levels of damping.

This project was built using the [Leiningen][leiningen] project automation tool,
with the `play-clj` template, so these are the only dependencies (though,
because of the way Leiningen handles dependencies, you probably won't worry
about it).

Also, please note there are no test functions. This is to be modified later.
Maybe.

# Usage

Simply launch the program (pre-compiled standalone `.jar` lives in the `target/`
folder), as any Java file

```
java -jar path_to_jar_file/vibration-sim-1.1.0-standalone.jar
```

(changing `path_to_jar_file` to the actual path of the downloaded file), and use
any keys listed in the section below. If you want to clone the whole project, it
is pretty much standart for git repositories. The command

```
git clone https://github.com/DonRyuDragoni/vibration-sim
```

will do the trick. To run with Leiningen, simply `lein run` on the cloned
folder (as of any Leiningen project, dependencies are downloaded in the first
run). To compile and generate the `.jar` file, simply run `lein uberjar`.

_Note_: if you change the screen size (variables `screen-dim-x` and
`screen-dim-y` defined in `src-common/vibration_sim/core.clj`), you have to make
sure `src/vibration_sim/core/desktop_launcher.clj` is recompiled. I actually
don't know how to force it in `lein run`, so I just recompile the whole project
with `lein uberjar`. Please let me know of a better method.

## Vibration Modes

This simulation presents an animated representation for the really basic MSD
responses, assuming the system is not affected by external forces. To start any
of the animations, simply press the corresponding key at any time:

1. <kbd>n</kbd> undamped system: the system is reduced to the mass-spring, in
   which the mass oscilates forever, and amplitude of the movemnt remains
   constant;
2. <kbd>l</kbd> low-damped system: damping means the system will lose energy
   over time, so the mass will oscilate, but its amplitude will reduce over
   time, untill the system comes to a stop;
3. <kbd>h</kbd> high-damped system: high damping means the system will not
   oscilate, instead, it will return as fast as possible to the equilibrium
   position;
4. <kbd>r</kbd> no movement: stops the movement.

Also, note that pressing any key will reset the time to zero.

# TODOS

- [ ] add [expectations][expectations] and [lein-autoexpect][lein-autoexpect] as
  dependencies;
- [ ] write test functions using expectations and lein-autoexpect;
- [ ] add forced vibrations and resonance;
- [x] show small marks at previous positions of the mass;
- [x] make the label at the botton show the current time.

# Changelog

- *1.2.0* 2015-04-21
    - took a while to figure it out, but we have the timer printed on the
        screen;
    - blue dots now follow the mass, showing the plot of the movement equation;
    - the dots are removed from the animation as soon as they leave the screen;
    - the dots are also removed with any keypress, so re-starting any animation
        actually re-starts everything.
- *1.1.0* 2015-04-20
    - first working version of the program;
    - the mass-rectangle-thingy changes movement type with certain keys;
    - any key resets the time (making restart easier);
    - resized the window (makes more sense to me as it is).

[play-clj]: https://github.com/oakes/play-clj
[leiningen]: http://leiningen.org/
[expectations]: https://github.com/jaycfields/expectations
[lein-autoexpect]: https://github.com/jakemcc/lein-autoexpect
