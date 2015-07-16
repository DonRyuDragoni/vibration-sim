;; This file defines the needed global constants used by the program.

(ns vibration-sim.constants)

;; screen size
(def ^:const screen-dim-x 300)
(def ^:const screen-dim-y 500)

;; mass size
(def ^:const rect-width 100)
(def ^:const rect-height 70)

;; program runs at 60 frames per second (ideally)
(def ^:const time-interval (/ 1 60))

;; start coordinates of the mass (middle of the screen)
;; since these refer to the lower left corner, we need to subtract
;; from it half the rectangle's dimensions to move it a little to the
;; right and down
(def ^:const mass-start-pos-x (- (/ screen-dim-x 2) (/ rect-width 2)))
(def ^:const mass-start-pos-y (- (/ screen-dim-y 2) (/ rect-height 2)))

;; size of the spawned dots
(def ^:const dot-size 2)

;; distance to move the dots at each step
(def ^:const dot-dx 1)

;; UI elements
;; |_ mass
(def ^:const mass 1.0)
;; |_ spring constant
(def ^:const spring-min-value 1.0)
(def ^:const spring-max-value 5.0)
(def ^:const spring-step 0.1)
(def ^:const spring-format "%.1f")
;; |_ damper constant
(def ^:const damper-min-value 0.0)
(def ^:const damper-max-value 5.0)
(def ^:const damper-step 0.1)
(def ^:const damper-format "%.1f")

;; error in floating-point comparison
(def ^:const float-max-error 0.01)

;; amplitude for the movement equations
(def ^:const amp 100)
