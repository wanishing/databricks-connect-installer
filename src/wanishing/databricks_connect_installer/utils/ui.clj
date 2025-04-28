(ns wanishing.databricks-connect-installer.utils.ui
  (:require
    [clojure.string :as str]))


(def style-header "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
(def style-section "────────────────────────────────────────────────────────────────────────────────────")

(def green "\u001B[32m")
(def red "\u001B[31m")
(def reset "\u001B[0m")


(defn success
  [& text]
  (str green "✓" reset " " (str/join " " text)))


(defn failure
  [text]
  (str red "✗" reset " " text))


(defn print-styled-header
  [text]
  (println)
  (println style-header)
  (println text)
  (println style-header))


(defn print-section
  [title]
  (println)
  (println style-section)
  (println title)
  (println style-section))


(defn display-help
  [version commands]
  (print-styled-header (str "🔌 Databricks Connect Setup Tool v" version))

  (println "\nThis tool simplifies the setup and configuration of Databricks Connect for your Scala and Python projects.")
  (println "It automates environment setup, profile selection, and provides the correct dependency versions")
  (println "based on your Databricks Runtime (DBR) version.")

  (print-section "📋 Quick Start")
  (println "1. Run setup:    ./databricks-connect-installer setup")
  (println "2. Run connect:  ./databricks-connect-installer connect")

  (print-section "🔄 Workflow Steps")
  (println "Step 1: Environment Setup (setup command)")
  (println "  ├─ Verifies and installs Homebrew if needed")
  (println "  ├─ Installs Databricks CLI through Homebrew")
  (println "  └─ Checks Java installation (required for Databricks Connect)")
  (println)
  (println "Step 2: Connect Configuration (connect command)")
  (println "  ├─ Lists available Databricks profiles")
  (println "  ├─ Lets you select your desired profile")
  (println "  ├─ Detects your cluster's DBR version")
  (println "  ├─ Auto-detects project type (Scala/Python)")
  (println "  │")
  (println "  ├─ For Scala Projects:")
  (println "  │  ├─ Validates local Scala version matches DBR's Scala version")
  (println "  │  ├─ Provides compatible Databricks Connect version")
  (println "  │  └─ Generates example code with your profile")
  (println "  │")
  (println "  └─ For Python Projects:")
  (println "     ├─ Verifies Python virtual environment")
  (println "     ├─ Validates Python version compatibility")
  (println "     ├─ Manages PySpark installation")
  (println "     └─ Installs compatible Databricks Connect version")

  (print-section "💡 Available Commands")
  (doseq [[cmd {:keys [desc]}] commands]
    (println (format "  %-10s %s" (name cmd) desc)))

  (print-section "🛠️  Usage")
  (println "  databricks-connect-installer <command> [options]")

  (print-section "⚙️  Options")
  (println "  -h, --help    Show this help message")

  (comment
    (print-section "📝 Notes")
    (println "• Your Scala version must match the one used in your DBR (e.g., Scala 2.12 for DBR 7.x)")
    (println "• For Python projects, ensure you have an active virtual environment")
    (println "• Python version compatibility follows the Databricks Connect matrix:")
    (println "  - DBR 16.1+ (Serverless): Python 3.12")
    (println "  - DBR 16.0+ (Cluster): Python 3.12")
    (println "  - DBR 15.0-15.4 LTS (Cluster): Python 3.11")
    (println "  - DBR 13.3-14.3 LTS (Cluster): Python 3.10")))
