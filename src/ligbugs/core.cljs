(ns ligbugs.core
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def peak-energy 100)
(def phase-length 1000)
(def delay (/ phase-length peak-energy))
(def refractory-length 5)

(defn timeout [ms]
  (let [c (a/chan)]
    (js/setTimeout (fn [] (a/close! c)) ms)
    c))

(let [b 3
      epsilon 0.1
      alpha (js/Math.exp (* b epsilon))
      beta (/ (- (js/Math.exp (* b epsilon)) 1)
              (- (js/Math.exp b) 1))]
  (defn observed-flash [energy]
    (if (< energy refractory-length)
      energy
      (min (+ (* alpha energy) beta)
           peak-energy))))

(defn bug [msg out-ch in-mults]
  (let [alive (atom true)
        energy (atom (rand peak-energy))
        in-chs (map (fn [m] (let [c (a/chan)]
                             (a/tap m c)
                             c))
                    in-mults)]
    (if-not (empty? in-mults)
      (go (while @alive
            (a/alts! in-chs)
            (swap! energy observed-flash))))
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


(defn bug-view [m style]
  (let [class (atom "")
        c (a/chan)]
    (a/tap m c)
    (go (while (<! c)
          (reset! class "flash")
          (js/setTimeout #(reset! class "") 500)))
    (fn []
      [:div {:class "bug-wrapper" :style style}
       [:div {:class (str @class " bug")}]])))

(defn bugs-view [mults style]
  (let [dim (js/Math.sqrt (count mults))]
    [:div (for [i (range dim)]
            ^{:key i} [:div {:class "row"}
                       (for [j (range dim)]
                         ^{:key [i j]} [bug-view (mults [i j]) style])])]))

(defn setup-grid! [n]
  (let [{:keys [mults stop]} (start-grid! n)]
    (reagent/render-component (fn [] [bugs-view mults {:height (str (dec (* 100 (/ 1 n))) "%")
                                                      :width (str (dec (* 100 (/ 1 n))) "%")}])
                              (.-body js/document))))
