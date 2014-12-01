(ns ligbugs.core
  (:require [cljs.core.async :as a]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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

(defn start! [n]
  (let [chans (map (fn [f] (f)) (repeat n a/chan))
        names (map #(keyword (str "bug" %)) (take n (iterate inc 0)))
        mults (into {} (map (fn [chan name] [name (a/mult chan)])
                            chans names))
        bugs (doall (map (fn [chan name]
                           (bug name chan (vals (dissoc mults name))))
                         chans names))]
    {:mults  mults :stop (fn [] (doseq [b bugs] (b)))}))


(defn flash! [style]
  (swap! style assoc :opacity 1)
  (go-loop [n 255]
           (when (> n 0)
             (swap! style assoc :opacity (/ n 255))
             (a/<! (timeout 2))
             (recur (dec n)))))

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
                     :height "30px"}}
       [:div {:style @style}]])))

(defn bugs-view [mults]
  [:div (for [[k m] mults]
          ^{:key k} [bug-view m])])

(defn setup! [n]
  (let [{:keys [mults stop]} (start! n)]
    (reagent/render-component (fn [] [bugs-view mults])
                              (.-body js/document))))
