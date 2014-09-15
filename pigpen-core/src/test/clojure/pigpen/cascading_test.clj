(ns pigpen.cascading-test
  (:import (java.io File)
           (org.apache.hadoop.fs FileSystem Path)
           (org.apache.hadoop.conf Configuration))
  (:use clojure.test pigpen.cascading)
  (:require [pigpen.extensions.test :refer [test-diff pigsym-zero pigsym-inc]]
            [pigpen.core :as pig]))

(comment test-taps
         (println (command->flowdef '{:type     :load
                                      :id       load0
                                      :location "some/path"
                                      :storage  {:type :storage
                                                 :func "text"
                                                 :args []}
                                      :fields   [a b c]
                                      :opts     {:type :load-opts}}
                                    {}))
         (is (= 1 1)))

(comment test-load-text
         (let [load-cmd (load-text "the/location")]
           (println "load-cmd:" load-cmd)
           (println (command->flowdef load-cmd {}))))

(deftest test-simple-flow
  (spit "/tmp/input" "1\t2\tfoo\n4\t5\tbar")
  (.delete (FileSystem/get (Configuration.)) (Path. "/tmp/output") true)
  (letfn
      [(func [data]
             (->> data
                  (pig/map (fn [[a b c]]
                             {:sum  (+ (Integer/valueOf a) (Integer/valueOf b))
                              :name c}))
                  (pig/filter (fn [{:keys [sum]}]
                                (< sum 5)))))
       (query [input-file output-file]
              (->>
                (load-tsv input-file)
                (func)
                (store-clj output-file)))]
    (let [flow (generate-flow (query "/tmp/input" "/tmp/output"))]
      (println flow)
      (.complete flow))
    (println "results:\n" (slurp "/tmp/output/part-00000"))))

(run-tests 'pigpen.cascading-test)
