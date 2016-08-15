(ns speicherstadt.chunks.fs.core-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer :all]
   [me.raynes.fs :as fs]
   [speicherstadt.chunks.fs.server :refer [chunk-routes]]
   [yada.test :refer [response-for]]))

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

    (fs/delete-dir tmp-dir)))
