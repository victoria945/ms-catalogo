package org.gestionpastelera.ms_catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableCaching
public class MsCatalogoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCatalogoApplication.class, args);
    }
}
