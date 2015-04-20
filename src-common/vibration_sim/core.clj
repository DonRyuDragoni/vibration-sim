(ns vibration-sim.core
  (:require [play-clj.core :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]))

;; === Constants ===
;; screen size
(def ^:const screen-dim-x 300)
(def ^:const screen-dim-y 500)
;; mass size
(def ^:const rect-width 100)
(def ^:const rect-height 70)
;; program runs at 60 frames per second (ideally)
(def ^:const time-interval (/ 1 60))
;; start coordinates of the mass (middle of the screen)
;; since these refer to the lower left corner, 
(def ^:const mass-start-pos-x (- (/ screen-dim-x 2) (/ rect-width 2)))
(def ^:const mass-start-pos-y (- (/ screen-dim-y 2) (/ rect-height 2)))

;; time-counter (starts at 0s and is updated at :on-timer)
(def time-test (ref 0))
;; oscilation of the mass (starts with none)
(def movement-type (ref :do-not-move))

;; purpose:
;;     calculate the sine of ang (given in radians)
;; contract:
;;     Number -> Number
(defn- sin [ang]
  (Math/sin ang))

;; purpose:
;;     calculate the square root of a number
;; contract:
;;     NonNegativeNumber -> Number
(defn- sqrt [x]
  (Math/sqrt x))

;; purpose:
;;     calculate the x to the nth power
;; contract:
;;     Number Number -> Number
(defn- pow [x n]
  (Math/pow x n))

;; purpose:
;;     calculate the square of a number
;; contract:
;;     Number -> Number
(defn- sqr [x]
  (* x x))

;; purpose:
;;     calculate the euler number to the nth power
;; contract:
;;     Number -> Number
(defn- euler [n]
  (Math/exp n))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with no damping factor
;; contract:
;;     Number -> Number
(defn- msd-undamp [t]
  (let [amplitude 130
        spr-const 1
        mass 0.1]
    (* amplitude (sin (* (sqrt (/ spr-const mass)) t)))))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with little damping
;; contract:
;;     Number -> Number
(defn- msd-low-damp [t]
  (let [amplitude 150
        spr-const 1
        damp-const 0.1
        mass 0.1
        omega_n (sqrt (/ spr-const mass))
        omega_D (* omega_n (sqrt (- 1 (sqr damp-const))))]
    (* amplitude (euler (* (- damp-const) omega_n t)) (sin (* omega_D t)))))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with high damping
;; contract:
;;     Number -> Number
(defn- msd-high-damp [t]
  (let [amplitude1 50
        amplitude2 80
        spr-const 1
        damp-const 1.1
        mass 0.1
        omega_n (sqrt (/ spr-const mass))
        omega_P (* omega_n (sqrt (- (sqr damp-const) 1)))]
    (* (euler (* (- damp-const) omega_n t)) (+ (* amplitude1 (euler (* omega_P t))) (* amplitude2 (euler (* (- omega_P) t)))))))

;; purpose:
;;     given an entity, if it is the mass, move it
;; contract:
;;     HashMap -> HashMap
(defn- move-mass [mov_eq {:keys [mass?] :as entity}]
  (if mass?
    (let [old-y (:y entity)
          new-y (+ mass-start-pos-y (mov_eq @time-test))]
      (assoc entity :y new-y))
    entity))

;; purpose:
;;     given a vector of entities, map the movement function to each element
;; contract:
;;     Sequence -> Sequence
(defn- move [entities mov-type]
  (let [mov_eq (case mov-type
                 :no-damp msd-undamp
                 :low-damp msd-low-damp
                 :high-damp msd-high-damp
                 :do-not-move (fn [_] 0)
                 (throw (Exception. "Unexpected movement type.")))]
    (map #(move-mass mov_eq %) entities)))


;; purpose:
;;     resets the timer
;; contract:
;;     nil -> nil
(defn- reset-time []
  (dosync (ref-set time-test 0))
  nil)

;; purpose:
;;     updates the time counter by one unit of time-interval
;; contract:
;;     nil -> nil
(defn- update-time []
  (dosync (ref-set time-test (+ @time-test time-interval)))
  nil)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (stage)
             :camera (orthographic))
    (add-timer! screen :update-time 0 time-interval)
    (let [mass (assoc (shape :filled
                             :set-color (color :green)
                             ;; start at point 0 0 and move with :x and :y
                             :rect 0 0 rect-width rect-height)
                      :mass? true
                      :x mass-start-pos-x
                      :y mass-start-pos-y)
          time-count (assoc (label (str @time-test) (color :white)) :x 5)]
      [mass time-count]))

  :on-key-down
  (fn [screen entities]
    (cond
      ;; no damping: system oscilates forever
      (key-pressed? :n) (dosync (ref-set movement-type :no-damp))
      ;; low damping: system oscilates losing energy
      (key-pressed? :l) (dosync (ref-set movement-type :low-damp))
      ;; high damping: system do not oscilate
      (key-pressed? :h) (dosync (ref-set movement-type :high-damp))
      ;; reset the system to initial position
      (key-pressed? :r) (dosync (ref-set movement-type :do-not-move)))
    ;; evey time a key is pressed, reset the time
    (reset-time)
    [screen entities])
  
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))

  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :update-time (do
                     (update-time)
                     (move entities @movement-type))))

  :on-resize
  (fn [screen entities]
    (height! screen screen-dim-y)))

(defgame vibration-sim-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
