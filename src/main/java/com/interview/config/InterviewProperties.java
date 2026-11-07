package com.interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "interview")
public class InterviewProperties {

    /**
     * JSON file that stores questions. Relative paths resolve from the working directory.
     */
    private String dataFile = "./data/questions.json";

    /**
     * Shared key required by add, edit, and delete requests.
     */
    private String adminKey = "local-admin";

    public String getDataFile() {
        return dataFile;
    }

    public void setDataFile(String dataFile) {
        this.dataFile = dataFile;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }

}
