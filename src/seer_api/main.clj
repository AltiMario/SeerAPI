(ns seer-api.main
  (:require [seer-api.core :as core]
            [clojure.edn :as edn]
            [seer-api.db :as db])
  (:gen-class))


(def default-config
  {:db     {:host "localhost" :db-name "seer-api" :collection "elaborations"}
   :seer   {:path "/home/altimario/seer/temp/"
            :core "/home/altimario/seer/seerCore"}
   :server {:port 9090}})


(defn init-system! [config]
  (let [conns (merge (db/start-connection (:db config))
                     {:server (core/start-server (:server config))}
                     {:config config})]
    (reset! core/conn-and-conf conns)))


(defn -main [config-file]
  (let [config (merge-with merge default-config
                           (edn/read-string (slurp config-file)))]
    (init-system! config)
    (println "seer-api started at: " (get-in config [:server :port]))))


(comment
  (init-system! default-config)
  )
