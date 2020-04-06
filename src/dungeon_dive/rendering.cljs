(ns dungeon-dive.rendering
  (:require
   [dungeon-dive.level :as level]
   [dungeon-dive.fov :as fov]))

;; We calculate the bit-mask-value depending on what surrounding tiles are *not* blocked
(defn lookup-by-bitmask-value
  [value]
  (case value
    0 [[0 13]]
    1 [[18 1] [19 2] [20 1]]
    2 [[19 0] [20 1] [19 2]]
    3 [[20 2]]
    4 [[18 1] [19 0] [19 2]]
    5 [[18 2]]
    6 [[19 0] [19 2]]
    7 [[19 2]]
    8 [[18 1] [19 0] [20 1]]
    9 [[18 1] [20 1]]
    10 [[20 0]]
    11 [[20 1]]
    12 [[18 0]]
    13 [[18 1]]
    14 [[19 0]]
    15 []
    [[0 0]]))

(defn render-wall
  [x y offscreen-ctx tiles bit-mask-value]
  (let [tiles-to-draw (lookup-by-bitmask-value bit-mask-value)]
    (doseq [[lx ly] tiles-to-draw]
      (.drawImage offscreen-ctx tiles (* lx 17) (* ly 17) 16 16 (* x 32) (* y 32) 32 32))))

(defn render-wall-debug
  [x y offscreen-ctx tiles bit-mask-value]
  (let [tiles-to-draw (lookup-by-bitmask-value bit-mask-value)]
    (doseq [[lx ly] tiles-to-draw]
      (set! (.-globalAlpha offscreen-ctx) 0.5)
      (.drawImage offscreen-ctx tiles (* lx 17) (* ly 17) 16 16 (* x 32) (* y 32) 32 32)
      (set! (.-globalAlpha offscreen-ctx) 1)
      (.fillText offscreen-ctx bit-mask-value (+ (* x 32) 8) (+ (* y 32) 16)))))

(defn draw-segment
  [offscreen-ctx {:keys [x y width height]}]
  (let [tile-size 32
        random-color (str "rgba(" (rand-int 255) "," (rand-int 255) "," (rand-int 255) ",0.5)")]
    (do 
      (.save offscreen-ctx)
      (set! (.-fillStyle offscreen-ctx) random-color)
      (.fillRect offscreen-ctx (* x tile-size) (* y tile-size) (* width tile-size) (* height tile-size))
      (.restore offscreen-ctx))))

(defn draw-bsp
  [offscreen-ctx bsp]
  (let [leaves (filter (fn [s] (nil? (:children s))) (tree-seq map? :children bsp))]
    (do
      (.save offscreen-ctx)
      (set! (.-globalAlpha offscreen-ctx) 0.5)
      (doseq [segment leaves]
        (draw-segment offscreen-ctx segment))
      (set! (.-globalAlpha offscreen-ctx) 1)
      (set! (.-strokeStyle offscreen-ctx) "#FFF")
      (.strokeRect offscreen-ctx 0 0 960 960)
      (.restore offscreen-ctx))))

(defn draw-connection
  [offscreen-ctx {:keys [sx sy ex ey]}]
  (do
    (.save offscreen-ctx)
    (set! (.-strokeStyle offscreen-ctx) "#61a601")
    (set! (.-lineWidth offscreen-ctx) 2)
    (.beginPath offscreen-ctx)
    (.moveTo offscreen-ctx (+ 16 (* 32 sx)) (+ 16 (* 32 sy)))
    (.lineTo offscreen-ctx (+ 16 (* 32 ex)) (+ 16 (* 32 ey)))
    (.stroke offscreen-ctx)
    (.restore offscreen-ctx)))

(defn draw-connections
  [offscreen-ctx bsp]
  (doseq [connection (:connections bsp)]
    (draw-connection offscreen-ctx connection)))

(defn render-floor
  [x y offscreen-ctx tiles]
  (do
    (.save offscreen-ctx)
    (set! (.-globalAlpha offscreen-ctx) 0.6)
    (.drawImage offscreen-ctx tiles (* 2 17) (* 0 17) 16 16 (* x 32) (* y 32) 32 32)
    (.restore offscreen-ctx)))

(defn draw-fov-line
  [ctx {:keys [sx sy ex ey]}]
  (do
    (.beginPath ctx)
    (.moveTo ctx sx sy)
    (.lineTo ctx ex ey)
    (.stroke ctx)))

(defn draw-fov-lines
  [ctx lines]
  (.save ctx)
  (set! (.-strokeStyle ctx) "#61a601")
  (set! (.-lineWidth ctx) 2)
  (set! (.-globalAlpha ctx) 0.6)
  (doseq [line lines]
    (draw-fov-line ctx line))
  (.restore ctx))

(defn draw-enemy
  [ctx tiles {:keys [x y max-health health]}]
  (let [health-remaining (/ health max-health)
        bar-width (* 32 health-remaining)]
    (do
      (.save ctx)
      (.drawImage ctx tiles (* 28 17) (* 6 17) 16 16 (* x 32) (* y 32) 32 32)
      (set! (.-fillStyle ctx) "#000")
      (.fillRect ctx (- (* x 32) 2) (+ (* y 32) 32) 36 8)
      (set! (.-fillStyle ctx) "#1e8000")
      (.fillRect ctx (* x 32) (+ (* y 32) 34) bar-width 4)
      (.restore ctx))))

(defn draw-enemies
  [ctx tiles enemies]
  (doseq [enemy enemies]
    (draw-enemy ctx tiles enemy)))

(defn draw-stairs
  [ctx tiles stairs]
  (doseq [{:keys [x y]} stairs]
    (.drawImage ctx tiles (* 21 17) (* 0 17) 16 16 (* x 32) (* y 32) 32 32)))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn render-level
  ""
  [tiles level bsp player-position enemies stairs fov]
  (let [offscreen-canvas (js/OffscreenCanvas. 1000 1000)
        offscreen-ctx (.getContext offscreen-canvas "2d")
        fov-canvas (js/OffscreenCanvas. 1000 1000)
        fov-ctx (.getContext fov-canvas "2d")]
    (set! (.-fillStyle offscreen-ctx) "#b9db6d")
    (set! (.-font offscreen-ctx) "12px Arial")
    (level/do-level level (fn [[x y] e]
                            (if
                             (= e 1)
                              (render-wall x y offscreen-ctx tiles (level/calc-bitmask-value level {:x x :y y}))
                              (render-floor x y offscreen-ctx tiles))))
    ;; FOV
    (set! (.-fillStyle fov-ctx) "rgba(0,0,0,1)")
    (.fillRect fov-ctx 0 0 1000 1000)
    ;; (draw-fov-lines fov-ctx fov-lines)
    (doseq [{:keys [x y]} fov]
      (.clearRect fov-ctx (- (* x 32) 10) (- (* y 32) 10) 52 52))
    (.drawImage offscreen-ctx fov-canvas 0 0)
    (draw-enemies offscreen-ctx tiles (filter (fn [{:keys [x y]}]
                                                (in? fov {:x x :y y})) enemies))
    (draw-stairs offscreen-ctx tiles (filter (fn [{:keys [x y]}]
                                               (in? fov {:x x :y y})) [stairs]))
    ;; (draw-bsp offscreen-ctx bsp)
    ;; (draw-connections offscreen-ctx bsp)
    offscreen-canvas))