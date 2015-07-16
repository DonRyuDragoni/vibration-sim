;; This file defines the movement equations for the MSD system.
;;
;; Currently, the program works with three functions:
;;     1. no damping -> the system will move untill the end of time
;;     2. low damp   -> there is a damping factor, but its kind of small
;;     3. high damp  -> no oscilation

(ns vibration-sim.math-mov-eq
  (:require [vibration-sim.math-funs :as mfuns]
            [vibration-sim.constants :refer :all]))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with no damping factor
;; contract:
;;     Number -> Number
(defn msd-undamp [t spr-const damp-const]
  (let [amplitude amp]
    (* amplitude (mfuns/sin (* (mfuns/sqrt (/ spr-const mass)) t)))))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with little damping
;; contract:
;;     Number -> Number
(defn msd-low-damp [t spr-const damp-const]
  (let [amplitude amp
        xi (/ damp-const (mfuns/sqrt (* 4 mass spr-const)))
        omega_n (mfuns/sqrt (/ spr-const mass))
        omega_D (* omega_n (mfuns/sqrt (- 1 (mfuns/sqr xi))))]
    (* amplitude (mfuns/euler (* (- xi) omega_n t))
       (mfuns/sin (* omega_D t)))))

;; purpose:
;;     given the point in time, calculare the displacement of the mass,
;;     considering a system with high damping
;; contract:
;;     Number -> Number
(defn msd-high-damp [t spr-const damp-const]
  (let [amplitude1 amp
        amplitude2 amp
        xi (/ damp-const (mfuns/sqrt (* 4 mass spr-const)))
        omega_n (mfuns/sqrt (/ spr-const mass))
        omega_P (* omega_n (mfuns/sqrt (- (mfuns/sqr xi) 1)))]
    (* (mfuns/euler (* (- xi) omega_n t))
       (+ (* amplitude1 (mfuns/euler (* omega_P t)))
          (* amplitude2 (mfuns/euler (* (- omega_P) t)))))))
