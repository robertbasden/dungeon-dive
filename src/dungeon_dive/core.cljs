(ns ^:figwheel-hooks dungeon-dive.core
  (:require
   [goog.dom :as gdom]
   [cljs.reader :as reader]
   [reagent.core :as reagent :refer [atom]]
   [dungeon-dive.level :as level]
   [dungeon-dive.rendering :as rendering]
   [dungeon-dive.localstorage :as localstorage]))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:current-screen :title
                          :game nil}))

(def localstorage-key "dungeon-dive")

;; Main navigation

(defn new-game []
  (let [{:keys [map-data bsp enemies]} (level/generate)]
    (swap! app-state assoc :current-screen :game
           :game {:floor 1
                  :level map-data
                  :bsp bsp
                  :enemies enemies
                  :player {:position {:x 1 :y 1}
                           :gold 0
                           :health 100
                           :magic 100
                           :level 1
                           :exp 0
                           :steps 0}
                  :messages [{:id (random-uuid) :added (.getTime (js/Date.)) :text "Your adventure has started!"}]})))

(defn back-to-title []
  (swap! app-state assoc :current-screen :title))

(defn continue-game []
  (swap! app-state assoc :current-screen :game)
  (swap! app-state assoc :game (reader/read-string (localstorage/get-item localstorage-key))))

(defn game-over []
  (localstorage/remove-item! localstorage-key)
  (swap! app-state assoc :current-screen :game-over
         :game nil))

;; Components

(defn adventure-log-entry
  [message]
  [:li message])

(defn adventure-log
  ""
  []
  (let [{:keys [messages]} (:game @app-state)]
    [:div {:class "adventure-log"}
     [:ol
      (for [message (reverse (sort-by :added messages))]
        ^{:key (:id message)} [adventure-log-entry (:text message)])]]))

(defn progress-bar [{:keys [value max]} type]
  (let [class (case type
                :magic "progress-bar-magic"
                "progress-bar-health")]
    [:div {:class (str "progress-bar " class)}
     [:div {:class "progress-bar-value" :style {:width (str (* 100 (/ value max)) "%")}}]]))

(defn sidebar []
  (let [{:keys [floor player]} (:game @app-state)
        {:keys [gold health magic level exp steps]} player]
    [:div {:class "sidebar"}
     [:ol {:class "status-list"}
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        gold]]
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        (progress-bar {:value health :max 100} :health)]]
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        (progress-bar {:value magic :max 100} :magic)]]
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        (str "Floor " floor)]]
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        (str "Level " level " / " exp "xp")]]
      [:li {:class "status-list-item"}
       [:div {:class "status-list-label"}]
       [:div {:class "status-list-value"}
        (str "Steps " steps)]]]]))

(defn title-screen [{:keys [new-game continue-game-available continue-game]}]
  [:div
   [:div {:class "page-dialog"}
    [:ol {:class "btn-list"}
     [:li
      [:button {:on-click new-game :class "btn"} "New game"]]
     [:li
      [:button {:disabled (not continue-game-available)
                :on-click continue-game
                :class "btn"} "Continue"]]]]])

(defn game-over-screen [{:keys [new-game back-to-title]}]
  [:div
   [:div {:class "page-dialog"}
    [:ol {:class "btn-list"}
     [:li
      [:button {:on-click new-game
                :class "btn"} "New game?"]]
     [:li
      [:button {:on-click back-to-title
                :class "btn"} "Back to title"]]]]])

(defn draw-enemy
  [{:keys [x y max-health health]} ctx tiles]
  (let [health-remaining (/ health max-health)
        bar-width (* 32 health-remaining)]
    (.save ctx)
    (.drawImage ctx tiles (* 28 17) (* 6 17) 16 16 (* x 32) (* y 32) 32 32)
    (set! (.-fillStyle ctx) "#000")
    (.fillRect ctx (- (* x 32) 2) (+ (* y 32) 32) 36 8)
    (set! (.-fillStyle ctx) "#1e8000")
    (.fillRect ctx (* x 32) (+ (* y 32) 34) bar-width 4)
    (.restore ctx)))

