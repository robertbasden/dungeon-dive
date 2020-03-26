(ns dungeon-dive.level)

(defn generate
  "Generate a new level"
  []
  [[1 1 1 1 1]
   [1 0 1 1 1]
   [1 0 1 1 1]
   [1 0 0 0 1]
   [1 0 1 0 1]
   [1 0 0 0 1]
   [1 1 0 1 1]
   [1 1 0 1 1]
   [1 1 0 0 1]
   [1 1 1 0 1]
   [1 1 1 1 1]])

(defn north
  "Get the position to the north of the supplied one"
  [{:keys [x y]}]
  {:x x :y (- y 1)})

(defn east
  "Get the position to the north of the supplied one"
  [{:keys [x y]}]
  {:x (+ x 1) :y y})

(defn south
  "Get the position to the south of the supplied one"
  [{:keys [x y]}]
  {:x x :y (+ y 1)})

(defn west
  "Get the position to the north of the supplied one"
  [{:keys [x y]}]
  {:x (- x 1) :y y})

(defn get-tile
  "Get the value of a tile at a certain position, supply a default that can be used in case the position is out of bounds"
  ([col position] (get-tile col position nil))
  ([col {:keys [x y]} default-value]
   (try
     (nth (nth col x) y)
     (catch js/Error _
       default-value))))

(defn is-blocked?
  "Check is a specific position is blocked (play/monsters can't pass through)"
  [collection position]
  (case (get-tile collection position 1)
        0 false
        1 true))

(defn positions-are-equal? [{:keys [x y]} {cx :x cy :y}]
  (and (= x cx) (= y cy)))

(defn calc-bitmask-value
  [collection position]
  (let
   [north-tile (get-tile collection (north position) 1)
    east-tile (get-tile collection (east position) 1)
    south-tile (get-tile collection (south position) 1)
    west-tile (get-tile collection (west position) 1)
    north-modifier (if (= north-tile 1) 1 0)
    east-modifier (if (= east-tile 1) 4 0)
    south-modifier (if (= south-tile 1) 8 0)
    west-modifier (if (= west-tile 1) 2 0)]
    (+ north-modifier east-modifier south-modifier west-modifier)))

(defn do-level
  "Call the provided fn for each position on the map, usually we would use this for some side effects like drawing"
  [collection func]
  (doseq [[x row] (map-indexed vector collection)]
    (doseq [[y e] (map-indexed vector row)]
      (func [x y] e))))