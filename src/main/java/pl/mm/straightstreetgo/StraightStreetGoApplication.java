package pl.mm.straightstreetgo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StraightStreetGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(StraightStreetGoApplication.class, args);
    }

}
