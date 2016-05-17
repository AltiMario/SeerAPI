(ns seer-api.utils
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn error-manager [x message]
  (let [error-id (str "ERR-" (.toString (java.util.UUID/randomUUID)))]
    (log/error x (str "Exception number: " error-id))
    {:status   "ERROR"
     :message  message
     :reason   (.getMessage x)
     :error-id error-id}))


(defn validate-csv [job-id base-path]
  (doall
    (->> (str base-path "/" job-id "/temp.csv")
         slurp
         (re-seq #"[^,\n\r]+")
         (map (fn [^String s] (Long/parseLong s))))))


(defn copy-input-to-location [job-id input base-path]
  (try
    (let [file-path (str base-path "/" job-id)
          file-name (str file-path "/temp.csv")]
      (io/make-parents file-name)
      (io/copy input (io/file file-name))
      file-name)
    (catch Exception e
      (throw
        (ex-info "Error copying input data"
                 (error-manager e (str "job-id:" job-id " to-path" base-path)))))))

