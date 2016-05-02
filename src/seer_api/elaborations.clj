(ns seer-api.elaborations
  (:require [seer-api.utils :as ut]
            [seer-api.db :as db])
  (:require [clojure.java.shell :refer [sh]]))


(defn calc-processing-eta [job-id base-path core-path]
  (let [eta (sh core-path "temp.csv" "10" "ETA" :dir (str base-path job-id))]
    (if (.contains (:out eta) "ERROR")
      (throw (ex-info "Error while calculating ETA"
                      {:status  "ERROR" :reason "ETA calculation issue"
                       :details (clojure.string/trim-newline (:out eta))}))
      {:eta (clojure.string/trim-newline (:out eta))})))

(comment
  (calc-processing-eta "76dc0ded-e902-4db3-92df-48376980e95f" "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  )


(defn forecast [job-id base-path core-path]
  (let [res (sh core-path "temp.csv" "10" :dir (str base-path job-id))]
    (if (.contains (:out res) "ERROR")
      (throw (ex-info "Error while computing forecasts"
                      {:status  "failed" :reason "forecasting error"
                       :details (clojure.string/trim-newline (:out res))}))
      {:status "completed"})))

(comment
  (forecast "76dc0ded-e902-4db3-92df-48376980e95f" "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  )


(defn processing-step [status-update-fn step-descr f]
  (try
    (status-update-fn {:status (str "starting " step-descr)})
    (let [result (f)]
      (status-update-fn (merge result {:status (str step-descr " completed")})))
    (catch Exception x
      (status-update-fn {:status "ERROR" :reason (str " Error during " step-descr)})
      (throw x))))


(defn start-background-processing [job-id db collection base-path core-path]
  (future
    (let [status-update-fn (fn [status]
                             (db/update-job-status db collection job-id status))]
      (processing-step status-update-fn "validation"
                       (fn [] (ut/validate-csv job-id base-path) nil))

      (processing-step status-update-fn "calculating ETA"
                       (fn [] (calc-processing-eta job-id base-path core-path)))

      (processing-step status-update-fn "calculating forecast"
                       (fn [] (forecast job-id base-path core-path) nil))

      (processing-step status-update-fn "storing data forecasted"
                       (fn [] (db/store-results job-id db collection base-path) nil))

      )))

(comment
  (start-background-processing "a08a2da7-7374-467f-8705-5d9e1e7771f9" db "/home/altimario/seer/temp/" "/home/altimario/seer/seerCore")
  (processing-step identity "test" (fn [] {:eta (+ 1 1)}))
)
