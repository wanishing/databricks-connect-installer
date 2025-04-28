(ns wanishing.databricks-connect-installer.utils.databricks
  (:require
    [babashka.curl :as curl]
    [babashka.fs :as fs]
    [babashka.process :refer [process]]
    [cheshire.core :as json]
    [clojure.string :as str]
    [wanishing.databricks-connect-installer.utils.shell :refer [run-command]]
    [wanishing.databricks-connect-installer.utils.ui :refer [success failure]]))


(defn profile-name
  [line]
  (when (str/starts-with? line "[")
    (-> line
        (str/replace #"^\[" "")
        (str/replace #"\]$" "")
        str/trim)))


(defn prompt-host
  []
  (print "\nEnter your Databricks workspace URL (e.g., https://your-workspace.cloud.databricks.com): ")
  (flush)
  (read-line))


(defn configure-databricks-auth
  [host]
  (println (str "\nRunning: databricks auth login --configure-cluster --host" host))
  (println "\nPlease follow the prompts to complete the Databricks authentication:")
  (println "1. Enter a name for your connection profile")
  (println "2. Select a cluster to use with Databricks Connect")
  (println "3. Complete the browser-based authentication if required\n")
  (let [{:keys [exit]} (-> (process ["databricks" "auth" "login" "--configure-cluster" "--host" host]
                                    {:inherit true
                                     :in :inherit
                                     :out :inherit
                                     :err :inherit})
                           deref)]
    (if (= 0 exit)
      (println (str "\n" (success "Successfully configured Databricks authentication")))
      (do
        (println (str "\n" (failure "Failed to configure Databricks authentication")))
        (System/exit 1)))))


(defn ensure-databricks-config
  []
  (let [config-path (str (System/getProperty "user.home") "/.databrickscfg")]
    (when-not (fs/exists? config-path)
      (println (str "\n" (failure "Databricks configuration not found at ~/.databrickscfg")))
      (println "Let's set up your Databricks authentication first.")
      (let [host (prompt-host)]
        (configure-databricks-auth host)))))


(defn databricks-config
  []
  (let [config-path (str (System/getProperty "user.home") "/.databrickscfg")]
    (when (fs/exists? config-path)
      (let [lines (->> (str/split-lines (slurp config-path))
                       (remove #(str/starts-with? % ";"))  ; Remove comment lines
                       (remove str/blank?))                ; Remove blank lines
            profiles (reduce (fn [acc line]
                               (if-let [profile-name (profile-name line)]
                                 (assoc acc :current-profile profile-name)
                                 (if (:current-profile acc)
                                   (if (str/includes? line "=")
                                     (let [[k v] (str/split line #"\s*=\s*")]
                                       (update-in acc [:profiles (:current-profile acc)]
                                                  assoc (keyword (str/trim k)) (str/trim v)))
                                     acc)
                                   acc)))
                             {:current-profile nil
                              :profiles {}}
                             lines)]
        (:profiles profiles)))))


(defn parse-profile-line
  [line]
  (let [[name host valid] (->> (str/split line #"\s+")
                               (remove empty?))]
    {:name name
     :host host
     :is-valid (= "YES" valid)}))


(defn available-profiles
  []
  (let [{:keys [success? result]} (run-command ["databricks" "auth" "profiles"])]
    (when success?
      (->> (str/split-lines (:out result))
           (drop 1)  ; Skip header line
           (remove empty?)
           (map parse-profile-line)))))


(defn prompt-profile-selection
  []
  (let [profiles (available-profiles)
        valid-profiles (filter :is-valid profiles)]
    (if (seq valid-profiles)
      (do
        (println "\nAvailable Databricks profiles:")
        (doseq [{:keys [name host is-valid]} profiles]
          (println (format "- %-20s %-50s %s"
                           name
                           host
                           (if is-valid
                             (success "Valid")
                             (failure "Invalid")))))
        (print "\nSelect a profile: ")
        (flush)
        (let [selected (read-line)
              selected-profile (first (filter #(= selected (:name %)) valid-profiles))]
          (if selected-profile
            (:name selected-profile)
            (do
              (println (str "\n" (failure "Invalid profile selected. Please choose from the available valid profiles.")))
              (System/exit 1)))))
      (do
        (println "\nNo valid Databricks profiles found. Let's set up a new one.")
        (let [host (prompt-host)]
          (configure-databricks-auth host)
          (recur))))))  ; Recursively call prompt-profile-selection to show newly created profile


(defn cluster-id
  [profile]
  (let [config (databricks-config)]
    (when (and config profile)
      (get-in config [profile :cluster_id]))))


(defn auth-token
  [profile]
  (let [{:keys [success? result]} (run-command ["databricks" "auth" "token" "--profile" profile])]
    (when success?
      (-> result
          :out
          (json/parse-string true)))))


(defn cluster-info
  [{:keys [host token]} cluster-id]
  (let [response (curl/get (str host "/api/2.1/clusters/get?cluster_id=" cluster-id)
                           {:headers {"Authorization" (str "Bearer " token)}})]
    (when (= 200 (:status response))
      (json/parse-string (:body response) true))))


(defn dbr-version
  [profile]
  (let [cluster-id (cluster-id profile)
        {:keys [access_token]} (auth-token profile)
        config (databricks-config)
        host (get-in config [profile :host])
        {:keys [spark_version]} (cluster-info {:host host :token access_token} cluster-id)]
    (println (success (str "Found remote DBR version:" spark_version)))
    spark_version))


;; Profile and Config Management
