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

(def b (atom 3))
(def epsilon (atom 0.15))

(defn observed-flash [energy]
  (let [alpha (js/Math.exp (* @b @epsilon))
        beta (/ (- (js/Math.exp (* @b @epsilon)) 1)
                (- (js/Math.exp @b) 1))]
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
            (swap! energy observed-flash)
            (when (>= @energy peak-energy)
              (a/put! out-ch msg)
              (reset! energy 0.0)))))
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
        in-mults (into {} (map (fn [xy] [xy (remove nil? (map mults (neighbors xy)))]) (keys chans)))
        stop-fns (doall (map (fn [xy] (bug xy (chans xy) (in-mults xy))) (keys chans)))]
    {:mults mults
     :stop (fn [] (doseq [f stop-fns] (f)))}))


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

(defn bugs-view [mults]
  (let [dim (js/Math.sqrt (count @mults))
        style {:height (str (dec (* 100 (/ 1 dim))) "%")
               :width (str (dec (* 100 (/ 1 dim))) "%")}]
    [:div (for [i (range dim)]
            ^{:key i} [:div {:class "row"}
                       (for [j (range dim)]
                         ^{:key [i j]} [bug-view (@mults [i j]) style])])]))

(defn setup-view [setup-fn]
  (let [value (atom 10)]
    (fn []
      [:div
       [:label "Rows:"]
       [:input {:type "number" :min 1 :max 1000
                :value @value
                :on-change #(reset! value (-> % .-target .-value))}]
       [:button {:on-click #(setup-fn @value)} "Synchronize!"]])))

(defn range-view
  ([label value min max]
     (range-view label value min max identity identity))
  ([label value min max f f-inv]
     [:div
      [:label label]
      [:input {:type "range" :min min :max max
               :value (f-inv @value)
               :on-change #(reset! value (-> % .-target .-value f))}]
      @value]))

(defn view []
  (let [bugs (atom {})
        stop-fn (atom nil)
        reset-fn (fn [n]
                   (if @stop-fn (@stop-fn))
                   (let [{:keys [mults stop]} (start-grid! n)]
                     (reset! bugs mults)
                     (reset! stop-fn stop)))]
    [:div
     [setup-view reset-fn]
     [range-view "b:" b 1 5]
     [range-view "epsilon:" epsilon 1 30 #(/ % 100) #(* % 100)]
     [bugs-view bugs]]))

(defn setup-grid! [n]
  (let [{:keys [mults stop]} (start-grid! n)]
    (reagent/render-component (fn [] [bugs-view mults stop])
                              (.-body js/document))))


(reagent/render-component (fn [] [view])
                              (.-body js/document))
