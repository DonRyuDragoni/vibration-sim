(defproject vibration-sim "1.2.5"
  :description "MSD simulation with simple graphics"
  
  :dependencies [[com.badlogicgames.gdx/gdx "1.6.3"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.6.3"]
                 [com.badlogicgames.gdx/gdx-box2d "1.6.3"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.6.3"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.6.3"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.6.3"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.6.3"
                  :classifier "natives-desktop"]
                 [org.clojure/clojure "1.7.0-alpha5"]
                 [com.climate/claypoole "1.0.0"]
                 [play-clj "0.4.7"]]
  
  :source-paths ["src" "src-common"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [vibration-sim.core.desktop-launcher]
  :main vibration-sim.core.desktop-launcher)
