(ns seer-api.core
  (:require [org.httpkit.server :as http])
  (:require [seer-api.elaborations :as ela])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [seer-api.utils :as ut]
            [seer-api.db :as db])
  (:require [clojure.tools.logging :as log])
  )

(defonce conn-and-conf (atom nil))

;;
;; REST interface
;;
;; POST v1/forecasts  --> :job-id
;;   - job creation
;;   - data validation (TODO: verify via SeerDataCruncher)
;;   - processing eta
;;   - processing
;;   - store final status to DB
;; GET  /forecasts/:job-id --> current status with eta
;; GET  /forecasts/:job-id/result --> output file (TODO: fare check se completo o meno)
;;


(defroutes
  site
  (context "/v1" []

    (POST "/forecasts" {body :body}
      (let [job-id (.toString (java.util.UUID/randomUUID))
            {seer-path :path seer-core :core} (get-in @conn-and-conf [:config :seer])]
        (log/info (db/store-job-status job-id (:db @conn-and-conf)))
        (ut/copy-input-to-location job-id (:db @conn-and-conf) body seer-path)
        (ela/start-background-processing job-id (:db @conn-and-conf) seer-path seer-core)
        {:status 202
         :body   {:job-id job-id}}))

    (GET "/forecasts/:job-id" [job-id]
      (let [data (db/find-user-by-job-id job-id (:db @conn-and-conf))]
        (if data
          {:status  200
           :headers {"Content-type" "application/json"}
           :body    data}
          {:status 404
           :body   "the job-id does not exist!\n"})))

    (GET "/forecasts/:job-id/result" [job-id]

      ))

  (route/not-found "Page not found\n"))

(comment
  (def conn-and-conf (atom {:config {:seer {:path "ciccio" :core "dd"}}}))
  (-> @conn-and-conf
      :config
      :seer
      :path)

  (get-in @conn-and-conf [:config :seer])
  (get-in @conn-and-conf [:db])
  )


(def app
  (-> site
      wrap-json-response
      (wrap-json-body {:keywords? true})
      ))


(defn start-server [{:keys [port]}]
  (println "Starting server on port:" port)
  (http/run-server #'app {:port port}))
