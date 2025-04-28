(ns wanishing.databricks-connect-installer.commands.setup
  (:require
    [babashka.process :refer [process check]]
    [wanishing.databricks-connect-installer.utils.shell :refer [run-command fail]]
    [wanishing.databricks-connect-installer.utils.ui :refer [success]]))


(defn check-brew-installed
  []
  (let [{:keys [success?]} (run-command ["brew" "--version"])]
    success?))


(defn check-databricks-cli
  []
  (let [{:keys [success?]} (run-command ["databricks" "--version"])]
    success?))


(defn install-databricks-cli
  []
  (println "\nRunning: brew tap databricks/tap")
  (let [_ (-> (process ["brew" "tap" "databricks/tap"]
                       {:inherit true})
              check)]
    (println "\nRunning: brew install databricks")
    (-> (process ["brew" "install" "databricks"]
                 {:inherit true})
        check)
    true))


(defn check-java-installed
  []
  (let [{:keys [success?]} (run-command ["java" "-version"])]
    success?))


(defn setup-env
  []
  (println "Setting up environment...")

  (println "\nChecking Homebrew installation...")
  (if (check-brew-installed)
    (println (success "Homebrew is installed"))
    (fail "Homebrew is not installed. Please install Homebrew first."))

  (println "\nChecking Databricks CLI installation...")
  (if (check-databricks-cli)
    (println (success "Databricks CLI is already installed"))
    (do
      (println "Installing Databricks CLI via Homebrew...")
      (if (install-databricks-cli)
        (println (success "Successfully installed Databricks CLI"))
        (fail "Failed to install Databricks CLI"))))

  (println "\nChecking Java installation...")
  (if (check-java-installed)
    (println (success "Java is installed"))
    (fail "Java is not installed. Please install Java Development Kit (JDK) that matches your Databricks cluster's JDK version."))

  (println (str "\n" (success "All environment setup completed!"))))
