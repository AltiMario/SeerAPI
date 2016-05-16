(ns seer-api.db
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [monger.operators :refer :all]
            [clojure.tools.logging :as log]
            [monger.conversion :refer [from-db-object]]
            [seer-api.utils :as ut]))


(defn store-job-status [job-id db collection]
  (try
    (mc/insert db collection {:_id job-id :status "new" :last-update (new java.util.Date)})
    (catch Exception e
      (throw
        (ex-info "Can't create new job"
                 (ut/error-manager e (str "ERROR creation job:" job-id)))))))


(defn update-job-status [db collection job-id status]
  (try
    (mc/update-by-id db collection job-id {$set (merge status {:last-update (new java.util.Date)})})
    (catch Exception e
      (throw
        (ex-info "Can't update the elaboration status"
                 (ut/error-manager e "ERROR status update"))))))


(defn store-results [job-id db collection base-path]
  (try
    (let [resF (slurp (str base-path "/" job-id "/temp.csvF"))
          resF2 (slurp (str base-path "/" job-id "/temp.csvF2"))]
      (mc/update-by-id db collection job-id {$set {:timeseries resF :forecasted resF2}}))
    (catch Exception e
      (throw
        (ex-info "Can't store the forecasted data"
                 (ut/error-manager e "ERROR store result"))))))

(defn find-elaboration-by-job-id [job-id db collection]
  (get (mc/find-one-as-map db collection {:_id job-id} ["timeseries"]) :timeseries))

(defn find-forecast-by-job-id [job-id db collection]
  (get (mc/find-one-as-map db collection {:_id job-id} ["forecasted"]) :forecasted))

(defn find-status-by-job-id [job-id db collection]
  (mc/find-map-by-id db collection job-id {:timeseries 0 :forecasted 0}))

(defn start-connection [{:keys [host db-name] :as config}]
  (let [conn (mg/connect {:host host})]
    {:db-conn conn
     :db      (mg/get-db conn db-name)}))


(comment
  ;; start new connection
  (def xconn (start-connection {:host "localhost" :db-name "seer-api"}))
  (def conn (:db-conn xconn))
  (def db (:db xconn))
  ;; get connection from configuration
  (def db (-> @seer-api.core/conn-and-conf :db))


  ;(store-job-status "z" db)
  ;(update-job-status db "z" {:status "validate" :some-new-test 1})
  ;(store-results db "elaborations" "7267a603-fa20-4d65-a786-29ca0797b50e" "/home/altimario/seer/temp/")
  (mc/find-maps db "elaborations" {:_id "e0cd02e5-a144-4d04-88aa-1364e8165207"}, {:timeseries 0})
  )