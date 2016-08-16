(ns speicherstadt.chunks.fs.core-test
  (:require
   [byte-streams :as bs]
   [cheshire.core :as json]
   [clojure.test :refer :all]
   [me.raynes.fs :as fs]
   [speicherstadt.chunks.fs.server :refer [chunk-routes]]
   [yada.test :refer [response-for]])
  (:import [java.math BigInteger]
           [java.security MessageDigest]))

(defn parse-body [body]
  (json/parse-string body true))

(def hash-of
  {"Hello World"
   "sha256-a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e"
   "Hello Foo"
   "sha256-c3a26588bb78f6c08a0ef07ad88a5a0f9ff3f66940606c656d44cb6a239c6343"})

(defn body-options [body]
  {:body body
   :headers {"content-type" "application/octet-stream"
             "content-length" (str (count body))}})

(deftest chunk-acceptance
  (let [tmp-dir (fs/temp-dir "chunktest-")
        handler (chunk-routes tmp-dir)]
    (testing "List of chunks is empty before uploading any chunks"
      (let [response (response-for handler :get "/chunks")]
        (is (= 200 (:status response)))
        (is (= {:chunks []} (parse-body (:body response))))))

    (testing "PUT a new chunk"
      (let [response (response-for
                      handler
                      :put
                      (str "/chunks/" (hash-of "Hello World"))
                      (body-options "Hello World"))]
        (is (= 201 (:status response)))))

    (testing "PUT same chunk again"
      (let [response (response-for
                      handler
                      :put
                      (str "/chunks/" (hash-of "Hello World"))
                      (body-options "Hello World"))]
        (is (= 204 (:status response)))))

    (testing "PUTting a chunk with wrong hash fails"
      (let [response (response-for
                      handler
                      :put
                      (str "/chunks/sha256-12345")
                      (body-options "Hello World"))]
        (is (= 409 (:status response)))))

    (testing "GET an existing chunk"
      (let [response (response-for
                      handler
                      :get
                      (str "/chunks/" (hash-of "Hello World")))]
        (is (= 200 (:status response)))
        (is (= "Hello World" (:body response)))))

    (testing "GET a non-existing chunk"
      (let [response (response-for
                      handler
                      :get
                      "/chunks/sha256-123456")]
        (is (= 404 (:status response)))))

    (testing "GET a list of one chunk"
      (let [response (response-for
                      handler
                      :get
                      "/chunks")]
        (is (= 200 (:status response)))
        (is (= {:chunks [(hash-of "Hello World")]}
               (parse-body (:body response))))))

    (testing "List of chunks is sorted"
      ;; PUT an additional chunk, so we have two
      (response-for
       handler
       :put
       (str "/chunks/" (hash-of "Hello Foo"))
       (body-options "Hello Foo"))
      (let [response (response-for
                      handler
                      :get
                      "/chunks")]
        (is (= {:chunks (map hash-of ["Hello World" "Hello Foo"])}
               (parse-body (:body response))))))

    (comment "FIXIT"
             (testing "PUT and GET a binary chunk"
               (let [size 1000
                     upload (byte-array (take size (repeatedly #(- (rand-int 256) 128))))
                     url (->> upload
                              (.digest (MessageDigest/getInstance "SHA-256"))
                              (BigInteger. 1)
                              (format "/chunks/sha256-%064x"))
                     _ (response-for
                        handler
                        :put
                        url
                        (body-options upload))
                     response (response-for
                               handler
                               :get
                               url)]
                 (is (bs/bytes= upload (:body response))))))

    (fs/delete-dir tmp-dir)))
