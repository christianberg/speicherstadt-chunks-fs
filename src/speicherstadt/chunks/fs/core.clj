(ns speicherstadt.chunks.fs.core
  (:require
   [com.stuartsierra.component :as component]
   [speicherstadt.chunks.fs.system :as system])
  (:gen-class))

(defn -main
  "Start the Chunks Service."
  [& args]
  (component/start
   (system/new-system :prod))
  @(promise))
