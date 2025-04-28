(ns wanishing.databricks-connect-installer.commands.connect
  (:require
    [babashka.fs :as fs]
    [wanishing.databricks-connect-installer.commands.connect.python :as python]
    [wanishing.databricks-connect-installer.commands.connect.scala :as scala]
    [wanishing.databricks-connect-installer.utils.databricks :as dbx]
    [wanishing.databricks-connect-installer.utils.shell :refer [fail]]))


(defn detect-project-type
  []
  (let [has-build-sbt? (fs/exists? "build.sbt")
        has-pyproject? (fs/exists? "pyproject.toml")
        has-requirements? (fs/exists? "requirements.txt")]
    (cond
      has-build-sbt? :scala
      (or has-pyproject? has-requirements?) :python
      :else (fail "No supported project configuration found (build.sbt, pyproject.toml, or requirements.txt)"))))


(defn connect-runtime
  []
  (dbx/ensure-databricks-config)
  (Thread/sleep 1000)
  (if-let [profile (dbx/prompt-profile-selection)]
    (do
      (println "\nChecking version compatibility...")
      (if-let [dbr-version (dbx/dbr-version profile)]
        (let [project-type (detect-project-type)]
          (case project-type
            :scala (scala/setup-connect profile dbr-version)
            :python (python/setup-connect dbr-version profile)))
        (fail "Failed to get DBR version. Please make sure you're logged in and have access to clusters.")))
    (fail "No valid profiles found. Please run the connect command again after configuring a profile.")))
