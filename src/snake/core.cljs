(ns ^:figwheel-always snake.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan <! put!]]
            [rum :as r]))

(enable-console-print!)

(def new-game-state
  {:history   '([1 3] [1 2] [1 1])
   :length    3
   :direction :south
   :crashed?  false
   :food      [10 10]})

(defonce app-state (atom new-game-state))

(defn on-js-reload []
;;   (reset! app-state new-game-state)
;;   (swap! app-state assoc :history '([1 19] [1 2] [1 1]))
)


;;
;; Behaviour
;;

(def direction->next-pos
  {:north (fn [[x y]] [x (- y 1)])
   :south (fn [[x y]] [x (+ y 1)])
   :west  (fn [[x y]] [(- x 1) y])
   :east  (fn [[x y]] [(+ x 1) y])})

(def directions (keys direction->next-pos))

(defn advance-snake [state]
  (swap! state
    (fn [{:keys [direction] :as old-state}]
      (update-in old-state [:history]
                 #(conj % ((direction->next-pos direction) (first %)))))))

(defn direction-valid? [old-direction new-direction]
  (let [opposites {:north :south, :south :north, :east :west, :west :east}]
    ((comp not =) new-direction (old-direction opposites))))

(defn set-direction [state new-direction]
  (swap! state
    (fn [{:keys [direction] :as old-state}]
      (assoc old-state :direction
        (if (direction-valid? direction new-direction) new-direction direction)))))

(defn eat-food-watcher [_k reference _os {[head & _] :history food :food}]
  (when (= head food)
    (swap! reference
           #(-> %
                (assoc :food [(rand-int 20) (rand-int 20)])
                (update-in [:length] inc)))))

(defn crash-watcher [channel _k reference _os {:keys [history length crashed?] :as ns}]
  (let [[head & tail] (take length history)
        [x y]         head]
    (when (and (not crashed?)
               (or (not (and (<= 0 x 20) (<= 0 y 20)))
                   (some #{head} tail)))
      (swap! reference assoc :crashed? true)
      (put! channel :done))))



(def channel (chan))

;; Start
(add-watch app-state :eat-food-watcher eat-food-watcher)
(add-watch app-state :crash-watcher (partial crash-watcher channel))

(defn cleanup-watchers []
  (remove-watch app-state :eat-food-watcher)
  (remove-watch app-state :crash-watcher))

;;       (js/clearInterval interval)


;; Render

(defn explode-coord [[x y]]
  [(+ 5 (* 10 x))
   (+ 5 (* 10 y))])

(r/defc ui []
  (let [state @app-state
        {:keys [history length food]} state]
    [:svg {:width 210 :height 210}
     (->> (take length history)
          (map explode-coord)
          (partition 2 1)
          (map (fn [[[x1 y1] [x2 y2]]]
                 [:line {:x1 x1 :y1 y1
                         :x2 x2 :y2 y2
                         :style {:stroke "rgb(200,0,100)"
                                 :strokeWidth 10}}])))
     (let [[cx cy] (explode-coord food)]
       [:circle {:cx cx :cy cy :r 5 :fill "red"}])]))


;; Keyboard handling
(def key->direction
  {38 :north
   40 :south
   37 :west
   39 :east})

(defn map-keycode [keycode-lookup out-channel event]
  (let [keycode (.-keyCode event)
        mapped (keycode-lookup keycode)]
    (if (not (nil? mapped))
      (put! out-channel mapped))))



;; Render it!
(let [container (.getElementById js/document "app")
      component (r/mount (ui) container)
      interval  (js/setInterval #(put! channel :advance) 100)
      ]
  (.focus container)
  (aset container "onkeydown" (partial map-keycode key->direction channel))
  (defn render [] (r/request-render component))

  ;; Event Handling / Channels
  (defn handle-events [token]
    (cond
     (= token :advance)
     (do
       (advance-snake app-state)
       (render))
     (some #{token} directions)
     (set-direction app-state token)
     (= token :done)
     (do
       (cleanup-watchers)
       (js/clearInterval interval))
    ))

  (go (while true
    (handle-events (<! channel)))))





