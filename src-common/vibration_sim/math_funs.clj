;; This file defines simple macros to warp java math functions.

(ns vibration-sim.math-funs
  (:require [vibration-sim.constants :as cnst]))

;; === Macros ===
;; purpose:
;;     calculate the sine of ang (given in radians)
;; contract:
;;     Number -> Number
(defn sin [ang]
  (Math/sin ang))

;; purpose:
;;     calculate the square root of a number
;; contract:
;;     NonNegativeNumber -> Number
(defn sqrt [x]
  (let [r (Math/sqrt x)]
    (if (Double/isNaN r)
      (throw (Exception. "math_funs/sqrt returned NaN"))
      r)))

;; purpose:
;;     calculate the x to the nth power
;; contract:
;;     Number Number -> Number
(defn pow [x n]
  (Math/pow x n))

;; purpose:
;;     calculate the square of a number
;; contract:
;;     Number -> Number
(defn sqr [x]
  (* x x))

;; purpose:
;;     calculate the euler number to the nth power
;; contract:
;;     Number -> Number
(defn euler [n]
  (Math/exp n))

;; purpose:
;;     compare two floating-point numbers, allowing some error
;; contract:
;;     Number Number -> Boolean
(defn float-= [num1 num2]
  (and (<= (- num1 cnst/float-max-error) num2)
       (<= num2 (+ num1 cnst/float-max-error))))
