(ns wanishing.databricks-connect-installer.core
  (:require
    [babashka.cli :as cli]
    [wanishing.databricks-connect-installer.commands.connect :refer [connect-runtime]]
    [wanishing.databricks-connect-installer.commands.setup :refer [setup-env]]
    [wanishing.databricks-connect-installer.utils.ui :refer [display-help]]))


(def version "1.0.0")


(def commands
  {:setup {:fn setup-env
           :desc "Setup the environment (brew, databricks cli, java)"}
   :connect {:fn connect-runtime
             :desc "Connect with selected profile and get connection details"}})


(defn setup
  [_]
  (setup-env))


(defn connect
  [_]
  (connect-runtime))


(defn help
  [opts]
  (display-help version commands)
  (assoc opts :fn :help))


(def cli-table
  [{:cmds ["setup"]    :fn setup   :desc "Setup the environment (brew, databricks cli, java)"}
   {:cmds ["connect"]  :fn connect :desc "Connect with selected profile and get connection details"}
   {:cmds []          :fn help}])


(defn -main
  [& args]
  (cli/dispatch cli-table args))


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
