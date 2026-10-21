package br.ufrn.dimap.siglab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal de inicialização da aplicação SIGLab.
 * <p>
 * O SIGLab é um sistema para gerenciamento de laboratórios e grupos de pesquisa
 * acadêmicos desenvolvido para a UFRN.
 * </p>
 *
 * @author Equipe SIGLab
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class SiglabApplication {

    /**
     * Ponto de entrada executável da aplicação Spring Boot.
     *
     * @param args argumentos da linha de comando
     */
    public static void main(String[] args) {
        SpringApplication.run(SiglabApplication.class, args);
    }
}
