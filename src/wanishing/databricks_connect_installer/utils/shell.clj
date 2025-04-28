(ns wanishing.databricks-connect-installer.utils.shell
  (:require
    [babashka.process :refer [process]]
    [clojure.string :as str]
    [wanishing.databricks-connect-installer.utils.ui :refer [failure]]))


(def verbose? false)


(defn run-command
  "Executes a command and returns a map with :success? and :result.
   Args can be either a vector of command and args, or a function that returns a process.
   :result will contain the process map on success or the exception on failure."
  [& args]
  (when verbose?
    (println (str/join " " (if (vector? (first args)) (first args) ["custom process"]))))
  (try
    (let [result (if (vector? (first args))
                   @(process (first args) {:out :string})
                   @((first args)))]
      {:success? (= 0 (:exit result))
       :result result})
    (catch Exception e
      {:success? false
       :result e})))


(defn fail
  "Print failure message and exit with status 1"
  [msg]
  (println (failure msg))
  (System/exit 1))
