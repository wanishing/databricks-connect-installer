(ns wanishing.databricks-connect-installer.commands.connect.scala
  (:require
    [clojure.string :as str]
    [wanishing.databricks-connect-installer.utils.shell :refer [run-command fail]]
    [wanishing.databricks-connect-installer.utils.ui :refer [success]]
    [wanishing.databricks-connect-installer.utils.version :as v]))


(defn extract-scala-version
  "Extract Scala version from DBR version string"
  [dbr-version]
  (when-let [matches (re-find #"scala(\d+\.\d+)" dbr-version)]
    (let [version-str (second matches)]
      (v/parse-version version-str))))


(comment
  (extract-scala-version "13.3.x-scala2.12")  ;=> {:major 2, :minor 12, :patch 0}
  (extract-scala-version "14.0-scala2.12")    ;=> {:major 2, :minor 12, :patch 0}
)


(defn current-scala-version
  "Get local Scala version from sbt"
  []
  (let [{:keys [success? result]} (run-command ["sbt" "show scalaVersion"])]
    (when success?
      (when-let [version-line (->> (str/split-lines (:out result))
                                   (filter #(re-matches #"\[info\] \d+\.\d+\.\d+" %))
                                   first)]
        (let [version (last (str/split version-line #" "))]
          (when-let [[_ major minor] (re-matches #"(\d+)\.(\d+)\.\d+" version)]
            {:major (parse-long major)
             :minor (parse-long minor)}))))))


(comment
  ;; Example output depends on your local sbt project
  (current-scala-version)  ;=> {:major 2, :minor 12}
)


(defn verify-scala-version
  "Verify if local Scala version matches DBR's Scala version"
  [dbr-version]
  (when-let [dbr-scala-version (extract-scala-version dbr-version)]
    (if-let [local-scala-version (current-scala-version)]
      (let [versions-match? (= (select-keys dbr-scala-version [:major :minor])
                               local-scala-version)]
        (if versions-match?
          (println (success (str "Local Scala version " (v/version->str local-scala-version)
                                 " matches DBR Scala version")))
          (fail (str "Scala version mismatch! Local version: " (v/version->str local-scala-version)
                     ", Required version: " (v/version->str dbr-scala-version)))))
      (fail (str "Scala not found! Required version: " (v/version->str dbr-scala-version))))))


(comment
  ;; Verify Scala version (output depends on local setup)
  (verify-scala-version "13.3.x-scala2.12")  ;=> nil (success) or throws exception

  ;; Example error handling
  (try
    (verify-scala-version "13.3.x-scala2.13")
    (catch Exception e
      (ex-data e)))  ;=> {:local "2.12", :required "2.13"}
)


(defn get-compatible-connect-version
  "Get compatible Databricks Connect version for given DBR version"
  [dbr-version]
  (let [version (v/parse-version dbr-version)]
    (v/version->str (assoc version :patch 1))))


(comment
  (get-compatible-connect-version "13.3.0")  ;=> "13.3.1"
  (get-compatible-connect-version "14.0")    ;=> "14.0.1"
)


(defn display-example-code
  [profile]
  (println "\nExample Main.scala file:")
  (println)
  (println "package org.example.application")
  (println)
  (println "import com.databricks.connect.DatabricksSession")
  (println "import com.databricks.sdk.core.DatabricksConfig")
  (println "import org.apache.spark.sql.SparkSession")
  (println)
  (println "object Main {")
  (println "  def main(args: Array[String]): Unit = {")
  (println (str "    val config = new DatabricksConfig().setProfile(\"" profile "\")"))
  (println "    val spark = DatabricksSession.builder().sdkConfig(config).getOrCreate()")
  (println "    val df = spark.read.table(\"samples.nyctaxi.trips\")")
  (println "    df.limit(5).show()")
  (println "  }")
  (println "}"))


(defn setup-connect
  "Setup Databricks Connect for Scala project"
  [profile dbr-version]
  (verify-scala-version dbr-version)
  (let [connect-version (get-compatible-connect-version dbr-version)]
    (println (success "Remote DBR version:" dbr-version))
    (println (success "Required Databricks Connect version:" connect-version))
    (println "\nAdd this to your build.sbt:")
    (println (str "libraryDependencies += \"com.databricks\" % \"databricks-connect\" % \"" connect-version "\""))
    (display-example-code profile)))
