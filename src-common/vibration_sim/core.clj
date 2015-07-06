(ns vibration-sim.core
  (:require [com.climate.claypoole :as cp]
            [play-clj.core :refer :all]
            [play-clj.math :refer :all]
            [play-clj.g2d :refer :all]
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
;; |_ spring constant
(def ^:const spring-min-value 1)
(def ^:const spring-max-value 10)
(def ^:const spring-step 0.1)
;; |_ damper constant
(def ^:const damper-min-value 1)
(def ^:const damper-max-value 10)
(def ^:const damper-step 0.1)
;; error in floating-point comparison
(def ^:const float-max-error 0.01)

;; === References ===
;; time-counter (starts at 0s and is updated at :on-timer)
(def time-test (ref 0))
;; oscilation of the mass (starts with none)
(def movement-type (ref :do-not-move))
;; constants for the spring and damper
(def spring-constant (ref 1))
(def damper-constant (ref 0))
;; |_ temporary variables
(def tmp-spring-const (ref 0.1))
(def tmp-damper-const (ref 0.0))

;; === Threadpool ===
;; threadpool with n threads (n = cpus on the machine)
;; daemon ensures the pool will close on program closing
(def pool (cp/threadpool (cp/ncpus) :daemon true))

;; === Macros ===
;; purpose:
;;     calculate the sine of ang (given in radians)
;; contract:
;;     Number -> Number
(defmacro sin [ang]
  `(Math/sin ~ang))

;; purpose:
;;     calculate the square root of a number
;; contract:
;;     NonNegativeNumber -> Number
(defmacro sqrt [x]
  `(Math/sqrt ~x))

;; purpose:
;;     calculate the x to the nth power
;; contract:
;;     Number Number -> Number
(defmacro pow [x n]
  `(Math/pow ~x ~n))

;; purpose:
;;     calculate the square of a number
;; contract:
;;     Number -> Number
(defmacro sqr [x]
  `(* ~x ~x))

;; purpose:
;;     calculate the euler number to the nth power
;; contract:
;;     Number -> Number
(defmacro euler [n]
  `(Math/exp ~n))

;; === Auxiliary functions ===

;; purpose:
;;     exits the program, forcing the threadpool to be killed
;; contract:
;;     nil -> nil
(defn- end-game []
  (cp/shutdown! pool) ;; force threadpool kill
  (java.lang.System/exit 0))

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
;;     Function (ListOf HashMap) -> (ListOf HashMap)
(defn- move-mass [mov_eq entities]
  (doall
   (cp/pfor pool [{:keys [mass?] :as entity} entities]
            (if mass?
              (let [old-y (:y entity)
                    new-y (+ mass-start-pos-y (mov_eq @time-test))]
                (assoc entity :y new-y))
              entity))))

;; purpose:
;;     given an entity, if it is one of the dots, move it a little bit to
;;     the right
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- move-dots [entities]
  ;; doall to force all computations before return
  (doall
   (cp/pfor pool [{:keys [dots?] :as entity} entities]
            (if dots?
              (let [old-x (:x entity)
                    new-x (- old-x dot-dx)]
                (assoc entity :x new-x))
              entity))))

;; purpose:
;;     given an entity, if it is one of the dots and it passed the left
;;     "barrier", remove it from the list
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- remove-dots [entities]
  (remove #(and (:dots? %) (< (:x %) 10)) entities))

;; purpose:
;;     given all the entities, remove all the dots from the "game"
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- remove-all-dots [entities]
  (remove #(:dots? %) entities))

;; purpose:
;;     given a vector of entities, map the movement functions to each element
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- move [entities mov-type]
  (let [mov_eq (case mov-type
                 :no-damp msd-undamp
                 :low-damp msd-low-damp
                 :high-damp msd-high-damp
                 :do-not-move (fn [_] 0)
                 (throw (Exception. "Unexpected movement type.")))]
    (->>
     entities
     (move-mass mov_eq)
     move-dots
     remove-dots)))
;; TODO: change the above function to calculate the movement function based on omega_n
;; (defn- move2 [entities]
;;   (let [omega_n = (sqrt (/ @spring-constant m))
;;         mov_eq (case omega_n
;;                  :no-damp msd-undamp
;;                  :low-damp msd-low-damp
;;                  :high-damp msd-high-damp
;;                  :do-not-move (fn [_] 0)
;;                  (throw (Exception. "Unexpected movement type.")))]
;;     (->>
;;      entities
;;      (move-mass mov_eq)
;;      move-dots
;;      remove-dots)))

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

;; purpose:
;;     updates the timer label at the bottom of the screen
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- update-label [entities]
  (for [{:keys [timer-label?] :as entity} entities]
    (if timer-label?
      (doto entity
        (label! :set-text (format "t = %.1f" (float @time-test))))
      entity)))

;; purpose:
;;     spawns a dot marking the current position of the mass
;; contract:
;;     (ListOf HashMap) -> HashMap
(defn- spawn-dot [entities]
  (for [{:keys [mass?] :as entity} entities]
    (if mass?
      (assoc (shape :filled
                    :set-color (color :blue)
                    :circle 0 0 dot-size)
             :dots? true
             :x (+ (:x entity) (/ rect-width 2))
             :y (+ (:y entity) (/ rect-height 2))))))

;; purpose:
;;     compare two floating-point numbers, allowing some error
;; contract:
;;     Number Number -> Boolean
(defn float-== [num1 num2]
  (and (<= (- num1 float-max-error) num2)
       (<= num2 (+ num1 float-max-error))))

;; UI actions
;; |_ buttons
(defn button-action [b]
  ;; there is only the "Apply" button, so no need to check
  ;; (note that the entities are managed in the main-screen :on-ui-changed function, not here!)
  (dosync (ref-set spring-constant @tmp-spring-const)
          (ref-set damper-constant @tmp-damper-const)))
;; |_ sliders
(defn slider-action [sl]
  ;; there are two sliders and each refer to direrent things
  (case (slider! sl :get-name)
    "spring" (dosync (ref-set tmp-spring-const (slider! sl :get-value))) ;; slider for the spring
    "damper" (dosync (ref-set tmp-damper-const (slider! sl :get-value))) ;; slider for the damper
    (throw (Exception. "Unknown slider name."))))                        ;; just in case

;; TODO: PROGRAM IS GETTING SLOWER
;; number of entities are increasing if we use the slider
;; dunno what is causing it
;; => tries failed: :dots?
;;                  :table?
(defn test-entities [screen entities]
  (println (count (filter #(:dots? %) entities))))

;; === Game screens ===
(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (stage)
             :camera (orthographic))
    (add-timer! screen :update-time 0 time-interval)
    (add-timer! screen :spawn-dots 0 time-interval)
    (let [mass (assoc (shape :filled
                             :set-color (color :green)
                             ;; start at point 0 0 and move with :x and :y
                             :rect 0 0 rect-width rect-height)
                      :mass? true
                      :x mass-start-pos-x
                      :y mass-start-pos-y)
          time-count (assoc (label (str @time-test) (color :white))
                            :timer-label? true
                            :x 5)
          ;; skin for the UI elements
          ui-skin (skin "uiskin.json")
          table (assoc (table [;; label to identify the element
                               (label (str "k = " spring-min-value)
                                      ui-skin
                                      :set-name "spring-label")
                               :row
                               ;; slider for user input
                               (slider {:min spring-min-value
                                        :max spring-max-value
                                        :step spring-step
                                        :vertical? false}
                                       ui-skin
                                       :set-name "spring") ;; name the slider to be able to tell the diference!
                               :row
                               ;; another label...
                               (label (str "c = " damper-min-value)
                                      ui-skin
                                      :set-name "damper-label")
                               :row
                               ;; and another slider
                               (slider {:min damper-min-value
                                        :max damper-max-value
                                        :step damper-step
                                        :vertical? false}
                                       ui-skin
                                       :set-name "damper") ;; again, another name
                               :row
                               (text-button "Apply!" ui-skin)])
                       :table? true
                       :x 100
                       :y 200)]
      [mass time-count table]))

  :on-ui-changed
  (fn [screen entities]
    (let [actor (:actor screen)]
      (cond
        (text-button? actor) (do (button-action actor)
                                 (reset-time)
                                 [screen (remove-all-dots entities)])
        (slider? actor) (do (slider-action actor)
                            [screen (remove-dots entities)])
        :else (throw (Exception. "Unknown actor change.")))))

  :on-key-down
  (fn [screen entities]
    (cond
      (key-pressed? :q) (end-game)
      ;; no damping: system oscilates forever
      (key-pressed? :n) (dosync (ref-set movement-type :no-damp))
      ;; low damping: system oscilates losing energy
      (key-pressed? :l) (dosync (ref-set movement-type :low-damp))
      ;; high damping: system do not oscilate
      (key-pressed? :h) (dosync (ref-set movement-type :high-damp))
      ;; reset the system to initial position
      (key-pressed? :r) (dosync (ref-set movement-type :do-not-move)))
    ;; evey time a key is pressed, reset the time...
    (reset-time)
    ;; ... and remove all the dots
    [screen (remove-all-dots entities)])
  
  :on-render
  (fn [screen entities]
    (test-entities screen entities)
    (clear!)
    (render! screen entities))

  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :update-time (do
                     (update-time)
                     (-> entities
                         update-label
                         (move @movement-type)))
      :spawn-dots (conj entities (spawn-dot entities))
      nil))
  
  :on-resize
  (fn [screen entities]
    (height! screen screen-dim-y)))

;; === Game ===
(defgame vibration-sim-game
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
