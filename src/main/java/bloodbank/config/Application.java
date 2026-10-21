package bloodbank.config;
/**S — Single Responsibility**/

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

    @SpringBootApplication(scanBasePackages = "bloodbank")
    // сущности лежат в bloodbank.persistence, а Application в bloodbank.config,
    // поэтому говорим JPA, где искать @Entity
    @EntityScan("bloodbank.persistence")
    public class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }

