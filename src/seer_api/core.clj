(ns seer-api.core
  (:require [org.httpkit.server :as http])
  (:require [clojure.tools.logging :as log])
  (:require [compojure.core :refer :all]
            [compojure.route :as route])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]])
  (:require [seer-api.utils :as ut]
            [seer-api.db :as db]
            [seer-api.elaborations :as ela]))

(defonce conn-and-conf (atom nil))

;;
;; REST interface
;;
;; POST v1/forecasts  --> :job-id
;;   - job creation
;;   - data validation (TODO: verify via SeerDataCruncher)
;;   - ETA calculation
;;   - processing time series
;;   - store status for every step
;;   - store data forecasted
;; GET  /forecasts/:job-id --> current status with ETA
;; GET  /forecasts/:job-id/result --> return the forecasted values
;; GET  /forecasts/:job-id/elaboration-details --> return also the full timeseries analyzed
;;


(defroutes
  site
  (context "/v1" []

    (POST "/forecasts" {body :body :as r}
      (let [job-id (.toString (java.util.UUID/randomUUID))
            {seer-path :path seer-core :core} (get-in @conn-and-conf [:config :seer])
            collection (get-in @conn-and-conf [:config :db :collection])
            input-file (or (get-in r [:params "file" :tempfile]) body)]

        (db/store-job-status job-id (:db @conn-and-conf) collection)
        (ut/copy-input-to-location job-id input-file seer-path)
        (ela/start-background-processing job-id (:db @conn-and-conf) collection seer-path seer-core)
        {:status 202
         :body   {:job-id job-id}}
        ))

    (GET "/forecasts/:job-id" [job-id]
      (let [data (db/find-status-by-job-id job-id (:db @conn-and-conf)
                                           (get-in @conn-and-conf [:config :db :collection]))]
        (if data
          {:status  200
           :headers {"Content-type" "application/json"}
           :body    data}
          {:status 404
           :body   "the job-id does not exist!\n"})))

    (GET "/forecasts/:job-id/result" [job-id]
      (let [data (db/find-forecast-by-job-id job-id (:db @conn-and-conf)
                                             (get-in @conn-and-conf [:config :db :collection]))]
        (if data
          {:status  200
           :headers {"Content-type" "application/json"}
           :body    data}
          {:status 404
           :body   "the job-id does not exist!\n"})))

    (GET "/forecasts/:job-id/elaboration-details" [job-id]
      (let [data (db/find-elaboration-by-job-id job-id (:db @conn-and-conf)
                                                (get-in @conn-and-conf [:config :db :collection]))]
        (if data
          {:status  200
           :headers {"Content-type" "application/json"}
           :body    data}
          {:status 404
           :body   "the job-id does not exist!\n"})))


    (GET "health-check" []
      {:status 200 :body "ok"})
    )
  (route/not-found "Page not found\n"))

(comment
  (def conn-and-conf-aus (atom {:config                  {:db {:host "localhost", :db-name "seer-api", :collection "elaborations"}}
                                :seer                    {:path "foo/path" :core "foo/core"}
                                :local-access-limitation false}))

  (get-in @conn-and-conf-aus [:config :seer])
  (get-in @conn-and-conf-aus [:config :db :collection])
  (get-in @conn-and-conf [:config :local-access-limitation])
  )


(defn catch-all [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception x
        (ut/error-manager x "The operation couldn't be completed due to an internal error.")))))


(defn print-request [handler]
  (fn [req]
    (clojure.pprint/pprint req)
    (handler req)))


(def app
  (-> site
      ;;print-request
      catch-all
      wrap-json-response
      (wrap-json-body {:keywords? true})
      wrap-params
      wrap-multipart-params
      ))


(defn start-server [{:keys [port]}]
  (log/info "Starting server on port:" port)
  (http/run-server #'app {:port port}))
