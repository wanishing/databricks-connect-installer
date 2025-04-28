(ns wanishing.databricks-connect-installer.commands.connect.python
  (:require
    [babashka.fs :as fs]
    [babashka.process :refer [process check]]
    [clojure.string :as str]
    [wanishing.databricks-connect-installer.utils.shell :refer [run-command fail]]
    [wanishing.databricks-connect-installer.utils.ui :refer [success failure]]
    [wanishing.databricks-connect-installer.utils.version :as v]))


(def version-matrix
  [{:min-version (v/parse-version "16.1")
    :compute "Serverless"
    :python-version (v/parse-version "3.12")}
   {:min-version (v/parse-version "16.0")
    :compute "Cluster"
    :python-version (v/parse-version "3.12")}
   {:min-version (v/parse-version "15.0")
    :max-version (v/parse-version "15.4")
    :compute "Cluster"
    :python-version (v/parse-version "3.11")}
   {:min-version (v/parse-version "13.3")
    :max-version (v/parse-version "14.3")
    :compute "Cluster"
    :python-version (v/parse-version "3.10")}])


(defn- current-python-version
  []
  (let [{:keys [success? result]} (run-command ["python3" "--version"])]
    (when success?
      (when-let [matches (re-find #"Python (\d+\.\d+\.\d+)" (:out result))]
        (second matches)))))


(comment
  ;; Get current Python version (output depends on your system)
  (current-python-version)  ;=> "3.12.0"
)


(defn- get-compatible-python-version
  "Get compatible Python version for given DBR version"
  [dbr-version]
  (let [version (v/parse-version dbr-version)]
    (->> version-matrix
         (filter #(v/version-in-range?
                    version
                    (:min-version %)
                    (:max-version %)))
         first
         :python-version
         v/version->str)))


(comment
  ;; Find compatible Python version for different DBR versions
  (get-compatible-python-version "16.1.0")  ;=> "3.12"
  (get-compatible-python-version "15.3")    ;=> "3.11"
  (get-compatible-python-version "14.0")    ;=> "3.10"
)


(defn verify-python-version
  "Verify if local Python version is compatible with DBR version"
  [local-version dbr-version]
  (println "--verify---")
  (println local-version)
  (println dbr-version)
  (println "------")
  (let [required-version (get-compatible-python-version dbr-version)
        local-v (v/parse-version local-version)
        required-v (v/parse-version required-version)]
    (when-not (and (= (:major local-v) (:major required-v))
                   (= (:minor local-v) (:minor required-v)))
      (fail (str "Python version mismatch! Local version: " local-version ", Required version: " required-version)))))


(comment
  ;; Verify Python version compatibility
  (verify-python-version "3.12.0" "16.1.0")
  (verify-python-version "3.12.0" "16.1.0") ;=> nil (success)
  (try
    (verify-python-version "3.11.0" "16.1.0")
    (catch Exception e
      (ex-data e))) ;=> {:local "3.11.0", :required "3.12"}
  )


(defn- has-poetry?
  []
  (let [{:keys [success?]} (run-command ["poetry" "--version"])]
    success?))


(defn- get-poetry-virtualenv
  []
  (when (has-poetry?)
    (let [{:keys [success? result]} (run-command ["poetry" "env" "info" "--path"])]
      (when success?
        (str/trim (:out result))))))


(defn- get-pip-virtualenv
  []
  (let [venv-path (System/getenv "VIRTUAL_ENV")]
    (when (and venv-path (fs/exists? venv-path))
      venv-path)))


(defn- display-venv-activation-instructions
  [venv-path]
  (println "\nTo activate the virtual environment, run:")
  (println (str "  source " venv-path "/bin/activate")))


(defn- check-virtualenv
  []
  (let [poetry-venv (get-poetry-virtualenv)
        pip-venv (get-pip-virtualenv)]
    (cond
      ;; Poetry environment exists but not activated
      (and poetry-venv (not pip-venv))
      (do
        (println (failure "Poetry virtual environment exists but is not activated"))
        (display-venv-activation-instructions poetry-venv)
        (println)
        (fail "Please activate the Poetry virtual environment"))

      ;; Poetry environment is active
      (and poetry-venv pip-venv (= poetry-venv pip-venv))
      (println (success "Poetry virtual environment is active:" poetry-venv))

      ;; Pip virtual environment is active
      pip-venv
      (println (success "Virtual environment is active:" pip-venv))

      ;; No virtual environment found
      :else
      (do
        (println (failure "No active Python virtual environment found"))
        (println "\nPlease set up and activate a virtual environment:")
        (println "\nFor Poetry projects:")
        (println "  1. Run: poetry install")
        (println "  2. Run: poetry shell")
        (println "\nFor pip-based projects:")
        (println "  1. Create: python3 -m venv .venv")
        (println "  2. Activate: source .venv/bin/activate")
        (fail "Virtual environment setup required")))))


(defn- uninstall-pyspark
  []
  (if (has-poetry?)
    (do
      (println "\nChecking for PySpark in Poetry...")
      (let [{:keys [success?]} (run-command ["poetry" "show" "pyspark"])]
        (when success?
          (println "Removing PySpark via Poetry...")
          (-> (process ["poetry" "remove" "pyspark"] {:inherit true})
              deref))))
    (do
      (println "\nChecking for PySpark in pip...")
      (let [{:keys [success?]} (run-command ["pip3" "show" "pyspark"])]
        (when success?
          (println "Removing PySpark via pip...")
          (-> (process ["pip3" "uninstall" "-y" "pyspark"] {:inherit true})
              deref))))))


(defn- extract-version-numbers
  [version-string]
  (when-let [matches (re-find #"(\d+\.\d+)" version-string)]
    (second matches)))


(defn- install-databricks-connect
  [version]
  (let [version-base (extract-version-numbers version)]
    (if (has-poetry?)
      (do
        (println (str "\nInstalling databricks-connect " version-base ".* via Poetry..."))
        (-> (process ["poetry" "add" (str "databricks-connect@^" version-base)] {:inherit true})
            check
            deref))
      (do
        (println (str "\nInstalling databricks-connect " version-base ".* via pip..."))
        (-> (process ["pip3" "install" "--upgrade" (str "databricks-connect==" version-base ".*")] {:inherit true})
            check
            deref)))))


(defn- display-example-code
  [profile]
  (println "\nExample Python code:")
  (println)
  (println "from databricks.connect import DatabricksSession")
  (println)
  (println (str "spark = DatabricksSession.builder.profile(\"" profile "\").getOrCreate()"))
  (println)
  (println "df = spark.read.table(\"samples.nyctaxi.trips\")")
  (println "df.show(5)"))


(defn setup-connect
  [dbr-version profile]
  (check-virtualenv)
  (verify-python-version (current-python-version) (extract-version-numbers dbr-version))
  (uninstall-pyspark)
  (install-databricks-connect dbr-version)
  (display-example-code profile))
