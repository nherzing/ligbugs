(defproject ligbugs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2411"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [reagent "0.4.3"]]
  :cljsbuild
  {:builds {:dev {:source-paths ["src"]
                  :compiler {:preamble ["reagent/react.js"]
                             :output-dir "out"
                             :output-to "main.js"
                             :pretty-print true}}
            :prod {:source-paths ["src"]
                   :compiler {:preamble ^:replace ["reagent/react.min.js"]
                              :output-to "prod.js"
                              :pretty-print false
                              :optimizations :advanced}}}})
