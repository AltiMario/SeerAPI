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


  (store-job-status "z" db)
  (update-job-status db "z" {:status "validate" :some-new-test 1})
  )


