package com.example.localdelivery.cachewithreplicas;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.datasource")
public class DualDataSourceProperties {

    private DbProperties primary = new DbProperties();
    private DbProperties replica = new DbProperties();

    public DbProperties getPrimary() {
        return primary;
    }

    public void setPrimary(DbProperties primary) {
        this.primary = primary;
    }

    public DbProperties getReplica() {
        return replica;
    }

    public void setReplica(DbProperties replica) {
        this.replica = replica;
    }

    public static class DbProperties {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
