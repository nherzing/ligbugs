(defproject ligbugs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cljsbuild "1.0.3"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2234"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.4.3"]]
  :cljsbuild
  {:builds [{:source-paths ["src"]
             :id "dev"
             :compiler {:preamble ["reagent/react.js"]
                        :output-dir "out"
                        :output-to "main.js"
                        :pretty-print true}}]})
