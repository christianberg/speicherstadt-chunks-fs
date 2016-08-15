(ns speicherstadt.chunks.fs.system
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [com.stuartsierra.component :as component]
   [speicherstadt.chunks.fs.server :as server]))

(defn config
  "Read EDN config with the given profile. Config file is searched in the resources directory"
  [profile]
  (aero/read-config (io/resource "config.edn") {:profile profile}))

(defn configure-components
  "Merge configuration to it's corresponding component prior to system startup."
  [system config]
  (merge-with merge system config))

(defn new-system-map
  "Create the system."
  []
  (component/system-map
   :server (server/new-server)))

(defn new-dependency-map
  "Declare the dependency relationships between components."
  []
  {})

(defn new-system
  "Construct a new system, configured with the given profile"
  [profile]
  (-> (new-system-map)
      (configure-components (config profile))
      (component/system-using (new-dependency-map))))
