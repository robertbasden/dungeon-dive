(ns dungeon-dive.level)

;;This is used as the minimum limit of the respective side where we stop splitting when splitting to create new segments
;;So for example if we set this to 4 and then we try and split a segment vertically whose height is less than 4, then we
;;don't and we stop
(def split-limit 8)

(def map-size 15)

(defn split-direction
  "Decide a random direction to split a segment"
  []
  (if (> (rand) 0.5)
    :horizontal
    :vertical))

(defn split-value
  "Returns a random integer between min and n (inclusive of both)"
  [min max]
  (+ min (rand-int (+ max 1))))

(defn random-int-between
  [min max]
  (rand-nth (range min (+ max 1))))

(defn split-segment
  [{:keys [x y width height]} direction]
  (case direction
    :horizontal (do
                  (if (>= height split-limit)
                    (let [v (+ (Math/floor (/ height 2)) (split-value -1 1))]
                      [{:x x :y y :width width :height v}
                       {:x x :y (+ y v) :width width :height (- height v)}])
                    nil))
    :vertical (do
                (if (>= width split-limit)
                  (let [v (+ (Math/floor (/ width 2)) (split-value -1 1))]
                    [{:x x :y y :width v :height height}
                     {:x (+ x v) :y y :width (- width v) :height height}])
                  nil))))


(defn old-segment->room
  [{:keys [x y width height]}]
  (let [min-width (max (- (Math/floor (/ width 2)) 1) 2)
        max-width (- width 3)
        min-height (max (- (Math/floor (/ height 2)) 1) 2)
        max-height (- height 3)
        room-width (random-int-between min-width max-width)
        room-height (random-int-between min-height max-height)
        min-x (+ x 1)
        max-x (+ (- (- width 2) room-width) x)
        min-y (+ y 1)
        max-y (+ (- (- height 2) room-height) y)
        room-x (random-int-between min-x max-x)
        room-y (random-int-between min-y max-y)]
    {:x (+ x 1)
     :y (+ y 1)
     :width (- width 3)
     :height (- height 3)}))

(defn segment->room
  [{:keys [x y width height]}]
  {:x (+ x 1)
   :y (+ y 1)
   :width (- width 3)
   :height (- height 3)})

(defn process-bsp
  [segment]
  (let [direction (split-direction)
        children (split-segment segment direction)]
    (if (nil? children)
      (assoc segment
             :room (segment->room segment))
      (assoc segment
             :children (map process-bsp children)
             :direction direction))))

(defn midpoint
  [{:keys [x y width height]}]
  {:x (Math/floor (+ x (/ width 2))) :y (Math/floor (+ y (/ height 2)))})

(defn add-connections
  [bsp]
  (let [pairs (remove nil? (map :children (tree-seq map? :children bsp)))
        connections (map (fn [[left-segment right-segment]]
                           (let [{sx :x sy :y} (midpoint left-segment)
                                 {ex :x ey :y} (midpoint right-segment)]
                             {:sx sx :sy sy :ex ex :ey ey})) pairs)]
    (assoc bsp :connections connections)))

(defn create-bsp
  "Create a BSP tree by splitting down leaf nodes (with some random variation of width / direction)
  until the segments can not be split down any further"
  [width height]
  (add-connections (process-bsp {:x 0 :y 0 :width width :height height})))

(defn get-leaves
  [bsp]
  (filter (fn [s] (nil? (:children s))) (tree-seq map? :children bsp)))

(defn bsp->rooms
  "Given a BSP tree created from the functions above pull out the rooms that are present in the leaf segments"
  [bsp]
  (let [leaves (filter (fn [s] (nil? (:children s))) (tree-seq map? :children bsp))]
    (map :room leaves)))

(defn map-level-data [func col]
  (map-indexed (fn [y row]
                 (map-indexed (fn [x e]
                                (func [x y] e))
                              row)) col))

(defn carve-connection
  [level-data {:keys [sx sy ex ey]}]
  (map-level-data (fn [[ty tx] current]
                    (if
                     (and
                      (<= sx tx ex)
                      (<= sy ty ey))
                      0
                      current)) level-data))

(defn carve-connections
  [level-data connections]
  (reduce carve-connection level-data connections))

(defn carve-room
  [level-data {:keys [x y width height]}]
  (map-level-data (fn [[ty tx] current]
                    (if
                     (and
                      (<= x tx (+ x width))
                      (<= y ty (+ y height)))
                      0
                      current)
                    ) level-data))

(defn carve-rooms
  [rooms level-data]
  (reduce carve-room level-data rooms))

(defn empty-level-data
  []
  (vec (map (fn [] (vec (take map-size (repeat 1)))) (take map-size (repeat 1)))))

(defn generate
  "Generate a new level"
  []
  (let [bsp (create-bsp map-size map-size)
        level-data (carve-connections (carve-rooms (bsp->rooms bsp) (empty-level-data)) (:connections bsp))]
    {:bsp bsp
     :map-data level-data
     :enemies [{:id (random-uuid) :x 5 :y 2 :name "orc" :max-health 100 :health 20}
               {:id (random-uuid) :x 3 :y 3 :name "orc" :max-health 100 :health 70}
               {:id (random-uuid) :x 8 :y 2 :name "orc" :max-health 100 :health 80}]}))

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