package com.meshlink.securitytest

/**
 * Generates a structured security coverage report summarizing the outcome
 * of each major protection mechanism after every test run.
 */
object SecurityCoverageReport {
    
    private val coverageStatus = mutableMapOf<String, Boolean>()

    fun markCovered(mechanism: String, success: Boolean = true) {
        coverageStatus[mechanism] = success
    }

    fun generateReport(): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("      SECURITY COVERAGE REPORT\n")
        sb.append("=========================================\n\n")

        if (coverageStatus.isEmpty()) {
            sb.append("No security mechanisms tested.\n")
        } else {
            coverageStatus.forEach { (mechanism, status) ->
                val statusStr = if (status) "[PASS]" else "[FAIL]"
                sb.append("$statusStr $mechanism\n")
            }
        }
        
        sb.append("\n=========================================\n")
        return sb.toString()
    }
    
    fun clear() {
        coverageStatus.clear()
    }
}
