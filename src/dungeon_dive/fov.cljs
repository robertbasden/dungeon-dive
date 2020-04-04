(ns dungeon-dive.fov
  (:require
   [dungeon-dive.level :as level]
   [clojure.set :as set]))

(def map-size 30)

(defn create-fov-map
  []
  (vec (map (fn [] (vec (take map-size (repeat 0)))) (take map-size (repeat 0)))))

(defn set-value
  [collection x y value]
  (let [row (get collection x)
        new-row (assoc row y value)]
    (assoc collection x new-row)))

(defn calc-lines 
  [{:keys [x y]} distance]
  (map (fn [deg]
         (let [px-x (+ 16 (* x 32))
               px-y (+ 16 (* y 32))
               x-component (* distance (Math/cos (* deg 0.0174533)))
               y-component (* distance (Math/sin (* deg 0.0174533)))
               ex (+ px-x x-component)
               ey (+ px-y y-component)]
           {:sx px-x :sy px-y :ex ex :ey ey})
         ) (range 0 1)))

;; take a square out of the map representing the possible viewable map squares
;; for each line calc the squares on the line - these are the possibly visible squares
;; move through squares one at time cons'ing them onto th actual visable squares, once you hit a block return
;; merge and de-dup all the above
;; done!

(defn calc-fov
  [{:keys [x y]} level-data distance]
  (let [sx (+ 16 (* x 32))
        sy (+ 16 (* y 32))
        m (map (fn [deg]
                 (let [x-component (Math/cos (* deg 0.0174533))
                       y-component (Math/sin (* deg 0.0174533))]
                   (:d (reduce (fn [acc i]
                                 (let [px (+ sx (* x-component i))
                                       py (+ sy (* y-component i))
                                       tx (Math/floor (/ px 32))
                                       ty (Math/floor (/ py 32))
                                       tile-blocked (level/is-blocked? level-data {:x tx :y ty})]
                                   (if (or 
                                        (:stop acc)
                                        tile-blocked)
                                     (assoc acc :stop true)
                                     (assoc acc :d (conj (:d acc) {:x tx :y ty}))))
                                 ){:stop false :d #{}} (range 0 distance 5))))
                 ) (range 0 360 5))
        f (apply set/union m)]
    (vec f)))

(defn do-fov
  "Call the provided fn for each position on the map, usually we would use this for some side effects like drawing"
  [collection func]
  (doseq [[x row] (map-indexed vector collection)]
    (doseq [[y e] (map-indexed vector row)]
      (func [x y] e))))

;;sin t = o/h