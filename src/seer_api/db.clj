(ns seer-api.db
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [monger.operators :refer :all]
            [clojure.tools.logging :as log]))


(defn store-job-status [job-id db collection]
  (try
    (mc/insert db collection {:_id job-id :status "new" :last-update (new java.util.Date)})
    (catch Exception e
      (throw
        (ex-info "Can't create new job"
                 {:status "ERROR"
                  :reason (.getMessage e)
                  :job-id job-id})))))


(defn update-job-status [db collection job-id status]
  (try
    (mc/update-by-id db collection job-id {$set (merge status {:last-update (new java.util.Date)})})
    (catch Exception e
      {:status "ERROR"
       :reason (str "Can't update the elaboration status: " (.getMessage e))})))


(defn find-user-by-job-id [job-id db collection]
  (mc/find-map-by-id db collection job-id))

(defn store-results [db collection job-id base-path]
  (try
    (let [resF (slurp (str base-path "/" job-id "/temp.csvF"))]
      (mc/update-by-id db collection job-id {$set {:timeseries resF :last-update (new java.util.Date) :status "storing timeserie elaborated"}})
      (let [resF2 (slurp (str base-path "/" job-id "/temp.csvF2"))]
        (mc/update-by-id db collection job-id {$set {:forecasted resF2 :last-update (new java.util.Date) :status "storing data forecasted"}})
        ))
    (catch Exception e
      {:status "ERROR"
       :reason (str "Can't store the forecasted data: " (.getMessage e))})))


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
  ;(store-results db "elaborations" "e0cd02e5-a144-4d04-88aa-1364e8165207" "/home/altimario/seer/temp/")
  )