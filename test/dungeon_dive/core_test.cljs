(ns dungeon-dive.core-test
    (:require
     [cljs.test :refer-macros [deftest is testing]]
     [dungeon-dive.core :as core]))

(deftest is-dead?-test
  (is (= false (core/is-dead? {:health 10})))
  (is (= false (core/is-dead? {:health 99})))
  (is (= false (core/is-dead? {:health 1})))
  (is (= true (core/is-dead? {:health 0})))
  (is (= true (core/is-dead? {:health -1}))))