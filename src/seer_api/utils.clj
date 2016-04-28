(ns seer-api.utils
  (:require [clojure.java.io :as io]
            [seer-api.db :as db]))


(defn is-bad-csv? [job-id base-path]
  (try
    (doall
      (->> (str base-path "/" job-id "/temp.csv")
           slurp
           (re-seq #"[^,\n\r]+")
           (map (fn [^String s] (Long/parseLong s)))
           (partition 2)))
    (catch Throwable x
      true))
  false)


(defn copy-input-to-location
  [job-id db input base-path]
  (try
    (let [file-path (str base-path "/" job-id)
          file-name (str file-path "/temp.csv")]
      (io/make-parents file-name)
      (io/copy input (io/file file-name))
      file-name)
    (catch Exception e
      (db/update-job-status job-id db {:status "ERROR" :reason "temporary file not copied"})
      false)))
