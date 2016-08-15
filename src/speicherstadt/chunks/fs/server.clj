(ns speicherstadt.chunks.fs.server
  (:require
   [aleph.http :as http]
   [bidi.vhosts :as bv]
   [byte-streams :as bs]
   [com.stuartsierra.component :as component]
   [manifold.deferred :as deferred]
   [manifold.stream :as stream]
   [me.raynes.fs :as fs]
   [schema.core :as s]
   [yada.yada :as yada])
  (:import [java.math BigInteger]
           [java.security DigestInputStream MessageDigest]))

(def chunk-regex #"(sha256)-([0-9a-f]{2})([0-9a-f]{2})[0-9a-f]+")

(defn consume-chunk-body [base-dir]
  (let [part-dir (fs/file base-dir "parts")]
    (fs/mkdir part-dir)
    (fn [ctx _ body-stream]
      (let [upload-path (fs/file part-dir (fs/temp-name "chunk-" ".part"))
            chunk-hash (deferred/future
                         (let [body-stream (DigestInputStream.
                                            (bs/to-input-stream body-stream)
                                            (MessageDigest/getInstance "SHA-256"))]
                           (bs/transfer body-stream upload-path)
                           (->> (.. body-stream
                                    getMessageDigest
                                    digest)
                                (BigInteger. 1)
                                (format "sha256-%064x"))))]
        (merge ctx {:chunk-hash chunk-hash
                    :upload-path upload-path})))))

(defn chunk-path [base-dir id]
  (fs/file base-dir id))

(defn store-chunk [base-dir]
  (fn [ctx]
    (let [hash @(:chunk-hash ctx)
          upload-path (:upload-path ctx)]
      (if (= hash (get-in ctx [:parameters :path :id]))
        (let [path (chunk-path base-dir hash)]
          (fs/rename upload-path path)
          nil)
        (do
          (fs/delete upload-path)
          (deferred/error-deferred
            (ex-info (format
                      "Conflict: Chunk ID and Hash do not match. Content hash is %s"
                      hash)
                     {:status 409})))))))

(defn chunk-properties [base-dir]
  (fn [ctx]
    (let [path (chunk-path base-dir (get-in ctx [:parameters :path :id]))]
      {:exists? (fs/exists? path)})))

(defn get-chunk [base-dir]
  (fn [ctx]
    (chunk-path base-dir (get-in ctx [:parameters :path :id]))))

(defn chunk-resource [base-dir]
  (yada/resource
   {:methods {:get {:produces #{"application/octet-stream"}
                    :response (get-chunk base-dir)}
              :put {:produces #{"application/json"}
                    :consumes #{"application/octet-stream"}
                    :consumer (consume-chunk-body base-dir)
                    :response (store-chunk base-dir)}}
    :parameters {:path {:id chunk-regex}}
    :properties (chunk-properties base-dir)}))

(def chunk-list-resource
  (yada/resource
   {:methods {:get {:produces #{"application/json" "application/edn"}
                    :response {:chunks []}}}}))

(defn chunk-routes [base-dir]
  ["/chunks"
   {"" chunk-list-resource
    ["/" [chunk-regex :id]] (chunk-resource base-dir)}])

(defn routes [base-dir]
  ["" (yada/swaggered (chunk-routes base-dir) {})])

(s/defrecord Server [port :- s/Int
                     base-dir :- s/Str
                     listener]
  component/Lifecycle
  (start [this]
    (assert (fs/directory? base-dir) (format "Directory %s does not exist" base-dir))
    (if listener
      this
      (let [vhosts-model (bv/vhosts-model [{:scheme :http
                                            :host (format "localhost:%d" port)}
                                           (routes base-dir)])
            listener (yada/listener vhosts-model {:port port})]
        (assoc this :listener listener))))

  (stop [this]
    (when-let [close (get-in this [:listener :close])]
      (close))
    (assoc this :listener nil)))

(defn new-server []
  (map->Server {}))