(defn render-game
  [ctx]
  (let [level (get-in @app-state [:game :level])
        bsp (get-in @app-state [:game :bsp])
        enemies (get-in @app-state [:game :enemies])
        {:keys [x y]} (get-in @app-state [:game :player :position])
        tiles (.getElementById js/document "tiles")
        level-ctx (rendering/render-level tiles level bsp)]
    (.clearRect ctx 0 0 10000 10000)
    (.drawImage ctx level-ctx 0 0)
    (.drawImage ctx tiles (* 28 17) (* 0 17) 16 16 (* x 32) (* y 32) 32 32)
    (doseq [enemy enemies]
      (draw-enemy enemy ctx tiles))
    (.drawImage ctx tiles (* 21 17) (* 0 17) 16 16 (* 2 32) (* 2 32) 32 32)))

(defn game-component
  []
  (let [dom-node (reagent/atom nil)]
    (reagent/create-class {:component-did-mount (fn [this]
                                                  (reset! dom-node (reagent/dom-node this))
                                                  (render-game (.getContext @dom-node "2d")))
                           :component-did-update (fn [] (render-game (.getContext @dom-node "2d")))
                           :reagent-render (fn []
                                             @app-state ;; Re-render any time app state updates
                                             [:canvas {:width 1000 :height 1000}])})))

(defn game-screen []
  [:div {:class "game-screen"}
   [:div {:class "game-wrapper"}
    [game-component]]
   (sidebar)
   (adventure-log)])

(defn main []
  [:div
   (case (:current-screen @app-state)
     :title (title-screen {:new-game new-game
                           :continue-game-available (not (nil? (localstorage/get-item localstorage-key)))
                           :continue-game continue-game})
     :game (game-screen)
     :game-over (game-over-screen {:new-game new-game
                                   :back-to-title back-to-title}))
   [:img {:src "images/colored_transparent.png" :id "tiles" :style {:display "none"}}]])

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount [el]
  (reagent/render-component [main] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)




(defn next-floor
  []
  (let [{:keys [map-data bsp enemies]} (level/generate)
        next-floor-number (inc (get-in @app-state [:game :floor]))
        new-messages (conj (get-in @app-state [:game :messages]) {:id (random-uuid)
                                                                  :added (.getTime (js/Date.))
                                                                  :text (str "You moved to floor " next-floor-number "!")})]
    (swap! app-state assoc :game {:floor next-floor-number
                                  :level map-data
                                  :bsp bsp
                                  :enemies enemies
                                  :player (merge (get-in @app-state [:game :player]) {:position {:x 1 :y 1}})
                                  :messages new-messages})))

(defn positions-are-equal? [{:keys [x y]} {cx :x cy :y}]
  (and (= x cx) (= y cy)))

(defn next-to? [{:keys [x y]} {cx :x cy :y}]
  (let [diff-x (Math/abs (- x cx))
        diff-y (Math/abs (- y cy))]
    (or
     (and (= diff-x 0) (<= diff-y 1))
     (and (<= diff-x 1) (= diff-y 0)))))

(defn is-dead?
  [{:keys [health]}]
  (<= health 0))

(defn get-enemy-by-position
  [position-to-check enemies]
  (first (filter (fn [enemy] (positions-are-equal? position-to-check enemy)) enemies)))

(defn damage-enemy
  [id-to-damage enemies]
  (map (fn [enemy]
         (if (= id-to-damage (:id enemy))
           (update enemy :health - 30)
           enemy)
         ) enemies))

(defn calculate-damage
  [attacker defender]
  5)

(defn move-enemy
  [id-to-move {:keys [x y]} game]
  (update game :enemies (fn [enemies] (map (fn [enemy] (if (= (:id enemy) id-to-move)
                                                         (assoc enemy :x x :y y)
                                                         enemy)) enemies))))

(defn damage-player
  [{enemy-name :name :as enemy} game]
  (let [damage (calculate-damage enemy (:player game))]
    (->
     game
     (update-in [:player :health] (fn [current-health] (- current-health damage)))
     (update :messages conj {:id (random-uuid) :added (.getTime (js/Date.)) :text (str "You got hit by the " enemy-name " for " damage " damage!")}))))

