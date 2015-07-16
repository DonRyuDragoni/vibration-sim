;; This is the core of the program, where the magic happens.

(ns vibration-sim.core
  (:require [vibration-sim.constants :refer :all]
            [vibration-sim.math-mov-eq :refer :all]
            [vibration-sim.math-funs :refer [sqrt float-=]]
            [com.climate.claypoole :as cp]
            [play-clj.core :refer :all]
            [play-clj.math :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all])
  (:import [com.badlogic.gdx.scenes.scene2d.ui]))

;; === References ===

;; time-counter (starts at 0s and is updated at :on-timer)
(def time-test (ref 0))
;; constants for the spring and damper
(def spring-constant (ref 1.0))
(def damper-constant (ref 0.0))
;; |_ temporary variables
(def tmp-spring-const (ref @spring-constant))
(def tmp-damper-const (ref @damper-constant))

;; === Threadpool ===

;; threadpool with n threads (n = cpus on the machine)
;; daemon ensures the pool will close on program closing
(def pool (cp/threadpool (cp/ncpus) :daemon true))
(def test-pool (cp/threadpool 1 :daemon true))

;; === Auxiliary functions ===

;; purpose:
;;     exits the program, forcing the threadpool to be killed
;; contract:
;;     nil -> nil
(defn- end-game []
  (cp/shutdown! pool) ;; force threadpool kill
  (cp/shutdown! test-pool) ;; force threadpool kill
  (java.lang.System/exit 0))

;; purpose:
;;     given an entity, if it is the mass, move it
;; contract:
;;     Function (ListOf HashMap) -> (ListOf HashMap)
(defn- move-mass [mov_eq entities]
  (doall
   (cp/pfor pool [{:keys [mass?] :as entity} entities]
            (if mass?
              (let [old-y (:y entity)
                    new-y (+ mass-start-pos-y (mov_eq @time-test @spring-constant @damper-constant))]
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
;;     find the movement type based on the damper constant
;; contract:
;;     nil -> Symbol
(defn- find-movement-type []
  (let [xi (/ @damper-constant (sqrt (* 4 mass @spring-constant)))]
    (cond
      (float-= @damper-constant 0.0) :no-damp
      (< xi 1.0) :low-damp
      (>= xi 1.0) :high-damp
      :else (throw (Exception. "Unexpected relation in the constants.")))))

;; purpose:
;;     given a vector of entities, map the movement functions to each element
;; contract:
;;     (ListOf HashMap) -> (ListOf HashMap)
(defn- move [entities]
  (let [mov_eq (case (find-movement-type)
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
;;     resets the game to initial state
;; contract:
;;     (ListOf HashMap) (ListOf HashMap) -> (ListOf HashMap) (ListOf HashMap)
(defn- reset-game [entities]
  (dosync (ref-set time-test 0))
  (remove-all-dots entities))

;; UI actions
;; |_ buttons
(defn button-action [b entities]
  (case (text-button! b :get-name)
    "apply" (dosync (ref-set spring-constant @tmp-spring-const)
                    (ref-set damper-constant @tmp-damper-const))
    (throw (Exception. "Unknown button name.")))
  (reset-game entities))
;; |_ sliders
(defn slider-action [sl entities]
  ;; there are two sliders and each refer to direrent things
  (case (slider! sl :get-name)
    "spring" (do
               ;; update the temporary variable...
               (dosync (ref-set tmp-spring-const (slider! sl :get-value)))
               ;; ... and the label
               (doall
                (cp/pfor test-pool [{:keys [table?] :as entity} entities
                                    cell (if table?
                                           (table! entity :get-cells)
                                           nil)]
                         (if cell
                           (let [actor (.getActor cell)]
                             (if (and (label? actor) (= (label! actor :get-name) "spring-label"))
                               (label! actor :set-text (format (str "k = " spring-format) (float @tmp-spring-const)))
                               nil))
                           nil)))
               entities)
    "damper" (do
               ;; again, update the temporary variable...
               (dosync (ref-set tmp-damper-const (slider! sl :get-value)))
               ;; ... and the label
               (doall
                (cp/pfor test-pool [{:keys [table?] :as entity} entities
                                    cell (if table?
                                           (table! entity :get-cells)
                                           nil)]
                         (if cell
                           (let [actor (.getActor cell)]
                             (if (and (label? actor) (= (label! actor :get-name) "damper-label"))
                               (label! actor :set-text (format (str "c = " damper-format) (float @tmp-damper-const)))
                               nil))
                           nil)))
               entities)
    ;; and, just in case
    (throw (Exception. "Unknown slider name.")))
  entities)

(defn debug-program [screen entities]
  entities)

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
          ;; create the table
          tbl (table [;; label to identify the element
                      (label (str "k = " (format spring-format spring-min-value))
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
                      (label (str "c = " (format damper-format damper-min-value))
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
                      (text-button "Apply" ui-skin :set-name "apply")])
          ;; and assoc it with its position
          table (assoc tbl
                       :table? true
                       :x (- screen-dim-x (/ (table! tbl :get-min-width) 2))
                       :y (- screen-dim-y (/ (table! tbl :get-min-height) 2)))]
      [mass time-count table]))

  :on-ui-changed
  (fn [screen entities]
    (let [actor (:actor screen)]
      (cond
        (text-button? actor) (do (button-action actor entities))
        (slider? actor) (do (slider-action actor entities))
        :else (throw (Exception. "Unknown actor change.")))))

  :on-key-down
  (fn [screen entities]
    (cond
      (key-pressed? :q) (end-game))
    ;; every time a key is pressed, restart the animation
    (reset-game entities))
  
  :on-render
  (fn [screen entities]
    (debug-program screen entities)
    (clear!)
    (render! screen entities))

  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :update-time (do
                     (update-time)
                     (-> entities
                         update-label
                         move))
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
