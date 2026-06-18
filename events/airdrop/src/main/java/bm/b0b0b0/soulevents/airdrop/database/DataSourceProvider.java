package bm.b0b0b0.soulevents.airdrop.database;

import javax.sql.DataSource;

public final class DataSourceProvider {

    private volatile DataSource dataSource;

    public DataSource get() {
        return dataSource;
    }

    public void assign(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void shutdown() {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) {
            hikari.close();
        }
        dataSource = null;
    }
}
