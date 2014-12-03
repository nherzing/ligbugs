(ns ligbugs.core
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def peak-energy 1000)
(def phase-length 1000)
(def delay (/ phase-length peak-energy))
(def refractory-length 5)

(defn timeout [ms]
  (let [c (a/chan)]
    (js/setTimeout (fn [] (a/close! c)) ms)
    c))

(defn observed-flash [energy]
  (if (< energy refractory-length)
    energy
    (+ energy
       (* (* peak-energy 0.10) (Math/pow (/ energy peak-energy) 2)))))

(defn bug [msg out-ch in-mults]
  (let [alive (atom true)
        energy (atom (rand peak-energy))
        in-chs (map (fn [m] (let [c (a/chan)]
                             (a/tap m c)
                             c))
                    in-mults)]
    (go (while @alive
          (a/alts! in-chs)
          (swap! energy observed-flash)))
    (go (while @alive
          (swap! energy inc)
          (when (>= @energy peak-energy)
            (a/put! out-ch msg)
            (reset! energy 0.0))
          (a/<! (timeout delay))))
    (fn [] (reset! alive false))))

(defn neighbors [[x y]]
  #{[(inc x) y] [(inc x) (inc y)] [(inc x) (dec y)]
    [(dec x) y] [(dec x) (inc y)] [(dec x) (dec y)]
    [x (inc y)] [x (dec y)]})

(defn start-grid! [n]
  (let [chans (into {} (for [x (range n)
                             y (range n)]
                         [[x y] (a/chan)]))
        mults (into {} (map (fn [[xy ch]] [xy (a/mult ch)]) chans))
        in-mults (into {} (map (fn [xy] [xy (remove nil? (map mults (neighbors xy)))]) (keys chans)))]
    (doall (map (fn [xy] (bug xy (chans xy) (in-mults xy))) (keys chans)))
    {:mults mults}))


(defn flash! [style]
  (swap! style assoc :opacity 1)
  (go-loop [n 255]
           (when (> n 0)
             (swap! style assoc :opacity (/ n 255))
             (a/<! (timeout 2))
             (recur (- n 5)))))

(defn bug-view [m]
  (let [style (atom {:width "100%" :height "100%"
                     :background "red" :opacity 0})
        c (a/chan)]
    (a/tap m c)
    (go (while (<! c)
          (flash! style)))
    (fn []
      [:div {:style {:border "1px solid black"
                     :width "30px"
                     :height "30px"
                     :display "inline-block"}}
       [:div {:style @style}]])))

(defn bugs-view [mults]
  (let [dim (js/Math.sqrt (count mults))]
    [:div (for [i (range dim)]
            ^{:key i} [:div {:class "row"}
                       (for [j (range dim)]
                         ^{:key [i j]} [bug-view (mults [i j])])])]))

(defn setup-grid! [n]
  (let [{:keys [mults stop]} (start-grid! n)]
    (reagent/render-component (fn [] [bugs-view mults])
                              (.-body js/document))))
