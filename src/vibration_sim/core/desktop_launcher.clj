(ns vibration-sim.core.desktop-launcher
  (:require [vibration-sim.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. vibration-sim-game "vibration-sim" screen-dim-x screen-dim-y)
  (Keyboard/enableRepeatEvents true))
