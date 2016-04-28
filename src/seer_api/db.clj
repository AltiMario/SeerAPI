(ns seer-api.db
  (:require [monger.collection :as mc]
            [monger.core :as mg]
            [monger.operators :refer :all]))


(defn store-job-status [job-id db]
  (try
    (mc/insert db "elaborations" {:_id job-id :status "new" :last-update (new java.util.Date)})
    (catch Exception e
      {:status "ERROR"
       :reason (str "Can't add the elaboration status: " (.getMessage e))})))


(defn update-job-status [db job-id status]
  (try
    (mc/update-by-id db "elaborations" job-id {$set (merge status {:last-update (new java.util.Date)})})
    (catch Exception e
      {:status "ERROR"
       :reason (str "Can't update the elaboration status: " (.getMessage e))})))


(defn find-user-by-job-id [job-id db]
  (mc/find-map-by-id db "elaborations" job-id))


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


