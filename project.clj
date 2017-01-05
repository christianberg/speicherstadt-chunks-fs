(defproject speicherstadt-chunks-fs "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aero "1.0.1"]
                 [aleph "0.4.1"]
                 [bidi "2.0.16"]
                 [com.stuartsierra/component "0.3.2"]
                 [me.raynes/fs "1.4.6"]
                 [yada "1.2.0"]]
  :main ^:skip-aot speicherstadt.chunks.fs.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
