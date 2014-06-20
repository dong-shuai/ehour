package net.rrm.ehour.persistence.appconfig;

import net.rrm.ehour.appconfig.EhourHomeUtil;
import net.rrm.ehour.domain.DomainObjects;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Configuration
public class HibernateConfiguration {
    @Autowired
    private DataSource dataSource;

    @Value("${ehour.database}")
    private String databaseName;

    @Value("${ehour.db.cache:true}")
    private String caching;

    private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private static final Logger LOGGER = Logger.getLogger(HibernateConfiguration.class);

    @Bean(name = "sessionFactory")
    public SessionFactory getSessionFactory() throws Exception {
        setDefaultCachingType();

        validateCachingAttribute();

        Properties configProperties = EhourHomeUtil.loadDatabaseProperties(databaseName);

        LOGGER.info("Using database type: " + databaseName);

        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        List<Resource> resources = getMappingResources(configProperties);
        factoryBean.setMappingLocations(resources.toArray(new Resource[resources.size()]));
        factoryBean.setAnnotatedClasses(DomainObjects.DOMAIN_OBJECTS);

        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect", configProperties.get("hibernate.dialect"));
        hibernateProperties.put("show_sql", "false");
        hibernateProperties.put("use_outer_join", "true");
        hibernateProperties.put("hibernate.cache.region.factory_class", "net.sf.ehcache.hibernate.EhCacheRegionFactory");
        hibernateProperties.put("hibernate.cache.use_second_level_cache", caching);
        hibernateProperties.put("net.sf.ehcache.configurationResourceName", "hibernate-ehcache.xml");
        hibernateProperties.put("hibernate.cache.use_query_cache", caching);
        hibernateProperties.put("hibernate.hbm2ddl.auto", configProperties.get("hibernate.hbm2ddl.auto"));

        factoryBean.setHibernateProperties(hibernateProperties);
        factoryBean.afterPropertiesSet();

        return factoryBean.getObject();
    }

    private void validateCachingAttribute() {
        if (!caching.equalsIgnoreCase("true") && !caching.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("ehour.db.cache property must either be true or false");
        }
    }

    private void setDefaultCachingType() {
        if (caching == null) {
            caching = "true";
        }
    }

    private List<Resource> getMappingResources(Properties configProperties) throws IOException {
        List<Resource> resources = new ArrayList<Resource>();

        Resource[] queryResources = resolver.getResources("classpath:query/common/*.hbm.xml");
        resources.addAll(Arrays.asList(queryResources));

        ClassPathResource dbQueryResource = new ClassPathResource("query/" + configProperties.getProperty("reportquery.filename"));
        resources.add(dbQueryResource);
        return resources;
    }

    @Bean(name = "transactionManager")
    public HibernateTransactionManager getTransactionManager() throws Exception {
        return new HibernateTransactionManager(getSessionFactory());
    }

    @Bean
    public JdbcTemplate getJdbcTemplate() throws Exception {
        return new JdbcTemplate(dataSource);
    }

    void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