(defn is-out-of-bounds?
  [{:keys [x y]}]
  (or
   (< x 0)
   (< y 0)))

(defn enemy-move
  [game {:keys [x y id] :as enemy}]
  (let [player-position (get-in game [:player :position])
        possible-move {:x (+ x (- (rand-int 3) 1)) :y (+ y (- (rand-int 3) 1))}
        is-blocked? (= (level/get-tile (:level game) possible-move 1) 1)]
    (cond
      (next-to? {:x x :y y} player-position) (damage-player enemy game)
      (and (not (is-out-of-bounds? possible-move))
           (not is-blocked?)) (move-enemy id possible-move game)
      :else game)))

(defn enemy-moves
  [game]
  (reduce enemy-move game (:enemies game)))

(defn move
  [direction]
  (let [{current-x :x current-y :y} (get-in @app-state [:game :player :position])
        {next-x :x next-y :y} (case direction
                                :up {:x current-x :y (- current-y 1)}
                                :down {:x current-x :y (+ current-y 1)}
                                :left {:x (- current-x 1) :y current-y}
                                :right {:x (+ current-x 1) :y current-y})
        is-out-of-bounds? (or (< next-x 0)
                              (< next-y 0))
        is-blocked? (= (level/get-tile (get-in @app-state [:game :level]) {:x next-x :y next-y} 1) 1)
        is-valid? (not (or is-out-of-bounds? is-blocked?))
        is-moving-to-stairs? (positions-are-equal? {:x 2 :y 2} {:x next-x :y next-y})
        current-enemies (get-in @app-state [:game :enemies])
        enemy-collided-with (get-enemy-by-position {:x next-x :y next-y} current-enemies)]
    (if is-valid?
      (if is-moving-to-stairs?
        (do 
          (next-floor)
          (localstorage/set-item! localstorage-key (prn-str (:game @app-state))))
        (do
          (if (not (nil? enemy-collided-with))
            (do
              (let [new-enemies (->> current-enemies
                                     (damage-enemy (:id enemy-collided-with))
                                     (filter (fn [enemy] (not (is-dead? enemy)))))
                    enemy-died? (not (= (count new-enemies) (count current-enemies)))
                    new-message (if enemy-died?
                                  "You killed the enemy!"
                                  "You damaged the enemy!")
                    new-messages (conj (get-in @app-state [:game :messages]) {:id (random-uuid)
                                                                              :added (.getTime (js/Date.))
                                                                              :text (str new-message)})]
                (swap! app-state assoc-in [:game :enemies] new-enemies)
                (swap! app-state assoc-in [:game :messages] new-messages)))
            (do
              (swap! app-state assoc-in [:game :player :steps] (inc (get-in @app-state [:game :player :steps])))
              (swap! app-state assoc-in [:game :player :position] {:x next-x :y next-y})))
          (swap! app-state assoc :game (enemy-moves (:game @app-state)))
          (if (<= (get-in @app-state [:game :player :health]) 0)
            (game-over)))))))

;; This function has to be definied using `defonce` so that we retain the reference for add / removing
;; the same event listener, rather than getting a new reference every time
(defonce handle-keypress (fn [e] (let [keyCode (.-keyCode e)
                                       character (.-key e)]
                                   (case character
                                     "ArrowLeft" (move :left)
                                     "ArrowRight" (move :right)
                                     "ArrowUp" (move :up)
                                     "ArrowDown" (move :down))
                                   (if (contains? #{"ArrowLeft" "ArrowRight" "ArrowUp" "ArrowDown"} character)
                                     (.preventDefault e))
                                   false)))



;; If we reload the page there is a danger we will add multiple event listeners
;; to counter this we just remove any existing first and then re-add
(.removeEventListener js/window "keydown" handle-keypress)
(.addEventListener js/window "keydown" handle-keypress)

(defn update-time
  "Use request animation frame to keep the time stored in the game state up to date on every tick"
  [time]
  (.requestAnimationFrame js/window update-time)
  (println time))

;; Start the request animation frame function to keep time up to date
; (update-time 0)