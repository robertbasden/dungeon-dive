(ns dungeon-dive.rendering
  (:require
   [dungeon-dive.level :as level]))

(def important-tiles {:blank [0 0]
                      :stairs [21 0]
                      :wall-north [19 0]
                      :wall-north-east [20 0]
                      :wall-east [20 1]
                      :wall-south [19 2]
                      :wall-south-west [18 2]
                      :wall-west [18 1]
                      :wall-north-west [18 0]
                      :wall-middle [0 13]})

(defn lookup-by-bitmask-value
  [value]
  (case value
    0 [0 13]
    3 [20 2]
    5 [18 2]
    7 [19 2]
    10 [20 0]
    11 [20 1]
    12 [18 0]
    13 [18 1]
    14 [19 0]
    [0 0]))

(defn render-wall
  [x y offscreen-ctx tiles bit-mask-value]
  (let [[lx ly] (lookup-by-bitmask-value bit-mask-value)]
    (.drawImage offscreen-ctx tiles (* lx 17) (* ly 17) 16 16 (* x 32) (* y 32) 32 32)))

(defn render-floor
  [x y offscreen-ctx tiles]
  (do
    (.save offscreen-ctx)
    (set! (.-globalAlpha offscreen-ctx) 0.6)
    (.drawImage offscreen-ctx tiles (* 2 17) (* 0 17) 16 16 (* x 32) (* y 32) 32 32)
    (.restore offscreen-ctx)))

(defn render-level
  ""
  [tiles level]
  (let [offscreen-canvas (js/OffscreenCanvas. 1000 1000)
        offscreen-ctx (.getContext offscreen-canvas "2d")]
    (level/do-level level (fn [[x y] e]
                            (if
                             (= e 1)
                              (render-wall x y offscreen-ctx tiles (level/calc-bitmask-value level {:x x :y y}))
                              (render-floor x y offscreen-ctx tiles))))
    offscreen-canvas))