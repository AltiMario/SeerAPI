(defproject seer-api "0.1.0-SNAPSHOT"
  :description "SEE-R API"
  :url "http://see-r.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main seer-api.main
  :plugins [[lein-cloverage "1.0.7-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.novemberain/monger "3.0.2"]
                 [http-kit "2.1.18"]
                 [compojure "1.5.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 ]
  :profiles {:uberjar {:aot :all :uberjar-name "seer-api-standalone.jar"}
             :dev     {:plugins [[lein-binplus "0.4.1"]]}}
  :bin {:name "seer-api"}
  )
