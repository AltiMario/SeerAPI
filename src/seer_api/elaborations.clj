(ns seer-api.elaborations
  (:require [seer-api.utils :as ut]
            [seer-api.db :as db]))
(use '[clojure.java.shell :only [sh]])



(defn calc-processing-eta [job-id base-path core-path]
  (let [eta (sh core-path "temp.csv" "10" "ETA" :dir (str base-path job-id))]
    (if (.contains (:out eta) "ERROR")
      {:status "ERROR" :reason "ETA calculation issue" :details (clojure.string/trim-newline(:out eta))}
      {:status "ETA calculated" :eta (clojure.string/trim-newline(:out eta))})))

(comment
  (calc-processing-eta "76dc0ded-e902-4db3-92df-48376980e95f" "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  )


(defn forecast [job-id base-path core-path]
  (let [res (sh core-path "temp.csv" "10" :dir (str base-path job-id))]
    (if (.contains (:out res) "ERROR")
      {:status "failed" :reason "forecasting error" :details (clojure.string/trim-newline (:out res))}
      {:status "completed"})))

(comment
  (forecast "76dc0ded-e902-4db3-92df-48376980e95f" "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  )


(defn start-background-processing [job-id db base-path core-path]
  (future
    (if-let [validation-error (ut/is-bad-csv? job-id base-path)]
      (db/update-job-status db job-id {:status "ERROR" :reason "validation failed" :details validation-error})
      (do
        (db/update-job-status db job-id {:status "validated"})
        (db/update-job-status db job-id (calc-processing-eta job-id base-path core-path))
        (db/update-job-status db job-id {:status "processing"})
        (let [result (forecast job-id base-path core-path)]
            (db/update-job-status db job-id result))))))

(comment
  (start-background-processing "a08a2da7-7374-467f-8705-5d9e1e7771f9" db "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  )
