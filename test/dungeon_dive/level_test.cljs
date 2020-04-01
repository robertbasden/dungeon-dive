(ns dungeon-dive.level-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]
   [dungeon-dive.level :as level]))

(deftest split-direction-test
  (let [direction (level/split-direction)]
    (is (or (= direction :horizontal)
            (= direction :vertical)))))